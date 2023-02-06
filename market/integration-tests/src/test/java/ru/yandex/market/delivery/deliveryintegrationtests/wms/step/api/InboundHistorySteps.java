package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;

import static org.hamcrest.Matchers.equalTo;

public class InboundHistorySteps {

    private static final ServiceBus serviceBus = new ServiceBus();

    protected InboundHistorySteps() {}

    @Step("Получаем историю изменений статусов продукта")
    public ValidatableResponse getInboundStatusHistory(Inbound inbound) {
        return serviceBus.getInboundHistory(inbound.getYandexId(), inbound.getFulfillmentId());
    }

    @Step("Проверяем, что история статусов включает статус {}")
    public void verifyHistoryHasStatus(Inbound inbound, String statusCode, Integer beforeCount) {
        getInboundStatusHistory(inbound)
            .body("root.response.inboundStatusHistory.history.inboundStatus.findAll{it.statusCode=='" + statusCode + "'}.size()",
                    equalTo(beforeCount + 1));
    }

}
