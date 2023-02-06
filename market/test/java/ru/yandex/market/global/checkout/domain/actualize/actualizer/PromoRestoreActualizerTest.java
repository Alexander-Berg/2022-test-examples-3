package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoRestoreActualizerTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(PromoRestoreActualizerTest.class).build();
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
            .withIgnoreAllExpectedNullFields(true)
            .build();

    private final PromoRestoreActualizer promoRestoreActualizer;
    private final TestOrderFactory testOrderFactory;

    @Test
    public void testRestoreUnchangedOrderItem() {
        OrderModel model = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("OFFER_1")
                                .setCount(2L)
                                .setPrice(10_00L)
                                .setTotalCostWithoutPromo(20_00L)
                                .setTotalCost(10_00L)
                ))
                .build()
        );

        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> model.getOrder())
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setOfferId("OFFER_1")
                                        .setCount(2L)
                                        .setPrice(10_00L)
                                        .setTotalCostWithoutPromo(20_00L)
                                        .setTotalCost(20_00L)
                        ))
                        .build()
        );

        actualization = promoRestoreActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactly(
                        new OrderItem()
                                .setOfferId("OFFER_1")
                                .setCount(2L)
                                .setPrice(10_00L)
                                .setTotalCostWithoutPromo(20_00L)
                                .setTotalCost(10_00L)
                );
    }


    @Test
    public void testUseNewItemOnPriceChange() {
        OrderModel model = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("OFFER_1")
                                .setCount(2L)
                                .setPrice(10_00L)
                                .setTotalCostWithoutPromo(20_00L)
                                .setTotalCost(10_00L)
                ))
                .build()
        );

        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> model.getOrder())
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setOfferId("OFFER_1")
                                        .setCount(2L)
                                        .setPrice(11_00L)
                                        .setTotalCostWithoutPromo(22_00L)
                                        .setTotalCost(22_00L)
                        ))
                        .build()
        );

        actualization = promoRestoreActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactly(
                        new OrderItem()
                                .setOfferId("OFFER_1")
                                .setCount(2L)
                                .setPrice(11_00L)
                                .setTotalCostWithoutPromo(22_00L)
                                .setTotalCost(22_00L)
                );
    }

    @Test
    public void testUseNewItemOnCountChange() {
        OrderModel model = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("OFFER_1")
                                .setCount(2L)
                                .setPrice(10_00L)
                                .setTotalCostWithoutPromo(20_00L)
                                .setTotalCost(10_00L)
                ))
                .build()
        );

        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> model.getOrder())
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setOfferId("OFFER_1")
                                        .setCount(3L)
                                        .setPrice(10_00L)
                                        .setTotalCostWithoutPromo(30_00L)
                                        .setTotalCost(30_00L)
                        ))
                        .build()
        );

        actualization = promoRestoreActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactly(
                        new OrderItem()
                                .setOfferId("OFFER_1")
                                .setCount(3L)
                                .setPrice(10_00L)
                                .setTotalCostWithoutPromo(30_00L)
                                .setTotalCost(30_00L)
                );
    }

    @Test
    public void testUseNewItem() {
        OrderModel model = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("OFFER_1")
                                .setCount(2L)
                                .setPrice(10_00L)
                                .setTotalCostWithoutPromo(20_00L)
                                .setTotalCost(10_00L)
                ))
                .build()
        );

        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> model.getOrder())
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setOfferId("OFFER_2")
                                        .setCount(3L)
                                        .setPrice(10_00L)
                                        .setTotalCostWithoutPromo(30_00L)
                                        .setTotalCost(30_00L)
                        ))
                        .build()
        );

        actualization = promoRestoreActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactly(
                        new OrderItem()
                                .setOfferId("OFFER_2")
                                .setCount(3L)
                                .setPrice(10_00L)
                                .setTotalCostWithoutPromo(30_00L)
                                .setTotalCost(30_00L)
                );
    }

    @Test
    public void testRestoreAppliedPromos() {
        OrderModel model = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setPromocodes("PROMOCODE")
                        .setAppliedPromoIds(1L)
                )
                .build()
        );

        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setId(model.getOrder().getId()))
                        .build()
        );

        actualization = promoRestoreActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setPromocodes("PROMOCODE")
                        .setAppliedPromoIds(1L)
                );
    }

    @Test
    public void testRestoreDeliveryCosts() {
        OrderModel model = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setDeliveryCostForRecipient(0L)
                        .setDeliveryCostForShop(0L)
                )
                .build()
        );

        OrderActualization actualization = testOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setId(model.getOrder().getId())
                                .setDeliveryCostForShop(10L)
                                .setDeliveryCostForRecipient(10L)
                        )
                        .build()
        );

        actualization = promoRestoreActualizer.actualize(actualization);
        Assertions.assertThat(actualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setDeliveryCostForRecipient(0L)
                        .setDeliveryCostForShop(0L)
                );
    }

}
