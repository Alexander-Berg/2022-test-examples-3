package ru.yandex.market.global.checkout.domain.promo;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.apply.referral_first_order_discount.ReferralFirstOrderDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.referral_first_order_discount.ReferralFirstOrderDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.apply.referral_first_order_discount.ReferralFirstOrderDiscountPromoApplyHandler;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.factory.TestReferralFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.enums.EReferralType;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;
import ru.yandex.market.global.db.jooq.tables.pojos.Referral;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReferralFirstOrderDiscountTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(
            ReferralFirstOrderDiscountTest.class
    ).build();
    private static final long MIN_ITEMS_PRICE = 100_00L;
    private static final long DISCOUNT = 30_00L;
    private static final long REFERRAL_UID = 1L;
    private static final long USER_UID = 2L;

    private final ReferralFirstOrderDiscountPromoApplyHandler referralFirstOrderDiscountPromoApplyHandler;

    private final Clock clock;
    private final TestOrderFactory testOrderFactory;
    private final TestPromoFactory testPromoFactory;
    private final TestReferralFactory testReferralFactory;

    @Test
    public void testDiscountAvailableForFirstOrder() {
        Referral referral = createUserReferral(REFERRAL_UID);
        Promo promo = createReferralFirstOrderDiscount(referral.getId(), REFERRAL_UID);
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        referralFirstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo() - DISCOUNT);
    }

    @Test
    public void testDiscountNotAvailableForSmallOrder() {
        Referral referral = createUserReferral(REFERRAL_UID);
        Promo promo = createReferralFirstOrderDiscount(referral.getId(), REFERRAL_UID);
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE - 1);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        referralFirstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountNotAvailableForSecondOrder() {
        Referral referral = createUserReferral(REFERRAL_UID);
        Promo promo = createReferralFirstOrderDiscount(referral.getId(), REFERRAL_UID);

        OrderModel model = createFinishedOrder(USER_UID);
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE);

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        referralFirstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountNotAvailableForReferral() {
        Referral referral = createUserReferral(REFERRAL_UID);
        Promo promo = createReferralFirstOrderDiscount(referral.getId(), REFERRAL_UID);

        OrderActualization actualization = createOrderActualization(REFERRAL_UID, MIN_ITEMS_PRICE);

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        referralFirstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());
    }

    @Test
    public void testDiscountSplitsOnToItems() {
        Referral referral = createUserReferral(REFERRAL_UID);
        Promo promo = createReferralFirstOrderDiscount(referral.getId(), REFERRAL_UID);

        OrderActualization actualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setUid(USER_UID)
                                .setDeliveryCostForRecipient(1000L)
                                .setTotalItemsCostWithPromo(MIN_ITEMS_PRICE * 2)
                                .setTotalItemsCost(MIN_ITEMS_PRICE * 2)
                        )
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setTotalCostWithoutPromo(MIN_ITEMS_PRICE)
                                        .setTotalCost(MIN_ITEMS_PRICE)
                                        .setPrice(MIN_ITEMS_PRICE)
                                        .setCount(1L),
                                RANDOM.nextObject(OrderItem.class)
                                        .setTotalCostWithoutPromo(MIN_ITEMS_PRICE)
                                        .setTotalCost(MIN_ITEMS_PRICE)
                                        .setPrice(MIN_ITEMS_PRICE)
                                        .setCount(1L)
                        ))
                        .build()
        );

        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        referralFirstOrderDiscountPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems())
                .allMatch(i -> i.getTotalCostWithoutPromo() - i.getTotalCost() == DISCOUNT / 2);
    }

    private OrderModel createFinishedOrder(long uid) {
        return testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setOrderState(EOrderState.FINISHED)
                        .setUid(uid)
                )
                .build()
        );
    }

    private PromoUser createPromoUser(long uid) {
        return new PromoUser()
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now(clock))
                .setUid(uid);
    }

    private OrderActualization createOrderActualization(long uid, long itemPrice) {
        return TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setUid(uid)
                                .setDeliveryCostForRecipient(1000L)
                                .setTotalItemsCostWithPromo(itemPrice)
                                .setTotalItemsCost(itemPrice)
                        )
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setTotalCostWithoutPromo(itemPrice)
                                        .setTotalCost(itemPrice)
                                        .setPrice(itemPrice)
                                        .setCount(1L)
                        ))
                        .build()
        );
    }

    private Referral createUserReferral(long uid) {
        return testReferralFactory.createReferral(r -> r
                .setType(EReferralType.USER)
                .setReferredByEntityId(String.valueOf(uid))
        );
    }

    private Promo createReferralFirstOrderDiscount(long referralId, long referralUid) {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName("REFERRAL_PROMO_30")
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                        .setType(PromoType.REFERRAL_FIRST_ORDER_DISCOUNT.name())
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setValidTill(OffsetDateTime.now(clock))
                )
                .setupState(() -> new ReferralFirstOrderDiscountCommonState()
                        .setBudgetUsed(0)
                )
                .setupArgs((a) -> new ReferralFirstOrderDiscountArgs()
                        .setReferralId(referralId)
                        .setReferralUid(referralUid)
                        .setDiscount(DISCOUNT)
                        .setBudget(1000_00L)
                        .setMinTotalItemsCost(MIN_ITEMS_PRICE)
                ).build()
        );
    }
}
