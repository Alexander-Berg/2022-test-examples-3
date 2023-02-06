package ru.yandex.market.logistics.yard.base;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;

public abstract class SoftAssertionSupport {
    protected final SoftAssertions softly = new SoftAssertions();

    @AfterEach
    public final void triggerSoftAssertions() {
        softly.assertAll();
    }
}
