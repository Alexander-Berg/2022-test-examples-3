package ru.yandex.direct.utils.compress;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class GzipUtilsTest {
    @Test
    @Parameters({
            "test",
            "",
            "a little bit longer string with spaces and !@#$%^&*()-ations!",
            "немного по-русски!"
    })
    public void simpleStrings(String data) {
        assertThat(GzipUtils.decompressString(GzipUtils.compressString(data)))
                .isEqualTo(data);
    }

    @Parameters({
            "0", "32", "33", "894", "65536", "33000321"
    })
    @Test
    public void longRandomText(int size) {
        String data = RandomStringUtils.randomAscii(size);
        assertThat(GzipUtils.decompressString(GzipUtils.compressString(data)))
                .isEqualTo(data);
    }

    @Test
    public void manyCompressions() {
        int maxlen = 65536;
        String fullData = RandomStringUtils.randomAscii(maxlen);
        for (int i = 0; i < 1000; i++) {
            String data = fullData.substring(0, RandomUtils.nextInt(0, maxlen));
            assertThat(GzipUtils.decompressString(GzipUtils.compressString(data)))
                    .isEqualTo(data);
        }
    }
}
