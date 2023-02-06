package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import ru.yandex.market.common.report.model.AbstractDeliveryResult;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryRouteResult;

/**
 * @author mmetlov
 */
public abstract class DeliveryResultProvider {

    public static final long WEIGHT = 10L;
    public static final long WIDTH = 20L;
    public static final long HEIGHT = 30L;
    public static final long DEPTH = 40L;
    public static final int SHIPMENT_DAY = 3;
    public static final String ROUTE = "{\"route\":111,\"this\":\"is\"}";

    private DeliveryResultProvider() {
    }

    private static void fillDeliveryResult(AbstractDeliveryResult abstractDeliveryResult,
                                           BigDecimal weight,
                                           BigDecimal width,
                                           BigDecimal height,
                                           BigDecimal depth) {
        abstractDeliveryResult.setWeight(weight);
        abstractDeliveryResult.setDimensions(Arrays.asList(width, height, depth));
    }

    public static ActualDeliveryResult getActualDeliveryResult() {
        ActualDeliveryResult actualDeliveryResult = new ActualDeliveryResult();
        fillDeliveryResult(
                actualDeliveryResult,
                BigDecimal.valueOf(WEIGHT),
                BigDecimal.valueOf(WIDTH),
                BigDecimal.valueOf(HEIGHT),
                BigDecimal.valueOf(DEPTH));
        actualDeliveryResult.setDelivery(Collections.emptyList());
        return actualDeliveryResult;
    }

    public static DeliveryRouteResult getDeliveryRouteResult() {
        DeliveryRouteResult deliveryRouteResult = new DeliveryRouteResult();
        fillDeliveryResult(
                deliveryRouteResult,
                BigDecimal.valueOf(WEIGHT),
                BigDecimal.valueOf(WIDTH),
                BigDecimal.valueOf(HEIGHT),
                BigDecimal.valueOf(DEPTH));
        deliveryRouteResult.setRoute(ROUTE);
        return deliveryRouteResult;
    }
}
