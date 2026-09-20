package __APP_ID__;

// The line above gets rewritten automatically at build time (see the
// "Add VaultMedia native plugin" step in build-android.yml) to match your
// real Capacitor appId from capacitor.config.json — you don't need to
// edit it by hand.

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
// requestCodes MUST list the delete-dialog code (9821 = DELETE_REQUEST_CODE below),
// otherwise Capacitor never calls handleOnActivityResult and the JS promise hangs.
@CapacitorPlugin(name = "VaultMedia", requestCodes = {9821})
public class VaultMediaPlugin extends Plugin {

    @PluginMethod
    public void pickMedia(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(call, intent, "pickMediaResult");
    }

    @ActivityCallback
    private void pickMediaResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK) {
            call.reject("cancelled");
            return;
        }

        Intent data = result.getData();
        List<Uri> uris = new ArrayList<>();
        if (data != null && data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                uris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data != null && data.getData() != null) {
            uris.add(data.getData());
        }

        JSArray items = new JSArray();
        for (Uri uri : uris) {
            // Keep read access beyond this call, so a later deleteMedia()
            // request can still reference the same URI.
            try {
                getContext().getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception e) {
                // Some providers don't support persistable permissions — fine.
            }

            String mimeType = getContext().getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "application/octet-stream";

            try (InputStream stream = getContext().getContentResolver().openInputStream(uri)) {
                if (stream != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int n;
                    while ((n = stream.read(chunk)) != -1) buffer.write(chunk, 0, n);
                    String base64 = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);

                    JSObject obj = new JSObject();
                    obj.put("uri", uri.toString());
                    obj.put("mimeType", mimeType);
                    obj.put("base64", base64);
                    items.put(obj);
                }
            } catch (Exception e) {
                // Skip a file we couldn't read rather than failing the whole batch.
            }
        }

        JSObject ret = new JSObject();
        ret.put("items", items);
        call.resolve(ret);
    }

    private String pendingDeleteCallbackId;
    private int pendingDeleteCount = 0;
    private static final int DELETE_REQUEST_CODE = 9821;


    /**
     * ACTION_OPEN_DOCUMENT gives "document" URIs such as
     *   content://com.android.providers.media.documents/document/image%3A1234
     * but MediaStore.createDeleteRequest only accepts real MediaStore URIs such as
     *   content://media/external/images/media/1234
     * Passing a document URI throws, so the delete never happened. This converts.
     * Returns null when the file isn't in MediaStore (e.g. Google Drive, Downloads).
     */
    private Uri toMediaStoreUri(Uri uri) {
        if (uri == null) return null;
        try {
            String authority = uri.getAuthority();
            if ("media".equals(authority)) {
                return uri; // already a MediaStore / picker URI
            }
            if ("com.android.providers.media.documents".equals(authority)) {
                String docId = DocumentsContract.getDocumentId(uri); // e.g. "image:1234"
                String[] parts = docId.split(":");
                if (parts.length == 2) {
                    long id = Long.parseLong(parts[1]);
                    if ("image".equals(parts[0])) {
                        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    } else if ("video".equals(parts[0])) {
                        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return MediaStore.getMediaUri(getContext(), uri);
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    @PluginMethod
    public void deleteMedia(PluginCall call) {
        JSArray uriArray = call.getArray("uris");
        List<Uri> uris = new ArrayList<>();
        try {
            if (uriArray != null) {
                for (Object o : uriArray.toList()) {
                    Uri converted = toMediaStoreUri(Uri.parse(String.valueOf(o)));
                    if (converted != null) uris.add(converted);
                }
            }
        } catch (Exception e) {
            // ignore malformed input, treated as empty below
        }

        if (uris.isEmpty()) {
            JSObject ret = new JSObject();
            ret.put("deleted", 0);
            call.resolve(ret);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: one system confirmation dialog covers every file
            // in the batch. The user taps "Allow" once; there is no way to
            // skip this dialog — it's an OS privacy requirement, not
            // something this plugin can bypass.
            //
            // We launch this the plain Android way (Activity.startIntentSenderForResult)
            // and catch the result in handleOnActivityResult below, rather than
            // relying on a newer Capacitor Plugin helper method that may not
            // exist in every Capacitor version.
            try {
                PendingIntent pendingIntent =
                        MediaStore.createDeleteRequest(getContext().getContentResolver(), uris);
                call.setKeepAlive(true);
                bridge.saveCall(call);
                pendingDeleteCallbackId = call.getCallbackId();
                pendingDeleteCount = uris.size();
                getActivity().startIntentSenderForResult(
                        pendingIntent.getIntentSender(), DELETE_REQUEST_CODE,
                        null, 0, 0, 0
                );
            } catch (Exception e) {
                call.reject("Could not request delete", e);
            }
        } else {
            // Android 10 and below: try a direct delete per file. This only
            // succeeds for files the app itself owns, or where the user has
            // already granted broad storage access — otherwise it silently
            // fails, which is fine since the vault copy already exists.
            int deleted = 0;
            for (Uri uri : uris) {
                try {
                    if (getContext().getContentResolver().delete(uri, null, null) > 0) deleted++;
                } catch (Exception e) {
                    // e.g. RecoverableSecurityException — skip this file.
                }
            }
            JSObject ret = new JSObject();
            ret.put("deleted", deleted);
            call.resolve(ret);
        }
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        if (requestCode != DELETE_REQUEST_CODE || pendingDeleteCallbackId == null) return;
        PluginCall savedCall = bridge.getSavedCall(pendingDeleteCallbackId);
        pendingDeleteCallbackId = null;
        if (savedCall == null) return;
        JSObject ret = new JSObject();
        // RESULT_OK means the user approved the whole batch.
        ret.put("deleted", resultCode == Activity.RESULT_OK ? pendingDeleteCount : 0);
        savedCall.resolve(ret);
        bridge.releaseCall(savedCall);
    }
}
