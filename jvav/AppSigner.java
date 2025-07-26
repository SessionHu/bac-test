import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URLEncoder;
import java.util.TreeMap;

/**
 * @author cctyl
 */
public class AppSigner {

    private static final String APP_KEY = "1d8b6e7d45233436";
    private static final String APP_SEC = "560c52ccd288fed045859ed18bffd973";

    public static String appSign(Map<String, String> params) {
        // 为请求参数进行 APP 签名
        params.put("appkey", APP_KEY);
        // 按照 key 重排参数
        Map<String, String> sortedParams = new TreeMap<>(params);
        // 序列化参数
        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append('&');
            }
            queryBuilder
                    .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        String md5 =generateMD5(queryBuilder.append(APP_SEC).toString());
        return queryBuilder.append("&sign=").append(md5).toString();
    }

    private static String generateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            params.put(args[i++], args[i]);
        }
        System.out.println(appSign(params));
    }
}
