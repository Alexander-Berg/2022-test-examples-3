package ru.yandex.market.global.checkout.domain.promo;

import java.time.Clock;
import java.time.OffsetDateTime;

import javax.annotation.Nullable;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.executor.PromoIssuedExpirationNotificationExecutor;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.mj.generated.server.model.PromoCommunicationArgsDto;

import static ru.yandex.market.global.checkout.configuration.ConfigurationProperties.PUSH_NOTIFICATIONS_ENABLED;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoNotificationServiceLocalTest extends BaseLocalTest {

    private static final long UID = 4092490744L;

    private final PromoNotificationService promoNotificationService;
    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;
    private final Clock clock;
    private final ConfigurationService configuration;

    private final PromoIssuedExpirationNotificationExecutor promoIssuedExpirationNotificationExecutor;

    @Test
    @SneakyThrows
    public void test() {

        configuration.mergeValue(PUSH_NOTIFICATIONS_ENABLED, true);

        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(it -> it.setUid(UID).setLocale("ru")).build());

        Promo fixedDiscount = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setAccessType(EPromoAccessType.ISSUED)
                )
                        .setupCommunicationTypes(it -> new EPromoCommunicationType[] {EPromoCommunicationType.PUSH})
                        .setupCommunicationArgs(it -> new PromoCommunicationArgsDto()
                                .push(PromoUtil.createDefaultIssuedPushCommunication()))
                .build()
        );

        promoNotificationService.pushNotification(fixedDiscount, UID, PromoUtil.createRecipientByUID(UID));
        Thread.sleep(20000);
    }

    private void createUsage(Promo promo, boolean used, @Nullable Long orderId) {
        testPromoFactory.createUsageUserRecord(it -> it
                .setPromoId(promo.getId())
                .setUsed(used)
                .setOrderId(orderId)
                .setUid(UID)
                .setValidTill(promo.getValidTill()));
    }

    @Test
    @SneakyThrows
    public void testExpiration() {
        configuration.mergeValue(PUSH_NOTIFICATIONS_ENABLED, true);
        OrderModel order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(it -> it.setUid(UID).setLocale("ru")).build());
        Promo promo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(it -> it
                        .setAccessType(EPromoAccessType.ISSUED)
                        .setValidFrom(OffsetDateTime.now(clock))
                        .setValidTill(OffsetDateTime.now(clock).plusDays(2).plusHours(10))
                )
                .setupCommunicationTypes(it -> new EPromoCommunicationType[] {EPromoCommunicationType.PUSH})
                .setupCommunicationArgs(it -> new PromoCommunicationArgsDto()
                        .push(PromoUtil.createDefaultIssuedPushCommunication()))
                .build());

        createUsage(promo, false, null);
        createUsage(promo, false, null);
        createUsage(promo, true, order.getOrder().getId());

        promoIssuedExpirationNotificationExecutor.doRealJob(null);
        Thread.sleep(10000);

    }

}
