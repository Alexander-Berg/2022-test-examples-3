package ru.yandex.market.admin.mvc;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.common.test.SerializationChecker;

/**
 * @author fbokovikov
 */
@Configuration
public class SerializationConfig {

    @Bean
    public SerializationChecker jsonSerializationChecker() {
        return new SerializationChecker(
                obj -> SerializationChecker.JSON_STUB,
                null,
                obj -> {
                    try {
                        JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
                        StringWriter stringWriter = new StringWriter();
                        Marshaller marshaller = jaxbContext.createMarshaller();
                        marshaller.marshal(obj, stringWriter);
                        return stringWriter.toString();
                    } catch (JAXBException e) {
                        throw new RuntimeException(e);
                    }
                },
                null
        );
    }
}
