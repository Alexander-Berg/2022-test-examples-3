package ru.yandex.market.global.checkout.domain.queue.task;

import java.time.OffsetDateTime;
import java.util.Comparator;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.jooq.JSONB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.PromoQueryService;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery.user.UserFreeDeliveryCommonState;
import ru.yandex.market.global.checkout.domain.promo.model.PromoTarget;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.domain.promo.model.PromoWithUsagesLeft;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RestorePromoTest extends BaseFunctionalTest {

    private static final long UID = 991000199;
    private static final RecursiveComparisonConfiguration COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withComparatorForType(
                            Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class
                    )
                    .withIgnoreCollectionOrder(true)
                    .withIgnoreAllExpectedNullFields(true)
                    .withIgnoredFieldsOfTypes(JSONB.class)
                    .build();
    private final TestOrderFactory testOrderFactory;
    private final TestPromoFactory testPromoFactory;
    private final RestorePromoConsumer restorePromoConsumer;
    private final PromoQueryService promoQueryService;

    private Promo freeDeliveryPromo;
    private Promo fixedDiscountPromo;

    @BeforeEach
    void setup() {
        freeDeliveryPromo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setLimitedUsagesCount(1)
                )
                .setupArgs((a) -> new UserFreeDeliveryArgs().setBudgetCount(10000))
                .setupState(() -> new UserFreeDeliveryCommonState().setTotalCountUsed(0))
                .build()
        );

        fixedDiscountPromo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setLimitedUsagesCount(1)
                )
                .setupArgs((a) -> new FixedDiscountArgs()
                        .setMinTotalItemsCost(0)
                        .setDiscount(5000)
                        .setBudget(5000)
                )
                .setupState(() -> new UserFreeDeliveryCommonState().setTotalCountUsed(0))
                .build()
        );


    }

    @Test
    public void testPromoUsagesIsRestored() {
        assertUsagesLeft(1, 1);

        OrderModel order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setAppliedPromoIds(freeDeliveryPromo.getId(), fixedDiscountPromo.getId())
                        .setUid(UID))
                .build()
        );

        testPromoFactory.createUsageUserRecord(s -> s
                .setOrderId(order.getOrder().getId())
                .setUid(order.getOrder().getUid())
                .setPromoId(fixedDiscountPromo.getId())
                .setValidTill(null)
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now())
        );

        testPromoFactory.createUsageUserRecord(s -> s
                .setOrderId(order.getOrder().getId())
                .setUid(order.getOrder().getUid())
                .setPromoId(freeDeliveryPromo.getId())
                .setValidTill(null)
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now())
        );

        assertUsagesLeft(0, 0);

        TestQueueTaskRunner.runTaskThrowOnFail(restorePromoConsumer, order.getOrder().getId());

        assertUsagesLeft(1, 1);
    }

    private void assertUsagesLeft(long freeDeliveryUsages, long fixedDiscountUsages) {
        Assertions.assertThat(promoQueryService.getPromosAvailableTo(PromoTarget.USER, UID))
                .usingRecursiveFieldByFieldElementComparator(COMPARISON_CONFIGURATION)
                .containsExactly(new PromoWithUsagesLeft()
                                .setPromo(freeDeliveryPromo)
                                .setValidTill(freeDeliveryPromo.getValidTill())
                                .setType(PromoType.FREE_DELIVERY_USER)
                                .setUsagesLeft(freeDeliveryUsages),
                        new PromoWithUsagesLeft()
                                .setPromo(fixedDiscountPromo)
                                .setValidTill(fixedDiscountPromo.getValidTill())
                                .setType(PromoType.FIXED_DISCOUNT)
                                .setUsagesLeft(fixedDiscountUsages)
                );
    }

}
