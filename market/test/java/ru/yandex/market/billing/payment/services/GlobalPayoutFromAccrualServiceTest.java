package ru.yandex.market.billing.payment.services;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ParametersAreNonnullByDefault
class GlobalPayoutFromAccrualServiceTest extends FunctionalTest {
    @Autowired
    private GlobalPayoutFromAccrualService globalPayoutFromAccrualService;

    @Test
    @DisplayName("Генерация payload на основе accrual")
    @DbUnitDataSet(
            before = "GlobalPayoutFromAccrualServiceTest.testGeneratePayoutsByGlobalAccruals.before.csv",
            after = "GlobalPayoutFromAccrualServiceTest.testGeneratePayoutsByGlobalAccruals.after.csv"
    )
    void testGeneratePayoutsByGlobalAccruals() {
        globalPayoutFromAccrualService.generatePayoutsByAccruals();
    }
}
