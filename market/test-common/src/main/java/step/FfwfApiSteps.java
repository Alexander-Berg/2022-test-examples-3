package step;

import java.time.Instant;
import java.util.List;

import client.FfwfApiClient;
import client.LesClient;
import dto.requests.les.EventDto;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.logistics.les.ff.FulfilmentBoxItemsReceivedEvent;

@Slf4j
public class FfwfApiSteps {

    private static final String EVENT_SOURCE = "fulfillment-workflow";
    private static final String EVENT_QUEUE = "fulfillment-workflow_out";
    private static final FfwfApiClient FFWFAPI = new FfwfApiClient();
    private static final LesClient LES = new LesClient();

    private ShopRequestDetailsDTO getRequest(Long requestId) {
        ShopRequestDetailsDTO request = FFWFAPI.getRequest(requestId);
        Assertions.assertNotNull(request.getType(), "Отсутствует тип у shop request'a " + requestId);
        Assertions.assertNotNull(request.getId(), "Не удалось получить id у shop request'a " + requestId);
        return request;
    }

    // В качестве эксперимента увеличиваю таймаут с 10 до 25 минут на ожидание статуса реквеста
    @Step("Проверяем статус shop request'a")
    public void verifyRequestStatus(long requestId, RequestStatus status) {
        log.debug("Verifying shop request status");
        Retrier.retry(() -> Assertions.assertEquals(
            status,
            getRequest(requestId).getStatus(),
            "Некорректный статус у shop request'a " + requestId), 160);
    }

    @Step("Создаём shop request для XDoc перемещений")
    public Long createXDocTransportations() {
        log.debug("Creating shop request for XDoc transportations");
        ShopRequestDTO request = FFWFAPI.createXdocRequest();
        Assertions.assertNotNull(request.getId(), "Не удалось получить id shop request'a ");
        return request.getId();
    }

    @Step("Создаём shop request для 1P XDoc перемещений")
    public Long create1PXDocTransportations(Long xDocServiceId, Long serviceId) {
        log.debug("Creating shop request for XDoc transportations");
        ShopRequestDTO request = FFWFAPI.create1PXDocRequest(xDocServiceId, serviceId);
        Assertions.assertNotNull(request.getId(), "Не удалось получить id shop request'a ");
        return request.getId();
    }

    @Step("Вторичная приёмка")
    public void confirmBoxRecieved(String orderId, String boxId, long warehousePartnerId) {
        long ffRequestId = 1;
        Retrier.clientRetry(() -> {
            LES.addEvent(
                new EventDto(
                    EVENT_SOURCE,
                    ffRequestId + "-" + boxId,
                    Instant.now().toEpochMilli(),
                    FulfilmentBoxItemsReceivedEvent.EVENT_TYPE,
                    new FulfilmentBoxItemsReceivedEvent(
                        boxId,
                        orderId,
                        ffRequestId,
                        Instant.now(),
                        warehousePartnerId,
                        0L,
                        List.of()
                    ),
                    String.format("FFWF событие вторичной приемка возврата %s, коробка %s", ffRequestId, boxId)
                ),
                EVENT_QUEUE
            );
        });
    }
}
