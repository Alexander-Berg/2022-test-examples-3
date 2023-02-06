package ru.yandex.market.logistics.lom.service.flow.platform;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNullableByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;
import ru.yandex.market.logistics.lom.utils.StorageUnitFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNullableByDefault
public class DefaultCalculateTotalWeightStrategyTest extends AbstractTest {

    @DisplayName("Стратегия для расчета веса грузомест")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    public void testCalcTotalWeight(String name, StorageUnit rootUnit, BigDecimal expectedWeight) {
        var strategy = new DefaultCalculateTotalWeightStrategy();
        assertThat(strategy.calcTotalWeight(rootUnit)).isEqualTo(expectedWeight);
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                "Есть вес только у ROOT",
                StorageUnitFactory.getStorageUnit(7),
                BigDecimal.valueOf(7)
            ),
            Arguments.of(
                "Есть вес ROOT и PLACE",
                StorageUnitFactory.getStorageUnit(7, 5),
                BigDecimal.valueOf(5)
            ),
            Arguments.of(
                "Есть вес ROOT и нескольких PLACE",
                StorageUnitFactory.getStorageUnit(7, 5, 6),
                BigDecimal.valueOf(11)
            ),
            Arguments.of(
                "Есть вес ROOT и существует PLACE без размера",
                StorageUnitFactory.getStorageUnit(7, null),
                BigDecimal.valueOf(7)
            )
        );
    }
}
