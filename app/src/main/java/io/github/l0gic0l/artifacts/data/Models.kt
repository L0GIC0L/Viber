package io.github.l0gic0l.artifacts.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(val login: String)

@Serializable
data class Owner(val login: String)

@Serializable
data class Repo(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: Owner,
    @SerialName("private") val isPrivate: Boolean = false,
    val fork: Boolean = false,
    val archived: Boolean = false,
    val description: String? = null,
    @SerialName("pushed_at") val pushedAt: String? = null,
)

@Serializable
data class ArtifactsResponse(
    @SerialName("total_count") val totalCount: Int,
    val artifacts: List<Artifact> = emptyList(),
)

@Serializable
data class Artifact(
    val id: Long,
    val name: String,
    @SerialName("size_in_bytes") val sizeInBytes: Long,
    @SerialName("archive_download_url") val downloadUrl: String,
    val expired: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("workflow_run") val workflowRun: WorkflowRun? = null,
)

@Serializable
data class WorkflowRun(
    val id: Long? = null,
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("head_sha") val headSha: String? = null,
)
