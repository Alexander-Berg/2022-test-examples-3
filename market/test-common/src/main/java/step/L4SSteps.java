package step;

import java.util.List;

import client.L4SClient;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import toolkit.Retrier;

import ru.yandex.market.logistics4shops.client.model.Outbound;

@Slf4j
public class L4SSteps {
    private static final L4SClient L4S_CLIENT = new L4SClient();

    @Step("Создание заявки на исключение заказа из отгрузки")
    public void excludeOrderFromShipment(Long orderId, Long shipmentId) {
        log.debug("Создание заявки на исключение заказа из отгрузки");
        Retrier.clientRetry(() -> L4S_CLIENT.excludeOrderFromShipment(orderId, shipmentId));
    }

    @Step("Получение отгрузки")
    public Outbound getOutbound(String outboundYandexId) {
        log.debug("Получение отгрузки из L4S");
        return Retrier.clientRetry(
            () -> L4S_CLIENT.searchOutbounds(List.of(outboundYandexId))
                .stream()
                .findFirst()
                .orElse(null)
        );
    }
}
