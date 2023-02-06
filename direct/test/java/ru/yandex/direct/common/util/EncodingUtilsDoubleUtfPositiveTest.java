package ru.yandex.direct.common.util;

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class EncodingUtilsDoubleUtfPositiveTest {
    @Parameterized.Parameter(0)
    public String correctString;

    @Parameterized.Parameter(1)
    public String brokenString;

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"", ""},
                {"Latin string\t ", "Latin string\t "},
                {"б z!", new String(DatatypeConverter.parseHexBinary("c390c2b1"), UTF_8) + " z!"},
        });
    }

    @Test
    public void doubleUtf8Encode() throws Exception {
        assertThat(EncodingUtils.doubleUtf8Encode(correctString)).isEqualTo(brokenString);
    }

    @Test
    public void doubleUtf8Decode() throws Exception {
        assertThat(EncodingUtils.doubleUtf8Decode(brokenString)).isEqualTo(correctString);
    }
}
