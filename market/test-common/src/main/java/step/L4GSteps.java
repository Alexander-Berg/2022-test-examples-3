package step;

import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import client.L4GClient;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.logistics4go.client.model.CancelOrderResponse;
import ru.yandex.market.logistics4go.client.model.CreateOrderRequest;
import ru.yandex.market.logistics4go.client.model.CreateOrderResponse;
import ru.yandex.market.logistics4go.client.model.GetOrderResponse;
import ru.yandex.market.logistics4go.client.model.WaybillSegmentDto;

@Slf4j
@ParametersAreNonnullByDefault
public class L4GSteps {
    private static final L4GClient L4G_CLIENT = new L4GClient();

    @Step("Ожидание чекпоинта {checkpoint} в сегменте {segmentId}")
    public void verifySegmentHasCheckpointStatus(
        Long orderId,
        Long segmentId,
        OrderDeliveryCheckpointStatus checkpoint
    ) {
        Retrier.retry(() -> {
            log.debug("Получение заказа из L4G");
            GetOrderResponse l4gOrder = L4G_CLIENT.getOrder(orderId);

            Assertions.assertNotNull(l4gOrder.getSegments(), String.format("У заказа %d не найдены сегменты", orderId));

            l4gOrder.getSegments().stream()
                .filter(segment -> segmentId.equals(segment.getId()))
                .map(WaybillSegmentDto::getWaybillSegmentStatusHistory)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(statusHistory -> checkpoint.name().equals(statusHistory.getTrackerStatus()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(String.format(
                    "Не найден чекпоинт %d в сегменте %d заказа %d",
                    checkpoint.getId(),
                    segmentId,
                    orderId
                )));
        });
    }

    @Step("Создание заказа")
    public long createOrder(CreateOrderRequest request) {
        log.debug("Создание заказа в L4G {}", request);
        CreateOrderResponse response = L4G_CLIENT.createOrder(request);
        Assertions.assertNotNull(response.getId(), "В ответе на создание заказа нет идентификатора заказа");
        return response.getId();
    }

    @Step("Отмена заказа {orderId}")
    public void cancelOrder(long orderId) {
        CancelOrderResponse response = L4G_CLIENT.cancelOrder(orderId);
        long cancelOrderRequestId = response.getId();
        Retrier.retry(() -> {
            GetOrderResponse l4gOrder = L4G_CLIENT.getOrder(orderId);

            boolean successfullyCancelled = CollectionUtils.emptyIfNull(l4gOrder.getCancellationRequests())
                .stream()
                .filter(cancellation -> Objects.equals(cancellation.getId(), cancelOrderRequestId))
                .map(CancelOrderResponse::getCancelledTimestamp)
                .anyMatch(Objects::nonNull);

            Assertions.assertTrue(
                successfullyCancelled,
                String.format(
                    "Заявка %d на отмену заказа %d не получила успешный статус",
                    cancelOrderRequestId,
                    orderId
                )
            );
        });
    }
}
