package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

object AppFolderManager {
    private const val PREFS_NAME = "bookish_storage_prefs"
    private const val KEY_APP_FOLDER_URI = "app_folder_uri"
    private const val KEY_APP_FOLDER_DISPLAY = "app_folder_display"

    private val _appFolderUri = MutableStateFlow<String?>(null)
    val appFolderUri: StateFlow<String?> = _appFolderUri

    private val _appFolderDisplayPath = MutableStateFlow<String?>(null)
    val appFolderDisplayPath: StateFlow<String?> = _appFolderDisplayPath

    fun getDefaultAppFolder(context: Context): String {
        val folder = File(context.filesDir, "SubsTrack")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder.absolutePath
    }

    fun init(context: Context, initialUserFolder: String? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultPath = getDefaultAppFolder(context)
        val uriStr = prefs.getString(KEY_APP_FOLDER_URI, null)
            ?: initialUserFolder?.takeIf { it.isNotBlank() }
            ?: defaultPath
        val display = prefs.getString(KEY_APP_FOLDER_DISPLAY, null)
            ?: formatDisplayPath(uriStr, context)
        _appFolderUri.value = uriStr
        _appFolderDisplayPath.value = display
    }

    fun setAppFolderPath(context: Context, pathOrUri: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (pathOrUri.isNullOrBlank()) {
            val defaultPath = getDefaultAppFolder(context)
            val display = formatDisplayPath(defaultPath, context)
            prefs.edit().putString(KEY_APP_FOLDER_URI, defaultPath).putString(KEY_APP_FOLDER_DISPLAY, display).apply()
            _appFolderUri.value = defaultPath
            _appFolderDisplayPath.value = display
            return
        }

        if (pathOrUri.startsWith("content://")) {
            val uri = Uri.parse(pathOrUri)
            setAppFolderUri(context, uri)
            return
        }

        // File system path
        val folder = File(pathOrUri)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val display = formatDisplayPath(pathOrUri, context)
        prefs.edit()
            .putString(KEY_APP_FOLDER_URI, pathOrUri)
            .putString(KEY_APP_FOLDER_DISPLAY, display)
            .apply()

        _appFolderUri.value = pathOrUri
        _appFolderDisplayPath.value = display
    }

    fun setAppFolderUri(context: Context, uri: Uri?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (uri == null) {
            val defaultPath = getDefaultAppFolder(context)
            val display = formatDisplayPath(defaultPath, context)
            prefs.edit().putString(KEY_APP_FOLDER_URI, defaultPath).putString(KEY_APP_FOLDER_DISPLAY, display).apply()
            _appFolderUri.value = defaultPath
            _appFolderDisplayPath.value = display
            return
        }

        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val display = formatDisplayPath(uri)
        prefs.edit()
            .putString(KEY_APP_FOLDER_URI, uri.toString())
            .putString(KEY_APP_FOLDER_DISPLAY, display)
            .apply()

        _appFolderUri.value = uri.toString()
        _appFolderDisplayPath.value = display
    }

    fun formatDisplayPath(uri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            if (docId.startsWith("primary:", ignoreCase = true)) {
                docId.substringAfter(":")
            } else {
                docId
            }
        } catch (_: Exception) {
            uri.lastPathSegment ?: uri.toString()
        }
    }

    fun formatDisplayPath(pathOrUri: String?, context: Context? = null): String {
        if (pathOrUri.isNullOrBlank()) return "Internal Storage / SubsTrack"
        if (pathOrUri.startsWith("content://")) {
            return formatDisplayPath(Uri.parse(pathOrUri))
        }
        if (context != null && pathOrUri.startsWith(context.filesDir.absolutePath)) {
            val rel = pathOrUri.removePrefix(context.filesDir.absolutePath).removePrefix("/")
            return "Internal Storage / $rel"
        }
        if (pathOrUri.endsWith("/SubsTrack") || pathOrUri.endsWith("\\SubsTrack")) {
            return "Internal Storage / SubsTrack"
        }
        return pathOrUri
    }

    fun saveFileToAppFolder(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri? {
        val pathOrUri = _appFolderUri.value ?: getDefaultAppFolder(context)
        return try {
            if (pathOrUri.startsWith("content://")) {
                val treeUri = Uri.parse(pathOrUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                val docUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocUri,
                    mimeType,
                    fileName
                ) ?: return null

                context.contentResolver.openOutputStream(docUri)?.use { out ->
                    out.write(bytes)
                }
                docUri
            } else {
                val folder = File(pathOrUri)
                if (!folder.exists()) {
                    folder.mkdirs()
                }
                val file = File(folder, fileName)
                file.writeBytes(bytes)
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppFolderManager", "Failed to save file to app folder", e)
            null
        }
    }
}
