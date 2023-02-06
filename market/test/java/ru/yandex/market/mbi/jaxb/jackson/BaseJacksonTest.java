package ru.yandex.market.mbi.jaxb.jackson;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xml.sax.SAXException;

/**
 * @author Denis Chernyshov
 */
@Ignore
public class BaseJacksonTest extends Assert {

    protected ObjectMapper jsonMapper;
    protected ObjectMapper xmlMapper;

    final ObjectMapper getJsonObjectMapper() {
        return jsonMapper;
    }

    final ObjectMapper getXmlObjectMapper() {
        return xmlMapper;
    }

    @Before
    public void setUp() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
    }

    public <T> void test(T obj, String expectedJson, String expectedXml) throws IOException, JSONException, SAXException {
        test(obj, obj, expectedJson, expectedXml);
    }

    public <T> void test(T original, T expected, String expectedJson, String expectedXml) throws IOException, JSONException, SAXException {
        String json = getJsonObjectMapper().writeValueAsString(original);
        System.out.println(json);
        JSONAssert.assertEquals(expectedJson, json, true);
        T actualObj = getJsonObjectMapper().readValue(json, (Class<T>) original.getClass());
        assertEquals(expected, actualObj);
        System.out.println("JSON validated");
        String xml = getXmlObjectMapper().writeValueAsString(original);
        System.out.println(xml);
        XMLAssert.assertXMLEqual(expectedXml, xml);
        actualObj = getXmlObjectMapper().readValue(xml, (Class<T>) original.getClass());
        assertEquals(expected, actualObj);
        System.out.println("XML validated");
    }

    public <T> void testDeserialization(T expected, String json, String xml) throws IOException, JSONException {
        T actualObj = getJsonObjectMapper().readValue(json, (Class<T>) expected.getClass());
        assertEquals(expected, actualObj);
        System.out.println(actualObj);
        System.out.println("JSON validated");
        actualObj = getXmlObjectMapper().readValue(xml, (Class<T>) expected.getClass());
        assertEquals(expected, actualObj);
        System.out.println(actualObj);
        System.out.println("XML validated");
    }
}
