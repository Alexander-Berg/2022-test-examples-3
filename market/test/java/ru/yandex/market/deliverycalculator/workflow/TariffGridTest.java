package ru.yandex.market.deliverycalculator.workflow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TariffGridTest {

    private TariffGrid tariffGrid;

    @BeforeEach
    void init() {
        final TariffGrid.Builder builder = TariffGrid.newBuilder()
                .addRule(1, 1.0, 2.0, 1)
                .addRule(1, 2.0, 3.0, 2)
                .addRule(2, 1.0, 5.0, 3);
        tariffGrid = builder.build();
    }

    @Test
    void hasTheSameStructure_shouldReturnTrue() {
        final TariffGrid.Builder builder = TariffGrid.newBuilder()
                .addRule(2, 1.0, 5.0, 4)
                .addRule(1, 2.0, 3.0, 5)
                .addRule(1, 1.0, 2.0, 6);

        Assertions.assertTrue(tariffGrid.hasTheSameStructure(builder.build()));
    }

    @Test
    void hasTheSameStructure_shouldReturnFalse() {
        final TariffGrid.Builder builder = TariffGrid.newBuilder();
        Assertions.assertFalse(tariffGrid.hasTheSameStructure(builder.build()));
    }
}