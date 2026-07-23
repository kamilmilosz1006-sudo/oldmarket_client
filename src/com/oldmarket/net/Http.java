package com.oldmarket.net;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Http {

    private static class CacheEntry {
        byte[] data;
        long time;
    }

    private static final long CACHE_TTL_MS = 30000L;
    private static final Map<String, CacheEntry> GET_CACHE = new HashMap<String, CacheEntry>();

    public static String getString(String url) throws Exception {
        byte[] b = getBytes(url);
        if (b == null) return null;
        return new String(b, "UTF-8");
    }

    public static byte[] getBytes(String urlStr) throws Exception {
        synchronized (GET_CACHE) {
            CacheEntry ce = GET_CACHE.get(urlStr);
            if (ce != null && ce.data != null && System.currentTimeMillis() - ce.time < CACHE_TTL_MS) {
                return ce.data;
            }
        }

        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(30000);
            conn.setUseCaches(false);
            conn.connect();

            int code = conn.getResponseCode();
            in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) return null;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            byte[] out = bos.toByteArray();
            if (out != null && out.length < 1024 * 1024) {
                CacheEntry ce = new CacheEntry();
                ce.data = out;
                ce.time = System.currentTimeMillis();
                synchronized (GET_CACHE) { GET_CACHE.put(urlStr, ce); }
            }
            return out;
        } finally {
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception e) {}
        }
    }

    // POST application/x-www-form-urlencoded
    public static String postForm(String urlStr, String[][] fields) throws Exception {
        HttpURLConnection conn = null;
        OutputStream out = null;
        InputStream in = null;

        String body = buildForm(fields);
        byte[] bodyBytes = body.getBytes("UTF-8");

        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(30000);
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));

            out = conn.getOutputStream();
            out.write(bodyBytes);
            out.flush();

            int code = conn.getResponseCode();
            in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) return null;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);

            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception e) {}
        }
    }

    private static String buildForm(String[][] fields) throws Exception {
        if (fields == null || fields.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append("&");
            String k = fields[i][0];
            String v = fields[i][1];
            if (k == null) k = "";
            if (v == null) v = "";
            sb.append(URLEncoder.encode(k, "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(v, "UTF-8"));
        }
        return sb.toString();
    }
    
    public static String postJson(String urlStr, String json) throws Exception {
        return sendJson("POST", urlStr, json);
    }

    public static String postJson(Context c, String urlStr, String json) throws Exception {
        return sendJson(c, "POST", urlStr, json);
    }

    public static String putJson(String urlStr, String json) throws Exception {
        return sendJson("PUT", urlStr, json);
    }

    public static String putJson(Context c, String urlStr, String json) throws Exception {
        return sendJson(c, "PUT", urlStr, json);
    }

    private static String sendJson(String method, String urlStr, String json) throws Exception {
        return sendJson(null, method, urlStr, json);
    }

    private static String sendJson(Context c, String method, String urlStr, String json) throws Exception {
        java.net.HttpURLConnection conn = null;
        java.io.OutputStream out = null;
        java.io.InputStream in = null;

        byte[] body = (json == null ? "" : json).getBytes("UTF-8");

        try {
            java.net.URL url = new java.net.URL(urlStr);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(30000);
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));
            if (c != null) {
                String token = com.oldmarket.util.Prefs.getAuthToken(c);
                if (token != null && token.length() > 0) {
                    conn.setRequestProperty("X-Oldmarket-Auth", token);
                }
            }

            out = conn.getOutputStream();
            out.write(body);
            out.flush();

            int code = conn.getResponseCode();
            in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) return null;

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception e) {}
        }
    }

    // multipart/form-data POST for file uploads
    public static String postMultipart(String urlStr, java.util.Map<String, String> fields,
                                        java.util.Map<String, String> fileFields,
                                        android.content.Context c) throws Exception {
        String boundary = "----Boundary" + System.currentTimeMillis();
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

        // text fields
        if (fields != null) {
            for (java.util.Map.Entry<String, String> e : fields.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                if (v == null) v = "";
                bos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                bos.write(("Content-Disposition: form-data; name=\"" + k + "\"\r\n").getBytes("UTF-8"));
                bos.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".getBytes("UTF-8"));
                bos.write(v.getBytes("UTF-8"));
                bos.write("\r\n".getBytes("UTF-8"));
            }
        }

        // file fields
        if (fileFields != null) {
            for (java.util.Map.Entry<String, String> e : fileFields.entrySet()) {
                String fieldName = e.getKey();
                String filePath = e.getValue();
                if (filePath == null || filePath.length() == 0) continue;
                java.io.File file = new java.io.File(filePath);
                if (!file.exists()) continue;
                String fileName = file.getName();
                String contentType = "application/octet-stream";
                String lower = fileName.toLowerCase();
                if (lower.endsWith(".png")) contentType = "image/png";
                else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
                else if (lower.endsWith(".apk")) contentType = "application/vnd.android.package-archive";

                bos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                bos.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n").getBytes("UTF-8"));
                bos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes("UTF-8"));

                java.io.FileInputStream fis = new java.io.FileInputStream(file);
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) bos.write(buf, 0, r);
                fis.close();
                bos.write("\r\n".getBytes("UTF-8"));
            }
        }

        // closing boundary
        bos.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        byte[] bodyBytes = bos.toByteArray();

        java.net.HttpURLConnection conn = null;
        java.io.OutputStream out = null;
        java.io.InputStream in = null;
        try {
            java.net.URL url = new java.net.URL(urlStr);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            if (c != null) {
                String token = com.oldmarket.util.Prefs.getAuthToken(c);
                if (token != null && token.length() > 0) {
                    conn.setRequestProperty("X-Oldmarket-Auth", token);
                }
            }

            out = conn.getOutputStream();
            out.write(bodyBytes);
            out.flush();

            int code = conn.getResponseCode();
            in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) return null;

            java.io.ByteArrayOutputStream res = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) res.write(buf, 0, r);
            return new String(res.toByteArray(), "UTF-8");
        } finally {
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception e) {}
        }
    }
}
