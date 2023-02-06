package ru.yandex.market.logistic.gateway;

import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
abstract public class BaseTest {

    protected final SoftAssertions assertions = new SoftAssertions();

    @After
    public void tearDown() {
        assertions.assertAll();
    }
}
