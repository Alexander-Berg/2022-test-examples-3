package ru.yandex.market.partner.mvc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.mbi.web.converter.MbiHttpMessageConverter;

/**
 * @author fbokovikov
 */
@Configuration
public class MvcTestSerializationConfig {

    @Bean
    public MbiHttpMessageConverter jsonConverter() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        new PartnerMvcConfig(null, null,
                null, null, null).configureMessageConverters(converters);
        return (MbiHttpMessageConverter) converters.get(0);
    }

    @Bean
    public SerializationChecker jsonSerializationChecker(MbiHttpMessageConverter jsonConverter) {
        return new SerializationChecker(
                obj -> {
                    try {
                        HttpOutputMessage msg = new MockHttpOutputMessage();
                        jsonConverter.write(obj, (MediaType) jsonConverter.getSupportedMediaTypes().get(0), msg);
                        ByteArrayOutputStream baos = (ByteArrayOutputStream) msg.getBody();
                        return new String(baos.toByteArray());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                null,
                obj -> SerializationChecker.XML_STUB,
                null
        );
    }
}
