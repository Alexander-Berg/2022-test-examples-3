package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api;

import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Transfer;

import static org.hamcrest.Matchers.is;

public class TransferSteps {

    private static final ServiceBus serviceBus = new ServiceBus();

    protected TransferSteps() {}

    @Step("Создаем трансфер")
    public Transfer createTransfer(Inbound inbound, Item item, int stockFrom, int stockTo, int count) {
        return createTransfer(
                inbound.getYandexId(),
                inbound.getFulfillmentId(),
                item,
                stockFrom,
                stockTo,
                count
        );
    }

    public Transfer createTransfer(long inbYandexId, String inbFfId, Item item, int stockFrom, int stockTo, int count) {
        ValidatableResponse responce = serviceBus.createTransfer(
                UniqueId.get(),
                inbYandexId,
                inbFfId,
                item,
                stockFrom,
                stockTo,
                count
        );

        return new Transfer(
                responce.extract().xmlPath().getLong("root.response.transferId.yandexId"),
                responce.extract().path("root.response.transferId.partnerId")
        );
    }

    @Step("Получаем статус трансфера")
    public ValidatableResponse getTransferStatus(long yandexId, String fulfillmentId) {
        return serviceBus.getTransferStatus(yandexId, fulfillmentId);
    }

    public void verifyTransferStatusIs(Transfer transfer, int status) {
        verifyTransferStatusIs(transfer.getYandexId(), transfer.getFulfillmentId(), status);
    }

    @Step("Проверяем что статус трансфера равен {status}")
    public void verifyTransferStatusIs(long yandexId, String fulfillmentId, int status) {
        getTransferStatus(yandexId, fulfillmentId)
                .body("root.response.transfersStatus.transferStatus.transferStatusEvent.statusCode",
                        is(String.valueOf(status))
                );
    }

    public void waitTransferStatusIs(Transfer transfer, int status) {
        waitTransferStatusIs(transfer.getYandexId(), transfer.getFulfillmentId(), status);
    }

    @Step("Ждем, чтотрансфер получит статус {status}")
    public void waitTransferStatusIs(long yandexId, String fulfillmentId, int status) {
        Retrier.retry(() -> verifyTransferStatusIs(
                yandexId,
                fulfillmentId,
                status),
                Retrier.RETRIES_MEDIUM
        );
    }

    public ValidatableResponse getTransferHistory(Transfer transfer) {
        return getTransferHistory(transfer.getYandexId(), transfer.getFulfillmentId());
    }

    @Step("Получаем историю трансфера")
    public ValidatableResponse getTransferHistory(long yandexId, String fulfillmentId) {
        return serviceBus.getTransferHistory(yandexId, fulfillmentId);
    }

    public ValidatableResponse getTransferDetails(Transfer transfer) {
        return getTransferDetails(transfer.getYandexId(), transfer.getFulfillmentId());
    }

    @Step("Получаем детали трансфера")
    public ValidatableResponse getTransferDetails(long yandexId, String fulfillmentId) {
        return serviceBus.getTransferDetails(yandexId, fulfillmentId);
    }

    @Step("Создаем трансфер с кизами")
    public Transfer createTransferWithCis(Item item,  int stockFrom, int stockTo, int count) {
        long yandexId = UniqueId.get();
        ValidatableResponse response = serviceBus.createTransferWithCis(yandexId, item, stockFrom, stockTo, count);

        return new Transfer(
                response.extract().xmlPath().getLong("root.response.transferId.yandexId"),
                response.extract().path("root.response.transferId.partnerId")
        );
    }
}
