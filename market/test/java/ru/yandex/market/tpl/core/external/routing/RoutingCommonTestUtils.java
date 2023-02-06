package ru.yandex.market.tpl.core.external.routing;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import ru.yandex.market.tpl.common.util.TplObjectMappers;
import ru.yandex.market.tpl.core.external.routing.api.RoutingTimeMultiplierUtil;

@UtilityClass
public class RoutingCommonTestUtils {
    private final static ObjectMapper objectMapper = TplObjectMappers.TPL_DB_OBJECT_MAPPER;

    public static <T> T mapFromResource(String filename, Class<T> valueType) throws Exception {
        String rawResponse = IOUtils.toString(RoutingTimeMultiplierUtil.class.getResourceAsStream(filename),
                StandardCharsets.UTF_8.name());
        return objectMapper.readValue(rawResponse, valueType);
    }

    public static String readResourceAsString(String fileName) throws Exception {
        return IOUtils.toString(RoutingTimeMultiplierUtil.class.getResourceAsStream(fileName), StandardCharsets.UTF_8.name());
    }
}
