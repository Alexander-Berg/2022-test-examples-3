package ru.yandex.market.api.partner.controllers.deserialization;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.view.json.PartnerJacksonMessageConverter;
import ru.yandex.market.checkout.common.xml.ClassMappingXmlMessageConverter;
import ru.yandex.market.common.test.SerializationChecker;

@Ignore
@ContextConfiguration("../serialization/BaseOldSerializationTest.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class BaseOldDeserializationTest {
    private static final String EXTENSION_DELIMITER = "_";
    private static final String UTF8 = "UTF-8";

    @Autowired
    PartnerJacksonMessageConverter partnerJsonMessageConverter;

    @Autowired
    ClassMappingXmlMessageConverter partnerApiXmlMessageConverter;

    private SerializationChecker checker;

    @Before
    public void setUp() throws Exception {
        checker = new SerializationChecker(null,
//                obj -> getString(obj, APPLICATION_JSON, partnerJsonMessageConverter),
                (str, classObj) -> getObject(str, classObj, partnerJsonMessageConverter),
//                obj -> getString(obj, APPLICATION_XML, partnerApiXmlMessageConverter),
                null,
                (str, classObj) -> getObject(str, classObj, partnerApiXmlMessageConverter)
        );
    }

    public SerializationChecker getChecker() {
        return checker;
    }


    private Object getObject(String inputString, Class objectClass, HttpMessageConverter deserializer) {
        MockHttpInputMessage msg = new MockHttpInputMessage(inputString.getBytes(StandardCharsets.UTF_8));
        try {
            return deserializer.read(objectClass, msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void test(T original,
                         T expected,
                         String expectedJson,
                         String expectedXml) throws IOException, JSONException, SAXException {
        checker.test(original, expected, expectedJson, expectedXml);
    }

    public <T> void testDeserialization(T expected,
                                        String inputJson,
                                        String inputXml) throws IOException, JSONException, SAXException {
        checker.testDeserialization(expected, inputJson, inputXml);
    }

    public String getContentAsString(String name) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(this.getClass().getSimpleName() + EXTENSION_DELIMITER + name), UTF8);
    }
}
