package ru.yandex.market.robot.xml;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author mkrasnoperov
 */
public class XmlParserTest {

    private String xmlifyString(String data) throws IOException, JSONException {
        InputStream stream = new ByteArrayInputStream(data.getBytes());
        stream = XmlParser.xmlifyStream(stream);
        return IOUtils.toString(stream);
    }

    @Test
    public void testXmlifyStreamWithObject() throws Exception {
        String data = "   {\"key\": \"value\", \"array_key\": [1, 2, 3]}";
        String xmlified = xmlifyString(data);
        assertEquals("<?xml version=\"1.0\"?><root><key>value</key><array_key>1</array_key>" +
            "<array_key>2</array_key><array_key>3</array_key></root>", xmlified);
    }

    @Test
    public void testXmlifyStreamWithArray() throws Exception {
        String data = "\n[\"a\", \"b\", \"c\"]";
        String xmlified = xmlifyString(data);
        assertEquals("<?xml version=\"1.0\"?><root><array_wrapper>a</array_wrapper><array_wrapper>b</array_wrapper>" +
            "<array_wrapper>c</array_wrapper></root>", xmlified);
    }

    @Test
    public void testXmlifyStreamWithXml() throws Exception {
        String data = "<?xml version=\"1.0\"?><root><array_wrapper>aaa</array_wrapper><array_wrapper>b</array_wrapper>" +
            "<array_wrapper>c</array_wrapper></root>";
        String xmlified = xmlifyString(data);
        assertEquals("<?xml version=\"1.0\"?><root><array_wrapper>aaa</array_wrapper><array_wrapper>b</array_wrapper>" +
            "<array_wrapper>c</array_wrapper></root>", xmlified);
    }


    @Test(expected = org.json.JSONException.class)
    public void testXmlifyStreamWithWrongJson() throws Exception {
        String data = "{illegal: json, []}";
        String xmlified = xmlifyString(data);
        assertNotNull(xmlified);
    }

    @Test
    public void testMinusReplace() throws Exception {
        String data = "{\"-192\" : \"-123\", \"-x1\": [1, 2, 3]}";
        String xmlified = xmlifyString(data);
        assertEquals("<?xml version=\"1.0\"?><root><prefix-192>-123</prefix-192><prefix-x1>1</prefix-x1>" +
            "<prefix-x1>2</prefix-x1><prefix-x1>3</prefix-x1></root>", xmlified);
    }
}
