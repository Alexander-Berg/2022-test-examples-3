package ru.yandex.market.global.checkout.domain.promo;

import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.actualize.actualizer.PromoActualizer;
import ru.yandex.market.global.checkout.domain.actualize.actualizer.PromoInitialAllLimitedActualizer;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryCommonState;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory.CreatePromoBuilder;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserFreeDeliveryTest extends BaseFunctionalTest {
    protected static final long UID = 1;
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
            .withIgnoreAllExpectedNullFields(true)
            .build();
    private static final long DELIVERY_COST_FOR_RECIPIENT = 1000L;

    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;
    private final PromoCommandSerivce promoCommandSerivce;
    private final TestClock clock;

    private final PromoInitialAllLimitedActualizer promoInitialAllLimitedActualizer;
    private final PromoActualizer promoActualizer;

    @Test
    public void testCreateOrderWithFreeDelivery() {
        Promo promo = createPromo();
        OrderActualization orderActualization = createOrderActualization();


        orderActualization = promoInitialAllLimitedActualizer.actualize(orderActualization);
        orderActualization = promoActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setDeliveryCostForRecipient(0L)
                );
    }

    @Test
    void testFreeDeliveriesWithAdditionalGrant() {
        Promo promo = createPromo();
        OrderActualization orderActualization = createOrderActualization();

        // Потратили все 3 бесплатных доставки
        for (int i = 0; i < 3; i++) {
            createOrderWithUsageUserRecord(promo.getId(), UID);
        }

        // Добавили еще одно использование
        promoCommandSerivce.grantUsage(promo, UID);

        promoActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setDeliveryCostForRecipient(0L)
                );
    }

    @Test
    void testFreeDeliveriesUsed() {
        Promo promo = createPromo();
        OrderActualization orderActualization = createOrderActualization(promo.getName());

        // Потратили все 3 бесплатных доставки
        for (int i = 0; i < 3; i++) {
            createOrderWithUsageUserRecord(promo.getId(), UID);
        }

        promoActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setDeliveryCostForRecipient(DELIVERY_COST_FOR_RECIPIENT)
                );
    }

    private void createOrderWithUsageUserRecord(long promoId, long uid) {
        Order someOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setAppliedPromoIds(promoId))
                .build()
        ).getOrder();

        testPromoFactory.createUsageUserRecord(p -> p.setPromoId(promoId)
                .setUid(uid)
                .setUsed(true)
                .setOrderId(someOrder.getId())
                .setUsedAt(OffsetDateTime.now(clock))
        );
    }

    private Promo createPromo() {
        return testPromoFactory.createPromo(CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName("USER_FREE_DELIVERY")
                        .setDescription("Free 3 deliveries for all new users!")
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setLimitedUsagesCount(3)
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                )
                .setupArgs((a) -> new UserFreeDeliveryArgs()
                        .setBudgetCount(10)
                )
                .setupState(() -> new UserFreeDeliveryCommonState()
                        .setTotalCountUsed(0)
                )
                .build()
        );
    }

    private OrderActualization createOrderActualization(String... promos) {
        return TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setUid(UID)
                                .setDeliveryCostForRecipient(DELIVERY_COST_FOR_RECIPIENT)
                                .setPromocodes(promos)
                        )
                        .build()
        );
    }
}
