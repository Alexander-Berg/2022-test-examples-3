package ru.yandex.market.api.partner.controllers.deserialization;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.view.JacksonMapperConfig;
import ru.yandex.market.common.test.SerializationChecker;

/**
 * Базовый класс для тестирования новой автоматической сериализации
 *
 * @author zoom
 */
@Ignore
@ContextConfiguration(classes = {JacksonMapperConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class BaseJaxbDeserializationTest {

    @Autowired
    protected ObjectMapper jsonMapper;

    @Autowired
    protected ObjectMapper xmlMapper;

    private SerializationChecker checker;

    @Before
    public void setUp() throws Exception {

        checker = new SerializationChecker(
                null,
                (str, classObj) -> {
                    try {
                        return jsonMapper.readValue(str, classObj);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                null,
                (str, classObj) -> {
                    try {
                        return xmlMapper.readValue(str, classObj);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public <T> void test(T original,
                         T expected,
                         String expectedJson,
                         String expectedXml) throws IOException, JSONException, SAXException {
        checker.test(original, expected, expectedJson, expectedXml);
    }

    public <T> void testDeserialization(T expected,
                                        String originalJson,
                                        String originalXml) throws IOException, JSONException, SAXException {
        checker.testDeserialization(expected, originalJson, originalXml);
    }

    public <T> void testXmlDeserialization(T expected,
                                           String originalXml) throws IOException, JSONException, SAXException {
        checker.testXmlDeserialization(expected, originalXml);
    }

    public <T> void testJsonDeserialization(T expected,
                                            String originalJson) throws IOException, JSONException, SAXException {
        checker.testJsonDeserialization(expected, originalJson);
    }

}
