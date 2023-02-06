package ru.yandex.mail.common.utils;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassPath {
    public static String fromClasspath(String path) throws IOException {
        try(final InputStream stream = ClassPath.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream, notNullValue());
            String result = IOUtils.toString(stream, Charsets.UTF_8);
            assertThat(result, notNullValue());
            return result;
        }
    }
}
