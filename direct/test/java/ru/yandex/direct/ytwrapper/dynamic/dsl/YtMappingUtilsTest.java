package ru.yandex.direct.ytwrapper.dynamic.dsl;

import java.math.BigDecimal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class YtMappingUtilsTest {

    @Test
    public void fromMicros_success() {
        BigDecimal actual = YtMappingUtils.fromMicros(123_456_789L);

        assertThat(actual)
                .describedAs("2 decimal digits in result of #fromMicros(123_456_789L)")
                .isEqualByComparingTo(BigDecimal.valueOf(123.46));
    }

}
