package ru.yandex.market.logistics.lom.controller.order.processing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.Recipient;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.producer.OrderValidationErrorNotificationProducer;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

@DisplayName("Производитель задач очереди NOTIFY_ORDER_VALIDATION_ERROR")
public class OrderValidationErrorNotificationProducerTest extends AbstractContextualTest {
    @Autowired
    private OrderValidationErrorNotificationProducer producer;

    @Test
    @DisplayName("Для стрельбового заказа задача не создается")
    void shootingOrder() {
        Order order = new Order()
            .setPlatformClient(PlatformClient.BERU)
            .setRecipient(new Recipient().setUid(UidConstants.NO_SIDE_EFFECT_UID));
        producer.produceTaskIfNeeded(order, 1L);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Для заказа DaaS задача не создается")
    void daasOrder() {
        Order order = new Order().setPlatformClient(PlatformClient.YANDEX_DELIVERY);
        producer.produceTaskIfNeeded(order, 1L);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Для заказа DBS задача создается")
    void dbsOrder() {
        Order order = new Order().setPlatformClient(PlatformClient.DBS).setId(1L);
        producer.produceTaskIfNeeded(order, 1L);
        queueTaskChecker.assertExactlyOneQueueTaskCreated(QueueType.NOTIFY_ORDER_VALIDATION_ERROR);
    }
}
