package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ApiClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.SchedulerJob;

public class ReplenishmentSteps {
    private static final ApiClient apiClient = new ApiClient();

    @Step("Запускаем джобу создания задач")
    public ValidatableResponse startWithdrawalReplenishmentJob() {
        return apiClient.executeSchedulerJob(SchedulerJob.WITHDRAWAL_REPLENISHMENT);
    }
}
