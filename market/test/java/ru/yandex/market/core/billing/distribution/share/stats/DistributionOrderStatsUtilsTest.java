package ru.yandex.market.core.billing.distribution.share.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.billing.distribution.share.DistributionShareAdditionalInfo;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderCalculation;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderItemStatusHistory;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsRaw;
import ru.yandex.market.core.billing.tasks.reports.distribution.cpa.AdmitadOrderStatus;
import ru.yandex.market.core.order.model.MbiOrderStatus;

public class DistributionOrderStatsUtilsTest {
    private static final LocalDateTime DT_2000_01_01 = LocalDate.of(2000, 1, 1).atStartOfDay();
    private static final LocalDateTime DT_2000_01_02 = LocalDate.of(2000, 1, 2).atStartOfDay();

    private static final LocalDateTime NOW = LocalDate.of(2000, 1, 13).atStartOfDay();

    @Test
    void testIsRecentStatsDiffers() {
        Assertions.assertThat(DistributionOrderStatsUtils.isRecentStatsDiffers(
                DistributionOrderStatsRaw.builder()
                        .setHasRecentStats(true)
                        .setRecentDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                        .setRecentPartnerPaymentNoVat(BigDecimal.ONE)
                        .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                        .build(),
                AdmitadOrderStatus.ON_HOLD,
                BigDecimal.TEN,
                Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF),
                1
        )).isTrue();

        Assertions.assertThat(DistributionOrderStatsUtils.isRecentStatsDiffers(
                DistributionOrderStatsRaw.builder()
                        .setHasRecentStats(true)
                        .setRecentDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                        .setRecentPartnerPaymentNoVat(BigDecimal.ONE)
                        .setRecentAdditionalInfo(Set.of(
                                DistributionShareAdditionalInfo.GENERAL_TARIFF,
                                DistributionShareAdditionalInfo.FRAUD
                        ))
                        .build(),
                AdmitadOrderStatus.ON_HOLD,
                BigDecimal.ONE,
                Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF),
                1
        )).isTrue();

        Assertions.assertThat(DistributionOrderStatsUtils.isRecentStatsDiffers(
                DistributionOrderStatsRaw.builder()
                        .setHasRecentStats(true)
                        .setRecentDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                        .setRecentPartnerPaymentNoVat(BigDecimal.ONE)
                        .setRecentAdditionalInfo(Set.of(
                                DistributionShareAdditionalInfo.GENERAL_TARIFF
                        ))
                        .build(),
                AdmitadOrderStatus.APPROVED,
                BigDecimal.ONE,
                Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF),
                1
        )).isTrue();

        Assertions.assertThat(DistributionOrderStatsUtils.isRecentStatsDiffers(
                DistributionOrderStatsRaw.builder()
                        .setHasRecentStats(false)
                        .build(),
                AdmitadOrderStatus.ON_HOLD,
                BigDecimal.ONE,
                Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF),
                1
        )).isTrue();

        Assertions.assertThat(DistributionOrderStatsUtils.isRecentStatsDiffers(
                DistributionOrderStatsRaw.builder()
                        .setHasRecentStats(true)
                        .setRecentDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                        .setRecentPartnerPaymentNoVat(BigDecimal.ONE)
                        .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                        .build(),
                AdmitadOrderStatus.ON_HOLD,
                BigDecimal.ONE,
                Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF),
                1
        )).isFalse();

    }

    @Test
    void testIsItemCanceled() {
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelled(DistributionOrderStatsRaw.builder()
                .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                .setItemsCount(1)
                .build()
        )).isTrue();
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelled(DistributionOrderStatsRaw.builder()
                .setOrderStatus(MbiOrderStatus.CANCELLED_IN_PROCESSING)
                .setItemsCount(1)
                .build()
        )).isTrue();
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelled(DistributionOrderStatsRaw.builder()
                .setOrderStatus(MbiOrderStatus.CANCELLED_BEFORE_PROCESSING)
                .setItemsCount(1)
                .build()
        )).isTrue();
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelled(DistributionOrderStatsRaw.builder()
                .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                .setItemsCount(1)
                .setShopId(534145L) // Здравсити
                .build()
        )).isFalse();
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelled(DistributionOrderStatsRaw.builder()
                .setOrderStatus(MbiOrderStatus.DELIVERED)
                .setItemsCount(1)
                .build()
        )).isFalse();
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelled(DistributionOrderStatsRaw.builder()
                .setOrderStatus(MbiOrderStatus.DELIVERED)
                .setItemsCount(0)
                .build()
        )).isFalse();
    }

    @Test
    void testGetItemBilledCost() {
        Assertions.assertThat(DistributionOrderStatsUtils.getItemBilledCost(
                DistributionOrderStatsRaw.builder()
                        .setOrderStatus(MbiOrderStatus.PROCESSING)
                        .setItemsCount(2)
                        .setItemsReturnCount(1)
                        .setItemPrice(BigDecimal.ONE)
                        .build()
        )).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void testGetApprovalTimeCalcDelivered() {
        DistributionOrderItemStatusHistory history = new DistributionOrderItemStatusHistory();
        history.getItems().addAll(Arrays.asList(
                DistributionOrderStatsRaw.builder()
                        .setClid(1L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setItemsCount(1)
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build(),
                DistributionOrderStatsRaw.builder()
                        .setClid(1L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setItemsCount(1)
                        .setOrderStatusChangeTime(DT_2000_01_01)
                        .setOrderStatus(MbiOrderStatus.PROCESSING)
                        .build()
        ));
        Assertions.assertThat(DistributionOrderStatsUtils.getApprovalTime(history, NOW))
                .isEqualTo(DT_2000_01_02.plusDays(14));
    }

    @Test
    void testGetApprovalTimeRespectZdravCity() {
        DistributionOrderItemStatusHistory history = new DistributionOrderItemStatusHistory();
        history.getItems().add(
                DistributionOrderStatsRaw.builder()
                        .setClid(1L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setItemsCount(1)
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                        .build()
        );
        Assertions.assertThat(DistributionOrderStatsUtils.getApprovalTime(history, NOW)).isNull();

        DistributionOrderItemStatusHistory history2 = new DistributionOrderItemStatusHistory();
        history2.getItems().add(
                DistributionOrderStatsRaw.builder()
                        .setClid(1L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setItemsCount(1)
                        .setShopId(534145L)
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                        .build()
        );
        Assertions.assertThat(DistributionOrderStatsUtils.getApprovalTime(
                history2, NOW
        )).isEqualTo(DT_2000_01_02.plusDays(14));
    }

    @Test
    void testGetApprovalTimeIgnoreProcessing() {
        DistributionOrderItemStatusHistory history = new DistributionOrderItemStatusHistory();
        history.getItems().addAll(List.of(
                DistributionOrderStatsRaw.builder()
                        .setClid(1L)
                        .setOrderId(2)
                        .setItemId(2)
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.PROCESSING)
                        .build(),
                DistributionOrderStatsRaw.builder()
                        .setClid(1L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                        .build(),
                DistributionOrderStatsRaw.builder()
                        .setClid(1L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setOrderStatusChangeTime(DT_2000_01_01)
                        .setOrderStatus(MbiOrderStatus.PROCESSING)
                        .build()
        ));
        Assertions.assertThat(DistributionOrderStatsUtils.getApprovalTime(history, NOW)).isNull();
    }

    @Test
    void testGetOrderBilledCostIgnoreCanceled() {
        Assertions.assertThat(DistributionOrderStatsUtils.getOrderBilledCost(
                new DistributionOrderCalculation(List.of(
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(2)
                                .setItemId(2)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .build(),
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(1)
                                .setItemId(1)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                                .build(),
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(1)
                                .setItemId(1)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_01)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .build()
                ))
        )).isEqualTo(BigDecimal.valueOf(2));
    }

    @Test
    void testGetOrderBilledCostSumNotCanceled() {
        Assertions.assertThat(DistributionOrderStatsUtils.getOrderBilledCost(
                new DistributionOrderCalculation(List.of(
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(2)
                                .setItemId(2)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .build(),
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(1)
                                .setItemId(1)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .build()
                ))
        )).isEqualTo(BigDecimal.valueOf(2));
    }

    @Test
    void testGetMapOrderBilledCostIgnoreCanceled() {
        Assertions.assertThat(DistributionOrderStatsUtils.getMapOrderBilledCost(
                new DistributionOrderCalculation(List.of(
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(2)
                                .setItemId(2)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .build(),
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(1)
                                .setItemId(1)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                                .build(),
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(1)
                                .setItemId(1)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_01)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .build()
                ))
        ).get(2L)).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void testGeMapOrderBilledCostSumNotCanceled() {
        Assertions.assertThat(DistributionOrderStatsUtils.getMapOrderBilledCost(
                new DistributionOrderCalculation(List.of(
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(2)
                                .setItemId(2)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .build(),
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(1)
                                .setItemId(1)
                                .setItemsCount(2)
                                .setItemsReturnCount(1)
                                .setItemPrice(BigDecimal.ONE)
                                .setOrderStatusChangeTime(DT_2000_01_02)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .build()
                ))
        ).get(2L)).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void testGetItemBilledCostOverMinimumPayoutLimit() {
        Assertions.assertThat(DistributionOrderStatsUtils.getItemBilledCost(
                DistributionOrderStatsRaw.builder()
                        .setOrderId(1)
                        .setItemId(1)
                        .setItemsCount(2)
                        .setItemsReturnCount(1)
                        .setItemPrice(BigDecimal.ONE)
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.UNKNOWN)
                        .build()
        )).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void testIsItemCancelledByAllItemsReturned() {
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelledByAdditionalInfos(
                Set.of(DistributionShareAdditionalInfo.ALL_ITEMS_RETURNED)
        )).isTrue();
    }

    @Test
    void testIsItemCancelledByFullCartCoupon() {
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelledByAdditionalInfos(
                Set.of(DistributionShareAdditionalInfo.FULL_CART_COUPON)
        )).isTrue();
    }

    @Test
    void testIsItemCancelledByPromoCode() {
        Assertions.assertThat(DistributionOrderStatsUtils.isItemCancelledByAdditionalInfos(
                Set.of(DistributionShareAdditionalInfo.PARTNER_PROMO_CODE)
        )).isTrue();
    }

    @Test
    void testApprovalTimeCalcStaleUpdate() {
        DistributionOrderItemStatusHistory history = new DistributionOrderItemStatusHistory();
        history.getItems().addAll(List.of(
                DistributionOrderStatsRaw.builder()
                        .setClid(2396891L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setItemsCount(1)
                        .setOrderStatusChangeTime(LocalDate.of(1999, 10, 11).atStartOfDay())
                        .setOrderCreationTime(LocalDate.of(1999, 10, 10).atStartOfDay())
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build(),
                DistributionOrderStatsRaw.builder()
                        .setClid(2396891L)
                        .setOrderId(1)
                        .setItemId(1)
                        .setItemsCount(1)
                        .setOrderStatusChangeTime(LocalDate.of(1999, 10, 10).atStartOfDay())
                        .setOrderCreationTime(LocalDate.of(1999, 10, 10).atStartOfDay())
                        .setOrderStatus(MbiOrderStatus.PROCESSING)
                        .build()
        ));
        Assertions.assertThat(DistributionOrderStatsUtils.getApprovalTime(history, NOW))
                .isEqualTo(NOW);
    }

    @Test
    void testApprovalTimeZdravcity() {
        DistributionOrderStatsRaw.Builder item =  DistributionOrderStatsRaw.builder()
                .setClid(333L)
                .setOrderId(1)
                .setItemId(1)
                .setItemsCount(1)
                .setOrderCreationTime(LocalDate.of(1999, 10, 10).atStartOfDay())
                .setShopId(534145L); //Zdravcity
        DistributionOrderItemStatusHistory history = new DistributionOrderItemStatusHistory();
        history.getItems().addAll(List.of(
                    item.but()
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build(),
                    item.but()
                        .setOrderStatusChangeTime(DT_2000_01_01)
                        .setOrderStatus(MbiOrderStatus.PICKUP)
                        .build()
        ));
        Assertions.assertThat(DistributionOrderStatsUtils.getApprovalTime(history, NOW))
                .isEqualTo(LocalDate.of(2000, 1, 15).atStartOfDay());
    }

    @Test
    void testApprovalTimeZdravcity2() {
        DistributionOrderStatsRaw.Builder item =  DistributionOrderStatsRaw.builder()
                .setClid(333L)
                .setOrderId(1)
                .setItemId(1)
                .setItemsCount(1)
                .setOrderCreationTime(LocalDate.of(1999, 10, 10).atStartOfDay())
                .setShopId(534145L); //Zdravcity
        DistributionOrderItemStatusHistory history = new DistributionOrderItemStatusHistory();
        history.getItems().addAll(List.of(
                item.but()
                        .setOrderStatusChangeTime(DT_2000_01_02)
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build(),
                item.but()
                        .setOrderStatusChangeTime(DT_2000_01_01)
                        .setOrderStatus(MbiOrderStatus.PICKUP)
                        .setRecentApprovalTime(LocalDate.of(2000, 1, 13).atStartOfDay())
                        .build()
        ));
        Assertions.assertThat(DistributionOrderStatsUtils.getApprovalTime(history, NOW))
                .isEqualTo(LocalDate.of(2000, 1, 13).atStartOfDay());
    }

    @Test
    public void testIsOrderIncomplete() {
        Assertions.assertThat(DistributionOrderStatsUtils.isOrderIncomplete(
                DistributionOrderStatsRaw.builder()
                    .setOrderId(1)
                    .setItemId(1)
                    .setOrderStatus(MbiOrderStatus.UNPAID)
                    .setOrderCreationTime(LocalDate.of(2022, Month.JULY, 5).atTime(12, 0))
                .build())).isTrue();
        Assertions.assertThat(DistributionOrderStatsUtils.isOrderIncomplete(
                DistributionOrderStatsRaw.builder()
                        .setOrderId(1)
                        .setItemId(1)
                        .setOrderStatus(MbiOrderStatus.PROCESSING)
                        .setOrderCreationTime(LocalDate.of(2022, Month.JULY, 5).atTime(12, 0))
                        .build())).isFalse();
        Assertions.assertThat(DistributionOrderStatsUtils.isOrderIncomplete(
                DistributionOrderStatsRaw.builder()
                        .setOrderId(1)
                        .setItemId(1)
                        .setOrderStatus(MbiOrderStatus.UNPAID)
                        .setOrderCreationTime(LocalDate.of(2022, Month.JULY, 2).atTime(12, 0))
                        .build())).isFalse();
    }
}
