package ru.yandex.market.fulfillment.wrap.core;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

public abstract class ParsingTest<T> extends BaseIntegrationTest {

    protected final ObjectMapper mapper;
    protected final Class<T> type;
    protected final String fileName;

    protected ParsingTest(ObjectMapper mapper, Class<T> type, String fileName) {
        this.mapper = mapper;
        this.type = type;
        this.fileName = fileName;
    }

    @Test
    void testSerializationAndDeserialization() throws Exception {
        String expected = extractFileContent(fileName);
        T forward = getMapper().readValue(expected, type);

        String actual = getMapper().writeValueAsString(forward);
        checkRawStrings(expected, actual);

        T reverse = getMapper().readValue(actual, type);

        softly.assertThat(forward)
            .as("Asserting that serialized and deserialized objects are the same")
            .isEqualToComparingFieldByFieldRecursively(reverse);
    }

    @Test
    void testExtractedValues() throws Exception {
        T object = getMapper().readValue(extractFileContent(fileName), type);

        for (Map.Entry<String, Object> nameValuePair : fieldValues().entrySet()) {
            String fieldName = nameValuePair.getKey();
            Object fieldValue = nameValuePair.getValue();
            softly.assertThat(object)
                .as("Asserting that object has valid value in [" + fieldName + "]")
                .hasFieldOrPropertyWithValue(fieldName, fieldValue);
        }

        performAdditionalAssertions(object);
    }

    /**
     * For providing custom object mappers,that cannot be passed to constructor
     */
    protected ObjectMapper getMapper() {
        return mapper;
    }


    /**
     * For field values assertions
     */
    protected Map<String, Object> fieldValues() {
        return Collections.emptyMap();
    }

    /**
     * For additional non-field values assertions
     */
    protected void performAdditionalAssertions(T t) {
    }

    protected void checkRawStrings(String expected, String actual) throws IOException, SAXException {
    }
}
