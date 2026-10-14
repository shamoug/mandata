// Mandata for Android: the phone app that Mandata on your computer serves (public/m), in a window of its own.
// It remembers the computer it was paired with and opens there. It pairs with the QR code in Mandata's Settings >
// Phone: scanned in the app (Scan QR code, Google's code scanner), or with the camera, whose mandata:// link opens here. Pages on the computer (http) open here; anything else (the
// website, mail links) opens in the phone's own apps. No data is kept here but the address and the pairing cookie.
package org.mandata.phone;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class MainActivity extends Activity {
    private static final String START = "file:///android_asset/start.html";
    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("mandata", MODE_PRIVATE);
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        // The phone page knows it runs in the app by this, and does not offer to install it.
        s.setUserAgentString(s.getUserAgentString() + " MandataApp/" + BuildConfig.VERSION_NAME);
        CookieManager.getInstance().setAcceptCookie(true);

        web.setWebChromeClient(new WebChromeClient());
        // The pages ask for the scanner (window.MandataApp.scan()); what it reads is opened here, never handed back.
        web.addJavascriptInterface(new Bridge(), "MandataApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                if (scheme.equals("http") || scheme.equals("file")) return false;   // the computer, and the start page
                if (scheme.equals("mandata")) { open(u); return true; }
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException e) { /* nothing can open it */ }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // The computer it reached is the one to open next time.
                Uri u = Uri.parse(url);
                if ("http".equals(u.getScheme()) && u.getHost() != null) {
                    prefs.edit().putString("server", "http://" + u.getAuthority() + "/").apply();
                }
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (!req.isForMainFrame() || !"http".equals(req.getUrl().getScheme())) return;
                view.loadUrl(START + "?error=1&last=" + Uri.encode("http://" + req.getUrl().getAuthority() + "/"));
            }
        });

        if (state != null) web.restoreState(state);
        else if (!open(getIntent().getData())) {
            String server = prefs.getString("server", null);
            web.loadUrl(server != null ? server + "#/home" : START);
        }
    }

    /** What the pages may ask of the app. */
    public class Bridge {
        @JavascriptInterface
        public void scan() { runOnUiThread(MainActivity.this::scan); }
    }

    /** Scan the pairing QR code with Google's code scanner: no camera permission, the scanner is Play services' own. */
    private void scan() {
        GmsBarcodeScannerOptions opts = new GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
        GmsBarcodeScanning.getClient(this, opts).startScan()
            .addOnSuccessListener(code -> {
                String v = code.getRawValue() == null ? "" : code.getRawValue().trim();
                Uri u = Uri.parse(v);
                if (open(u)) return;
                // The QR code for Safari and Chrome works too: http://<computer>:<port>/#pair=CODE
                if ("http".equals(u.getScheme()) && u.getHost() != null && v.contains("#pair=")) { web.loadUrl(v); return; }
                Toast.makeText(this, "That is not a Mandata pairing code. Scan the one in Mandata, Settings > Phone.", Toast.LENGTH_LONG).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "The scanner is not available on this phone: type the address and code instead.", Toast.LENGTH_LONG).show());
    }

    /**
     * A pairing link opens the computer and pairs: the QR code's https://shamoug.github.io/mandata/open.html#192.168.1.20:4321:ABCD2345,
     * or mandata://192.168.1.20:4321/pair?code=ABCD2345 from the phone version's "Open in the Mandata app".
     */
    private boolean open(Uri u) {
        if (u != null && "https".equals(u.getScheme()) && "shamoug.github.io".equals(u.getHost()) && u.getFragment() != null) {
            String[] p = u.getFragment().split(":");
            if (p.length != 3 || !p[0].matches("[A-Za-z0-9.-]+") || !p[1].matches("[0-9]{1,5}") || !p[2].matches("[A-Za-z0-9]{4,12}")) return false;
            web.loadUrl("http://" + p[0] + ":" + p[1] + "/#pair=" + p[2]);
            return true;
        }
        if (u == null || !"mandata".equals(u.getScheme()) || u.getHost() == null) return false;
        String code = u.getQueryParameter("code");
        web.loadUrl("http://" + u.getAuthority() + "/" + (code != null ? "#pair=" + Uri.encode(code) : "#/home"));
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        open(intent.getData());
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }
}
