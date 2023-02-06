package ru.yandex.market.global.checkout.domain.queue.tracker;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.push.model.PushTankerKeyField;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.key.GlobalNotificationTextTankerKey;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.key.GlobalNotificationTitleTankerKey;
import ru.yandex.market.global.checkout.domain.queue.task.notification.SendPushPayload;
import ru.yandex.market.global.checkout.domain.queue.task.notification.SendPushProducer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.CheckOrderDeliveredInTimeConsumer;
import ru.yandex.market.global.checkout.domain.queue.task.tracker.TrackerNotificationService;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;

import static org.mockito.Mockito.verify;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CheckOrderDeliveredInTimeTest extends BaseFunctionalTest {

    private final CheckOrderDeliveredInTimeConsumer checkOrderDeliveredInTimeConsumer;
    private final TestOrderFactory testOrderFactory;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private SendPushProducer sendPushProducer;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private TrackerNotificationService trackerNotificationService;

    @Test
    public void testCorrectPush() {

        OrderModel order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(it -> it
                        .setDeliveryState(EDeliveryOrderState.DELIVERING_ORDER)
                        .setProcessingMode(EProcessingMode.AUTO)
                        .setLocale("ru"))
                .build());

        ArgumentCaptor<SendPushPayload> captor = ArgumentCaptor.forClass(SendPushPayload.class);

        TestQueueTaskRunner.runTaskThrowOnFail(checkOrderDeliveredInTimeConsumer, order.getOrder().getId());
        verify(sendPushProducer).enqueueNonSleepTime(captor.capture());

        Assertions.assertThat(captor.getValue())
                .usingRecursiveComparison()
                .ignoringExpectedNullFields().isEqualTo(
                new SendPushPayload()
                        .setTitleTankerKey(new PushTankerKeyField(
                                GlobalNotificationTitleTankerKey.PROMO_ISSUED_WE_ARE_LATE_TITLE, null))
                        .setTextTankerKey(new PushTankerKeyField(
                                GlobalNotificationTextTankerKey.PROMO_ISSUED_WE_ARE_LATE_TEXT, null))
                        .setLocale("ru")
                        .setUserId(order.getOrder().getYaTaxiUserId())
                        .setUserIdType(SendPushPayload.SendPushUserIdentifierType.GO_USER_ID));
    }
}
