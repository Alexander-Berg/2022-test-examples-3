package ru.yandex.calendar;

import java.io.File;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import ru.yandex.misc.io.file.FileInputStreamSource;

@UtilityClass
public class LoadFileUtils {
    @SneakyThrows
    public static FileInputStreamSource getResourceAsFileInputStreamSource(String name) {
        return new FileInputStreamSource(
                new File(LoadFileUtils.class
                                .getClassLoader()
                                .getResource(name)
                                .toURI()));
    }
}
