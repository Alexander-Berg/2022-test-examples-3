package ru.yandex.market.global.checkout.domain.queue.task.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import ru.yoomoney.tech.dbqueue.api.EnqueueParams;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.configuration.ConfigurationProperties;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.push.model.PushTankerKeyField;
import ru.yandex.market.global.checkout.domain.referral.reward.OrderWithReferralModel;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;

import static ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderBuilder;
import static ru.yandex.market.global.common.util.StringFormatter.sf;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ReferralNotificationTest extends BaseFunctionalTest {

    public static final NotificationContent CONTENT = NotificationContent.REWARD_PROMOCODE_RECEIVED;
    public static final String YTAXI_ID = "12356ffs";
    public static final String LOCALE = "he";
    public static final String CCY = "EUR";
    public static final Long PROMO_AMT = 1_000_00L;
    public static final Long REFERRED_UID = 1333234L;

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(ReferralNotificationTest.class).build();

    @SpyBean
    private SendPushProducer testProducer;

    @Autowired
    private ReferralRewardNotificationConsumer consumer;
    @Autowired
    private QueueShard<DatabaseAccessLayer> shard;

    @Autowired
    private TestClock clock;
    @Autowired
    private TestOrderFactory testOrderFactory;

    @Autowired
    private ConfigurationService configurationProvider;


    @BeforeEach
    public void setup() {
        configurationProvider.deleteValue(ConfigurationProperties.USER_REFERRAL_PUSH_NOTIFICATIONS_ENABLED);
        configurationProvider.insertValue(ConfigurationProperties.USER_REFERRAL_PUSH_NOTIFICATIONS_ENABLED, true);
    }

    private ReferralRewardNotificationPayload getValidPayload() {
        return new ReferralRewardNotificationPayload()
                .setOrder(RANDOM.nextObject(OrderWithReferralModel.class)
                        .setReferredByEntityId(REFERRED_UID.toString()))
                .setCurrency(CCY)
                .setContent(CONTENT)
                .setDiscountAmount(PROMO_AMT);
    }

    @Test
    public void testPush() {

        Mockito.when(testProducer.enqueue(EnqueueParams.create(new SendPushPayload()))).thenReturn(null);

        var payload = getValidPayload();

        OrderModel srcOrder = testOrderFactory.createOrder(CreateOrderBuilder.builder()
                .setupOrder(order -> order.setUid(REFERRED_UID).setLocale(LOCALE)).build()
        );

        SendPushPayload pushPayload = new SendPushPayload()
                .setPushName(sf("{} referral promo (type B) for orderID {} to user {}",
                        CONTENT, payload.getOrder().getOrderId(), REFERRED_UID))
                .setIdempotencyKey(payload.getOrder().getOrderId() + " " + CONTENT.name())
                .setUserIdType(SendPushPayload.SendPushUserIdentifierType.YANDEX_UID)
                .setUserId(String.valueOf(REFERRED_UID))
                .setTitleTankerKey(new PushTankerKeyField(CONTENT.getTitleTankerKey()))
                //A0 = non-breaking space , 200f = RLM
                .setTextTankerKey(new PushTankerKeyField(CONTENT.getTextTankerKey(),
                        Map.of("sum", "\u200f1,000\u00A0€")))
                .setLocale(LOCALE);

        TestQueueTaskRunner.runTaskThrowOnFail(consumer, payload);

        ArgumentCaptor<EnqueueParams> captor = ArgumentCaptor.forClass(EnqueueParams.class);
        Mockito.verify(this.testProducer).enqueue(captor.capture());

        EnqueueParams value = captor.getValue();
        Assertions.assertThat(value.getExecutionDelay()).isEqualTo(Duration.ofMillis(0));
        Assertions.assertThat(value.getPayload()).usingRecursiveComparison().isEqualTo(pushPayload);
    }

    @Test
    public void testDelayedPush() {

        Mockito.when(testProducer.enqueue(EnqueueParams.create(new SendPushPayload()))).thenReturn(null);

        var payload = getValidPayload();


        OrderModel srcOrder = testOrderFactory.createOrder(CreateOrderBuilder.builder()
                .setupOrder(order -> order.setUid(REFERRED_UID).setLocale(LOCALE)).build()
        );

        Instant time = Instant.parse("2022-03-25T02:00:00Z");
        clock.setTime(time);
        TestQueueTaskRunner.runTaskThrowOnFail(consumer, payload);

        ArgumentCaptor<EnqueueParams> captor = ArgumentCaptor.forClass(EnqueueParams.class);
        Mockito.verify(this.testProducer).enqueue(captor.capture());

        //time is set to 9
        //current time is 2 UTC
        //default ZoneID = HE (+3) = 5 IDT
        // 9 - 5 = 4 with execution time
        EnqueueParams value = captor.getValue();
        Assertions.assertThat(value.getExecutionDelay())
                .isBetween(Duration.ofMinutes(3 * 59), Duration.ofHours(4));
    }


}
