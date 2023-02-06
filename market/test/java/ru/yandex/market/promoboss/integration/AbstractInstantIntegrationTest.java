package ru.yandex.market.promoboss.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.promoboss.service.TimeService;

import static org.mockito.Mockito.when;

public abstract class AbstractInstantIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    protected TimeService timeService;

    @BeforeEach
    void setUpNow() {
        when(timeService.getEpochSecond()).thenReturn(getNowEpochSecond());
    }

    protected abstract long getNowEpochSecond();
}
