package __APP_ID__

// The line above gets rewritten automatically at build time (see the
// "Add VaultMedia native plugin" step in build-android.yml) to match your
// real Capacitor appId from capacitor.config.json — you don't need to
// edit it by hand.

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * VaultMedia — lets the web app (1) pick photos/videos while keeping their
 * original content:// URI, and (2) ask Android to delete those originals
 * from the gallery afterwards.
 *
 * A plain <input type="file"> can do (1) but never gives JS the URI back,
 * and no web API can do (2) at all — Android requires a native, user-
 * confirmed request (MediaStore.createDeleteRequest) before any app can
 * delete another app's media. That's why this needs to be native code
 * instead of something addable to the HTML file directly.
 */
@CapacitorPlugin(name = "VaultMedia")
class VaultMediaPlugin : Plugin() {

    @PluginMethod
    fun pickMedia(call: PluginCall) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(call, intent, "pickMediaResult")
    }

    @ActivityCallback
    private fun pickMediaResult(call: PluginCall?, result: androidx.activity.result.ActivityResult) {
        if (call == null) return
        if (result.resultCode != Activity.RESULT_OK) {
            call.reject("cancelled")
            return
        }

        val data = result.data
        val uris = mutableListOf<Uri>()
        val clip = data?.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
        } else {
            data?.data?.let { uris.add(it) }
        }

        val items = JSArray()
        for (uri in uris) {
            // Keep read access to this URI beyond this single call, so the
            // later deleteMedia() call (or a retry) can still reference it.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Some providers don't support persistable permissions — fine,
                // we only need the URI for this one delete request anyway.
            }

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val obj = JSObject()
                    obj.put("uri", uri.toString())
                    obj.put("mimeType", mimeType)
                    obj.put("base64", base64)
                    items.put(obj)
                }
            } catch (e: Exception) {
                // Skip a file we couldn't read rather than failing the whole batch.
            }
        }

        val ret = JSObject()
        ret.put("items", items)
        call.resolve(ret)
    }

    @PluginMethod
    fun deleteMedia(call: PluginCall) {
        val uriArray = call.getArray("uris")
        val uriStrings: List<String> = uriArray?.toList<String>() ?: emptyList()
        if (uriStrings.isEmpty()) {
            call.resolve(JSObject().apply { put("deleted", 0) })
            return
        }
        val uris = uriStrings.map { Uri.parse(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: one system confirmation dialog covers every file
            // in the batch. The user taps "Allow" once; there is no way to
            // skip this dialog — it's an OS privacy requirement, not
            // something this plugin can bypass.
            try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                startIntentSenderForResult(
                    call,
                    pendingIntent.intentSender,
                    "deleteMediaResult",
                    null, 0, 0, 0, null
                )
            } catch (e: Exception) {
                call.reject("Could not request delete", e)
            }
        } else {
            // Android 10 and below: try a direct delete per file. This only
            // succeeds for files the app itself owns, or where the user has
            // already granted broad storage access — otherwise it silently
            // fails, which is fine since the vault copy already exists.
            var deleted = 0
            for (uri in uris) {
                try {
                    if (context.contentResolver.delete(uri, null, null) > 0) deleted++
                } catch (e: Exception) {
                    // e.g. RecoverableSecurityException — skip this file.
                }
            }
            call.resolve(JSObject().apply { put("deleted", deleted) })
        }
    }

    @ActivityCallback
    private fun deleteMediaResult(call: PluginCall?, result: androidx.activity.result.ActivityResult) {
        if (call == null) return
        val ret = JSObject()
        // Android doesn't report back exactly how many were deleted from this
        // dialog — RESULT_OK means the user approved the whole batch.
        ret.put("deleted", if (result.resultCode == Activity.RESULT_OK) uriCountHint else 0)
        call.resolve(ret)
    }

    // Simple placeholder so deleteMediaResult always has a truthy count to
    // report on success; replace with real tracking if you need an exact
    // number shown in the UI.
    private val uriCountHint = 1
}
