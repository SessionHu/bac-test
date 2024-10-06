import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CurlWbi {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || !args[0].equals("curl")) {
            System.err.println("Usage: java CurlWbi curl [curl options] --data-urlencode [key=value]...");
            System.exit(255);
        }
        List<String> argsList = new ArrayList<>();
        JsonObject params = new JsonObject();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--url-query")) {
                String data = args[++i];
                for (int j = 0; j < data.length(); j++) {
                    char c = data.charAt(j);
                    if (c == '=') {
                        String key = data.substring(0, j);
                        String value = data.substring(j + 1);
                        params.addProperty(key, value);
                        break;
                    }
                }
            } else {
                argsList.add(args[i]);
            }
        }
        params = wbiSign(params);
        for (String key : params.keySet()) {
            argsList.add("--url-query");
            argsList.add(key + "=" + params.get(key).getAsString());
        }
        System.err.println("$ " + String.join(" ", argsList));
        Process process = new ProcessBuilder(argsList).start();
        InputStream in = process.getInputStream();
        InputStream err = process.getErrorStream();
        while (true) {
            if (in.available() > 0) {
                System.out.write(in.read());
                System.out.flush();
            } else if (err.available() > 0) {
                System.err.write(err.read());
            } else if (!process.isAlive()) {
                break;
            }
        }
        System.exit(process.exitValue());
    }

    private static final byte[] MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
            61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
            36, 20, 34, 44, 52
    };

    public static final String API_NAV_URL = "https://api.bilibili.com/x/web-interface/nav";

    /**
     * Get the mixin key from the Bilibili API.
     * 
     * @return The mixin key as a string.
     * @throws URISyntaxException
     * @throws IOException
     * @throws MalformedURLException
     */
    private static String getMixinKey() throws Exception {
        // get imgKey and subKey
        HttpURLConnection conn = (HttpURLConnection) new URI(API_NAV_URL).toURL().openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0");
        InputStream in = conn.getInputStream();
        int c;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((c = in.read()) != -1) {
            out.write(c);
        }
        JsonObject nav = new Gson().fromJson(new String(out.toByteArray(), StandardCharsets.UTF_8), JsonObject.class);
        JsonObject wbiImg = nav.get("data").getAsJsonObject().get("wbi_img").getAsJsonObject();
        String imgUrl = wbiImg.get("img_url").getAsString();
        String imgKey = imgUrl.substring(imgUrl.lastIndexOf("/") + 1, imgUrl.lastIndexOf("."));
        String subUrl = wbiImg.get("sub_url").getAsString();
        String subKey = subUrl.substring(subUrl.lastIndexOf("/") + 1, subUrl.lastIndexOf("."));
        // System.err.println("imgUrl: " + imgUrl);
        // System.err.println("subUrl: " + subUrl);
        // System.err.println("imgKey: " + imgKey);
        // System.err.println("subKey: " + subKey);
        // mixin key
        String s = imgKey + subKey;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            key.append(s.charAt(MIXIN_KEY_ENC_TAB[i]));
        }
        // return
        return key.toString();
    }

    public static String encodeURIComponent(Object o) {
        try {
            return URLEncoder.encode(o.toString(), "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sign a request to the Bilibili API using the Wbi Sign method.
     * 
     * @param params The request parameters to sign.
     * @return The signed request parameters.
     * @throws NoSuchAlgorithmException 
     */
    public static JsonObject wbiSign(JsonObject params) throws Exception {
        // mixin key
        String mixinKey = getMixinKey();
        // System.err.println("Mixin key: " + mixinKey);
        // add timestamp
        Map<String, Object> map = new TreeMap<>();
        JsonObject copy = params.deepCopy();
        for (Map.Entry<String, JsonElement> entry : copy.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAsString());
        }
        map.put("wts", String.valueOf(System.currentTimeMillis() / 1000));
        String param = map.entrySet().stream()
                .map(it -> String.format("%s=%s", it.getKey(), encodeURIComponent(it.getValue())))
                .collect(Collectors.joining("&"));
        String s = param + mixinKey;
        // System.err.println("Message to be signed: " + s);
        // md5
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5 = md.digest(s.getBytes(StandardCharsets.UTF_8));
        params.addProperty("w_rid", bytesToHex(md5));
        params.addProperty("wts", Long.parseLong(map.get("wts").toString()));
        return params;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
