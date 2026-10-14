// The phone version's own files (its page, script, styles, icons), kept in the app: fetched from the computer when it
// answers, and served from the app's copy when it does not, so Mandata opens without the computer. The data is the
// page's business (IndexedDB, public/m/app.js); only files outside /api/ are kept here.
package org.mandata.phone;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.security.MessageDigest;

final class Shell {
    private static volatile long quietUntil = 0;   // after a failed fetch, the copy is used without asking for 20 seconds

    private Shell() {}

    /** The file at u, from the computer or from the app's copy; null when there is neither (the WebView says so). */
    static WebResourceResponse get(Context c, Uri u) {
        File dir = new File(c.getFilesDir(), "shell"), body, type;
        try {
            dir.mkdirs();
            String key = hex(MessageDigest.getInstance("SHA-1").digest((u.getAuthority() + u.getPath()).getBytes(StandardCharsets.UTF_8)));
            body = new File(dir, key); type = new File(dir, key + ".type");
        } catch (Exception e) { return null; }

        if (System.currentTimeMillis() > quietUntil) {
            HttpURLConnection h = null;
            try {
                h = (HttpURLConnection) new URL(u.toString()).openConnection();
                h.setConnectTimeout(3000); h.setReadTimeout(10000);
                if (h.getResponseCode() == 200) {
                    byte[] b = Notices.read(h.getInputStream());
                    String t = h.getContentType() == null ? "application/octet-stream" : h.getContentType();
                    write(body, b);
                    write(type, t.getBytes(StandardCharsets.UTF_8));
                    return response(t, new ByteArrayInputStream(b));
                }
            } catch (Exception e) {
                quietUntil = System.currentTimeMillis() + 20000;
            } finally { if (h != null) h.disconnect(); }
        }
        try {
            if (!body.exists() || !type.exists()) return null;
            return response(new String(Notices.read(new FileInputStream(type)), StandardCharsets.UTF_8), new FileInputStream(body));
        } catch (Exception e) { return null; }
    }

    private static WebResourceResponse response(String contentType, java.io.InputStream in) {
        String[] p = contentType.split(";");
        String mime = p[0].trim(), charset = null;
        for (String x : p) if (x.trim().toLowerCase().startsWith("charset=")) charset = x.trim().substring(8);
        return new WebResourceResponse(mime, charset, in);
    }

    private static void write(File f, byte[] b) throws java.io.IOException {
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(b); }
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x));
        return s.toString();
    }
}
