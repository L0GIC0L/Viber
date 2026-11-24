package io.github.l0gic0l.artifacts.install

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import io.github.l0gic0l.artifacts.data.InstallerChoice
import java.io.File
import java.util.zip.ZipInputStream

object Installer {
    const val APP_MANAGER = "io.github.muntashirakon.AppManager"
    private const val APK_MIME = "application/vnd.android.package-archive"
    val INSTALLABLE = setOf("apk", "apks", "apkm", "xapk")

    fun isAppManagerInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(APP_MANAGER, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Pulls every installable file out of an artifact zip, flattening folders. */
    fun extract(zip: File, outDir: File): List<File> {
        val out = mutableListOf<File>()
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                // Use only the file name: blocks "../" paths escaping outDir.
                val name = File(entry.name).name
                if (entry.isDirectory || name.substringAfterLast('.', "").lowercase() !in INSTALLABLE) continue
                var target = File(outDir, name)
                var n = 1
                while (target.exists()) target = File(outDir, "${n++}-$name")
                target.outputStream().use { zis.copyTo(it) }
                out += target
            }
        }
        return out
    }

    /** Existing extracted files from an earlier download, if any. */
    fun cached(dir: File): List<File> =
        dir.listFiles()?.filter { it.extension.lowercase() in INSTALLABLE }?.sortedBy { it.name }.orEmpty()

    /** Returns an error message, or null when an installer was launched. */
    fun install(context: Context, file: File, choice: InstallerChoice): String? {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val isApk = file.extension.equals("apk", ignoreCase = true)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, if (isApk) APK_MIME else "application/octet-stream")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

        val hasAm = isAppManagerInstalled(context)
        val target = when {
            // Split bundles (.apks etc.) need App Manager; the system installer can't read them.
            !isApk && hasAm -> intent.setPackage(APP_MANAGER)
            !isApk -> return "${file.name} is a split bundle. Install App Manager to install it."
            choice == InstallerChoice.APP_MANAGER && hasAm -> intent.setPackage(APP_MANAGER)
            choice == InstallerChoice.SYSTEM -> systemInstaller(context, intent)?.let { intent.setPackage(it) } ?: intent
            choice == InstallerChoice.ASK -> Intent.createChooser(intent, "Install with")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            else -> intent // App Manager chosen but missing: let Android pick.
        }
        return try {
            context.startActivity(target)
            null
        } catch (_: ActivityNotFoundException) {
            "No app can install ${file.name}"
        }
    }

    private fun systemInstaller(context: Context, intent: Intent): String? =
        context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY)
            .firstOrNull()?.activityInfo?.packageName
}
