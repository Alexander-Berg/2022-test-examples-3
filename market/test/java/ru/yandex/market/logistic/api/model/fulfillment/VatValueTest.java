package ru.yandex.market.logistic.api.model.fulfillment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit тесты для {@link VatValue}.
 *
 * @author avetokhin 26/10/17.
 */
class VatValueTest {

    /**
     * Проверка валидного создания энума по коду.
     */
    @Test
    void createTest() {
        for (VatValue vatValue : VatValue.values()) {
            assertEquals(vatValue, VatValue.create(vatValue.getCode()));
        }
    }
}
