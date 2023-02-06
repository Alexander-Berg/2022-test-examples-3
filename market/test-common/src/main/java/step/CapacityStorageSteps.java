package step;

import java.util.List;

import client.CapacityStorageClient;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.logistics.cs.domain.dto.InternalEventDto;
import ru.yandex.market.logistics.cs.domain.enums.InternalEventType;

public class CapacityStorageSteps {
    private static final CapacityStorageClient CAPACITY_STORAGE_CLIENT = new CapacityStorageClient();

    @Step("Триггерим job snapshot capacity для cs")
    public void snapshot() {
        CAPACITY_STORAGE_CLIENT.snapshot();
    }

    @Step("Проверяем,  CS по id заказа")
    public void verifyOrderRouteWasChanged(Long orderId) {
        Retrier.clientRetry(() -> {
            List<InternalEventDto> events = CAPACITY_STORAGE_CLIENT.getOrderEvents(orderId);
            Assertions.assertEquals(
                1,
                events.stream().filter(e -> e.getType().equals(InternalEventType.NEW) && e.isProcessed()).count(),
                "В Capacity Storage несколько NEW ивентов заказа"
            );
            Assertions.assertTrue(
                events.stream().anyMatch(e -> e.getType().equals(InternalEventType.CHANGE_ROUTE) && e.isProcessed()),
                "В Capacity Storage не пришел ивент CHANGE_ROUTE"
            );
        });
    }
}
