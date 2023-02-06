package ru.yandex.market.logistics.lom.jobs.producer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.OrderReturn;
import ru.yandex.market.logistics.lom.entity.enums.OrderReturnStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Отправка возвратных статузов заказа в LRM")
class PushCancellationReturnDeliveryServiceStatusesProducerTest extends AbstractContextualTest {
    @Autowired
    private PushCancellationReturnDeliveryServiceStatusesProducer pushReturnStatusesToLrmProducer;

    @Test
    @DisplayName("Есть активные возвраты. Задача ставится в очередь")
    void taskCreated() {
        pushReturnStatusesToLrmProducer.produceTaskIfNeeded(
            new Order()
                .setId(1L)
                .addReturn(new OrderReturn().setReturnStatus(OrderReturnStatus.COMMITTED))
        );
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Нет активных возвратов. Задача не ставится в очередь")
    void taskNotCreated() {
        pushReturnStatusesToLrmProducer.produceTaskIfNeeded(new Order());
        checkTaskCreation(false);
    }

    private void checkTaskCreation(boolean created) {
        if (created) {
            queueTaskChecker.assertQueueTaskCreated(
                QueueType.PUSH_CANCELLATION_RETURN_DELIVERY_SERVICE_STATUSES,
                PayloadFactory.createOrderIdPayload(1, "1", 1)
            );
        } else {
            queueTaskChecker.assertQueueTaskNotCreated(QueueType.PUSH_CANCELLATION_RETURN_DELIVERY_SERVICE_STATUSES);
        }
    }
}
