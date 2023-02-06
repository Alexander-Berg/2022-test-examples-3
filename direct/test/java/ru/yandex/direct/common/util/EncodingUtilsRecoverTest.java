package ru.yandex.direct.common.util;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.util.EncodingUtils.doubleUtf8Encode;
import static ru.yandex.direct.common.util.EncodingUtils.recoverUtf8Decode;

@RunWith(Parameterized.class)
public class EncodingUtilsRecoverTest {
    @Parameterized.Parameter(0)
    public String testString;

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {""},
                {"Latin string\t "},
                {"москва"},
                {"б z!"},
        });
    }


    @Test
    public void recoverUtf8Decode_validUtf() {
        assertThat(recoverUtf8Decode(testString)).isEqualTo(testString);
    }

    @Test
    public void recoverUtf8Decode_doubleEncodedUtf() {
        String broken = doubleUtf8Encode(testString);
        assertThat(recoverUtf8Decode(broken)).isEqualTo(testString);
    }
}
