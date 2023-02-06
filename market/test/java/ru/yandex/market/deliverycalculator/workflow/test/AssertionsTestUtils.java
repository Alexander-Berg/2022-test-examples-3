package ru.yandex.market.deliverycalculator.workflow.test;

import org.junit.jupiter.api.Assertions;

import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.workflow.util.FeedParserWorkflowUtils;

public final class AssertionsTestUtils {

    private AssertionsTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void assertDeliveryOptionEquals(DeliveryCalcProtos.DeliveryOption expected,
                                                  DeliveryCalcProtos.DeliveryOption actual) {
        Assertions.assertEquals(expected.getDeliveryCost(), actual.getDeliveryCost());
        Assertions.assertEquals(expected.getMinDaysCount(), actual.getMinDaysCount());
        Assertions.assertEquals(expected.getMaxDaysCount(), actual.getMaxDaysCount());
        Assertions.assertEquals(expected.getOrderBefore(), actual.getOrderBefore());
        Assertions.assertEquals(expected.hasShopDeliveryCost(), actual.hasShopDeliveryCost());
        Assertions.assertEquals(expected.getShopDeliveryCost(), actual.getShopDeliveryCost());
    }

    public static DeliveryCalcProtos.DeliveryOption buildDeliveryOptionFromRequiredParams(long deliveryCost, int minDays,
                                                                                          int maxDays, int orderBefore) {
        return DeliveryCalcProtos.DeliveryOption.newBuilder()
                .setDeliveryCost(deliveryCost)
                .setMinDaysCount(minDays)
                .setMaxDaysCount(maxDays)
                .setOrderBefore(orderBefore)
                .build();
    }

    public static DeliveryCalcProtos.DeliveryOption buildDeliveryOptionFromRequiredParams(long deliveryCost, int minDays, int maxDays) {
        return buildDeliveryOptionFromRequiredParams(deliveryCost, minDays, maxDays, FeedParserWorkflowUtils.DEFAULT_ORDER_BEFORE);
    }

    public static DeliveryCalcProtos.DeliveryOption buildDeliveryOption(long deliveryCost, int minDays, int maxDays,
                                                                        int orderBefore, long shopDeliveryCost) {
        return DeliveryCalcProtos.DeliveryOption.newBuilder()
                .setDeliveryCost(deliveryCost)
                .setMinDaysCount(minDays)
                .setMaxDaysCount(maxDays)
                .setOrderBefore(orderBefore)
                .setShopDeliveryCost(shopDeliveryCost)
                .build();
    }
}
