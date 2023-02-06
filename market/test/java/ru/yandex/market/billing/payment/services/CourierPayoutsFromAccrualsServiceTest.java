package ru.yandex.market.billing.payment.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
/**
 * Tests for {@link CourierPayoutsFromAccrualsService}.
 */
public class CourierPayoutsFromAccrualsServiceTest extends FunctionalTest {

    @Autowired
    private CourierPayoutsFromAccrualsService courierPayoutsFromAccrualsService;

    @Test
    @DisplayName("Правильно ли создаются пэйауты из акрулов")
    @DbUnitDataSet(
            before = "CourierPayoutsFromAccrualsServiceTest.createPayoutsFromAccruals.before.csv",
            after = "CourierPayoutsFromAccrualsServiceTest.createPayoutsFromAccruals.after.csv"
    )
    void testCreatePayoutsFromAccruals() {
        courierPayoutsFromAccrualsService.process();
    }

    @Test
    @DisplayName("Правильно ли сохраняется тип партнера")
    @DbUnitDataSet(
            before = "CourierPayoutsFromAccrualsServiceTest.saveCourierTypeField.before.csv",
            after = "CourierPayoutsFromAccrualsServiceTest.saveCourierTypeField.after.csv"
    )
    void shouldSaveSelfEmployedWhenValueGiven() {
        courierPayoutsFromAccrualsService.process();
    }
}
