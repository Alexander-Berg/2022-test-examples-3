package ru.yandex.market.logistic.api.model.fulfillment.wrap.test;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class FulfillmentWrapTest {

    private SoftAssertions assertions;

    @BeforeEach
    public void initSoftAssertions() {
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerSoftAssertions() {
        assertions.assertAll();
    }

    protected SoftAssertions assertions() {
        return assertions;
    }
}
