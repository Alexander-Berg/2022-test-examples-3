package ru.yandex.market.api.admin.external.javasec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.api.admin.external.javasec.response.JavaSecResponse;
import ru.yandex.market.api.admin.serialization.CustomObjectMapperFactory;
import ru.yandex.market.api.admin.serialization.CustomXmlDeserializer;

public class JavaSecClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(JavaSecClientTest.class);

    CustomObjectMapperFactory objectMapperFactory = new CustomObjectMapperFactory();
    CustomXmlDeserializer xmlDeserializer = new CustomXmlDeserializer(objectMapperFactory);

    @Test
    public void testResponseParsing() throws IOException {
        byte[] response = getResponse("java-sec-response.xml");
        LOG.info("response: {}", new String(response, StandardCharsets.UTF_8));
        JavaSecResponse parsed = xmlDeserializer.readObject(JavaSecResponse.class, response);
        LOG.info("parsed: {}", parsed);
        Assert.assertNotNull(parsed);
        Assert.assertNotNull(parsed.result);
        Assert.assertTrue(parsed.result.value);
    }

    private byte[] getResponse(String resourceName) throws IOException {
        try (InputStream in = JavaSecClientTest.class.getResourceAsStream(resourceName)) {
            return IOUtils.toByteArray(in);
        }
    }

}
