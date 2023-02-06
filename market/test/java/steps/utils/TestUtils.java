package steps.utils;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.io.IOUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule());

    private TestUtils() {
    }

    public static String readFile(String filePath) throws IOException {
        InputStream inputStream = TestUtils.class.getResourceAsStream(filePath);
        return IOUtils.toString(inputStream, UTF_8);
    }

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }
}
