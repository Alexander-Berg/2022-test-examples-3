package ru.yandex.market.notifier.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.application.AbstractServicesTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zagidullinri
 * @date 05.02.2022
 */
public class RetryRelaxerTest extends AbstractServicesTestBase {

    @Autowired
    private RetryRelaxer retryRelaxer;

    @ParameterizedTest
    @CsvSource({"1,60000","3600001,300000","28800000,900000"})
    public void getRetryTimeoutShouldReturnCorrectTimeout(long failingForMillis, long expectedRetryTimeout){
        Long retryTimeout = retryRelaxer.getRetryTimeout(failingForMillis);

        assertEquals(expectedRetryTimeout, retryTimeout);
    }

}
