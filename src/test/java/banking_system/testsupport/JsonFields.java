package banking_system.testsupport;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonFields {

    private JsonFields() {
    }

    public static String string(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("String field '" + field + "' not found in: " + json);
        }
        return matcher.group(1);
    }

    public static BigDecimal decimal(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Numeric field '" + field + "' not found in: " + json);
        }
        return new BigDecimal(matcher.group(1));
    }
}