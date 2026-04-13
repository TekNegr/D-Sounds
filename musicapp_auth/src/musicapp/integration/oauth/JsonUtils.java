package musicapp.integration.oauth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtils {
    private JsonUtils() {}

    public static String extractString(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? unescape(m.group(1)) : "";
    }

    public static long extractLong(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0L;
    }

    public static String unescape(String s) {
        return s.replace("\\/", "/").replace("\\n", "\n").replace("\\\"", "\"");
    }
}
