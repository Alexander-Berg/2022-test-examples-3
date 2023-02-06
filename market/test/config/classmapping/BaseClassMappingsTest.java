package config.classmapping;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("/WEB-INF/push-api-class-mappings.xml")
public abstract class BaseClassMappingsTest {

    @Autowired
    protected HttpMessageConverter<Object> httpMessageConverter;

    public <T> T deserialize(Class<T> clazz, String string) throws IOException {
        return (T) httpMessageConverter.read(clazz, new MockHttpInputMessage(string.getBytes()));
    }

    public String serialize(Object object) throws IOException {
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        httpMessageConverter.write(object, MediaType.APPLICATION_XML, outputMessage);
        return outputMessage.getBodyAsString(StandardCharsets.UTF_8);
    }

    public void serializeAndCompare(Object object, String string) throws IOException, SAXException {
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        httpMessageConverter.write(object, MediaType.APPLICATION_XML, outputMessage);

        assertThat(
                new InputSource(new ByteArrayInputStream(outputMessage.getBodyAsBytes())),
                CompareMatcher.isIdenticalTo(new InputSource(new ByteArrayInputStream(string.getBytes())))
        );
    }
}
