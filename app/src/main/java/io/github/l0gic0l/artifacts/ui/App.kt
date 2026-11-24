package io.github.l0gic0l.artifacts.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.l0gic0l.artifacts.data.Artifact
import io.github.l0gic0l.artifacts.data.InstallerChoice
import io.github.l0gic0l.artifacts.data.Repo
import io.github.l0gic0l.artifacts.install.Installer
import java.io.File
import java.time.Instant

@Composable
fun App(vm: MainViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    when {
        s.loadingSettings -> Surface(Modifier.fillMaxSize()) {}
        s.token.isBlank() -> TokenScreen(s, vm::saveToken)
        else -> MainScreen(s, vm)
    }
}

// --- Sign in -------------------------------------------------------------------

@Composable
private fun TokenScreen(s: UiState, onSave: (String) -> Unit) {
    val context = LocalContext.current
    var token by rememberSaveable { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Key, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text("Connect GitHub", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Paste a personal access token. A fine-grained token needs " +
                        "Repository access: All repositories, with Actions: Read and Metadata: Read. " +
                        "A classic token needs the repo scope.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    isError = s.tokenError != null,
                    supportingText = if (s.tokenError != null) {
                        { Text(s.tokenError) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/personal-access-tokens/new"))
                        )
                    }) { Text("Create token") }
                    Button(onClick = { onSave(token) }, enabled = token.isNotBlank() && !s.validating) {
                        if (s.validating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Continue")
                        }
                    }
                }
            }
        }
    }
}

// --- Main: list/detail, side by side when unfolded ------------------------------------

@Composable
private fun MainScreen(s: UiState, vm: MainViewModel) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 600.dp
        if (twoPane) {
            // 50/50 keeps content off the Pixel Fold hinge.
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { RepoPane(s, vm, onSettings = { showSettings = true }) }
                VerticalDivider()
                Box(Modifier.weight(1f)) {
                    val repo = s.selected
                    if (repo == null) EmptyDetail() else ArtifactPane(s, repo, vm, showBack = false)
                }
            }
        } else {
            val repo = s.selected
            if (repo == null) {
                RepoPane(s, vm, onSettings = { showSettings = true })
            } else {
                BackHandler { vm.select(null) }
                ArtifactPane(s, repo, vm, showBack = true)
            }
        }
    }
    if (showSettings) SettingsSheet(s, vm) { showSettings = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepoPane(s: UiState, vm: MainViewModel, onSettings: () -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Repositories") },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = s.loadingRepos,
            onRefresh = vm::loadRepos,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    OutlinedTextField(
                        value = s.query,
                        onValueChange = vm::setQuery,
                        placeholder = { Text("Search repositories") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (s.query.isNotEmpty()) {
                                IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Clear, "Clear") }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = s.hideEmpty,
                            onClick = { vm.setHideEmpty(!s.hideEmpty) },
                            label = { Text("Only with artifacts") },
                            leadingIcon = if (s.hideEmpty) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null,
                        )
                        Spacer(Modifier.width(12.dp))
                        if (s.scanning) {
                            Text(
                                "Scanning ${s.counts.size}/${s.repos.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (s.scanning) {
                        LinearProgressIndicator(
                            progress = { s.counts.size.toFloat() / s.repos.size },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
                s.reposError?.let { item { ErrorCard(it, vm::loadRepos) } }
                items(s.visibleRepos, key = { it.id }) { repo ->
                    RepoCard(repo, s.counts[repo.id], selected = repo.id == s.selected?.id) { vm.select(repo) }
                }
                if (!s.loadingRepos && !s.scanning && s.visibleRepos.isEmpty() && s.reposError == null) {
                    item {
                        Text(
                            if (s.hideEmpty) "No repositories with artifacts." else "No repositories.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoCard(repo: Repo, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val colors = if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    }
    Card(onClick = onClick, colors = colors, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (repo.isPrivate) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Lock, "Private", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    listOfNotNull(repo.owner.login, repo.pushedAt?.let { "pushed ${relative(it)}" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            when (count) {
                null -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else -> Surface(
                    shape = CircleShape,
                    color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (count > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// --- Artifacts -------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtifactPane(s: UiState, repo: Repo, vm: MainViewModel, showBack: Boolean) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(repo.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { vm.select(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = vm::loadArtifacts) { Icon(Icons.Default.Refresh, "Refresh") }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = s.loadingArtifacts,
            onRefresh = vm::loadArtifacts,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                s.artifactsError?.let { item { ErrorCard(it, vm::loadArtifacts) } }
                items(s.artifacts, key = { it.id }) { artifact ->
                    ArtifactCard(
                        artifact = artifact,
                        download = s.downloads[artifact.id],
                        onDownload = { vm.download(artifact) },
                        onCancel = { vm.cancelDownload(artifact.id) },
                        onInstall = vm::install,
                    )
                }
                if (!s.loadingArtifacts && s.artifacts.isEmpty() && s.artifactsError == null) {
                    item {
                        Text(
                            "No artifacts in this repository.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactCard(
    artifact: Artifact,
    download: DownloadState?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: (File) -> Unit,
) {
    val context = LocalContext.current
    ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Icon(
                        AppIcons.Package, null,
                        Modifier.padding(8.dp).size(22.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        artifact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            Formatter.formatShortFileSize(context, artifact.sizeInBytes),
                            artifact.createdAt?.let { relative(it) },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            artifact.workflowRun?.let { run ->
                val label = listOfNotNull(run.headBranch, run.headSha?.take(7)).joinToString(" @ ")
                if (label.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(AppIcons.Branch, null, Modifier.size(18.dp)) },
                    )
                }
            }

            when {
                artifact.expired -> Text(
                    "Expired",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
                download is DownloadState.Progress -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val fraction = if (download.total > 0) download.read.toFloat() / download.total else 0f
                        LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${Formatter.formatShortFileSize(context, download.read)} of " +
                                Formatter.formatShortFileSize(context, download.total),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Cancel") }
                }
                download is DownloadState.Ready -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    download.files.forEach { file ->
                        FilledTonalButton(onClick = { onInstall(file) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(AppIcons.InstallMobile, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Install ${file.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    TextButton(onClick = onDownload) { Text("Download again") }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (download is DownloadState.Failed) {
                        Text(download.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(AppIcons.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (download is DownloadState.Failed) "Retry" else "Download & install")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDetail() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(AppIcons.Package, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))
            Text("Pick a repository", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

// --- Settings -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(s: UiState, vm: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val hasAppManager = remember { Installer.isAppManagerInstalled(context) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "Signed in as ${s.user ?: "…"}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "Install with",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            InstallerChoice.entries.forEach { choice ->
                val note = when {
                    choice == InstallerChoice.APP_MANAGER && !hasAppManager -> "Not installed — falls back to Android's picker"
                    choice == InstallerChoice.SYSTEM -> "Needs “Install unknown apps” allowed for this app"
                    else -> null
                }
                ListItem(
                    headlineContent = { Text(choice.label) },
                    supportingContent = if (note != null) {
                        { Text(note) }
                    } else null,
                    leadingContent = { RadioButton(selected = s.installer == choice, onClick = null) },
                    modifier = Modifier.selectable(
                        selected = s.installer == choice,
                        role = Role.RadioButton,
                        onClick = { vm.setInstaller(choice) },
                    ),
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text("Clear downloaded files") },
                leadingContent = { Icon(Icons.Default.Delete, null) },
                modifier = Modifier.selectable(selected = false, onClick = { vm.clearDownloads(); onDismiss() }),
            )
            ListItem(
                headlineContent = { Text("Sign out") },
                supportingContent = { Text("Removes the stored token") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                modifier = Modifier.selectable(selected = false, onClick = { onDismiss(); vm.signOut() }),
            )
        }
    }
}

private fun relative(iso: String): String = runCatching {
    DateUtils.getRelativeTimeSpanString(
        Instant.parse(iso).toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}.getOrDefault(iso)
