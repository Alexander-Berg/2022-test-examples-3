package ru.yandex.market.global.checkout.executor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.TestTimeUtil;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.PromoQueryService;
import ru.yandex.market.global.checkout.domain.promo.model.PromoTarget;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.domain.promo.model.PromoWithUsagesLeft;
import ru.yandex.market.global.checkout.domain.referral.ReferralQueryService;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.factory.TestReferralFactory;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EReferralType;
import ru.yandex.market.global.db.jooq.tables.pojos.Referral;
import ru.yandex.market.global.db.jooq.tables.pojos.ReferralReward;

import static org.assertj.core.api.Assertions.assertThat;

@Import(ReferralRewardExecutor.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReferralRewardExecutorTest extends BaseFunctionalTest {

    private static final Long REFERRAL_SHOP_ID = 101L;
    private static final Long REFERRAL_UID = 202L;
    private static final Long UID = 909L;
    private static final OffsetDateTime NOW = dt(2022, 1, 20);

    private final TestClock clock;
    private final ReferralRewardExecutor executor;

    private final TestOrderFactory orderFactory;
    private final TestPromoFactory promoFactory;
    private final TestReferralFactory referralFactory;
    private final PromoQueryService promoQueryService;
    private final ReferralQueryService referralQueryService;

    private Referral shopReferral;
    private Referral userReferral;

    @BeforeEach
    void prepare() {
        clock.setTime(NOW.toInstant());
        promoFactory.createShopFreeDeliveryPromo();

        shopReferral = referralFactory.createReferral(r -> r
                .setReferredByEntityId(REFERRAL_SHOP_ID.toString())
                .setType(EReferralType.SHOP));

        userReferral = referralFactory.createReferral(r -> r
                .setReferredByEntityId(REFERRAL_UID.toString())
                .setType(EReferralType.USER));
    }

    @Test
    void testSingleOrderSimpleCase() {
        OrderModel order = createOrder(shopReferral.getId(), UID, dt(2022, 1, 5));

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 0);

        executor.doRealJob(null);

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 1);
        assertRewarded(order.getOrder().getId(), true);
    }

    @Test
    void testSingleOrderSimpleCaseUserReferral() {
        OrderModel order = createOrder(userReferral.getId(), UID, dt(2022, 1, 5));

        assertPromocodeUsagesCount(REFERRAL_UID, 0);

        executor.doRealJob(null);

        assertPromocodeUsagesCount(REFERRAL_UID, 1);
        assertRewarded(order.getOrder().getId(), true);
    }

    @Test
    void testMultipleOrderOfSingleUser() {
        OrderModel order1 = createOrder(shopReferral.getId(), UID, dt(2021, 12, 5));
        OrderModel order2 = createOrder(shopReferral.getId(), UID, dt(2022, 1, 2));
        OrderModel order3 = createOrder(shopReferral.getId(), UID, dt(2022, 1, 3));
        OrderModel order4 = createOrder(shopReferral.getId(), UID, dt(2022, 1, 5));

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 0);

        executor.doRealJob(null);

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 1);
        assertRewarded(order1.getOrder().getId(), true);
        assertRewarded(order2.getOrder().getId(), false);
        assertRewarded(order3.getOrder().getId(), false);
        assertRewarded(order4.getOrder().getId(), false);
    }

    @Test
    void testMultipleOrderOfSingleUserUserReferral() {
        OrderModel order1 = createOrder(userReferral.getId(), UID, dt(2021, 12, 5));
        OrderModel order2 = createOrder(userReferral.getId(), UID, dt(2022, 1, 2));
        OrderModel order3 = createOrder(userReferral.getId(), UID, dt(2022, 1, 3));
        OrderModel order4 = createOrder(userReferral.getId(), UID, dt(2022, 1, 5));

        assertPromocodeUsagesCount(REFERRAL_UID, 0);

        executor.doRealJob(null);

        assertPromocodeUsagesCount(REFERRAL_UID, 1);
        assertRewarded(order1.getOrder().getId(), true);
        assertRewarded(order2.getOrder().getId(), false);
        assertRewarded(order3.getOrder().getId(), false);
        assertRewarded(order4.getOrder().getId(), false);
    }

    @Test
    void testDifferentUsers() {
        OrderModel order1 = createOrder(shopReferral.getId(), UID, dt(2022, 1, 2));
        OrderModel order2 = createOrder(shopReferral.getId(), UID + 1, dt(2022, 1, 3));

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 0);

        executor.doRealJob(null);

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 2);
        assertRewarded(order1.getOrder().getId(), true);
        assertRewarded(order2.getOrder().getId(), true);
    }

    @Test
    void testDifferentUsersUserReferral() {
        OrderModel order1 = createOrder(userReferral.getId(), UID, dt(2022, 1, 2));
        OrderModel order2 = createOrder(userReferral.getId(), UID + 1, dt(2022, 1, 3));

        assertPromocodeUsagesCount(REFERRAL_UID, 0);

        executor.doRealJob(null);

        assertPromocodeUsagesCount(REFERRAL_UID, 2);
        assertRewarded(order1.getOrder().getId(), true);
        assertRewarded(order2.getOrder().getId(), true);
    }

    @Test
    void testNoRewardIf1DaysPassed() {
        OrderModel order = createOrder(shopReferral.getId(), UID, NOW.minusDays(1));

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 0);

        executor.doRealJob(null);

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 1);
        assertRewarded(order.getOrder().getId(), true);
    }

    @Test
    void testNoRewardIfOrderNotCompleted() {
        OrderModel order1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setUid(UID)
                        .setFinishedAt(null)
                        .setOrderState(EOrderState.CANCELED)
                        .setReferralId(shopReferral.getId()))
                .build());

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 0);

        executor.doRealJob(null);

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 0);
        assertRewarded(order1.getOrder().getId(), false);
    }

    @Test
    void testRewardOnlyFirstCompletedOrder() {
        OrderModel order1 = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setUid(UID)
                        .setFinishedAt(null)
                        .setOrderState(EOrderState.CANCELED)
                        .setReferralId(shopReferral.getId()))
                .build());
        OrderModel order2 = createOrder(shopReferral.getId(), UID, dt(2022, 1, 5));

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 0);

        executor.doRealJob(null);

        assertFreeDeliveriesLeft(REFERRAL_SHOP_ID, 1);
        assertRewarded(order1.getOrder().getId(), false);
        assertRewarded(order2.getOrder().getId(), true);
    }

    private OrderModel createOrder(long referralId, long uid, OffsetDateTime finishedAt) {
        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setUid(uid)
                        .setFinishedAt(finishedAt)
                        .setOrderState(EOrderState.FINISHED)
                        .setReferralId(referralId))
                .build());
    }

    private static OffsetDateTime dt(int year, int month, int day) {
        return TestTimeUtil.getTimeInDefaultTZ(
                LocalDateTime.of(year, month, day, 15, 16, 23, 42));
    }

    private void assertPromocodeUsagesCount(long uid, int count) {
        assertThat(
                promoQueryService.getPromosAvailableTo(PromoTarget.USER, uid).stream()
                        .filter(p -> p.getType().equals(PromoType.FIXED_DISCOUNT))
                        .mapToLong(PromoWithUsagesLeft::getUsagesLeft)
                        .sum()
        ).isEqualTo(count);
    }

    private void assertFreeDeliveriesLeft(long shopId, int count) {
        assertThat(
                promoQueryService.getPromosAvailableTo(PromoTarget.SHOP, shopId).stream()
                        .filter(p -> p.getType().equals(PromoType.FREE_DELIVERY_SHOP))
                        .findFirst()
                        .map(PromoWithUsagesLeft::getUsagesLeft)
                        .orElse(0L)
        ).isEqualTo(count);
    }

    private void assertRewarded(long orderId, boolean rewarded) {
        ReferralReward reward = referralQueryService.getReward(orderId);
        if (rewarded) {
            assertThat(reward).isNotNull();
            assertThat(reward.getRewarded()).isTrue();
        } else {
            assertThat(reward).isNull();
        }
    }

}
