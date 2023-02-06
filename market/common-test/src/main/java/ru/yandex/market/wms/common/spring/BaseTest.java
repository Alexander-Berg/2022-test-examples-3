package ru.yandex.market.wms.common.spring;

import java.util.TimeZone;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    private static final double EPS = 1E-9;

    protected SoftAssertions assertions;

    @BeforeEach
    public void setup() {
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @BeforeAll
    public static void setTestTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }

    protected void assertDoubleEquals(double actual, double expected) {
        assertions.assertThat(actual).isEqualTo(expected, Offset.offset(EPS));
    }
}
