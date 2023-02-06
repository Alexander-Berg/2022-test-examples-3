package ru.yandex.market.logistic.api.model.fulfillment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit тесты для {@link CargoType}.
 *
 * @author avetokhin 26/10/17.
 */
class CargoTypeTest {

    /**
     * Проверка валидного создания энума по коду.
     */
    @Test
    void createTest() {
        for (CargoType cargoType : CargoType.values()) {
            assertEquals(cargoType, CargoType.create(cargoType.getCode()));
        }
    }
}
