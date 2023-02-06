package ru.yandex.market.pvz.core;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@UtilityClass
public class TestUtils {

    @SneakyThrows
    public static String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public static byte[] getFileContentInBytes(String filename) {
        return IOUtils.toByteArray(Objects.requireNonNull(getSystemResourceAsStream(filename)));
    }

}
