package ru.yandex.market.logistics.lom.service.les;

import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.tpl.TplOrderDeliveryTaskFailedEvent;
import ru.yandex.market.logistics.les.tpl.enums.TplOrderDeliveryTaskFailReason;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;

@DisplayName("Тесты обработчика событий неуспешного задания на доставку в Курьерской платформе")
@DatabaseSetup("/service/les/tpl_order_delivery_task_failed/before/setup.xml")
@ParametersAreNonnullByDefault
public class TplOrderDeliveryTaskFailedEventProcessorTest extends AbstractContextualTest {

    private static final String BARCODE = "12345";

    @Autowired
    private LesConsumer lesConsumer;

    @Test
    @DisplayName("Обработка невалидного события")
    void eventIsNotValid() {
        processEvent(new TplOrderDeliveryTaskFailedEvent(null, TplOrderDeliveryTaskFailReason.UNKNOWN));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Обработка события, заказа из которого не существует")
    void orderDoesNotExist() {
        processEvent(new TplOrderDeliveryTaskFailedEvent("1234", TplOrderDeliveryTaskFailReason.UNKNOWN));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Обработка события, заказ не в постомат")
    @DatabaseSetup(
        value = "/service/les/tpl_order_delivery_task_failed/before/wrong_subtype.xml",
        type = DatabaseOperation.REFRESH
    )
    void notLockerOrder() {
        processEvent(new TplOrderDeliveryTaskFailedEvent(BARCODE, TplOrderDeliveryTaskFailReason.DIMENSIONS_EXCEEDED));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Заявка с таким типом уже существует")
    @DatabaseSetup(
        value = "/service/les/tpl_order_delivery_task_failed/before/cor_exists.xml",
        type = DatabaseOperation.REFRESH
    )
    void changeOrderRequestAlreadyExists() {
        processEvent(new TplOrderDeliveryTaskFailedEvent(BARCODE, TplOrderDeliveryTaskFailReason.DIMENSIONS_EXCEEDED));
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "Change request CHANGE_LAST_MILE_FROM_PICKUP_TO_PICKUP already exists"
        );
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Обработка события приводит к созданию и обработке нового запроса на изменение последней мили")
    @ExpectedDatabase(
        value = "/service/les/tpl_order_delivery_task_failed/after/change_request_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeOrderRequestCreated() {
        processEvent(new TplOrderDeliveryTaskFailedEvent(BARCODE, TplOrderDeliveryTaskFailReason.DIMENSIONS_EXCEEDED));
        queueTaskChecker.assertQueueTasksCreated(
            QueueType.PROCESS_UPDATE_LAST_MILE_FROM_PICKUP_TO_PICKUP,
            1
        );
    }

    void processEvent(TplOrderDeliveryTaskFailedEvent payload) {
        var event = new Event(
            "lom",
            "event_id_3",
            Instant.now().toEpochMilli(),
            TplOrderDeliveryTaskFailedEvent.EVENT_TYPE,
            payload,
            "Тест"
        );
        lesConsumer.processEvent("messageId", event);
    }
}
