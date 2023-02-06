package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;

@DisplayName("Обработка заявки на изменение состава заказа в статусе INFO_RECEIVED")
class OrderItemsRemovalInfoReceivedProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;

    @Test
    @DisplayName("Успешная обработка события")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/itemsremoval/inforeceived/db/after/item_removal_task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/inforeceived/diff/info_received.json",
                "logbroker/lom/event/itemsremoval/inforeceived/snapshot/info_received.json"
            )
        ));
    }

    @Test
    @DisplayName("Заказ не FaaS")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/no_queue_tasks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderIsNotFaaS() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/inforeceived/diff/info_received.json",
                "logbroker/lom/event/itemsremoval/inforeceived/snapshot/not_faas.json"
            )
        ));
    }

    @Test
    @DisplayName("Неверный статус заявки")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/no_queue_tasks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failStatus() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/inforeceived/diff/fail_status.json",
                "logbroker/lom/event/itemsremoval/inforeceived/snapshot/info_received.json"
            )
        ));
    }

    @Test
    @DisplayName("Неверный тип заявки")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/no_queue_tasks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void wrongStatus() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/itemsremoval/inforeceived/diff/fail_status.json",
                "logbroker/lom/event/itemsremoval/inforeceived/snapshot/not_item_removal.json"
            )
        ));
    }
}
