package ru.yandex.market.logistic.gateway.common.model.delivery;


import java.util.Set;

import org.junit.Test;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertTrue;

public class OrderStatusTypeMappingTest {
    @Test
    public void isSubsetOfLogisticApiStatuses() {
        Set<Integer> logisticApiStatusCodes =
            stream(ru.yandex.market.logistic.api.model.common.OrderStatusType.values())
                .map(ru.yandex.market.logistic.api.model.common.OrderStatusType::getCode)
                .collect(toSet());

        Set<Integer> lgwDsStatusCodes = stream(OrderStatusType.values()).map(OrderStatusType::getCode).collect(toSet());

        assertTrue(
            "Lgw delivery service order statuses isn't a subset of logistic api statuses",
            logisticApiStatusCodes.containsAll(lgwDsStatusCodes)
        );
    }
}
