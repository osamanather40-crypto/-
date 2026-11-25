package com.example.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ProofPhotoStorage {

    private fun ensurePhotoDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, "proof_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createCameraDestination(context: Context): Pair<File, Uri> {
        val dir = ensurePhotoDir(context)
        val file = File(dir, "qayd_cam_${System.currentTimeMillis()}.jpg")
        val authority = "${context.applicationContext.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context.applicationContext, authority, file)
        return file to contentUri
    }

    fun persistPickedPhoto(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = ensurePhotoDir(context)
            val destFile = File(dir, "qayd_pick_${System.currentTimeMillis()}.jpg")
            context.applicationContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (destFile.exists() && destFile.length() > 0L) destFile.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    fun getShareableUri(context: Context, photoPath: String): Uri? {
        return try {
            val trimmed = photoPath.trim()
            if (trimmed.startsWith("content://")) {
                return Uri.parse(trimmed)
            }
            val cleanPath = trimmed.removePrefix("file://")
            val file = File(cleanPath)
            if (!file.exists()) return null
            val appContext = context.applicationContext
            val authority = "${appContext.packageName}.fileprovider"
            FileProvider.getUriForFile(appContext, authority, file)
        } catch (_: Exception) {
            null
        }
    }
}
