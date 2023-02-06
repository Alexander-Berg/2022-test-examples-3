package ru.yandex.market.api.partner.controllers.serialization;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.view.json.PartnerJacksonMessageConverter;
import ru.yandex.market.checkout.common.xml.ClassMappingXmlMessageConverter;
import ru.yandex.market.common.test.SerializationChecker;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Базовый класс для тестирования старой ручной сериализации
 *
 * @author zoom
 */
@SpringJUnitConfig(locations = "BaseOldSerializationTest.xml")
public abstract class BaseOldSerializationTest {
    private static final String EXTENSION_DELIMITER = "_";
    private static final String UTF8 = "UTF-8";

    @Autowired
    PartnerJacksonMessageConverter partnerJsonMessageConverter;

    @Autowired
    ClassMappingXmlMessageConverter partnerApiXmlMessageConverter;

    private SerializationChecker checker;

    @BeforeEach
    public void setUp() {
        checker = new SerializationChecker(
                obj -> getString(obj, APPLICATION_JSON, partnerJsonMessageConverter),
                null,
                obj -> getString(obj, APPLICATION_XML, partnerApiXmlMessageConverter),
                null
        );
    }

    protected final SerializationChecker getChecker() {
        return checker;
    }

    private String getString(Object obj, MediaType mediaType, HttpMessageConverter serializer) {
        MockHttpOutputMessage msg = new MockHttpOutputMessage();
        try {
            serializer.write(obj, mediaType, msg);
            return msg.getBodyAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public String getContentAsString(String name) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(this.getClass().getSimpleName() + EXTENSION_DELIMITER + name), UTF8);
    }

}
