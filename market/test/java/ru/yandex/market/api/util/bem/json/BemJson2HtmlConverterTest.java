package ru.yandex.market.api.util.bem.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.assertEquals;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class BemJson2HtmlConverterTest extends UnitTestBase {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldProcessString() throws Exception {
        assertEquals("Some test string", convert("\"Some test string\""));
    }

    @Test
    public void shouldProcessStringWithHtmlSymbols() throws Exception {
        assertEquals("&lt;this is not a &quot;html&quot; tag&gt;", convert("\"<this is not a \\\"html\\\" tag>\""));
    }

    @Test
    public void shouldProcessArray() throws Exception {
        assertEquals("abc", convert("[\"a\", \"b\", \"c\"]"));
    }

    @Test
    public void shouldProcessSimpleTag() throws Exception {
        assertEquals("<br/>", convert("{\"tag\":\"br\"}"));
    }

    @Test
    public void shouldProcessTagWithContent() throws Exception {
        assertEquals("<b>Test</b>", convert("{\"tag\":\"b\",\"content\":\"Test\"}"));
    }

    @Test
    public void shouldProcessTagWithComplexContent() throws Exception {
        assertEquals(
            "<table border=\"1\"><tbody><tr><td><i>Head_1</i></td><td><i>Head_2</i></td></tr><tr><td><i>Content_1</i></td><td>Content_2(<b>notabene</b>)</td></tr></tbody></table>",
            convert("{\"tag\":\"table\",\"content\":[\"\",{\"tag\":\"tbody\",\"content\":[{\"tag\":\"tr\",\"content\":[{\"tag\":\"td\",\"content\":{\"tag\":\"i\",\"content\":\"Head_1\"}},{\"tag\":\"td\",\"content\":{\"tag\":\"i\",\"content\":\"Head_2\"}}]},{\"tag\":\"tr\",\"content\":[{\"tag\":\"td\",\"content\":{\"tag\":\"i\",\"content\":\"Content_1\"}},{\"tag\":\"td\",\"content\":[\"Content_2(\",{\"tag\":\"b\",\"content\":\"notabene\"},\")\"]}]}]}],\"attrs\":{\"border\":\"1\"}}")
        );
    }

    @Test
    public void shouldProcessTagWithAttributes() throws Exception {
        assertEquals(
            "<a href=\"http://dns.yandex.ru\" target=\"_blank\">Яндекс.DNS</a>",
            convert("{\"tag\":\"a\",\"content\":\"Яндекс.DNS\",\"attrs\":{\"href\":\"http://dns.yandex.ru\",\"target\":\"_blank\"}}")
        );
    }

    private String convert(String s) throws IOException {
        return BemJson2HtmlConverter.convert(
            objectMapper.readTree(s)
        );
    }
}
