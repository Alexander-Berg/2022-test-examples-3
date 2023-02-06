package ru.yandex.direct.utils;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static ru.yandex.direct.utils.TimeConvertUtils.millisToSecond;

public class TimeConvertUtilsTest {

    @Test
    public void checkMillisToSecond() {
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(millisToSecond(0)).isEqualTo(0);
        softly.assertThat(millisToSecond(1_500)).isEqualTo(1.5);
        softly.assertThat(millisToSecond(12_345)).isEqualTo(12.345);
        softly.assertThat(millisToSecond(20_000)).isEqualTo(20);

        softly.assertAll();
    }
}
