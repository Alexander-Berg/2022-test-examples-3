package ru.yandex.market.wms.shared.libs.async.retry.queuehelper.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.wms.shared.libs.async.retry.JmsRetry.MAX_RETRY_TIMES;

class ExpDelayTest {

    @Test
    void getDelayInSecReturnsCorrectValuesWhenRetriesValueLessThanOrEqualMaxRetryTimes() {
        Assertions.assertEquals(5, ExpDelay.getDelayInSec(0));
        Assertions.assertEquals(15, ExpDelay.getDelayInSec(1));
        Assertions.assertEquals(35, ExpDelay.getDelayInSec(2));
        Assertions.assertEquals(100, ExpDelay.getDelayInSec(3));
    }

    @Test
    void getDelayInSecReturnsMinimalValueWhenRetriesValueGreaterThanMaxRetryTimes() {
        Assertions.assertEquals(5, ExpDelay.getDelayInSec(MAX_RETRY_TIMES + 1));
    }

    @Test
    void getDelayInMillisReturnsCorrectValuesWhenRetriesValueLessThanOrEqualMaxRetryTimes() {
        Assertions.assertEquals(5000, ExpDelay.getDelayInMillis(0));
        Assertions.assertEquals(15000, ExpDelay.getDelayInMillis(1));
        Assertions.assertEquals(35000, ExpDelay.getDelayInMillis(2));
        Assertions.assertEquals(100000, ExpDelay.getDelayInMillis(3));
    }

    @Test
    void getDelayInMillisReturnsMinimalValueWhenRetriesValueGreaterThanMaxRetryTimes() {
        Assertions.assertEquals(5000, ExpDelay.getDelayInMillis(MAX_RETRY_TIMES + 1));
    }
}
