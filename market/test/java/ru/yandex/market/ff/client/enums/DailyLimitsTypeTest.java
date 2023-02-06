package ru.yandex.market.ff.client.enums;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DailyLimitsTypeTest {
    @DisplayName("Получить тип лимитов по категории")
    @ParameterizedTest
    @MethodSource("getTypesByCategory")
    void ofCategory(DailyLimitsType root, DailyLimitsCategory category, DailyLimitsType expected) {
        Assertions.assertEquals(expected, root.ofCategory(category));
    }

    static Stream<Arguments> getTypesByCategory() {
        return Stream.of(
            Arguments.of(
                DailyLimitsType.SUPPLY, DailyLimitsCategory.ORDERS, DailyLimitsType.SUPPLY
            ),
            Arguments.of(
                DailyLimitsType.SUPPLY, DailyLimitsCategory.INTERWAREHOUSE, DailyLimitsType.MOVEMENT_SUPPLY
            ),
            Arguments.of(
                DailyLimitsType.SUPPLY, DailyLimitsCategory.XDOCK_TRANSPORT, DailyLimitsType.XDOCK_TRANSPORT_SUPPLY
            ),
            Arguments.of(
                DailyLimitsType.WITHDRAW, DailyLimitsCategory.ORDERS, DailyLimitsType.WITHDRAW
            ),
            Arguments.of(
                DailyLimitsType.WITHDRAW, DailyLimitsCategory.INTERWAREHOUSE, DailyLimitsType.MOVEMENT_WITHDRAW
            ),
            Arguments.of(
                DailyLimitsType.WITHDRAW, DailyLimitsCategory.XDOCK_TRANSPORT, DailyLimitsType.XDOCK_TRANSPORT_WITHDRAW
            )
        );
    }
}
