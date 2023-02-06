package ru.yandex.market.logistics.cte.base;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class SoftAssertionsSupportedTest {
    protected SoftAssertions assertions;

    @BeforeEach
    public void prepareAssertions() {
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void verifyAssertions() {
        assertions.assertAll();
    }
}
