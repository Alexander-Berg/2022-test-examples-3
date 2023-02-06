package ru.yandex.market.billing.payment.services;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class UpdatePaidOutAccrualPayoutStatusServiceTest extends FunctionalTest {
    @Autowired
    private UpdatePaidOutAccrualPayoutStatusService updatePaidOutAccrualPayoutStatusService;

    @Test
    @DisplayName("Обновление статустов у accrual'ов, для которых уже созданы payout")
    @DbUnitDataSet(
            before = "UpdatePaidOutAccrualPayoutStatusServiceTest.updateAccrualPayoutStatus.before.csv",
            after = "UpdatePaidOutAccrualPayoutStatusServiceTest.updateAccrualPayoutStatus.after.csv"
    )
    void updateAccrualPayoutStatus() {
        updatePaidOutAccrualPayoutStatusService.process(LocalDate.of(2022, 1, 20));
    }
}
