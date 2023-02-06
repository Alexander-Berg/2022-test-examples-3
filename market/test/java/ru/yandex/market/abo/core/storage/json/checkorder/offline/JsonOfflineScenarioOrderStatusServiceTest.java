package ru.yandex.market.abo.core.storage.json.checkorder.offline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.core.checkorder.model.OfflineScenarioOrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 17.08.2020
 */
class JsonOfflineScenarioOrderStatusServiceTest extends EmptyTest {

    private static final long ORDER_ID = 3215325L;

    @Autowired
    private JsonOfflineScenarioOrderStatusService service;

    @Test
    void serializationTest() {
        var scenarioStatusByOrder = new OfflineScenarioOrderStatus(
                ORDER_ID, 123L, OrderStatus.PROCESSING, CheckOrderScenarioStatus.CANCELLED
        );

        service.save(ORDER_ID, scenarioStatusByOrder);
        flushAndClear();

        assertEquals(scenarioStatusByOrder, service.getScenarioOrder(ORDER_ID).get().getStoredEntity());
    }
}
