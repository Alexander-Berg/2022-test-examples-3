package ru.yandex.market.common.test;

import org.json.JSONException;
import org.junit.Ignore;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Утилитный класс, позволяющий протестировать xml/json-сериализацию без большого количества инфраструктурного кода.
 *
 * @author zoom
 */
@Ignore
public class SerializationChecker {

    public static final String XML_STUB = "<xml-stub></xml-stub>";
    public static final String JSON_STUB = "{\"jsonStub\":\"\"}";

    private final Function<Object, String> jsonSerializer;
    private final BiFunction<String, Class, Object> jsonDeserializer;

    private final Function<Object, String> xmlSerializer;
    private final BiFunction<String, Class, Object> xmlDeserializer;

    public SerializationChecker(Function<Object, String> jsonSerializer,
                                BiFunction<String, Class, Object> jsonDeserializer,
                                Function<Object, String> xmlSerializer,
                                BiFunction<String, Class, Object> xmlDeserializer) {
        this.jsonSerializer = jsonSerializer;
        this.jsonDeserializer = jsonDeserializer;
        this.xmlSerializer = xmlSerializer;
        this.xmlDeserializer = xmlDeserializer;
    }

    /**
     * Проверяет сериализацию объекта <code>original</code> в json/xml, так и десериализацию json/xml обратно в объект.
     */
    public <T> void test(T original,
                         T expected,
                         String expectedJson,
                         String expectedXml) throws IOException, JSONException, SAXException {
        String json = jsonSerializer.apply(original);
        System.out.println(json);
        JSONAssert.assertEquals(expectedJson, json, true);
        T actualObj = (T) jsonDeserializer.apply(json, original.getClass());
        assertEquals(expected, actualObj);
        System.out.println("JSON validated");
        String xml = xmlSerializer.apply(original);
        System.out.println(xml);
        assertXmlSimilar(expectedXml, xml);
        actualObj = (T) xmlDeserializer.apply(xml, original.getClass());
        assertEquals(expected, actualObj);
        System.out.println("XML validated");
    }

    /**
     * Проверяет десериализацию объекта из json/xml.
     */
    public <T> void testDeserialization(T expected, String json, String xml) {
        T actualObj = (T) jsonDeserializer.apply(json, expected.getClass());
        assertEquals(expected, actualObj);
        System.out.println(actualObj);
        System.out.println("JSON validated");
        actualObj = (T) jsonDeserializer.apply(xml, expected.getClass());
        assertEquals(expected, actualObj);
        System.out.println(actualObj);
        System.out.println("XML validated");
    }

    /**
     * Проверяет десериализацию объекта из json.
     */
    public <T> void testJsonDeserialization(T expected, String json) {
        T actualObj = (T) jsonDeserializer.apply(json, expected.getClass());
        assertEquals(expected, actualObj);
        System.out.println(actualObj);
        System.out.println("JSON validated");
    }

    /**
     * Проверяет десериализацию объекта из xml.
     */
    public <T> void testXmlDeserialization(T expected, String xml) {
        T actualObj = (T) xmlDeserializer.apply(xml, (Class<T>) expected.getClass());
        assertEquals(expected, actualObj);
        System.out.println(actualObj);
        System.out.println("XML validated");
    }

    /**
     * Проверяет сериализацию объекта в json/xml.
     */
    public <T> void testSerialization(T original,
                                      String expectedJson,
                                      String expectedXml,
                                      boolean verbose,
                                      boolean strictOrder) {
        try {
            String json = jsonSerializer.apply(original);
            if (verbose) {
                System.out.println(json);
            }
            JSONAssert.assertEquals(expectedJson, json, strictOrder ? JSONCompareMode.STRICT : JSONCompareMode.LENIENT);
            if (verbose) {
                System.out.println("JSON validated");
            }
            String xml = xmlSerializer.apply(original);
            if (verbose) {
                System.out.println(xml);
            }
            assertXmlSimilar(expectedXml, xml);
            if (verbose) {
                System.out.println("XML validated");
            }
        } catch (JSONException jsonEx) {
            throw new RuntimeException(jsonEx);
        }

    }

    private void assertXmlSimilar(String expected, String actual) {
        Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreWhitespace()
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    public <T> void testSerialization(T original,
                                      String expectedJson,
                                      String expectedXml) {
        testSerialization(original, expectedJson, expectedXml, true, true);
    }

    public <T> void testSerializationWithNonStrictOrder(
            T original,
            String expectedJson,
            String expectedXml
    ) {
        testSerialization(original, expectedJson, expectedXml, true, false);
    }

    /**
     * Проверяет сериализацию объекта только в json.
     */
    public <T> void testJsonSerialization(T original, String expectedJson) {
        testSerialization(original, expectedJson, XML_STUB);
    }

    /**
     * Проверяет сериализацию объекта только в XML.
     */
    public <T> void testXmlSerialization(T original, String expectedXml) {
        testSerialization(original, JSON_STUB, expectedXml);
    }
}
