package ru.yandex.market.global.checkout.domain.queue.task.notification;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.executor.ReferralRewardExecutor;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestReferralFactory;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EReferralType;
import ru.yandex.market.global.db.jooq.tables.pojos.Referral;

import static ru.yandex.market.global.checkout.configuration.ConfigurationProperties.PUSH_NOTIFICATIONS_ENABLED;
import static ru.yandex.market.global.checkout.configuration.ConfigurationProperties.USER_REFERRAL_PUSH_NOTIFICATIONS_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Disabled
public class ReferralNotificationLocalTest extends BaseFunctionalTest {

    private static final Long UID = 1234567L;
    private static final Long REFERRAL_UID = 1200000L;
    private static final String TAXI_USER_ID = UUID.randomUUID().toString();

    private static final Instant NOW = Instant.parse("2022-01-31T16:15:30.00Z");

    @Autowired
    private QueueShard<DatabaseAccessLayer> shard;

    private final ReferralRewardExecutor executor;
    private final TestOrderFactory orderFactory;
    private final TestReferralFactory referralFactory;
    private final TestClock clock;

    private Referral userReferral;

    protected final ConfigurationService configurationProvider;

    @BeforeEach
    void prepare() {
        clock.setTime(NOW);

        userReferral = referralFactory.createReferral(r -> r
                .setReferredByEntityId(REFERRAL_UID.toString())
                .setType(EReferralType.USER));

        configurationProvider.insertValue(PUSH_NOTIFICATIONS_ENABLED, true);
        configurationProvider.insertValue(USER_REFERRAL_PUSH_NOTIFICATIONS_ENABLED, true);
    }


    @Test
    void testNotification() throws InterruptedException, ExecutionException {
        OrderModel order = createOrder(userReferral.getId(), OffsetDateTime.now(clock).minusDays(7));
        executor.doRealJob(null);
        Thread.sleep(1000000);
        //get data from ucommunication
    }

    private OrderModel createOrder(long referralId, OffsetDateTime finishedAt) {
        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setUid(UID)
                        .setFinishedAt(finishedAt)
                        .setOrderState(EOrderState.FINISHED)
                        .setReferralId(referralId))
                .build());
    }

}
