package ru.yandex.market.mbo.cms.core.utils;

import com.amazonaws.util.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

/**
 * @author commince
 * @date 09.11.2018
 */
public class XssEscapeHelperTest {

    private static final String XSS_STRING = "<script";
    private static final String XSS_HTML = "<p>123</p><br />" + XSS_STRING;
    private static final String NO_SANITIZE_NEEDED_JSON = "{" +
            "  \"sanitize_inner\": true," +
            "  \"xssfield\": \"<script>xss</script>\"," +
            "  \"dummyfield\": \"123\"," +
            "  \"inner\": {" +
            "    \"sanitize_inner\": false," +
            "    \"xssfield\": \"<br /> Русский текст\"," +
            "    \"dummyfield\": \"456\"," +
            "    \"dummyarray\": [\"123\", \"456\", \"789\"]," +
            "    \"inner\": {" +
            "      \"sanitize_inner\": true," +
            "      \"xssfield\": \"<script>xss</script>\"," +
            "      \"dummyfield\": \"456\"," +
            "      \"dummyarray\": [\"123\", \"456\", \"789\"]" +
            "    }" +
            "  }" +
            "}";

    private static final String SANITIZE_NEEDED_JSON = "{" +
            "  \"sanitize_inner\": true," +
            "  \"xssfield\": \"<script>xss</script>\"," +
            "  \"dummyfield\": \"123\"," +
            "  \"inner\": {" +
            "    \"sanitize_inner\": false," +
            "    \"xssfield\": \"<script>xss</script><br /> Русский текст\"," +
            "    \"dummyfield\": \"456\"," +
            "    \"dummyarray\": [\"123\", \"456\", \"789\"]," +
            "    \"inner\": {" +
            "      \"sanitize_inner\": true," +
            "      \"xssfield\": \"<script>xss</script>\"," +
            "      \"dummyfield\": \"456\"," +
            "      \"dummyarray\": [\"123\", \"456\", \"789\"]" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void testEraseXssInHtml() throws Exception {
        assertNotEquals(XSS_HTML, XssEscapeHelper.escapeString(XSS_HTML));
        Assert.assertEquals(XSS_HTML.replace(XSS_STRING, ""), XssEscapeHelper.escapeString(XSS_HTML));
    }

    @Test
    public void testNoChangeIfNoMatch() throws Exception {
        assertEquals(new JSONObject(NO_SANITIZE_NEEDED_JSON),
                new JSONObject(XssEscapeHelper.escapeJson(NO_SANITIZE_NEEDED_JSON,
                        o -> o.has("sanitize_inner") && o.get("sanitize_inner").equals(true), "inner.xssfield")));
    }

    @Test
    public void testEraseXssTags() throws Exception {
        assertEquals(new JSONObject(NO_SANITIZE_NEEDED_JSON),
                new JSONObject(XssEscapeHelper.escapeJson(SANITIZE_NEEDED_JSON,
                        o -> o.has("sanitize_inner") && o.get("sanitize_inner").equals(true), "inner.xssfield")));
    }

    private void assertEquals(JSONObject expected, JSONObject actual) {
        Assert.assertEquals(expected.toString(), actual.toString());
    }
}
