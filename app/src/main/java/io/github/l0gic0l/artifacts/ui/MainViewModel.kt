package io.github.l0gic0l.artifacts.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.l0gic0l.artifacts.data.Artifact
import io.github.l0gic0l.artifacts.data.GitHubApi
import io.github.l0gic0l.artifacts.data.GitHubException
import io.github.l0gic0l.artifacts.data.InstallerChoice
import io.github.l0gic0l.artifacts.data.Repo
import io.github.l0gic0l.artifacts.data.SettingsStore
import io.github.l0gic0l.artifacts.install.Installer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface DownloadState {
    data class Progress(val read: Long, val total: Long) : DownloadState
    data class Ready(val files: List<File>) : DownloadState
    data class Failed(val message: String) : DownloadState
}

data class UiState(
    val loadingSettings: Boolean = true,
    val token: String = "",
    val validating: Boolean = false,
    val tokenError: String? = null,
    val user: String? = null,
    val installer: InstallerChoice = InstallerChoice.APP_MANAGER,
    val hideEmpty: Boolean = true,
    val repos: List<Repo> = emptyList(),
    val counts: Map<Long, Int> = emptyMap(),
    val loadingRepos: Boolean = false,
    val reposError: String? = null,
    val query: String = "",
    val selected: Repo? = null,
    val artifacts: List<Artifact> = emptyList(),
    val loadingArtifacts: Boolean = false,
    val artifactsError: String? = null,
    val downloads: Map<Long, DownloadState> = emptyMap(),
) {
    val visibleRepos: List<Repo>
        get() = repos.filter { r ->
            (query.isBlank() || r.fullName.contains(query.trim(), ignoreCase = true)) &&
                (!hideEmpty || (counts[r.id] ?: 0) > 0)
        }
    val scanning: Boolean get() = repos.isNotEmpty() && counts.size < repos.size
}

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var api: GitHubApi? = null
    private var reposJob: Job? = null
    private var artifactsJob: Job? = null
    private val downloadJobs = mutableMapOf<Long, Job>()
    private val cacheRoot get() = File(app.cacheDir, "artifacts")

    init {
        viewModelScope.launch {
            val s = store.load()
            _state.update {
                it.copy(loadingSettings = false, token = s.token, installer = s.installer, hideEmpty = s.hideEmpty)
            }
            if (s.token.isNotBlank()) {
                api = GitHubApi(s.token)
                loadRepos()
            }
        }
    }

    // --- Account -----------------------------------------------------------

    fun saveToken(raw: String) {
        val token = raw.trim()
        if (token.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(validating = true, tokenError = null) }
            try {
                val candidate = GitHubApi(token)
                val user = candidate.me()
                store.setToken(token)
                api = candidate
                _state.update { it.copy(validating = false, token = token, user = user.login) }
                loadRepos()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(validating = false, tokenError = e.friendly()) }
            }
        }
    }

    fun signOut() {
        reposJob?.cancel()
        artifactsJob?.cancel()
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        api = null
        viewModelScope.launch { store.setToken("") }
        _state.update { UiState(loadingSettings = false, installer = it.installer, hideEmpty = it.hideEmpty) }
    }

    // --- Settings ------------------------------------------------------------

    fun setInstaller(choice: InstallerChoice) {
        _state.update { it.copy(installer = choice) }
        viewModelScope.launch { store.setInstaller(choice) }
    }

    fun setHideEmpty(value: Boolean) {
        _state.update { it.copy(hideEmpty = value) }
        viewModelScope.launch { store.setHideEmpty(value) }
    }

    fun setQuery(value: String) = _state.update { it.copy(query = value) }

    fun clearDownloads() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        viewModelScope.launch(Dispatchers.IO) { cacheRoot.deleteRecursively() }
        _state.update { it.copy(downloads = emptyMap()) }
    }

    // --- Repos -----------------------------------------------------------------

    fun loadRepos() {
        val api = api ?: return
        reposJob?.cancel()
        reposJob = viewModelScope.launch {
            _state.update { it.copy(loadingRepos = true, reposError = null) }
            try {
                if (_state.value.user == null) {
                    runCatching { api.me() }.onSuccess { u -> _state.update { it.copy(user = u.login) } }
                }
                val repos = api.repos()
                _state.update { it.copy(repos = repos, counts = emptyMap(), loadingRepos = false) }

                // Count artifacts per repo so empty repos can be hidden.
                val gate = Semaphore(8)
                coroutineScope {
                    repos.forEach { repo ->
                        launch {
                            val count = gate.withPermit {
                                try {
                                    api.artifacts(repo.fullName, perPage = 1).totalCount
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    0 // Actions disabled or no access.
                                }
                            }
                            _state.update { it.copy(counts = it.counts + (repo.id to count)) }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loadingRepos = false, reposError = e.friendly()) }
            }
        }
    }

    fun select(repo: Repo?) {
        artifactsJob?.cancel()
        _state.update { it.copy(selected = repo, artifacts = emptyList(), artifactsError = null) }
        if (repo != null) loadArtifacts()
    }

    fun loadArtifacts() {
        val api = api ?: return
        val repo = _state.value.selected ?: return
        artifactsJob?.cancel()
        artifactsJob = viewModelScope.launch {
            _state.update { it.copy(loadingArtifacts = true, artifactsError = null) }
            try {
                val list = api.artifacts(repo.fullName).artifacts
                // Re-attach files from earlier downloads.
                val cached = withContext(Dispatchers.IO) {
                    list.associate { a -> a.id to Installer.cached(File(cacheRoot, a.id.toString())) }
                        .filterValues { it.isNotEmpty() }
                }
                _state.update { s ->
                    val downloads = s.downloads.toMutableMap()
                    cached.forEach { (id, files) ->
                        if (downloads[id] !is DownloadState.Progress) downloads[id] = DownloadState.Ready(files)
                    }
                    s.copy(artifacts = list, loadingArtifacts = false, downloads = downloads)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loadingArtifacts = false, artifactsError = e.friendly()) }
            }
        }
    }

    // --- Download & install ------------------------------------------------------

    fun download(artifact: Artifact) {
        val api = api ?: return
        if (downloadJobs[artifact.id]?.isActive == true) return
        downloadJobs[artifact.id] = viewModelScope.launch {
            setDownload(artifact.id, DownloadState.Progress(0, artifact.sizeInBytes))
            try {
                val dir = File(cacheRoot, artifact.id.toString())
                val files = withContext(Dispatchers.IO) {
                    dir.deleteRecursively()
                    dir.mkdirs()
                    val zip = File(dir, "artifact.zip.part")
                    var lastReported = 0L
                    api.download(artifact.downloadUrl, zip) { read, total ->
                        if (read - lastReported >= 256 * 1024 || read == total) {
                            lastReported = read
                            val size = if (total > 0) total else artifact.sizeInBytes
                            setDownload(artifact.id, DownloadState.Progress(read, size))
                        }
                    }
                    Installer.extract(zip, dir).also { zip.delete() }
                }
                if (files.isEmpty()) {
                    setDownload(artifact.id, DownloadState.Failed("No APK found in this artifact"))
                } else {
                    setDownload(artifact.id, DownloadState.Ready(files))
                    if (files.size == 1) install(files.first())
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(downloads = it.downloads - artifact.id) }
                throw e
            } catch (e: Exception) {
                setDownload(artifact.id, DownloadState.Failed(e.friendly()))
            }
        }
    }

    fun cancelDownload(id: Long) {
        downloadJobs.remove(id)?.cancel()
        _state.update { it.copy(downloads = it.downloads - id) }
    }

    fun install(file: File) {
        val error = Installer.install(app, file, _state.value.installer)
        if (error != null) Toast.makeText(app, error, Toast.LENGTH_LONG).show()
    }

    private fun setDownload(id: Long, value: DownloadState) =
        _state.update { it.copy(downloads = it.downloads + (id to value)) }
}

private fun Throwable.friendly(): String = when (this) {
    is GitHubException -> when (code) {
        401 -> "GitHub rejected the token (401). Check it hasn't expired."
        403 -> "Access denied (403). The token needs Actions: read, or the rate limit was hit."
        404 -> "Not found (404). The token may not cover this repository."
        410 -> "This artifact has expired (410)."
        else -> "GitHub error $code"
    }
    is IOException -> "Network error: ${message ?: javaClass.simpleName}"
    else -> message ?: javaClass.simpleName
}
