package ru.yandex.market.core.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import ru.yandex.bolts.function.forhuman.Comparator;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.order.model.MbiOrderItemPromo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.BLUE_SET;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.CASHBACK;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.CHEAPEST_AS_GIFT;

@DbUnitDataSet(before = "db/datasource.csv")
class DbOrderServiceOrderItemPromosTest extends FunctionalTest {
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withComparatorForType(Comparator.naturalComparator(), BigDecimal.class)
                    .build();

    @Autowired
    private DbOrderService orderService;

    @Test
    @Description("Проверяем, что новые поля (partner_id, market_cashback_percent, partner_cashback_percent) читаются")
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemPromosTest_getOrderItemPromos.before.csv"
    )
    void test_getOrderItemPromos() {
        Map<Long, List<MbiOrderItemPromo>> actualItems = orderService.getOrderItemPromos(
                List.of(1L), PromoType.values()
        );

        assertThat(actualItems)
                .containsOnlyKeys(1L)
                .hasEntrySatisfying(1L, v -> assertThat(v)
                        .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                        .containsExactlyInAnyOrder(
                                itemPromo(CASHBACK, "cashback_1")
                                        .setPartnerId(465852L)
                                        .setMarketCashbackPercent(new BigDecimal("1.5"))
                                        .setPartnerCashbackPercent(new BigDecimal("13.5"))
                                        .build(),
                                itemPromo(BLUE_SET, "blue_set")
                                        .build()
                        )
                );
    }

    @Test
    @Description("Проверяем, что читаются только промо для переданных item_ids")
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemPromosTest_getOrderItemPromos.before.csv"
    )
    void test_getOrderItemPromos_selectByOrderItemIds() {
        Map<Long, List<MbiOrderItemPromo>> actualItems = orderService.getOrderItemPromos(
                List.of(2L, 4L, 5L, 6L), PromoType.values()
        );

        assertThat(actualItems)
                .containsOnlyKeys(2L, 4L, 5L)
                .hasEntrySatisfying(2L, v -> assertThat(v)
                        .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                        .containsExactlyInAnyOrder(
                                itemPromo(CASHBACK, "cashback_1").build()
                        ))
                .hasEntrySatisfying(4L, v -> assertThat(v)
                        .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                        .containsExactlyInAnyOrder(
                                itemPromo(CASHBACK, "cashback_1").build(),
                                itemPromo(CHEAPEST_AS_GIFT, "2=3").build()
                        ))
                .hasEntrySatisfying(5L, v -> assertThat(v)
                        .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                        .containsExactlyInAnyOrder(
                                itemPromo(CASHBACK, "cashback_1").build(),
                                itemPromo(CASHBACK, "cashback_2").build()
                        )
                );
    }

    @Test()
    @Description("Проверяем фильтрацию промо по типам")
    @DbUnitDataSet(
            before = "db/DbOrderServiceOrderItemPromosTest_getOrderItemPromos.before.csv"
    )
    void test_getOrderItemPromos_filterByPromoTypes() {
        Map<Long, List<MbiOrderItemPromo>> actualItems = orderService.getOrderItemPromos(
                List.of(2L, 3L, 4L, 5L, 6L), CHEAPEST_AS_GIFT, BLUE_SET
        );

        assertThat(actualItems)
                .containsOnlyKeys(4L)
                .hasEntrySatisfying(4L, v -> assertThat(v)
                        .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                        .containsExactlyInAnyOrder(
                                itemPromo(CHEAPEST_AS_GIFT, "2=3").build()
                        )
                );
    }

    private static MbiOrderItemPromo.Builder itemPromo(PromoType type, String marketPromoId) {
        return new MbiOrderItemPromo.Builder()
                .setPromoType(type)
                .setMarketPromoId(marketPromoId)
                .setSubsidy(new BigDecimal("12.8"));
    }

}
