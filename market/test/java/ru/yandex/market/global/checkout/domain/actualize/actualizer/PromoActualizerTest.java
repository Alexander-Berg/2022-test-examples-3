package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountPromoApplyHandler;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryCommonState;
import ru.yandex.market.global.checkout.domain.promo.model.PromoApplication;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoActualizerTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(PromoActualizerTest.class).build();

    private static final long DISCOUNT = 50_00L;
    private static final long BUDGET = 50_00L;
    private static final String WELOME_PROMOCODE = "WELOME";
    private static final String SORRY_PROMOCODE = "SORRY";
    private static final long UID = 1L;

    private final Clock clock;

    private final PromoInitialAllLimitedActualizer promoInitialAllLimitedActualizer;
    private final PromoActualizer promoActualizer;
    private final ClearUnusedPromocodesActualizer clearUnusedPromocodesActualizer;
    private final OrderItemsCostsActualizer orderItemsCostsActualizer;
    private final OrderTotalCostsActualizer orderTotalCostsActualizer;

    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;

    @Test
    public void testFreeDeliveryResetDeliveryCost() {
        Promo promo = createFreeDeliveryPromo();
        OrderActualization actualizationBefore = TestOrderFactory.buildOrderActualization();

        Assertions.assertThat(actualizationBefore.getOrder().getDeliveryCostForRecipient())
                .isGreaterThan(0);

        OrderActualization actualizationAfter = promoInitialAllLimitedActualizer.actualize(actualizationBefore);
        actualizationAfter = promoActualizer.actualize(actualizationAfter);

        Assertions.assertThat(actualizationAfter.getOrder().getDeliveryCostForRecipient())
                .isEqualTo(0);
    }

    @Test
    public void testFreeDeliveryProduceCorrectApplication() {
        Promo promo = createFreeDeliveryPromo();
        OrderActualization actualizationBefore = TestOrderFactory.buildOrderActualization();

        OrderActualization actualizationAfter = promoInitialAllLimitedActualizer.actualize(actualizationBefore);
        actualizationAfter = promoActualizer.actualize(actualizationAfter);

        Assertions.assertThat(actualizationAfter.getOrder().getAppliedPromoIds())
                .contains(promo.getId());

        Assertions.assertThat(actualizationAfter.getAppliedPromos())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .containsExactlyElementsOf(List.of(
                        new PromoApplication()
                                .setNewSubject(new PromoUser()
                                        .setPromoId(promo.getId())
                                        .setUid(actualizationBefore.getOrder().getUid())
                                        .setUsed(true)
                                )
                                .setNewState(new UserFreeDeliveryCommonState()
                                        .setTotalCountUsed(1)
                                )
                ));
    }

    @Test
    public void testUsagesUsedCorrectly() {
        Promo promocode = createWelcomePromocode(0, DISCOUNT, OffsetDateTime.now(clock).plusDays(1));
        Promo freeDeliveryPromo = createFreeDeliveryPromo();
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE);

        createOrderWithUsageUserRecord(freeDeliveryPromo.getId(), actualization.getOrder().getUid());

        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = orderItemsCostsActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = orderTotalCostsActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getWarnings())
                .isEmpty();

        Assertions.assertThat(actualization.getOrder().getTotalItemsCost() -
                actualization.getOrder().getTotalItemsCostWithPromo()
        ).isEqualTo(DISCOUNT);
    }

    @Test
    public void testWelcomeGivesDiscount() {
        Promo promo = createWelcomePromocode(0, DISCOUNT, OffsetDateTime.now(clock).plus(1, ChronoUnit.DAYS));
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE);

        actualization = orderItemsCostsActualizer.actualize(actualization);
        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = orderTotalCostsActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getOrder().getTotalItemsCost() -
                actualization.getOrder().getTotalItemsCostWithPromo()
        ).isEqualTo(DISCOUNT);
    }

    @Test
    public void testWelcomeSplitDiscountCorrectly() {
        Promo promo = createWelcomePromocode(
                0, DISCOUNT, OffsetDateTime.now(clock).plus(1, ChronoUnit.DAYS)
        );
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE);

        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = orderItemsCostsActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = orderTotalCostsActualizer.actualize(actualization);

        for (OrderItem orderItem : actualization.getOrderItems()) {
            double itemCostShare = (double) orderItem.getTotalCostWithoutPromo()
                    / actualization.getOrder().getTotalItemsCost();
            double discountShare = (double) (orderItem.getTotalCostWithoutPromo() - orderItem.getTotalCost())
                    / DISCOUNT;

            Assertions.assertThat(Math.abs(itemCostShare - discountShare))
                    .isLessThan(0.1);
        }
    }

    @Test
    public void testWelcomeSplitBigDiscountCorrectly() {
        Promo promo = createWelcomePromocode(
                0, 1000_00L, OffsetDateTime.now(clock).plus(1, ChronoUnit.DAYS)
        );
        OrderActualization actualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setPromocodes(WELOME_PROMOCODE))
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setPrice(101L)
                                        .setCount(1L)
                        ))
                        .build()
        );

        actualization = orderItemsCostsActualizer.actualize(actualization);
        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = orderTotalCostsActualizer.actualize(actualization);

        for (OrderItem orderItem : actualization.getOrderItems()) {
            Assertions.assertThat(orderItem.getTotalCost())
                    .isGreaterThanOrEqualTo(FixedDiscountPromoApplyHandler.MIN_COST_AFTER_DISCOUNT);
        }

        Assertions.assertThat(actualization.getOrder().getTotalCost())
                .isGreaterThan(0);
    }

    @Test
    public void testWelcomeSplitForSmallCostCorrectly() {
        Promo promo = createWelcomePromocode(
                0, DISCOUNT, OffsetDateTime.now(clock).plusDays(1)
        );
        OrderActualization actualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setPromocodes(WELOME_PROMOCODE))
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setPrice(111_00L)
                                        .setCount(1L),
                                RANDOM.nextObject(OrderItem.class)
                                        .setPrice(100L)
                                        .setCount(1L)
                        ))
                        .build()
        );

        actualization = orderItemsCostsActualizer.actualize(actualization);
        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = orderTotalCostsActualizer.actualize(actualization);

        for (OrderItem orderItem : actualization.getOrderItems()) {
            Assertions.assertThat(orderItem.getTotalCost())
                    .isGreaterThanOrEqualTo(FixedDiscountPromoApplyHandler.MIN_COST_AFTER_DISCOUNT);
        }

        Assertions.assertThat(actualization.getOrder().getTotalItemsCost() -
                actualization.getOrder().getTotalItemsCostWithPromo()
        ).isEqualTo(DISCOUNT);
    }


    @Test
    public void testWelcomeWarningMinimalCost() {
        Promo promo = createWelcomePromocode(
                10000000_00L, DISCOUNT, OffsetDateTime.now(clock).plus(1, ChronoUnit.DAYS)
        );
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE);

        actualization = orderItemsCostsActualizer.actualize(actualization);
        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = orderTotalCostsActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getWarnings())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .contains(new ActualizationError()
                        .setPromoName(promo.getName())
                        .setCode(ActualizationError.Code.PROMO_PROBLEM)
                        .setMessageTankerKey("promocode_bottom_sheet.cart_cost_10000000_error")
                );
    }

    @Test
    public void testWelcomeWarningExpired() {
        Promo promo = createWelcomePromocode(
                0L, DISCOUNT, OffsetDateTime.now(clock).minus(1, ChronoUnit.DAYS)
        );
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE);

        actualization = orderItemsCostsActualizer.actualize(actualization);
        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = orderTotalCostsActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getWarnings())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .contains(new ActualizationError()
                        .setPromoName(promo.getName())
                        .setCode(ActualizationError.Code.PROMO_PROBLEM)
                        .setMessageTankerKey("promocode_bottom_sheet.expired_error")
                );
    }

    @Test
    public void testSorryWarningUsed() {
        Promo promo = createSorryPromocode();
        OrderActualization actualization = createOrderActualization(UID, SORRY_PROMOCODE);
        createOrderWithUsageUserRecord(promo.getId(), actualization.getOrder().getUid());

        actualization = promoActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getWarnings())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .contains(new ActualizationError()
                        .setPromoName(promo.getName())
                        .setCode(ActualizationError.Code.PROMO_PROBLEM)
                        .setMessageTankerKey("promocode_bottom_sheet.used_error")
                );
    }

    @Test
    public void testIncorrectPromoWarning() {
        Promo promo = createSorryPromocode();
        OrderActualization actualization = createOrderActualization(UID, "INCORRECT_PROMO");
        createOrderWithUsageUserRecord(promo.getId(), actualization.getOrder().getUid());

        actualization = promoActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getWarnings())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .contains(new ActualizationError()
                        .setPromoName("INCORRECT_PROMO")
                        .setCode(ActualizationError.Code.PROMO_PROBLEM)
                        .setMessageTankerKey("promocode_bottom_sheet.invalid_error")
                );
    }

    @Test
    public void testActualizationReturnAllPromocodes() {
        Promo promocode = createWelcomePromocode(0, DISCOUNT, OffsetDateTime.now(clock).plusDays(1));
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE, "INCORRECT_PROMO");

        actualization = promoActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getOrder().getPromocodes())
                .containsExactlyInAnyOrder(WELOME_PROMOCODE, "INCORRECT_PROMO");
    }

    @Test
    public void testClearUnusedPromocodesActualizerDoCleanup() {
        Promo promocode = createWelcomePromocode(0, DISCOUNT, OffsetDateTime.now(clock).plusDays(1));
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE, "INCORRECT_PROMO");

        actualization = promoInitialAllLimitedActualizer.actualize(actualization);
        actualization = promoActualizer.actualize(actualization);
        actualization = clearUnusedPromocodesActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getOrder().getPromocodes())
                .containsExactly(WELOME_PROMOCODE);
    }

    @Test
    public void testWelomePromocodeAdditionalUsageWorks() {
        Promo promocode = createWelcomePromocode(0, DISCOUNT, OffsetDateTime.now(clock).plusDays(1));
        OrderActualization actualization = createOrderActualization(UID, WELOME_PROMOCODE);

        createOrderWithUsageUserRecord(promocode.getId(), actualization.getOrder().getUid());
        createOrderWithUsageUserRecord(promocode.getId(), actualization.getOrder().getUid());
        actualization = promoActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getWarnings())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .containsExactly(new ActualizationError()
                        .setPromoName(WELOME_PROMOCODE)
                        .setMessageTankerKey("promocode_bottom_sheet.used_error")
                );

        actualization = createOrderActualization(UID, WELOME_PROMOCODE);
        createUsageUserRecord(promocode.getId(), actualization.getOrder().getUid(), null, false);
        actualization = promoActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getWarnings())
                .isEmpty();
    }


    private PromoUser createOrderWithUsageUserRecord(long promoId, long uid) {
        Order someOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setAppliedPromoIds(promoId))
                .build()
        ).getOrder();
        return createUsageUserRecord(promoId, uid, someOrder.getId(), true);
    }

    private PromoUser createUsageUserRecord(long promoId, long uid, Long orderId, boolean used) {
        return testPromoFactory.createUsageUserRecord(u -> u
                .setPromoId(promoId)
                .setUid(uid)
                .setUsed(used)
                .setOrderId(orderId)
                .setUsedAt(used ? OffsetDateTime.now(clock) : null)
        );
    }

    private Promo createFreeDeliveryPromo() {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName("FREE_DELIVERY_LIMITED_UNCONDITIONAL")
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setLimitedUsagesCount(1)
                )
                .setupArgs((a) -> new UserFreeDeliveryArgs().setBudgetCount(100_00))
                .setupState(() -> new UserFreeDeliveryCommonState().setTotalCountUsed(0))
                .build()
        );
    }

    private Promo createWelcomePromocode(long minimalCost, long discount, OffsetDateTime validTill) {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName(WELOME_PROMOCODE)
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setValidTill(validTill)
                        .setLimitedUsagesCount(1)
                )
                .setupArgs((a) -> new FixedDiscountArgs()
                        .setDiscount(discount)
                        .setMinTotalItemsCost(minimalCost)
                        .setBudget(discount))
                .setupState(() -> new FixedDiscountCommonState().setBudgetUsed(0L))
                .build()
        );
    }

    private Promo createSorryPromocode() {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName(SORRY_PROMOCODE)
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setAccessType(EPromoAccessType.ISSUED)
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setLimitedUsagesCount(1)
                )
                .setupArgs((a) -> new FixedDiscountArgs()
                        .setDiscount(DISCOUNT)
                        .setMinTotalItemsCost(0)
                        .setBudget(BUDGET))
                .setupState(() -> new FixedDiscountCommonState().setBudgetUsed(0L))
                .build()
        );
    }

    private OrderActualization createOrderActualization(long uid, String... promocodes) {
        return TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setPromocodes(promocodes)
                                .setUid(uid)
                        )
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setPrice(70_00L)
                                        .setCount(1L),
                                RANDOM.nextObject(OrderItem.class)
                                        .setPrice(15_33L)
                                        .setCount(2L),
                                RANDOM.nextObject(OrderItem.class)
                                        .setPrice(10_00L)
                                        .setCount(1L)
                        ))
                        .build()
        );
    }

}
