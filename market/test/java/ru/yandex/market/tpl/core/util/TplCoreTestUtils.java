package ru.yandex.market.tpl.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.jeasy.random.EasyRandom;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class TplCoreTestUtils {
    private final static ObjectMapper objectMapper = ObjectMappers.TPL_DB_OBJECT_MAPPER;
    public final static EasyRandom OBJECT_GENERATOR = new EasyRandom();

    public static <T> T mapFromResource(String filename, Class<T> valueType) throws Exception {
        String rawResponse = IOUtils.toString(TplCoreTestUtils.class.getResourceAsStream(filename),
                StandardCharsets.UTF_8);
        return objectMapper.readValue(rawResponse, valueType);
    }

    public static String readResourceAsString(String fileName) throws Exception {
        return IOUtils.toString(TplCoreTestUtils.class.getResourceAsStream(fileName), StandardCharsets.UTF_8);
    }
}
