package ru.yandex.market.api.tests.feature.agencies;

import org.junit.After;
import org.junit.Before;
import ru.yandex.market.api.listener.expectations.HttpExpectations;

public class BaseTest {
    protected HttpExpectations httpExpectations = new HttpExpectations();

    @Before
    public void setUp() {
        httpExpectations.reset();
    }

    @After
    public void tearDown() {
        httpExpectations.verify();
    }
}
