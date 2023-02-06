package ru.yandex.market.billing.distribution.share.stats;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.BigDecimalComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.distribution.share.DistributionTariffRateAndName;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.core.billing.distribution.share.DistributionShareAdditionalInfo;
import ru.yandex.market.core.billing.distribution.share.DistributionTariffName;
import ru.yandex.market.core.billing.distribution.share.stats.DistributionOrderStatsUtils;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderItemStatusHistory;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStats;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsRaw;
import ru.yandex.market.core.billing.tasks.reports.distribution.cpa.AdmitadOrderStatus;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.report.model.Color;
import ru.yandex.market.core.util.DateTimes;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DistributionOrderStatsCalculatorTest {
    private static final Instant NOW = Instant.parse("2022-01-02T00:00:00.00Z");
    private static final LocalDateTime ORDER_CREATION_TIME = LocalDateTime.parse("2022-01-01T00:00:00");
    private static final LocalDateTime ORDER_CREATION_TIME_PREV = LocalDateTime.parse("2021-01-01T00:00:00");

    private static final DistributionTariffRateAndName CEHAC_TARIFF =
            new DistributionTariffRateAndName(BigDecimal.valueOf(0.5), DistributionTariffName.CEHAC, null);

    private static final DistributionOrderStatsRaw.Builder RAW_RECORD_1_1 = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(1)
            .setOrderStatus(MbiOrderStatus.PROCESSING)
            .setOrderStatusEventId(1)
            .setColor(Color.WHITE)
            .setItemId(11)
            .setOfferId("offer_999_1")
            .setShopId(111)
            .setFeedId(999)
            .setCategoryId(9999)
            .setItemsCount(1)
            .setItemsReturnCount(0)
            .setItemPrice(BigDecimal.valueOf(300_00))
            .setClid(1L)
            .setVid("vid_1_1")
            .setDistrType(1)
            .setMsku(99999L)
            .setIsFirstOrder(false)
            .setIsFraud(false)
            .setIsOverLimit(false)
            .setOrderCreationTime(ORDER_CREATION_TIME)
            .setOrderStatusChangeTime(ORDER_CREATION_TIME)
            .setItemDataUpdateTime(ORDER_CREATION_TIME)
            .setFraudUpdateTime(null)
            .setItemsReturnTime(null)
            .setHasRecentStats(false)
            .setRecentAdditionalInfo(Set.of())
            .setDeliveryRegionId(35L);

    private static final DistributionOrderStatsRaw.Builder RAW_RECORD_1_2 = RAW_RECORD_1_1.but()
            .setItemId(12);

    private static final DistributionOrderStatsRaw.Builder RAW_RECORD_2_1 = RAW_RECORD_1_1.but()
            .setOrderStatusEventId(2)
            .setOrderId(2)
            .setItemId(21);

    private static final DistributionOrderStatsRaw.Builder RAW_RECORD_2_2 = RAW_RECORD_1_1.but()
            .setOrderStatusEventId(2)
            .setOrderId(2)
            .setItemId(22);

    private static final DistributionOrderStats.Builder STATS_RECORD_1_1 = DistributionOrderStats.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(1)
            .setOrderStatus(MbiOrderStatus.PROCESSING)
            .setOrderStatusEventId(1)
            .setItemId(11)
            .setOfferId("offer_999_1")
            .setFeedId(999)
            .setCategoryId(9999)
            .setItemsCount(1)
            .setItemsReturnCount(0)
            .setItemPrice(BigDecimal.valueOf(300_00))
            .setClid(1L)
            .setVid("vid_1_1")
            .setDistrType(1)
            .setMsku(99999L)
            .setIsFirstOrder(false)
            .setOrderCreationTime(ORDER_CREATION_TIME)
            .setOrderStatusChangeTime(ORDER_CREATION_TIME)
            .setDistributionStatus(AdmitadOrderStatus.PROCESSING)
            .setOrderBilledCost(BigDecimal.valueOf(600_00))
            .setOrderPaymentNoVat(BigDecimal.valueOf(300_00.00))
            .setItemBilledCost(BigDecimal.valueOf(300_00))
            .setPartnerPaymentNoVat(BigDecimal.valueOf(150_00.00))
            .setPartnerPaymentNoVatMax(BigDecimal.valueOf(150_00.00))
            .setPartnerPaymentWithVat(BigDecimal.valueOf(180_00.00))
            .setTariffRate(CEHAC_TARIFF.getTariffRate())
            .setTariffName(CEHAC_TARIFF.getTariffName())
            .setTariffGrid("general")
            .setStatsCreationTime(DateTimes.toLocalDateTime(NOW))
            .setAdditionalInfos(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
            .setDeliveryRegionId(10995L);


    private static final DistributionOrderStats.Builder STATS_RECORD_1_2 = STATS_RECORD_1_1.but()
            .setItemId(12);

    private static final DistributionOrderStats.Builder STATS_RECORD_2_1 = STATS_RECORD_1_1.but()
            .setOrderStatusEventId(2)
            .setOrderId(2)
            .setItemId(21);

    private static final DistributionOrderStats.Builder STATS_RECORD_2_2 = STATS_RECORD_1_1.but()
            .setOrderStatusEventId(2)
            .setOrderId(2)
            .setItemId(22);

    private static final Comparator<BigDecimal> BIG_DECIMAL_AS_DOUBLE_COMPARATOR =
            Comparator.nullsFirst(Comparator.comparing(BigDecimal::doubleValue));

    private TestableClock clock = new TestableClock();

    @Mock
    private DistributionOrderStatsTariffService distributionOrderStatsTariffService;

    @Mock
    private DistributionOrderStatsRegionalSettings distributionOrderStatsRegionalSettings;

    private DistributionOrderStatsCalculator distributionOrderStatsCalculator;

    @BeforeEach
    public void setup() {
        clock.setFixed(NOW, ZoneOffset.UTC);

        when(distributionOrderStatsTariffService.getTariff(any(), any(), any(), any()))
                .thenReturn(CEHAC_TARIFF);
        when(distributionOrderStatsRegionalSettings.resolveRegionRepublicLevel(35L)).thenReturn(10995L);

        distributionOrderStatsCalculator = new DistributionOrderStatsCalculator(
                new VatService(), distributionOrderStatsTariffService, distributionOrderStatsRegionalSettings, clock
        );
    }

    @Test
    public void testRequiredFieldsIsFilled() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.build(),
                RAW_RECORD_1_2.build(),
                RAW_RECORD_2_1.build()
        ), Set.of(),true, true);

        Assertions.assertThat(result)
                .allMatch(DistributionOrderStatsCalculatorTest::onlyNullableFieldsIsNull);
    }

    @Test
    public void testCalcNewSatsCorrectly() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.build(),
                RAW_RECORD_1_2.build(),
                RAW_RECORD_2_1.build(),
                RAW_RECORD_2_2.build()
        ), Set.of(2L), true, true);

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrder(
                    STATS_RECORD_1_1.build(),
                    STATS_RECORD_1_2.build(),
                    STATS_RECORD_2_1.build(),
                    STATS_RECORD_2_2.build()
                );
    }

    @Test
    public void testSetApprovedStatus() {
        Instant now = NOW.plus(13, ChronoUnit.DAYS);
        clock.setFixed(now, ZoneOffset.UTC);
        DistributionOrderStatsRaw rawRecord = RAW_RECORD_1_1.but()
                .setOrderStatus(MbiOrderStatus.DELIVERED)
                .setItemPrice(BigDecimal.valueOf(10000_00))
                .setHasRecentStats(true)
                .setRecentPartnerPaymentNoVat(BigDecimal.valueOf(150_00.00))
                .setRecentDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                .build();

        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(rawRecord), Set.of(), true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrder(
                        STATS_RECORD_1_1.but()
                                .setItemPrice(BigDecimal.valueOf(10000_00))
                                .setItemBilledCost(BigDecimal.valueOf(10000_00))
                                .setOrderBilledCost(BigDecimal.valueOf(10000_00))
                                .setOrderPaymentNoVat(BigDecimal.valueOf(5000_00))
                                .setPartnerPaymentNoVat(BigDecimal.valueOf(5000_00))
                                .setPartnerPaymentNoVatMax(BigDecimal.valueOf(5000_00))
                                .setPartnerPaymentWithVat(BigDecimal.valueOf(6000_00))
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setDistributionStatus(AdmitadOrderStatus.APPROVED)
                                .setStatsCreationTime(DateTimes.toLocalDateTime(now))
                                .setApprovalTime(rawRecord.getOrderStatusChangeTime().plusDays(14))
                                .build()
                );
    }

    @Test
    public void testSaveOnlyUpdatedRecords() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        // Новый статус - должна попасть в результат
                        RAW_RECORD_1_1.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .build(),
                        // Была уже такая запись - не должна попасть в результат
                        RAW_RECORD_1_1.but()
                                .setHasRecentStats(true)
                                .build(),
                        // APPROVED - не должна попасть в результат
                        RAW_RECORD_1_2.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setHasRecentStats(true)
                                .setRecentDistributionStatus(AdmitadOrderStatus.APPROVED)
                                .setRecentApprovalTime(LocalDateTime.parse("2022-01-01T00:00"))
                                .setRecentPartnerPaymentNoVat(BigDecimal.valueOf(6000))
                                .build(),
                        // Не должна попасть в результат, потому что была уже approved-запись на
                        // эту позицию
                        RAW_RECORD_1_2.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERY)
                                .setHasRecentStats(true)
                                .setRecentDistributionStatus(AdmitadOrderStatus.APPROVED)
                                .setRecentApprovalTime(LocalDateTime.parse("2022-01-01T00:00"))
                                .build(),
                        // Была уже такая запись -
                        // не должна попасть в результат потому что есть более свежий статус для этой записи
                        RAW_RECORD_1_2.but()
                                .setHasRecentStats(true)
                                .setRecentDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                .setRecentApprovalTime(LocalDateTime.parse("2022-01-01T00:00"))
                                .build(),
                        // Была уже такая запись но с другими флагами - должна попасть в результат
                        RAW_RECORD_2_1.but()
                                .setHasRecentStats(true)
                                .setRecentPartnerPaymentNoVat(BigDecimal.valueOf(150))
                                .setRecentAdditionalInfo(Set.of())
                                .build(),
                        // Была уже такая запись но с другой суммой выплат - должна попасть в результат
                        RAW_RECORD_2_2.but()
                                .setHasRecentStats(true)
                                .setRecentPartnerPaymentNoVat(BigDecimal.valueOf(120))
                                .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                                .build()
                ), Set.of(), true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrder(
                                STATS_RECORD_1_1.but()
                                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                                        .setDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                        .setApprovalTime(LocalDateTime.parse("2022-01-15T00:00"))
                                        .build(),
                                STATS_RECORD_2_1.build(),
                                STATS_RECORD_2_2.build()
                );
    }

    @Test
    public void testFlappingClid() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        // Новый статус - должна попасть в результат
                        RAW_RECORD_1_1.but()
                                .setHasRecentStats(true)
                                .setClid(5L)
                                .setRecentClid(1L)
                                .build(),
                        RAW_RECORD_1_2.but()
                                .setHasRecentStats(true)
                                .setClid(5L)
                                .setRecentClid(1L)
                                .build()),
                Set.of(),true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrder(
                        STATS_RECORD_1_1.but().setClid(5L).build(),
                        STATS_RECORD_1_2.but().setClid(5L).build()
                );
    }

    @Test
    public void testNoSkipApproved() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_2.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setHasRecentStats(true)
                                .setRecentDistributionStatus(AdmitadOrderStatus.APPROVED)
                                .build()
                ), Set.of(),true, false
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrder(
                        STATS_RECORD_1_2.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                .setOrderBilledCost(BigDecimal.valueOf(30000))
                                .setOrderPaymentNoVat(BigDecimal.valueOf(15000))
                                .setApprovalTime(ORDER_CREATION_TIME.plusDays(14))
                                .build()
                );
    }

    @Test
    public void testNoSkipIdentical() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_2.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setHasRecentStats(true)
                                .setRecentDistributionStatus(AdmitadOrderStatus.PROCESSING)
                                .build()
                ), Set.of(),false, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrder(
                        STATS_RECORD_1_2
                                .setOrderBilledCost(BigDecimal.valueOf(30000))
                                .setOrderPaymentNoVat(BigDecimal.valueOf(15000))
                                .build()
                );
    }

    @Test
    public void testUseFraud() {
        testAdditionalInfo(b ->
                        b.setOrderCreationTime(ORDER_CREATION_TIME).setIsFraud(true),
                DistributionShareAdditionalInfo.ABUSE
        );
    }

    @Test
    public void testUseCoupon() {
        testAdditionalInfo(b ->
                        b.setOrderCreationTime(ORDER_CREATION_TIME).setIsCoupon(true),
                DistributionShareAdditionalInfo.FULL_CART_COUPON
        );
    }

    @Test
    public void testRefCoupon() {
        testAdditionalInfo(b ->
                        b.setOrderCreationTime(LocalDate.parse("2022-05-02").atStartOfDay())
                        .setIsRefCoupon(true),
                DistributionShareAdditionalInfo.REFERRAL_PROMO_CODE
        );
    }

    @Test
    public void testBannedSource() {
        LocalDateTime orderCreationTime = DistributionOrderStatsCalculator.BANNED_SOURCE_START_DATE.plusDays(1);
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.but().setOrderCreationTime(orderCreationTime).build(),
                RAW_RECORD_1_2.but().setOrderCreationTime(orderCreationTime).build(),
                RAW_RECORD_2_1.but()
                        .setOrderCreationTime(orderCreationTime)
                        .setIsBannedSource(true)
                        .setFraudUpdateTime(orderCreationTime)
                        .build(),
                RAW_RECORD_2_2.but()
                        .setOrderCreationTime(orderCreationTime)
                        .setIsBannedSource(true)
                        .setFraudUpdateTime(orderCreationTime)
                        .build()
        ), Set.of(), true, true);

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder -> builder.but()
                                .setOrderCreationTime(orderCreationTime)
                                .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                .setOrderPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                .setAdditionalInfos(Set.of(
                                        DistributionShareAdditionalInfo.GENERAL_TARIFF,
                                        DistributionShareAdditionalInfo.BANNED_SOURCE
                                ))
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    void testAdditionalInfo(Function<DistributionOrderStatsRaw.Builder, DistributionOrderStatsRaw.Builder> rawRecordFunc,
                            DistributionShareAdditionalInfo expectedAdditionalInfo) {
        var record21 = rawRecordFunc.apply(RAW_RECORD_2_1.but()).build();
        var record22 = rawRecordFunc.apply(RAW_RECORD_2_2.but()).build();

        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.but().setOrderCreationTime(record21.getOrderCreationTime()).build(),
                RAW_RECORD_1_2.but().setOrderCreationTime(record22.getOrderCreationTime()).build(),
                record21,
                record22
        ), Set.of(),true, true);

        Assertions.assertThat(result)
                .usingElementComparatorIgnoringFields("orderCreationTime")
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder ->
                                setCancelledBy(builder, expectedAdditionalInfo, 10995L)
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testNoAllItemsReturnedIfOnlySomeItemsReturnedFromMultiorder() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.but()
                        .setItemsReturnCount(1)
                        .setItemsReturnTime(ORDER_CREATION_TIME_PREV),
                RAW_RECORD_1_2.but()
                        .setItemsReturnCount(1)
                        .setItemsReturnTime(ORDER_CREATION_TIME_PREV),
                RAW_RECORD_2_1,
                RAW_RECORD_2_2
        ).stream().map(builder -> builder.but()
                .setOrderCreationTime(ORDER_CREATION_TIME_PREV)
                .setOrderStatusChangeTime(ORDER_CREATION_TIME_PREV)
                .setItemDataUpdateTime(ORDER_CREATION_TIME_PREV)
                .build()).collect(Collectors.toList()), Set.of(), true, true);

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1.but()
                                        .setItemBilledCost(BigDecimal.ZERO)
                                        .setOrderBilledCost(BigDecimal.ZERO)
                                        .setItemsReturnCount(1)
                                        .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                        .setOrderPaymentNoVat(BigDecimal.ZERO)
                                        .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                        .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                        .setAdditionalInfos(Set.of(
                                                DistributionShareAdditionalInfo.GENERAL_TARIFF,
                                                DistributionShareAdditionalInfo.ALL_ITEMS_RETURNED
                                        )),
                                STATS_RECORD_1_2.but()
                                        .setItemBilledCost(BigDecimal.ZERO)
                                        .setOrderBilledCost(BigDecimal.ZERO)
                                        .setItemsReturnCount(1)
                                        .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                        .setOrderPaymentNoVat(BigDecimal.ZERO)
                                        .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                        .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                        .setAdditionalInfos(Set.of(
                                                DistributionShareAdditionalInfo.GENERAL_TARIFF,
                                                DistributionShareAdditionalInfo.ALL_ITEMS_RETURNED
                                        )),
                                STATS_RECORD_2_1.but()
                                        .setAdditionalInfos(Set.of(
                                                DistributionShareAdditionalInfo.GENERAL_TARIFF
                                        )),
                                STATS_RECORD_2_2.but()
                                        .setAdditionalInfos(Set.of(
                                                DistributionShareAdditionalInfo.GENERAL_TARIFF
                                        ))
                        ).stream().map(builder -> builder.but()
                                .setOrderCreationTime(ORDER_CREATION_TIME_PREV)
                                .setOrderStatusChangeTime(ORDER_CREATION_TIME_PREV)
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testNoAllItemsReturnedIfAllOrdersIsCanceled() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1,
                RAW_RECORD_1_2,
                RAW_RECORD_2_1,
                RAW_RECORD_2_2
        ).stream().map(builder -> builder.but()
                .setOrderStatus(MbiOrderStatus.CANCELLED_BEFORE_PROCESSING)
                .setOrderCreationTime(ORDER_CREATION_TIME_PREV)
                .setOrderStatusChangeTime(ORDER_CREATION_TIME_PREV)
                .setItemDataUpdateTime(ORDER_CREATION_TIME_PREV)
                .build()).collect(Collectors.toList()), Set.of(), true, true);


        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder -> builder.but()
                                .setOrderCreationTime(ORDER_CREATION_TIME_PREV)
                                .setOrderStatusChangeTime(ORDER_CREATION_TIME_PREV)
                                .setItemBilledCost(BigDecimal.valueOf(300_00))
                                .setOrderBilledCost(BigDecimal.valueOf(600_00))
                                .setOrderPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                .setOrderStatus(MbiOrderStatus.CANCELLED_BEFORE_PROCESSING)
                                .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                .setAdditionalInfos(Set.of(
                                        DistributionShareAdditionalInfo.GENERAL_TARIFF
                                ))
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testAllItemsReturned() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                    RAW_RECORD_1_1,
                    RAW_RECORD_1_2,
                    RAW_RECORD_2_1,
                    RAW_RECORD_2_2
            ).stream().map(builder -> builder.but()
                    .setItemsReturnCount(1)
                    .setItemsReturnTime(ORDER_CREATION_TIME)
                    .build()
            ).collect(Collectors.toList()), Set.of(), true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder -> builder.but()
                                .setItemsReturnCount(1)
                                .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                .setItemBilledCost(BigDecimal.ZERO)
                                .setOrderBilledCost(BigDecimal.ZERO)
                                .setOrderPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                .setAdditionalInfos(Set.of(
                                        DistributionShareAdditionalInfo.ALL_ITEMS_RETURNED,
                                        DistributionShareAdditionalInfo.GENERAL_TARIFF
                                ))
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testPromocode() {
        String promocodeData = "2222:best_deal_AF,3333:best_deal_3_AF,3333:best_deal_33_AF";
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.build(),
                RAW_RECORD_1_2.build(),
                RAW_RECORD_2_1.but().setPromocodeData(promocodeData).build(),
                RAW_RECORD_2_2.but().setPromocodeData(promocodeData).build()
        ), Set.of(), true, true);

        List<DistributionOrderStats> expected = List.of(
                STATS_RECORD_1_1,
                STATS_RECORD_1_2,
                STATS_RECORD_2_1,
                STATS_RECORD_2_2
        ).stream().map(builder -> builder.but()
                .setClid(2222L)
                .setVid(null)
                .setDistributionStatus(AdmitadOrderStatus.PROCESSING)
                .setAdditionalInfos(Set.of(
                        DistributionShareAdditionalInfo.GENERAL_TARIFF
                ))
                .setPromocodeData(promocodeData)
                .build()
        ).collect(Collectors.toList());

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        expected
                );
    }

    @Test
    public void testPromocodeNullClid() {
        String promocodeData = "2222:best_deal_AF,3333:best_deal_3_AF,3333:best_deal_33_AF";
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.but().setPromocodeData(promocodeData).setClid(null).setVid("A").build(),
                RAW_RECORD_1_2.but().setPromocodeData(promocodeData).setClid(null).setVid("A").build(),
                RAW_RECORD_2_1.but().setPromocodeData(promocodeData).setClid(null).setVid("A").build(),
                RAW_RECORD_2_2.but().setPromocodeData(promocodeData).setClid(null).setVid("A").build()
        ), Set.of(), true, true);

        List<DistributionOrderStats> expected = List.of(
                STATS_RECORD_1_1,
                STATS_RECORD_1_2,
                STATS_RECORD_2_1,
                STATS_RECORD_2_2
        ).stream().map(builder -> builder.but()
                .setClid(2222L)
                .setVid(null)
                .setDistributionStatus(AdmitadOrderStatus.PROCESSING)
                .setAdditionalInfos(Set.of(
                        DistributionShareAdditionalInfo.GENERAL_TARIFF
                ))
                .setPromocodeData(promocodeData)
                .build()
        ).collect(Collectors.toList());

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        expected
                );
    }

    @Test
    public void testPromocodesUnpaidClid() {
        String promocodeData = DistributionOrderStatsUtils.UNPAID_PROMOCODE_CLID + ":";
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.but().setPromocodeData(promocodeData).build(),
                RAW_RECORD_1_2.but().setPromocodeData(promocodeData).build(),
                RAW_RECORD_2_1.but().setPromocodeData(promocodeData).build(),
                RAW_RECORD_2_2.but().setPromocodeData(promocodeData).build()
        ), Set.of(), true, true);

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder -> builder.but()
                                .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                .setAdditionalInfos(Set.of(
                                        DistributionShareAdditionalInfo.PARTNER_PROMO_CODE,
                                        DistributionShareAdditionalInfo.GENERAL_TARIFF
                                ))
                                .setOrderPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testPromocodesPaidAndUnpaidClid() {
        String promocodeData = DistributionOrderStatsUtils.UNPAID_PROMOCODE_CLID + ":adcd,2222:best_deal_AF";
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                RAW_RECORD_1_1.but().setPromocodeData(promocodeData).build(),
                RAW_RECORD_1_2.but().setPromocodeData(promocodeData).build(),
                RAW_RECORD_2_1.but().setPromocodeData(promocodeData).build(),
                RAW_RECORD_2_2.but().setPromocodeData(promocodeData).build()
        ), Set.of(), true, true);

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder -> builder.but()
                                .setClid(2222L)
                                .setVid(null)
                                .setDistributionStatus(AdmitadOrderStatus.PROCESSING)
                                .setAdditionalInfos(Set.of(
                                        DistributionShareAdditionalInfo.GENERAL_TARIFF
                                ))
                                .setPromocodeData(promocodeData)
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testOnHold() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_1,
                        RAW_RECORD_1_2,
                        RAW_RECORD_2_1,
                        RAW_RECORD_2_2
                ).stream().map(builder -> builder.but()
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build()
                ).collect(Collectors.toList()), Set.of(), true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder -> builder.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                .setApprovalTime(ORDER_CREATION_TIME.plusDays(14))
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testApproved() {
        LocalDateTime orderCreationTime = ORDER_CREATION_TIME.minusDays(16);
        LocalDateTime statusChangeTime = ORDER_CREATION_TIME.minusDays(15);
        Instant now = NOW.minus(Duration.ofDays(2));
        clock.setFixed(now, ZoneOffset.UTC);

        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_1,
                        RAW_RECORD_1_2,
                        RAW_RECORD_2_1,
                        RAW_RECORD_2_2
                ).stream().map(builder -> builder.but()
                        .setOrderCreationTime(orderCreationTime)
                        .setOrderStatusChangeTime(statusChangeTime)
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build()
                ).collect(Collectors.toList()), Set.of(), true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1,
                                STATS_RECORD_1_2,
                                STATS_RECORD_2_1,
                                STATS_RECORD_2_2
                        ).stream().map(builder -> builder.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setDistributionStatus(AdmitadOrderStatus.APPROVED)
                                .setOrderCreationTime(orderCreationTime)
                                .setOrderStatusChangeTime(statusChangeTime)
                                .setApprovalTime(statusChangeTime.plusDays(14))
                                .setStatsCreationTime(LocalDateTime.ofInstant(
                                        now, ZoneId.of(DateTimes.MOSCOW_TIME_ZONE_STR)))
                                .build()
                        ).collect(Collectors.toList())
                );
    }


    @Test
    public void testCancelSubstatus() {
        var result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_1.but()
                                .setMultiOrderId(null)
                                .setSubstatus(OrderSubstatus.BROKEN_ITEM)
                                .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                                .build()
                ), Set.of(), true, true
        );
        Assertions.assertThat(result)
                .usingElementComparatorOnFields("additionalInfos", "distributionOrderStatus")
                .containsExactlyInAnyOrder(STATS_RECORD_1_1.but()
                        .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                        .setAdditionalInfos(Set.of(
                                DistributionShareAdditionalInfo.GENERAL_TARIFF,
                                DistributionShareAdditionalInfo.SELLER_CANCEL))
                        .build()
                );
    }

    @Test
    public void testCancelSubstatusNull() {
        var result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_1.but()
                                .setMultiOrderId(null)
                                .setSubstatus(null)
                                .setOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                                .build()
                ), Set.of(), true, true
        );
        Assertions.assertThat(result)
                .usingElementComparatorOnFields("additionalInfos", "distributionOrderStatus")
                .containsExactlyInAnyOrder(STATS_RECORD_1_1.but()
                        .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                        .setAdditionalInfos(Set.of(
                                DistributionShareAdditionalInfo.GENERAL_TARIFF))
                        .build()
                );
    }

    @Test
    public void testCancelledByItemCount() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_1.but()
                                .setMultiOrderId(null)
                                .setItemsCount(0)
                ).stream().map(builder -> builder.but()
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build()
                ).collect(Collectors.toList()), Set.of(), true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1
                        ).stream().map(builder -> builder.but()
                                .setMultiOrderId(null)
                                .setItemsCount(0)
                                .setItemBilledCost(BigDecimal.ZERO)
                                .setOrderBilledCost(BigDecimal.ZERO)
                                .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setOrderPaymentNoVat(BigDecimal.ZERO)
                                .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                .setPartnerPaymentNoVatMax(BigDecimal.ZERO)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    @Test
    public void testPartiallyCancelledByItemCount() {
        List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(
                List.of(
                        RAW_RECORD_1_1.but()
                                .setMultiOrderId(null)
                                .setItemsCount(0),
                        RAW_RECORD_1_2.but()
                                .setMultiOrderId(null)
                ).stream().map(builder -> builder.but()
                        .setOrderStatus(MbiOrderStatus.DELIVERED)
                        .build()
                ).collect(Collectors.toList()), Set.of(), true, true
        );

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                STATS_RECORD_1_1.but()
                                        .setItemsCount(0)
                                        .setPartnerPaymentNoVat(BigDecimal.ZERO)
                                        .setItemBilledCost(BigDecimal.ZERO)
                                        .setPartnerPaymentWithVat(BigDecimal.ZERO)
                                        .setPartnerPaymentNoVatMax(BigDecimal.ZERO),
                                STATS_RECORD_1_2
                        ).stream().map(builder -> builder.but()
                                .setMultiOrderId(null)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                .setOrderBilledCost(BigDecimal.valueOf(30000))
                                .setOrderPaymentNoVat(BigDecimal.valueOf(15000))
                                .setApprovalTime(LocalDateTime.parse("2022-01-15T00:00"))
                                .build()
                        ).collect(Collectors.toList())
                );
    }

    private static boolean onlyNullableFieldsIsNull(DistributionOrderStats record) {
        try {
            for (Field declaredField : DistributionOrderStats.class.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Nullable.class)) {
                    continue;
                }
                boolean access = declaredField.canAccess(record);
                try {
                    declaredField.setAccessible(true);
                    if (declaredField.get(record) == null) {
                        throw new NullPointerException(
                                "Field: " + declaredField.getName() + " is null but not marked as @Nullable");
                    }
                } finally {
                    declaredField.setAccessible(access);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Test
    public void testCancelledByRegion() {
        try {
            when(distributionOrderStatsRegionalSettings
                    .isCancelledByRegion(55L, ORDER_CREATION_TIME, false))
                    .thenReturn(true);
            when(distributionOrderStatsRegionalSettings
                    .isCancelledByRegion(66L, ORDER_CREATION_TIME, false))
                    .thenReturn(false);
            when(distributionOrderStatsRegionalSettings.resolveRegionRepublicLevel(3L))
                    .thenReturn(55L);
            when(distributionOrderStatsRegionalSettings.resolveRegionRepublicLevel(4L))
                    .thenReturn(66L);
            List<DistributionOrderStats> result = distributionOrderStatsCalculator.getStatsToSave(List.of(
                    RAW_RECORD_1_1.but().setDeliveryRegionId(3L).build(),
                    RAW_RECORD_1_2.but().setDeliveryRegionId(3L).build(),
                    RAW_RECORD_2_1.but().setDeliveryRegionId(4L).build(),
                    RAW_RECORD_2_2.but().setDeliveryRegionId(4L).build()
            ), Set.of(), true, true);

            Assertions.assertThat(result)
                    .usingRecursiveFieldByFieldElementComparator()
                    .usingComparatorForElementFieldsWithType(BIG_DECIMAL_AS_DOUBLE_COMPARATOR, BigDecimal.class)
                    .containsExactlyInAnyOrderElementsOf(
                            List.of(
                                    setCancelledBy(STATS_RECORD_1_1, DistributionShareAdditionalInfo.BANNED_REGIONS, 55L),
                                    setCancelledBy(STATS_RECORD_1_2, DistributionShareAdditionalInfo.BANNED_REGIONS, 55L),
                                    STATS_RECORD_2_1.but().setDeliveryRegionId(66L).build(),
                                    STATS_RECORD_2_2.but().setDeliveryRegionId(66L).build()
                            )
                    );
        } finally {
            Mockito.reset(distributionOrderStatsRegionalSettings);
        }
    }

    @Test
    public void testOrderToBilledCost() {
        var input = List.of(
                statusHistory(1L, 11L, BigDecimal.valueOf(1000), 2),
                statusHistory(2L, 21L, BigDecimal.valueOf(1500), 1),
                statusHistory(1L, 12L, BigDecimal.valueOf(2000), 3)
        );
        var result = DistributionOrderStatsCalculator.getOrderToBilledCost(input);
        Assertions.assertThat(result).containsAllEntriesOf(
                Map.of(1L, BigDecimal.valueOf(8000), 2L, BigDecimal.valueOf(1500)));
    }

    @Test
    public void testItemToPaymentNoVatUnlessCancelled() {
        var tariffs =
                Map.of(11L,
                        new DistributionTariffRateAndName(
                            BigDecimal.valueOf(0.05), DistributionTariffName.ALL, null),
                       12L,
                        new DistributionTariffRateAndName(
                                BigDecimal.valueOf(0.03), DistributionTariffName.ALL, null),
                       21L,
                        new DistributionTariffRateAndName(
                                BigDecimal.valueOf(0.1), DistributionTariffName.ALL, BigDecimal.valueOf(1000)),
                       31L,
                        new DistributionTariffRateAndName(
                                BigDecimal.valueOf(0.08), DistributionTariffName.ALL, BigDecimal.valueOf(3)),
                       32L,
                        new DistributionTariffRateAndName(
                                BigDecimal.valueOf(0.08), DistributionTariffName.ALL, BigDecimal.valueOf(3)));
        var data =  List.of(
                statusHistory(1L, 11L, BigDecimal.valueOf(1000), 2),
                statusHistory(1L, 12L, BigDecimal.valueOf(2000), 3),
                statusHistory(2L, 21L, BigDecimal.valueOf(1500), 1),
                statusHistory(3L, 31L, BigDecimal.valueOf(4500), 1),
                statusHistory(3L, 32L, BigDecimal.valueOf(2000), 2)
        );
        var result =
                DistributionOrderStatsCalculator.getItemToPaymentNoVatUnlessCancelled(data, tariffs);

        Assertions.assertThat(result).containsOnlyKeys(11L, 12L, 21L, 31L, 32L);
        assertTrue(areEqualBigDecimals(result.get(11L).getValue(), BigDecimal.valueOf(1000 * 2 * 0.05)));
        assertFalse(result.get(11L).isLimit());
        assertTrue(areEqualBigDecimals(result.get(12L).getValue(), BigDecimal.valueOf(2000 * 3 * 0.03)));
        assertFalse(result.get(12L).isLimit());
        assertTrue(areEqualBigDecimals(result.get(21L).getValue(), BigDecimal.valueOf(1500 * 1 * 0.1)));
        assertFalse(result.get(21L).isLimit());
        assertThat(areEqualBigDecimals(result.get(31L).getValue(), BigDecimal.valueOf(300 * 4500.0 / 8500)));
        assertTrue(result.get(31L).isLimit());
        assertThat(areEqualBigDecimals(result.get(32L).getValue(), BigDecimal.valueOf(300 * 4000.0 / 8500)));
        assertTrue(result.get(32L).isLimit());
    }

    private static boolean areEqualBigDecimals(BigDecimal d1, BigDecimal d2) {
        var eps = BigDecimal.valueOf(0.0001);
        return d1.subtract(d2).abs().compareTo(eps) < 0;
    }

    private static DistributionOrderItemStatusHistory statusHistory(
            long orderId, long itemId, BigDecimal itemPrice, int itemsCount) {
        var result = new DistributionOrderItemStatusHistory();
        result.getItems().add(DistributionOrderStatsRaw
                .builder()
                .setOrderId(orderId)
                .setItemId(itemId)
                .setItemPrice(itemPrice)
                .setItemsCount(itemsCount)
                .build()
        );
        return result;
    }

    private static DistributionOrderStats setCancelledBy(
            DistributionOrderStats.Builder source, DistributionShareAdditionalInfo additionalInfo, Long regionId) {
        Set<DistributionShareAdditionalInfo> additionalInfos = new LinkedHashSet<>();
        additionalInfos.add(DistributionShareAdditionalInfo.GENERAL_TARIFF);
        additionalInfos.add(additionalInfo);
        return source.but()
                .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                .setOrderPaymentNoVat(BigDecimal.ZERO)
                .setPartnerPaymentNoVat(BigDecimal.ZERO)
                .setPartnerPaymentWithVat(BigDecimal.ZERO)
                .setAdditionalInfos(additionalInfos)
                .setDeliveryRegionId(regionId)
                .build();
    }
}
