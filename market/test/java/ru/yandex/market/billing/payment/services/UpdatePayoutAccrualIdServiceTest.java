package ru.yandex.market.billing.payment.services;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class UpdatePayoutAccrualIdServiceTest extends FunctionalTest {
    @Autowired
    private UpdatePayoutAccrualIdService updatePayoutAccrualIdService;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Обновление accrual_id у payout")
    @DbUnitDataSet(
            before = "UpdatePayoutAccrualIdServiceTest.updatePayoutAccrualId.before.csv",
            after = "UpdatePayoutAccrualIdServiceTest.updatePayoutAccrualId.after.csv"
    )
    void updatePayoutAccrualId() {
        updatePayoutAccrualIdService.processNextAccrualBatch(1L, 10);
    }

    @Test
    @DisplayName("Проверка граничных условий")
    @DbUnitDataSet(
            before = "UpdatePayoutAccrualIdServiceTest.updatePayoutAccrualIdWithEnvParam.before.csv",
            after = "UpdatePayoutAccrualIdServiceTest.updatePayoutAccrualIdWithEnvParam.after.csv"
    )
    void updatePayoutAccrualIdWithEnvParam() {
        UpdatePayoutAccrualIdCommand command = new UpdatePayoutAccrualIdCommand(
                updatePayoutAccrualIdService, environmentService
        );
        String[] strings = {};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        ImmutableMap.<String, String>builder()
                                .build()
                ),
                null
        );
    }
}
