package ru.yandex.market.api.controller.jackson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.api.controller.serialization.StringCodec;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.codecs.Base64Codec;
import ru.yandex.market.api.util.codecs.DefaultCodec;
import ru.yandex.market.api.util.codecs.StringCodecStorageConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * @author Denis Chernyshov
 */
@Ignore
public class BaseJacksonTest extends UnitTestBase {

    private ObjectMapperFactory objectMapperFactory;

    @Before
    public void setUp() throws Exception {
        StringCodecStorageConfiguration storageConfiguration = new StringCodecStorageConfiguration();
        Map<String, StringCodec> codecs = new HashMap<>(
                storageConfiguration.getCodecs(
                        null,
                        null,
                        new Base64Codec(),
                        new DefaultCodec()
                )
        );
        codecs.put("reverse", new StringCodec() {
            @Override
            public String decode(String value) {
                return new StringBuilder(value).reverse().toString();
            }

            @Override
            public String encode(String value) {
                return new StringBuilder(value).reverse().toString();
            }
        });

        objectMapperFactory = new CapiObjectMapperFactory(storageConfiguration.stringCodecStorage(codecs));
    }

    final ObjectMapper getJsonObjectMapper() {
        return objectMapperFactory.getJsonObjectMapper();
    }

    final ObjectMapper getXmlObjectMapper() {
        return objectMapperFactory.getXmlObjectMapper();
    }

    public <T> void test(T obj, String expectedJson, String expectedXml) throws IOException, JSONException {
        String json = getJsonObjectMapper().writeValueAsString(obj);
        System.out.println(json);
        JSONAssert.assertEquals(expectedJson, json, true);
        T actualObj = getJsonObjectMapper().readValue(json, (Class<T>) obj.getClass());
        assertEquals(obj, actualObj);
        System.out.println("JSON validated");
        String xml = getXmlObjectMapper().writeValueAsString(obj);
        System.out.println(xml);
        assertEquals(expectedXml, xml);
        actualObj = getXmlObjectMapper().readValue(xml, (Class<T>) obj.getClass());
        assertEquals(obj, actualObj);
        System.out.println("XML validated");
    }
}
