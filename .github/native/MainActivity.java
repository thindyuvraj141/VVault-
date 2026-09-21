package __APP_ID__;

// The line above gets rewritten automatically at build time (same as
// VaultMediaPlugin.java) to match your real Capacitor appId from
// capacitor.config.json — you don't need to edit it by hand.

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import com.getcapacitor.BridgeActivity;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * MainActivity — the single Activity that hosts the Capacitor WebView.
 *
 * On top of the normal Capacitor boilerplate, this also receives Android's
 * "Share" intent (ACTION_SEND / ACTION_SEND_MULTIPLE) so that sharing a
 * photo or video into V Vault from Gallery (or any other app) works. The
 * shared files are read into base64 here and handed to the web app via
 * window.receiveSharedMedia(...), which is already defined in index.html.
 *
 * VaultMediaPlugin registers itself automatically (it's annotated with
 * @CapacitorPlugin), so nothing else needs to change there.
 */
public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleShareIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShareIntent(intent);
    }

    /**
     * Reads any shared photos/videos out of the given intent and passes them
     * to the web app as base64 data URLs. Safe to call with any intent —
     * it quietly does nothing if the intent isn't a media share.
     */
    private void handleShareIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();
        if (type == null || !(type.startsWith("image/") || type.startsWith("video/"))) return;

        ArrayList<Uri> uris = new ArrayList<>();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) uris.add(uri);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (list != null) uris.addAll(list);
        } else {
            return;
        }
        if (uris.isEmpty()) return;

        JSONArray items = new JSONArray();
        for (Uri uri : uris) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) continue;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
                is.close();

                String mime = getContentResolver().getType(uri);
                if (mime == null) mime = type;
                String base64 = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);
                items.put("data:" + mime + ";base64," + base64);
            } catch (Exception e) {
                // Skip a file we couldn't read rather than failing the whole batch.
            }
        }
        if (items.length() == 0) return;

        // Wait for the WebView to be ready, then hand the data to the web app.
        final JSONArray finalItems = items;
        getBridge().getWebView().post(() -> {
            String js = "window.receiveSharedMedia && window.receiveSharedMedia(" + finalItems.toString() + ")";
            getBridge().getWebView().evaluateJavascript(js, null);
        });
    }
}
