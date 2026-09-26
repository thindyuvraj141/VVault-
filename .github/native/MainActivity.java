package __APP_ID__;

// __APP_ID__ above gets replaced automatically at build time (same pattern as
// VaultMediaPlugin.java) — you don't need to edit it by hand.

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;

import com.getcapacitor.BridgeActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity — hosts the Capacitor WebView.
 *
 * Registers VaultMediaPlugin (pick/delete media) and also receives Android's
 * "Share" intent (ACTION_SEND / ACTION_SEND_MULTIPLE), so sharing a photo or
 * video into V Vault from Gallery (or any other app) works.
 *
 * Shared files are read into base64 here and handed straight to
 * VaultMediaPlugin's static queue (addSharedMedia) — NOT injected into the
 * WebView directly. That matters: on a cold start (app wasn't already
 * running), the WebView hasn't finished loading index.html yet when this
 * runs, so any JS call made from here would silently be lost. Queuing on the
 * native side instead means the web app can safely pull the data whenever
 * it's actually ready, via VaultMedia.getSharedMedia().
 *
 * FLAG_SECURE (set below, before the window's content is created) blocks
 * screenshots and screen recording of this app system-wide, and also makes
 * the app's preview in the Android "recent apps" switcher show as a blank
 * screen instead of the vault's contents.
 */
public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        registerPlugin(VaultMediaPlugin.class);
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
     * Reads any shared photos/videos out of the given intent and queues them
     * on VaultMediaPlugin as base64 data URLs. Safe to call with any intent —
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

        List<String> dataUris = new ArrayList<>();
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
                dataUris.add("data:" + mime + ";base64," + base64);
            } catch (Exception e) {
                // Skip a file we couldn't read rather than failing the whole batch.
            }
        }
        if (dataUris.isEmpty()) return;

        VaultMediaPlugin.addSharedMedia(dataUris);
    }
}
