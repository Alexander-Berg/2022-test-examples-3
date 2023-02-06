package ru.yandex.market.global.checkout.domain.promo;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.promo.apply.first_order_discount.FirstOrderDiscountCommonState;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount_no_adult.FixedDiscountNoAdultArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount_no_adult.FixedDiscountNoAdultPromoApplyHandler;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FixedDiscountNoAdultTest extends BasePromoTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(
            FirstOrderDiscountTest.class
    ).build();
    private static final long MIN_ITEMS_PRICE = 100_00L;
    private static final long DISCOUNT = 30_00L;
    private static final long USER_UID = 2L;

    private final FixedDiscountNoAdultPromoApplyHandler promoApplyHandler;

    private final Clock clock;
    private final TestOrderFactory testOrderFactory;
    private final TestPromoFactory testPromoFactory;

    @Test
    public void testDiscountAvailableForNoAdultOrder() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE, false);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo() - DISCOUNT);
    }

    @Test
    public void testDiscountNotAvailableForAdultOrder() {
        Promo promo = createPromo();
        OrderActualization actualization = createOrderActualization(USER_UID, MIN_ITEMS_PRICE, true);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        promoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrderItems().get(0).getTotalCost())
                .isEqualTo(actualization.getOrderItems().get(0).getTotalCostWithoutPromo());

        Assertions.assertThat(actualization.getWarnings())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .contains(new ActualizationError()
                        .setMessageTankerKey("promocode_bottom_sheet.cart_have_unacceptable_items")
                );
    }


    private PromoUser createPromoUser(long uid) {
        return new PromoUser()
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now(clock))
                .setUid(uid);
    }

    private OrderActualization createOrderActualization(long uid, long itemPrice, boolean adult) {
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
                                        .setAdult(adult)
                                        .setCount(1L)
                        ))
                        .build()
        );
    }

    private Promo createPromo() {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName("FIXED_DISCOUNT_PROMO_NO_ADULT_30")
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                        .setType(PromoType.FIXED_DISCOUNT_NO_ADULT.name())
                        .setApplicationType(EPromoApplicationType.PROMOCODE)
                        .setValidTill(OffsetDateTime.now(clock))
                )
                .setupState(() -> new FirstOrderDiscountCommonState()
                        .setBudgetUsed(0)
                )
                .setupArgs((a) -> new FixedDiscountNoAdultArgs()
                        .setDiscount(DISCOUNT)
                        .setBudget(1000_00L)
                        .setMinTotalItemsCost(MIN_ITEMS_PRICE)
                ).build()
        );
    }
}
