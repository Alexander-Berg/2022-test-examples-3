package ru.yandex.market.tpl.integration.tests.utils;

import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

@UtilityClass
public class TplTestUtils {

    @SneakyThrows
    public static String readRequestAsString(String fileName, Object... args) {
        String rawInput = IOUtils.toString(TplTestUtils.class.getResourceAsStream(fileName), StandardCharsets.UTF_8);
        if (args.length > 0) {
            rawInput = String.format(rawInput, args);
        }

        return rawInput;
    }
}
