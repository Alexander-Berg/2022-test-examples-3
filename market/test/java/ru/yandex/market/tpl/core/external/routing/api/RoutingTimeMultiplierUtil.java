package ru.yandex.market.tpl.core.external.routing.api;

import java.math.BigDecimal;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RoutingTimeMultiplierUtil {
    public static final RoutingTimeMultiplier DEFAULT_CAR = new RoutingTimeMultiplier(
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
    );
    public static final RoutingTimeMultiplier DEFAULT_NONE = new RoutingTimeMultiplier(
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE
    );
}
