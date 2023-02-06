package ru.yandex.market.global.checkout.domain.promo;

import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.actualize.actualizer.PromoActualizer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderActualizationBuilder;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopFreeDeliveryTest extends BaseFunctionalTest {
    private static final long SHOP_ID = 1;
    private static final long DELIVERY_COST_FOR_SHOP = 1000L;

    private final PromoCommandSerivce promoCommandSerivce;
    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;
    private final PromoActualizer promoActualizer;
    private final TestClock clock;

    @Test
    void testSimpleUsageOfFreeDelivery() {
        Promo promo = testPromoFactory.createShopFreeDeliveryPromo();
        promoCommandSerivce.grantUsage(promo, SHOP_ID);

        OrderActualization before = createOrderActualization();
        OrderActualization after = promoActualizer.actualize(before);

        Assertions.assertThat(after.getOrder().getDeliveryCostForShop()).isZero();
    }

    @Test
    void testTwoGrantsMakes2OrdersDeliveryFree() {
        Promo promo = testPromoFactory.createShopFreeDeliveryPromo();
        createOrderWithUsageShopRecord(promo);
        createOrderWithUsageShopRecord(promo);

        OrderActualization before = createOrderActualization();
        OrderActualization after = promoActualizer.actualize(before);

        Assertions.assertThat(after.getOrder().getDeliveryCostForShop()).isNotZero();
    }

    private void createOrderWithUsageShopRecord(Promo promo) {
        Order someOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setAppliedPromoIds(promo.getId()))
                .build()
        ).getOrder();

        testPromoFactory.createUsageShopRecord(p -> p
                .setPromoId(promo.getId())
                .setShopId(SHOP_ID)
                .setOrderId(someOrder.getId())
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now(clock))
        );
    }

    private OrderActualization createOrderActualization() {
        return TestOrderFactory.buildOrderActualization(
                CreateOrderActualizationBuilder.builder()
                        .setupShop(s -> s.id(SHOP_ID))
                        .setupOrder(o -> o
                                .setDeliveryCostForShop(DELIVERY_COST_FOR_SHOP)
                        )
                        .build()
        );
    }
}
