package ru.yandex.market.core.billing.distribution.share.stats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.distribution.share.DistributionShareAdditionalInfo;
import ru.yandex.market.core.billing.distribution.share.DistributionTariffName;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStats;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsAggregate;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsDimensionAggregate;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsClosedEntry;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsRaw;
import ru.yandex.market.core.billing.tasks.reports.distribution.cpa.AdmitadOrderStatus;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.report.model.Color;


/**
 * @author moskovkin
 */
@DbUnitDataSet(before = {
        "db/DistributionOrderStatsDaoTest.before.csv",
        "db/DistributionOrderStatsDaoTest.old-order.before.csv"
})
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class DistributionOrderStatsDaoTest extends FunctionalTest {
    private static final DistributionOrderStatsRaw.Builder ORDER_1_BASE_ITEM = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(1)
            .setOrderStatus(MbiOrderStatus.PROCESSING)
            .setColor(Color.WHITE)
            .setItemId(11)
            .setOfferId("offer_999_1")
            .setShopId(111)
            .setFeedId(999)
            .setCategoryId(9999)
            .setItemsCount(2)
            .setItemsReturnCount(0)
            .setItemPrice(BigDecimal.valueOf(300))
            .setClid(1L)
            .setVid("vid_1_1")
            .setDistrType(1)
            .setMsku(99999L)
            .setIsFirstOrder(true)
            .setIsFraud(false)
            .setIsOverLimit(false)
            .setOrderCreationTime(LocalDateTime.parse("2020-01-01T00:00:00"))
            .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
            .setFraudUpdateTime(null)
            .setItemsReturnTime(null)
            .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
            .setHasRecentStats(false)
            .setRecentAdditionalInfo(Set.of())
            .setPromocodeData("1:promo_2_AF,1:promo_1_AF")
            .setDeliveryRegionId(2L)
            ;

    private static final DistributionOrderStatsRaw.Builder ORDER_2_BASE_ITEM = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(2)
            .setColor(Color.WHITE)
            .setOrderStatus(MbiOrderStatus.PROCESSING)
            .setItemId(22)
            .setOfferId("offer_888_1")
            .setShopId(111)
            .setFeedId(888)
            .setCategoryId(8888)
            .setItemsCount(2)
            .setItemsReturnCount(0)
            .setItemPrice(BigDecimal.valueOf(400))
            .setClid(1L)
            .setVid("vid_1_1")
            .setDistrType(1)
            .setMsku(88888L)
            .setIsFirstOrder(true)
            .setIsFraud(false)
            .setIsOverLimit(false)
            .setOrderCreationTime(LocalDateTime.parse("2019-01-01T00:00:00"))
            .setOrderStatusChangeTime(LocalDateTime.parse("2019-01-01T00:00:01"))
            .setFraudUpdateTime(null)
            .setItemsReturnTime(null)
            .setItemDataUpdateTime(LocalDateTime.parse("2019-01-01T00:00:01"))
            .setHasRecentStats(false)
            .setRecentAdditionalInfo(Set.of());

    private static final DistributionOrderStatsRaw.Builder ORDER_3_BASE_ITEM = ORDER_1_BASE_ITEM.but()
            .setMultiOrderId(null)
            .setOrderId(3)
            .setClid(null)
            .setVid(null)
            .setItemId(33)
            .setPromocodeData("3:promo_3_AF")
            .setDeliveryRegionId(null);

    private static final DistributionOrderStatsRaw.Builder ORDER_4_BASE_ITEM = DistributionOrderStatsRaw.builder()
            .setMultiOrderId(null)
            .setOrderId(4)
            .setColor(Color.WHITE)
            .setOrderStatus(MbiOrderStatus.PROCESSING)
            .setItemId(44)
            .setOfferId("offer_777_1")
            .setShopId(111)
            .setFeedId(777)
            .setCategoryId(7777)
            .setItemsCount(2)
            .setItemsReturnCount(0)
            .setItemPrice(BigDecimal.valueOf(500))
            .setClid(1L)
            .setVid("vid_1_1")
            .setDistrType(1)
            .setMsku(77777L)
            .setIsFirstOrder(true)
            .setIsFraud(false)
            .setIsOverLimit(false)
            .setOrderCreationTime(LocalDateTime.parse("2020-01-01T00:00:00"))
            .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
            .setFraudUpdateTime(null)
            .setItemsReturnTime(null)
            .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
            .setHasRecentStats(false)
            .setRecentAdditionalInfo(Set.of());

    private static final DistributionOrderStatsRaw.Builder ORDER_5_BASE_ITEM = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_5")
            .setOrderId(5)
            .setColor(Color.WHITE)
            .setOrderStatus(MbiOrderStatus.PROCESSING)
            .setItemId(55)
            .setOfferId("offer_777_1")
            .setShopId(111)
            .setFeedId(777)
            .setCategoryId(7777)
            .setItemsCount(2)
            .setItemsReturnCount(0)
            .setItemPrice(BigDecimal.valueOf(500))
            .setClid(1L)
            .setVid("vid_1_1")
            .setDistrType(1)
            .setMsku(77777L)
            .setIsFirstOrder(true)
            .setIsFraud(false)
            .setIsOverLimit(false)
            .setOrderCreationTime(LocalDateTime.parse("2020-01-01T00:00:00"))
            .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
            .setFraudUpdateTime(null)
            .setItemsReturnTime(null)
            .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
            .setHasRecentStats(false)
            .setRecentAdditionalInfo(Set.of());

    private static final DistributionOrderStatsRaw.Builder ORDER_5_BASE_ITEM_2 = ORDER_5_BASE_ITEM.but()
            .setItemId(56);

    private static final DistributionOrderStats.Builder DISTR_ORDER_STATS_1 = DistributionOrderStats.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(1)
            .setItemId(11)
            .setOrderStatus(MbiOrderStatus.PROCESSING)
            .setDistributionStatus(AdmitadOrderStatus.ON_HOLD)
            .setOrderStatusEventId(1)
            .setCategoryId(9999)
            .setFeedId(111)
            .setOfferId("offer_999_1")
            .setMsku(99999L)
            .setItemsCount(2)
            .setItemsReturnCount(0)
            .setItemPrice(BigDecimal.TEN)
            .setItemBilledCost(BigDecimal.TEN)
            .setOrderBilledCost(BigDecimal.TEN)
            .setOrderPaymentNoVat(BigDecimal.valueOf(2.54))
            .setPartnerPaymentNoVat(BigDecimal.valueOf(2.54))
            .setPartnerPaymentNoVatMax(BigDecimal.valueOf(2.54))
            .setPartnerPaymentWithVat(BigDecimal.valueOf(4.50))
            .setClid(123)
            .setVid("vid_1_1")
            .setPromocodeData("123:PROMOCODE_123_AF,1234:PROMOCODE_1234_AF")
            .setDistrType(1)
            .setTariffRate(BigDecimal.valueOf(0.05))
            .setTariffName(DistributionTariffName.CEHAC)
            .setTariffGrid("general")
            .setAdditionalInfos(Set.of(
                    DistributionShareAdditionalInfo.GENERAL_TARIFF
            ))
            .setIsFirstOrder(true)
            .setOrderCreationTime(LocalDateTime.parse("2020-01-01T02:00:01"))
            .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
            .setApprovalTime(LocalDateTime.parse("2020-01-01T00:00:00"))
            .setStatsCreationTime(LocalDateTime.parse("2020-01-02T02:00:01"))
            .setDeliveryRegionId(10995L);

    private static final DistributionOrderStats.Builder DISTR_ORDER_STATS_2 = DISTR_ORDER_STATS_1.but()
            .setOrderId(3)
            .setItemId(33);

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final TestableClock clock = new TestableClock();

    private DistributionOrderStatsDao distributionOrderStatsDao;

    @BeforeEach
    public void setup() {
        distributionOrderStatsDao = new DistributionOrderStatsDao(namedParameterJdbcTemplate, clock);
    }

    @Test
    public void testReturnRecordsChangedInInterval() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T00:00:01"),
                LocalDateTime.parse("2020-01-01T02:00:02")
        );

        Assertions.assertThat(allRecords)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(4)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(3)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.no-multiorder.before.csv")
    public void testSortMultiorders() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T00:00:01"),
                LocalDateTime.parse("2020-01-01T02:00:02")
        );

        Assertions.assertThat(allRecords)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build(),
                        ORDER_5_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(11)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_5_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(9)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build(),
                        ORDER_5_BASE_ITEM_2.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(11)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_5_BASE_ITEM_2.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(9)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(4)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(3)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build(),
                        ORDER_4_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(10)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_4_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(8)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    public void testFreshChangesTouchAllHistory() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T02:00:01"),
                LocalDateTime.parse("2020-01-01T02:00:02")
        );

        Assertions.assertThat(allRecords)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(4)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(3)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    public void testFromIsToBig() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T02:00:02"),
                LocalDateTime.parse("2020-01-01T02:00:03")
        );

        Assertions.assertThat(allRecords).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.very.old.tariff.csv")
    public void testDoNotFilterOldOrderByTariff() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2017-01-01T00:00:00"),
                LocalDateTime.parse("2020-01-01T00:00:00")
        );

        Assertions.assertThat(allRecords)
                .usingElementComparatorOnFields("orderId", "itemId", "orderStatus")
                .containsExactly(
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(9)
                                .setItemId(99)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .build(),
                        DistributionOrderStatsRaw.builder()
                                .setOrderId(9)
                                .setItemId(99)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .build()
                );
    }

    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.getChangesWithUpdateItemCount.before.csv")
    @Test
    void testGetChangesWithUpdateItemCount() {
        List<DistributionOrderStatsRaw> actual = getChanges(
                LocalDateTime.parse("2021-06-16T00:00:00.000000"),
                LocalDateTime.parse("2021-06-17T14:11:56.610000")
        );

        Assertions.assertThat(actual)
                .usingElementComparatorOnFields("itemsCount", "orderId", "itemId", "multiOrderId", "orderStatus")
                .containsExactlyInAnyOrder(
                        DistributionOrderStatsRaw.builder()
                                .setMultiOrderId("d72810ea-0207-4317-806d-7a4ef33d5b19")
                                .setOrderId(48923168)
                                .setItemId(91852557)
                                .setItemsCount(0)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .build()
                );
    }

    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.deleteOrderStats.before.csv",
            after = "db/DistributionOrderStatsDaoTest.deleteOrderStats.after.csv")
    @Test
    void testDeleteOrders() {
        distributionOrderStatsDao.deleteOrders(List.of(48769730L));
    }

    @Test
    public void testToIsToSmall() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T00:00:00"),
                LocalDateTime.parse("2020-01-01T00:00:01")
        );

        Assertions.assertThat(allRecords).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.return.before.csv")
    public void testReturnTimeMarkRecordsAsChanged() {
        List<DistributionOrderStatsRaw> records = getChanges(
                LocalDateTime.parse("2020-01-01T03:00:01"),
                LocalDateTime.parse("2020-01-01T04:00:01")
        );

        DistributionOrderStatsRaw.Builder recordWithReturn = ORDER_1_BASE_ITEM.but()
                .setItemsReturnTime(LocalDateTime.parse("2020-01-01T03:00:01"))
                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T03:00:01"))
                .setItemsReturnCount(1);

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        recordWithReturn.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        recordWithReturn.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.update.approval.before.csv",
            after = "db/DistributionOrderStatsDaoTest.update.approval.after.csv")
    public void testUpdateApprovalTime() {
        LocalDate dateFrom = LocalDate.of(2020, 1, 1);
        LocalDate dateTo = LocalDate.of(2020, 2, 1);
        distributionOrderStatsDao.updateApprovalTime(dateFrom, dateTo);
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.fraud.before.csv")
    public void testFraudUpdateTimeMarkRecordsAsChanged() {
        List<DistributionOrderStatsRaw> records = getChanges(
                LocalDateTime.parse("2020-01-02T00:00:00"),
                LocalDateTime.parse("2020-01-02T00:00:01")
        );

        DistributionOrderStatsRaw.Builder recordWithFraud = ORDER_1_BASE_ITEM.but()
                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setFraudUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setIsFraud(true);

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        recordWithFraud.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        recordWithFraud.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.coupon.before.csv")
    public void testCouponUpdateTimeMarkRecordsAsChanged() {
        List<DistributionOrderStatsRaw> records = getChanges(
                LocalDateTime.parse("2020-01-02T00:00:00"),
                LocalDateTime.parse("2020-01-02T00:00:01")
        );

        DistributionOrderStatsRaw.Builder recordWithFraud = ORDER_1_BASE_ITEM.but()
                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setFraudUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setIsCoupon(true);

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        recordWithFraud.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        recordWithFraud.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.bannedSource.before.csv")
    public void testBannedSource() {
        List<DistributionOrderStatsRaw> records = getChanges(
                LocalDateTime.parse("2020-01-02T00:00:00"),
                LocalDateTime.parse("2020-01-02T00:00:01")
        );

        DistributionOrderStatsRaw.Builder recordWithFraud = ORDER_1_BASE_ITEM.but()
                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setFraudUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setIsBannedSource(true);

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        recordWithFraud.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        recordWithFraud.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.fraud.before.csv",
            "db/DistributionOrderStatsDaoTest.return.before.csv",
    })
    public void testFraudAndReturnUpdateItemDataAndUpdateTime() {
        List<DistributionOrderStatsRaw> records = getChanges(
                LocalDateTime.parse("2020-01-02T00:00:00"),
                LocalDateTime.parse("2020-01-02T00:00:01")
        );

        DistributionOrderStatsRaw.Builder recordWithFraudAndReturn = ORDER_1_BASE_ITEM.but()
                .setItemsReturnTime(LocalDateTime.parse("2020-01-01T03:00:01"))
                .setFraudUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-02T00:00:00"))
                .setItemsReturnCount(1)
                .setIsFraud(true);

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        recordWithFraudAndReturn.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        recordWithFraudAndReturn.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.multiorder.before.csv"
    })
    public void testAllItemsFromMultiorderReturned() {
        List<DistributionOrderStatsRaw> records = getChanges(
                LocalDateTime.parse("2020-01-01T02:00:01"),
                LocalDateTime.parse("2020-01-01T02:00:02")
        );

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build(),
                        ORDER_2_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(10)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(4)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(3)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    public void testDoNotReturnChangesFromEmptyInterval() {
        List<DistributionOrderStatsRaw> records = getChanges(
                LocalDateTime.parse("2019-01-01T00:00:00"),
                LocalDateTime.parse("2019-12-31T23:59:59")
        );

        Assertions.assertThat(records).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.to.approve.stats.csv"
    })
    public void testUseApprovalTimeActAsUpdateTime() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T02:00:02"),
                LocalDateTime.parse("2020-01-01T02:00:03")
        );
        Assertions.assertThat(allRecords)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:02"))
                                .setRecentApprovalTime(LocalDateTime.parse("2020-01-01T02:00:02"))
                                .setHasRecentStats(true)
                                .setRecentPartnerPaymentNoVat(BigDecimal.valueOf(2.54))
                                .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                                .setRecentDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                .setRecentClid(1L)
                                .build()
                );
    }



    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.previous.stats.csv"
    })
    public void testUsePreviousStats() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T00:00:01"),
                LocalDateTime.parse("2020-01-01T02:00:02")
        );

        Assertions.assertThat(allRecords)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setHasRecentStats(true)
                                .setRecentPartnerPaymentNoVat(BigDecimal.valueOf(3.62))
                                .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                                .setRecentDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                .setRecentClid(1L)
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(4)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .build(),
                        ORDER_3_BASE_ITEM.but()
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(3)
                                .setOrderStatusChangeTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2020-01-01T00:00:01"))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.filterByOrderIds.before.csv"
    })
    public void testOrderIdsFilter() {
        List<DistributionOrderStatsRaw> result = new ArrayList<>();
        distributionOrderStatsDao.getStatsByOrderIds(Arrays.asList(10L, 12L), result::add);
        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderId(10)
                                .setMultiOrderId("multi_2")
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(10L)
                                .setItemId(111L)
                                .setOrderCreationTime(LocalDateTime.parse("2021-01-01T00:00:01"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setPromocodeData("")
                                .setDeliveryRegionId(null)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderId(14)
                                .setMultiOrderId("multi_2")
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(10L)
                                .setItemId(144L)
                                .setOrderCreationTime(LocalDateTime.parse("2021-01-01T00:00:01"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setPromocodeData("")
                                .setDeliveryRegionId(null)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderId(12)
                                .setVid(null)
                                .setMultiOrderId(null)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(10L)
                                .setItemId(112L)
                                .setOrderCreationTime(LocalDateTime.parse("2021-01-01T00:00:01"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setPromocodeData("")
                                .setDeliveryRegionId(null)
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.flappingAbuse.before.csv"
    })
    public void testFlappingAbuse() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2020-01-01T02:00:02"),
                LocalDateTime.parse("2020-01-18T02:00:03")
        );
        Assertions.assertThat(allRecords)
                .usingElementComparatorOnFields("orderId", "orderStatus")
                .containsExactly(
                        ORDER_1_BASE_ITEM.but().setOrderStatus(MbiOrderStatus.DELIVERED).build(),
                        ORDER_1_BASE_ITEM.but().setOrderStatus(MbiOrderStatus.PROCESSING).build()
        );
    }

    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.filter.query.before.csv"
    })
    public void testQueryFilter() {
        List<DistributionOrderStatsRaw> result = new ArrayList<>();
        distributionOrderStatsDao.getStatsByOrderIdsFromQuery(
                "select order_id from market_billing.distribution_order_stats where order_id in (10, 12)",
                result::add);
        Assertions.assertThat(result)
                .usingElementComparatorOnFields("orderId", "itemId")
                .containsExactlyInAnyOrder(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderId(10)
                                .setItemId(111)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderId(12)
                                .setItemId(112)
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = {
            "db/DistributionOrderStatsDaoTest.filterByClids.before.csv"
    })
    public void testFilterByClids() {
        List<DistributionOrderStatsRaw> result = new ArrayList<>();
        distributionOrderStatsDao.getStatsByClids(Arrays.asList(4L, 5L), result::add);
        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        ORDER_1_BASE_ITEM.but()
                                .setOrderId(10)
                                .setMultiOrderId("multi_2")
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(10L)
                                .setItemId(111L)
                                .setOrderCreationTime(LocalDateTime.parse("2021-01-01T00:00:01"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setPromocodeData("")
                                .setClid(4L)
                                .setDeliveryRegionId(null)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setOrderId(14)
                                .setMultiOrderId("multi_2")
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(10L)
                                .setItemId(144L)
                                .setOrderCreationTime(LocalDateTime.parse("2021-01-01T00:00:01"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-01-01T00:01:01"))
                                .setPromocodeData("")
                                .setClid(5L)
                                .setDeliveryRegionId(null)
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(after = {
            "db/DistributionOrderStatsDaoTest.insert.csv"
    })
    public void testInsert() {
        distributionOrderStatsDao.insert(List.of(
                DISTR_ORDER_STATS_1.build(),
                DISTR_ORDER_STATS_2.build()
        ));
    }

    @Test
    public void testInsertOrder() {
        namedParameterJdbcTemplate.update("ALTER SEQUENCE market_billing.s_distribution_order_stats RESTART WITH "
                + (DistributionOrderStatsDao.LAST_RESTORED_ID + 1), new MapSqlParameterSource());
        distributionOrderStatsDao.insert(List.of(
                DISTR_ORDER_STATS_1.but()
                        .setOrderStatusEventId(14)
                        .setDistributionStatus(AdmitadOrderStatus.CANCELLED)
                        .build(),
                DISTR_ORDER_STATS_1.but()
                        .setOrderStatusEventId(13)
                        .build()
        ));
        List<DistributionOrderStats> stats = getDistributionOrderStatsByStatsCreationTime(
                LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-03"), 10);
        long eventId0 = stats.get(0).getOrderStatusEventId();
        long eventId1 = stats.get(1).getOrderStatusEventId();
        if (eventId0 < eventId1) {
            Assertions.assertThat(stats.get(0).getId()).isLessThan(stats.get(1).getId());
        } else {
            Assertions.assertThat(stats.get(0).getId()).isGreaterThan(stats.get(1).getId());
        }
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.fetch.before.csv")
    public void testGetOrderStats() {
        List<DistributionOrderStats> records = getDistributionOrderStatsByStatsCreationTime(
                LocalDate.of(2020, 1, 2),
                LocalDate.of(2020, 1, 3),
                1000
        );

        Assertions.assertThat(records)
                .usingElementComparatorOnFields("id", "orderId", "itemId", "orderStatus")
                .containsExactlyInAnyOrder(
                        DistributionOrderStats.builder()
                                .setId(59625798)
                                .setOrderId(1)
                                .setItemId(11)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                .build(),
                        DistributionOrderStats.builder()
                                .setId(59625799)
                                .setOrderId(3)
                                .setItemId(33)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setDistributionStatus(AdmitadOrderStatus.ON_HOLD)
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.fetch.before.csv")
    public void testGetOrderStatsEmpty() {
        List<DistributionOrderStats> records = getDistributionOrderStatsByStatsCreationTime(
                LocalDate.of(2020, 3, 2),
                LocalDate.of(2020, 3, 3),
                1000
        );

        Assertions.assertThat(records).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.get.approved.aggregate.before.csv")
    public void testGetApprovedDistributionOrderStatsAggregate() {
        List<DistributionOrderStatsAggregate> records =
                distributionOrderStatsDao.getApprovedDistributionOrderStatsAggregate(
                LocalDate.of(2021, 3, 3)
        );

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        DistributionOrderStatsAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setPromocode("BEBEBE")
                                .setOrdersCount(2L)
                                .setPartnerPayment(BigDecimal.valueOf(5.08d))
                                .setOrdersBilledCost(BigDecimal.valueOf(26))
                                .build(),
                        DistributionOrderStatsAggregate.builder()
                                .setClid(124L)
                                .setVid("vid_1_1")
                                .setOrdersCount(1L)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setOrdersBilledCost(BigDecimal.valueOf(15))
                                .build(),
                        DistributionOrderStatsAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_2_1")
                                .setOrdersCount(1L)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setOrdersBilledCost(BigDecimal.valueOf(13))
                                .build(),
                        DistributionOrderStatsAggregate.builder()
                                .setClid(124L)
                                .setVid("vid_2_2")
                                .setOrdersCount(1L)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setOrdersBilledCost(BigDecimal.valueOf(16))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.get.approved.category.aggregate.before.csv")
    public void testGetApprovedDistributionOrderStatsAggregateByCategory() {
        List<DistributionOrderStatsDimensionAggregate> records =
                distributionOrderStatsDao.getApprovedDistributionOrderStatsAggregateByDimension(
                        LocalDate.of(2021, 3, 3)
                );

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setPromocode("BEBEBE")
                                .setCategoryId(9999)
                                .setDeliveryRegionId(10995L)
                                .setItemsCount(4L)
                                .setPartnerPayment(BigDecimal.valueOf(5.08d))
                                .setPartnerPaymentMax(BigDecimal.valueOf(5.08d))
                                .setItemsBilledCost(BigDecimal.valueOf(23))
                                .build(),
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_2_1")
                                .setCategoryId(9998)
                                .setDeliveryRegionId(10995L)
                                .setItemsCount(2L)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentMax(BigDecimal.valueOf(2.54d))
                                .setItemsBilledCost(BigDecimal.valueOf(13))
                                .build(),
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setCategoryId(9998)
                                .setDeliveryRegionId(10995L)
                                .setItemsCount(2L)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentMax(BigDecimal.valueOf(2.54d))
                                .setItemsBilledCost(BigDecimal.valueOf(14))
                                .build(),
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(124L)
                                .setVid("vid_2_2")
                                .setCategoryId(9998)
                                .setDeliveryRegionId(10995L)
                                .setItemsCount(2L)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentMax(BigDecimal.valueOf(2.54d))
                                .setItemsBilledCost(BigDecimal.valueOf(16))
                                .build(),
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(124L)
                                .setVid("vid_1_1")
                                .setCategoryId(9999)
                                .setDeliveryRegionId(10995L)
                                .setItemsCount(2L)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentMax(BigDecimal.valueOf(2.54d))
                                .setItemsBilledCost(BigDecimal.valueOf(15))
                                .build()
                );
    }


    @Test
    @DbUnitDataSet(before = "db/DistributionOrderStatsDaoTest.get.created.aggregate.before.csv")
    public void testGetCreatedDistributionOrderStatsAggregate() {
        List<DistributionOrderStatsAggregate> records =
                distributionOrderStatsDao.getCreatedDistributionOrderStatsAggregate(
                LocalDate.of(2021, 3, 1)
        );

        Assertions.assertThat(records)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        DistributionOrderStatsAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setPromocode("BEBE_LEFT")
                                .setOrdersCount(1L)
                                .setOrdersCountCancelled(0)
                                .setOrdersCountNotCancelled(1)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentExcludingCancelled(BigDecimal.valueOf(2.54d))
                                .setOrdersBilledCost(BigDecimal.valueOf(20))
                                .setOrdersBilledCostNotCancelled(BigDecimal.valueOf(20))
                                .build(),
                        DistributionOrderStatsAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setPromocode("BEBE_RIGHT")
                                .setOrdersCount(2L)
                                .setOrdersCountCancelled(0)
                                .setOrdersCountNotCancelled(2)
                                .setPartnerPayment(BigDecimal.valueOf(6.08d))
                                .setPartnerPaymentExcludingCancelled(BigDecimal.valueOf(6.08d))
                                .setOrdersBilledCost(BigDecimal.valueOf(60))
                                .setOrdersBilledCostNotCancelled(BigDecimal.valueOf(60))
                                .build(),
                        DistributionOrderStatsAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setPromocode("BEBE_ONLY")
                                .setOrdersCount(1L)
                                .setOrdersCountCancelled(1)
                                .setOrdersCountNotCancelled(0)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentExcludingCancelled(BigDecimal.valueOf(0))
                                .setOrdersBilledCost(BigDecimal.valueOf(20))
                                .setOrdersBilledCostNotCancelled(BigDecimal.valueOf(0))
                                .build(),
                        DistributionOrderStatsAggregate.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setPromocode(null)
                                .setOrdersCount(1L)
                                .setOrdersCountCancelled(0)
                                .setOrdersCountNotCancelled(1)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentExcludingCancelled(BigDecimal.valueOf(2.54d))
                                .setOrdersBilledCost(BigDecimal.valueOf(20))
                                .setOrdersBilledCostNotCancelled(BigDecimal.valueOf(20))
                                .build(),
                        DistributionOrderStatsAggregate.builder()
                                .setClid(124L)
                                .setVid("vid_1_1")
                                .setPromocode("OTHER")
                                .setOrdersCount(1L)
                                .setOrdersCountCancelled(0)
                                .setOrdersCountNotCancelled(1)
                                .setPartnerPayment(BigDecimal.valueOf(2.54d))
                                .setPartnerPaymentExcludingCancelled(BigDecimal.valueOf(2.54d))
                                .setOrdersBilledCost(BigDecimal.valueOf(20))
                                .setOrdersBilledCostNotCancelled(BigDecimal.valueOf(20))
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(before =
            "db/DistributionOrderStatsDaoTest.multi_order.part.approved.before.csv"
    )
    public void testMultiOrderPartApproved() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2021-10-31T00:00:01"),
                LocalDateTime.parse("2021-11-03T02:00:02")
        );
        Assertions.assertThat(allRecords)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        ORDER_1_BASE_ITEM.but()
                                .setMultiOrderId("multi_5")
                                .setOrderId(6)
                                .setItemId(601)
                                .setClid(22L)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(4)
                                .setPromocodeData("3:PROMO-AF")
                                .setOrderCreationTime(LocalDateTime.parse("2021-11-01T00:00"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-11-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-11-01T00:00:01"))
                                .setRecentPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setRecentDistributionStatus(AdmitadOrderStatus.PROCESSING)
                                .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                                .setRecentClid(22L)
                                .setHasRecentStats(true)
                                .setDeliveryRegionId(null)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setMultiOrderId("multi_5")
                                .setOrderId(6)
                                .setItemId(601)
                                .setClid(22L)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(3)
                                .setPromocodeData("3:PROMO-AF")
                                .setOrderCreationTime(LocalDateTime.parse("2021-11-01T00:00"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-11-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-11-01T00:00:01"))
                                .setDeliveryRegionId(null)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setMultiOrderId("multi_5")
                                .setOrderId(5)
                                .setItemId(501)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setPromocodeData("3:PROMO-AF")
                                .setOrderCreationTime(LocalDateTime.parse("2021-11-01T00:00"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-11-01T02:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-11-01T14:11:56.610"))
                                .setRecentPartnerPaymentNoVat(BigDecimal.valueOf(60))
                                .setRecentDistributionStatus(AdmitadOrderStatus.APPROVED)
                                .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                                .setRecentApprovalTime(LocalDateTime.parse("2021-11-01T14:11:56.610"))
                                .setRecentClid(1L)
                                .setHasRecentStats(true)
                                .setDeliveryRegionId(null)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setMultiOrderId("multi_5")
                                .setOrderId(5)
                                .setItemId(501)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setPromocodeData("3:PROMO-AF")
                                .setOrderCreationTime(LocalDateTime.parse("2021-11-01T00:00"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-11-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-11-01T14:11:56.610"))
                                .setHasRecentStats(true)
                                .setRecentPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setRecentDistributionStatus(AdmitadOrderStatus.PROCESSING)
                                .setRecentApprovalTime(LocalDateTime.parse("2021-11-01T14:11:56.610"))
                                .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                                .setRecentClid(1L)
                                .setDeliveryRegionId(null)
                                .build()

                );
    }

    @Test
    @DbUnitDataSet(before =
            "db/DistributionOrderStatsDaoTest.old_recent_entries.before.csv"
    )
    public void testOldRecentEntries() {
        List<DistributionOrderStatsRaw> allRecords = getChanges(
                LocalDateTime.parse("2021-10-31T00:00:01"),
                LocalDateTime.parse("2021-11-03T02:00:02")
        );
        Assertions.assertThat(allRecords)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        ORDER_1_BASE_ITEM.but()
                                .setMultiOrderId(null)
                                .setOrderId(5)
                                .setItemId(501)
                                .setOrderStatus(MbiOrderStatus.DELIVERED)
                                .setOrderStatusEventId(2)
                                .setPromocodeData((String) null)
                                .setOrderCreationTime(LocalDateTime.parse("2021-08-31T00:00"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-11-01T02:00:01"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-11-01T02:00:01"))
                                .setHasRecentStats(false)
                                .setDeliveryRegionId(null)
                                .build(),
                        ORDER_1_BASE_ITEM.but()
                                .setMultiOrderId(null)
                                .setOrderId(5)
                                .setItemId(501)
                                .setOrderStatus(MbiOrderStatus.PROCESSING)
                                .setOrderStatusEventId(1)
                                .setPromocodeData((String) null)
                                .setOrderCreationTime(LocalDateTime.parse("2021-08-31T00:00"))
                                .setOrderStatusChangeTime(LocalDateTime.parse("2021-09-01T00:00:01"))
                                .setItemDataUpdateTime(LocalDateTime.parse("2021-09-01T00:00:01"))
                                .setRecentPartnerPaymentNoVat(BigDecimal.ZERO)
                                .setRecentDistributionStatus(AdmitadOrderStatus.PROCESSING)
                                .setRecentAdditionalInfo(Set.of(DistributionShareAdditionalInfo.GENERAL_TARIFF))
                                .setRecentClid(1L)
                                .setHasRecentStats(true)
                                .setDeliveryRegionId(null)
                                .build());
    }

    @Test
    @DbUnitDataSet(before =
            "db/DistributionOrderStatsDaoTest.fetch.closed.month.before.csv"
    )
    public void testGetClosedMonth() {
        List<DistributionOrderStatsClosedEntry> result = new ArrayList<>();
        distributionOrderStatsDao.getDistributionOrderStatsClosedMonth(LocalDate.parse("2022-01-01"), result::add, 5);
        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .containsOnly(
                        DistributionOrderStatsClosedEntry.builder()
                                .setOrderId(1)
                                .setClid(2357259)
                                .setVid("f045fc5aba18aece2cb2aa82286305d1")
                                .setOrderPaymentNoVat(BigDecimal.valueOf(21723.6))
                                .setApprovalTime(LocalDateTime.parse("2022-01-01T00:02:55.854000"))
                                .setStatsCreationTime(LocalDateTime.parse("2022-01-01T01:21:52.495106"))
                                .build());
    }

    @Test
    @DbUnitDataSet(before =
            "db/DistributionOrderStatsDaoTest.referral.coupon.before.csv"
    )
    public void testReferralCoupon() {
        var result = getChanges(
                LocalDateTime.parse("2022-04-01T00:00:00"),
                LocalDateTime.parse("2022-04-05T00:00:00"));
        Assertions.assertThat(result)
                .usingElementComparatorOnFields("orderId", "isRefCoupon")
                .containsExactlyInAnyOrder(
                        DistributionOrderStatsRaw.builder().setOrderId(255).setIsRefCoupon(true).build(),
                        DistributionOrderStatsRaw.builder().setOrderId(256).setIsRefCoupon(false).build(),
                        DistributionOrderStatsRaw.builder().setOrderId(257).setIsRefCoupon(true).build()
                );
    }

    private List<DistributionOrderStatsRaw> getChanges(LocalDateTime from, LocalDateTime to) {
        List<DistributionOrderStatsRaw> result = new ArrayList<>();
        distributionOrderStatsDao.getChangesCustomDepth(from, to, result::add, 100000);
        return result;
    }

    private List<DistributionOrderStats> getDistributionOrderStatsByStatsCreationTime(
            LocalDate dateFrom,
            LocalDate dateTo,
            int fetchSize
    ) {
        List<DistributionOrderStats> result = new ArrayList<>();
        distributionOrderStatsDao.getDistributionOrderStatsByStatsCreationTime(
                dateFrom, dateTo, result::add, fetchSize);
        return List.copyOf(result);
    }
}
