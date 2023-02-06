package ru.yandex.market.core.history;

import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.InputSource;

import static org.junit.Assert.assertEquals;

/**
 * @author ashevenkov
 */
public class SnapshotHandlerTest {

    private static final String SNAPSHOT = "<snapshot><aaa>bbb</aaa></snapshot>";

    @Test
    public void testParse() throws Exception {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        SnapshotSaxHandler handler = new SnapshotSaxHandler();
        saxParser.parse(new InputSource(new StringReader(SNAPSHOT)), handler);
        Map<String, String> map = handler.getResult();
        assertEquals(map.size(), 1);
        assertEquals(map.get("aaa"), "bbb");
    }
}
