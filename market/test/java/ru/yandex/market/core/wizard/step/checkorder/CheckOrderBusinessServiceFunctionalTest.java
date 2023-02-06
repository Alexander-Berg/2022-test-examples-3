package ru.yandex.market.core.wizard.step.checkorder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Функциональные тесты для {@link CheckOrderBusinessService}.
 */
@DbUnitDataSet(before = "CheckOrderBusinessServiceFunctionalTest.before.csv")
public class CheckOrderBusinessServiceFunctionalTest extends FunctionalTest {
    @Autowired
    private CheckOrderBusinessService checkOrderBusinessService;

    @Test
    void testIsExperimentRequired() {
        assertThat(checkOrderBusinessService.isCheckOrderRequired(1L)).isTrue();
        assertThat(checkOrderBusinessService.isCheckOrderRequired(2L)).isFalse();
    }

    @Test
    @DbUnitDataSet(after = "CheckOrderBusinessServiceFunctionalTest.testAddBusinessRequired.after.csv")
    void testAddBusinessRequired() {
        checkOrderBusinessService.addBusinessToRequired(2L);
    }

    @Test
    @DbUnitDataSet(after = "CheckOrderBusinessServiceFunctionalTest.testRemoveBusinessRequired.after.csv")
    void testRemoveBusinessRequired() {
        checkOrderBusinessService.removeBusinessFromRequired(1L);
    }

    @Test
    void testIsExperimentRequiredForPartner() {
        assertThat(checkOrderBusinessService.isCheckOrderRequiredForPartner(3L)).isTrue();
        assertThat(checkOrderBusinessService.isCheckOrderRequiredForPartner(4L)).isFalse();
    }
}
