package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ApiClient;

@Slf4j
public class InventorizationSteps {

    private static final ApiClient apiClient = new ApiClient();

    protected InventorizationSteps() {}

    @Step("Обновляем троганность палет")
    public void refreshTouchedPallets() {
        apiClient.refreshTouchedPallets();
    }

}
