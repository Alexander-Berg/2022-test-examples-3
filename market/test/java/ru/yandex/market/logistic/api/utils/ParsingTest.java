package ru.yandex.market.logistic.api.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.logistic.api.client.LogisticApiClientFactory;
import ru.yandex.market.logistic.api.model.fulfillment.wrap.test.FulfillmentWrapTest;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class ParsingTest<T> extends FulfillmentWrapTest {
    protected final ObjectMapper mapper;
    protected final Class<T> type;
    protected final String fileName;

    public ParsingTest(Class<T> type, String fileName) {
        this(LogisticApiClientFactory.createXmlMapper(), type, fileName);
    }

    public ParsingTest(ObjectMapper mapper, Class<T> type, String fileName) {
        this.mapper = mapper;
        this.type = type;
        this.fileName = fileName;
    }

    @Test
    public void testSerializationAndDeserialization() throws Exception {
        String expected = getFileContent(fileName);
        JavaType javaType = getType();
        T forward = getMapper().readValue(expected, javaType);

        String actual = getMapper().writeValueAsString(forward);
        checkRawStrings(expected, actual);

        T reverse = getMapper().readValue(actual, javaType);

        assertThat(forward)
            .as("Asserting that serialized and deserialized objects are the same")
            .isEqualToComparingFieldByFieldRecursively(reverse);
    }

    @Test
    public void testExtractedValues() throws Exception {
        JavaType javaType = getType();
        T object = getMapper().readValue(getFileContent(fileName), javaType);

        for (Map.Entry<String, Object> nameValuePair : fieldValues().entrySet()) {
            String fieldName = nameValuePair.getKey();
            Object fieldValue = nameValuePair.getValue();
            assertions().assertThat(object)
                .as("Asserting that object has valid value in [" + fieldName + "]")
                .hasFieldOrPropertyWithValue(fieldName, fieldValue);
        }

        performAdditionalAssertions(object);
    }

    protected JavaType getType() {
        return mapper.constructType(type);
    }

    protected String getFileContent(String fileName) throws IOException {
        return IOUtils.toString(getSystemResourceAsStream(fileName), StandardCharsets.UTF_8);
    }

    /**
     * For providing custom object mappers,that cannot be passed to constructor.
     */
    protected ObjectMapper getMapper() {
        return mapper;
    }


    /**
     * For field values assertions.
     */
    protected Map<String, Object> fieldValues() {
        return Collections.emptyMap();
    }

    /**
     * For additional non-field values assertions.
     */
    protected void performAdditionalAssertions(T t) {
    }

    protected void checkRawStrings(String expected, String actual) throws IOException, SAXException {
    }
}
