package ru.yandex.market.api.partner.controllers.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
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
@ContextConfiguration(classes = {JacksonMapperConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class BaseJaxbSerializationTest {

    @Autowired
    ObjectMapper jsonMapper;

    @Autowired
    ObjectMapper xmlMapper;

    private SerializationChecker checker;

    @Before
    public void setUp() throws Exception {

        checker = new SerializationChecker(
                obj -> {
                    try {
                        return jsonMapper.writeValueAsString(obj);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                },
                null,
                obj -> {
                    try {
                        return xmlMapper.writeValueAsString(obj);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                },
                null
        );
    }

    public <T> void test(T original,
                         T expected,
                         String expectedJson,
                         String expectedXml) throws IOException, SAXException {
        checker.test(original, expected, expectedJson, expectedXml);
    }

    public <T> void testSerialization(T original,
                                      String expectedJson,
                                      String expectedXml) {
        checker.testSerialization(original, expectedJson, expectedXml);
    }
}
