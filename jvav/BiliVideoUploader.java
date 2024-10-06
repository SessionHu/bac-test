import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * A demo class for uploading videos to Bilibili.
 * 
 * @author SessX6cf
 */
public class BiliVideoUploader {

    private static String SESSDATA;
    private static File VIDEO_FILE;

    public static void main(String[] args) throws IOException {
        long ts = System.currentTimeMillis();
        if (args.length < 2) {
            System.out.println("Usage: java BiliVideoUploader <video_file> <sessdata>");
            return;
        } else {
            VIDEO_FILE = new File(args[0]);
            if (!VIDEO_FILE.isFile()) {
                System.out.println("It is not a file!");
                return;
            } else if (!VIDEO_FILE.canRead()) {
                System.out.println("Cannot read the file!");
                return;
            } else if (VIDEO_FILE.isDirectory()) {
                System.out.println("You can play a directory?!");
                return;
            }
            SESSDATA = args[1];
        }
        // step 1: preupload video
        System.out.println("step 1: preupload video");
        JsonObject preuploadVideo = preuploadVideo();
        // step 2: post video meta
        System.out.println("step 2: post video meta");
        JsonObject postVideoMeta = postVideoMeta(preuploadVideo);
        // step 3: upload video
        System.out.println("step 3: upload video");
        int chunks = uploadVideo(preuploadVideo, postVideoMeta);
        // step 4: end upload
        System.out.println("step 4: end upload");
        endupload(preuploadVideo, postVideoMeta, chunks);
        // finished
        System.out.println("finished (" + (System.currentTimeMillis() - ts) + "ms)");
    }

    private static String querypart(String key, String value) throws IOException {
        return key + "=" + URLEncoder.encode(value, "UTF-8");
    }

    private static HttpURLConnection conn(String url, String method) throws IOException {
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        } catch (java.net.URISyntaxException e) {
            throw new IOException(e);
        }
        conn.setRequestMethod(method);
        // conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0");
        if (url.contains("bilibili.com")) conn.setRequestProperty("Cookie", "SESSDATA=" + SESSDATA);
        return conn;
    }

    private static byte[] inputStreamToString(HttpURLConnection conn) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in;
        in = conn.getInputStream();
        int b;
        while ((b = in.read()) != -1) {
            baos.write(b);
        }
        in.close();
        return baos.toByteArray();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject preuploadVideo() throws IOException {
        StringJoiner url = new StringJoiner("&", "https://member.bilibili.com/preupload?", "");
        url.add(querypart("name", VIDEO_FILE.getName()));
        // url.add(querypart("size", String.valueOf(VIDEO_FILE.length())));
        url.add(querypart("r", "upos"));
        url.add(querypart("profile", "ugcfx/bup"));
        HttpURLConnection conn = conn(url.toString(), "GET");
        System.out.println("GET " + url.toString());
        String response = new String(inputStreamToString(conn), StandardCharsets.UTF_8);
        try {
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            System.out.println(GSON.toJson(json));
            return json;
        } catch (JsonSyntaxException e) {
            System.out.println(response);
            throw e;
        }
    }

    private static JsonObject postVideoMeta(JsonObject preuploadVideo) throws IOException {
        String schemeandhost = "https:" + preuploadVideo.get("endpoint").getAsString();
        String path = preuploadVideo.get("upos_uri").getAsString().replaceFirst("upos:/", "");
        StringJoiner url = new StringJoiner("&", schemeandhost + path + "?", "");
        url.add(querypart("uploads", "")); // WARNING: this is not a typo, it's required, or 404
        url.add(querypart("output", "json"));
        url.add(querypart("profile", "ugcfx/bup"));
        url.add(querypart("filesize", String.valueOf(VIDEO_FILE.length())));
        url.add(querypart("partsize", preuploadVideo.get("chunk_size").getAsString()));
        url.add(querypart("biz_id", preuploadVideo.get("biz_id").getAsString()));
        HttpURLConnection conn = conn(url.toString(), "POST");
        conn.setRequestProperty("X-Upos-Auth", preuploadVideo.get("auth").getAsString()); // 403 without it
        System.out.println("POST " + url.toString());
        String response = new String(inputStreamToString(conn), StandardCharsets.UTF_8);
        try {
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            System.out.println(GSON.toJson(json));
            return json;
        } catch (JsonSyntaxException e) {
            System.out.println(response);
            throw e;
        }
    }

    private static int uploadVideo(JsonObject preuploadVideo, JsonObject postVideoMeta) throws IOException {
        long startts = System.currentTimeMillis() - 1;
        String schemeandhost = "https:" + preuploadVideo.get("endpoint").getAsString();
        String path = preuploadVideo.get("upos_uri").getAsString().replaceFirst("upos:/", "");
        String urlp = schemeandhost + path + "?";
        long length = VIDEO_FILE.length();
        byte[] buffer = new byte[preuploadVideo.get("chunk_size").getAsInt()];
        int size = 0;
        int chunks = (int) Math.ceil(length / (double) buffer.length);
        InputStream in = new FileInputStream(VIDEO_FILE);
        for (int chunk = 0; chunk < chunks; chunk++) {
            System.out.println("speed: " + (chunk * buffer.length) / (System.currentTimeMillis() - startts) + "bytes/s");
            System.out.println("chunk: " + (chunk + 1) + "/" + chunks);
            size = in.read(buffer, 0, buffer.length);
            if (size == -1) {
                break;
            }
            StringJoiner url = new StringJoiner("&", urlp, "");
            url.add(querypart("partNumber", String.valueOf(chunk + 1)));
            url.add(querypart("uploadId", postVideoMeta.get("upload_id").getAsString()));
            url.add(querypart("chunk", String.valueOf(chunk)));
            url.add(querypart("chunks", String.valueOf(chunks)));
            url.add(querypart("size", String.valueOf(size)));
            url.add(querypart("start", String.valueOf(chunk * buffer.length)));
            url.add(querypart("end", String.valueOf((chunk) * buffer.length + size)));
            url.add(querypart("total", String.valueOf(length)));
            HttpURLConnection conn = conn(url.toString(), "PUT");
            conn.setRequestProperty("X-Upos-Auth", preuploadVideo.get("auth").getAsString());
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(size));
            conn.setDoOutput(true);
            conn.getOutputStream().write(buffer, 0, size);
            System.out.println("PUT " + url.toString());
            String response = new String(inputStreamToString(conn), StandardCharsets.UTF_8);
            System.out.println(response);
        }
        in.close();
        return chunks;
    }

    private static void endupload(JsonObject preuploadVideo, JsonObject postVideoMeta, int chunks) throws IOException {
        String schemeandhost = "https:" + preuploadVideo.get("endpoint").getAsString();
        String path = preuploadVideo.get("upos_uri").getAsString().replaceFirst("upos:/", "");
        StringJoiner url = new StringJoiner("&", schemeandhost + path + "?", "");
        url.add(querypart("output", "json"));
        url.add(querypart("name", VIDEO_FILE.getName()));
        url.add(querypart("profile", "ugcfx/bup"));
        url.add(querypart("uploadId", postVideoMeta.get("upload_id").getAsString()));
        url.add(querypart("biz_id", preuploadVideo.get("biz_id").getAsString()));
        JsonArray parts = new JsonArray();
        for (int i = 1; i <= chunks; i++) {
            JsonObject part = new JsonObject();
            part.addProperty("partNumber", i);
            part.addProperty("eTag", "etag");
            parts.add(part);
        }
        JsonObject body = new JsonObject();
        body.add("parts", parts);
        HttpURLConnection conn = conn(url.toString(), "POST");
        conn.setRequestProperty("X-Upos-Auth", preuploadVideo.get("auth").getAsString());
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("POST " + url.toString());
        String response = new String(inputStreamToString(conn), StandardCharsets.UTF_8);
        try {
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            System.out.println(GSON.toJson(json));
        } catch (JsonSyntaxException e) {
            System.out.println(response);
            throw e;
        }
    }

}
