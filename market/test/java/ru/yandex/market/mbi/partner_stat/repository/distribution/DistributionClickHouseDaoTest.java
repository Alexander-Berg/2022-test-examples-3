package ru.yandex.market.mbi.partner_stat.repository.distribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner_stat.config.ClickHouseTestConfig;
import ru.yandex.market.mbi.partner_stat.mvc.distribution.model.SortingField;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionBalance;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionGraphsInfo;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionOrder;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionOrderItem;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionTopOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParametersAreNonnullByDefault
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(
        classes = ClickHouseTestConfig.class
)
@ActiveProfiles("clickHouseTest")
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class DistributionClickHouseDaoTest extends ClickhouseFunctionalTest {
    public static final DistributionOrder DISTRIBUTION_ORDER_1_2 = DistributionOrder.builder()
            .setClid(1L)
            .setCart(BigDecimal.valueOf(1.11))
            .setPayment(BigDecimal.valueOf(1.11))
            .setPromocode("BE_1")
            .setDateCreated(LocalDateTime.parse("2021-02-01T10:00:00"))
            .setDateUpdated(LocalDateTime.parse("2021-02-02T10:00:00"))
            .setStatus(1)
            .setVid("some_vid")
            .setOrderId(1001L)
            .setTariff("general")
            .setItems(
                    Arrays.asList(DistributionOrderItem.builder()
                            .setItemId(0)
                            .setCart(BigDecimal.valueOf(1.11))
                            .setPayment(BigDecimal.valueOf(1.11))
                            .setTariffRate(new BigDecimal("0.01800000"))
                            .setTariffName("CEHAC")
                            .setItemCount(0)
                            .build())
            )
            .setAdditionalInfo(new ArrayList<>())
            .build();

    public static final DistributionOrder DISTRIBUTION_ORDER_2_2 = DistributionOrder.builder()
            .setClid(1L)
            .setCart(BigDecimal.valueOf(1.12))
            .setPayment(BigDecimal.valueOf(1.12))
            .setPromocode("BE_1")
            .setDateCreated(LocalDateTime.parse("2021-02-01T02:00:00"))
            .setDateUpdated(LocalDateTime.parse("2021-02-03T02:00:00"))
            .setStatus(1)
            .setVid("some_vid")
            .setOrderId(1002L)
            .setTariff("general")
            .setItems(
                    Arrays.asList(DistributionOrderItem.builder()
                            .setItemId(0)
                            .setCart(BigDecimal.valueOf(1.12))
                            .setPayment(BigDecimal.valueOf(1.12))
                            .setTariffRate(new BigDecimal("0.08600000"))
                            .setTariffName("CEHAC")
                            .setItemCount(0)
                            .build())
            )
            .setAdditionalInfo(new ArrayList<>())
            .build();

    @Autowired
    private DistributionClickHouseDao distributionClickHouseDao;

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/balance_order_stats.csv")
    public void testDistributionOrderStatsGetBalance() {
        DistributionBalance balance = distributionClickHouseDao.getDistributionOrderStatsBalanceForMonth(
                List.of(1L), LocalDateTime.of(2021, 2, 3, 13, 0, 0));
        DistributionBalance expected = DistributionBalance.builder()
                .setBalance(BigDecimal.valueOf(10.01))
                .setApprovedQuantity(12)
                .setProcessingQuantity(5).build();
        assertEquals(expected.getApprovedQuantity(), balance.getApprovedQuantity());
        assertEquals(expected.getProcessingQuantity(), balance.getProcessingQuantity());
        assertThat(expected.getBalance().doubleValue()).isEqualTo(balance.getBalance().doubleValue());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/graph_complex_distribution_order_stats.csv")
    public void testGraphComplexDistributionOrderStats() {
        LocalDate dateFrom = LocalDate.of(2020, 3, 14);
        LocalDate dateTo = LocalDate.of(2020, 3, 16);
        List<DistributionGraphsInfo> infos =
                distributionClickHouseDao.getDistributionOrderStatsGraphsInfo(List.of(1L), null, null,
                dateFrom, dateTo);
        assertEquals(2, infos.size());
        var expected14 = DistributionGraphsInfo.builder()
                .setDate(dateFrom)
                .setClicks(210L)
                .setPayment(new BigDecimal("1.56"))
                .setConversion(BigDecimal.valueOf(4.3))
                .setPending(9)
                .setApproved(6)
                .build();
        var expected15 = DistributionGraphsInfo.builder()
                .setDate(dateFrom.plusDays(1))
                .setClicks(217L)
                .setPayment(new BigDecimal("1.66"))
                .setConversion(BigDecimal.valueOf(2.8))
                .setPending(6)
                .setApproved(5)
                .build();
        assertEquals(expected14, infos.get(0));
        assertEquals(expected15, infos.get(1));
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/graph_missing_clicks_distribution_order_stats.csv")
    public void testGraphNoClicksDistributionOrderStats() {
        LocalDate dateFrom = LocalDate.of(2020, 3, 14);
        LocalDate dateTo = LocalDate.of(2020, 3, 15);
        List<DistributionGraphsInfo> infos =
                distributionClickHouseDao.getDistributionOrderStatsGraphsInfo(List.of(1L), null, null,
                dateFrom, dateTo);
        assertEquals(1, infos.size());
        var actual = infos.get(0);
        assertThat(actual.getConversion()).isNull();
        assertEquals(0, actual.getClicks());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/graph_missing_15th_distribution_order_stats.csv")
    public void testGraphNoDataForADayDistributionOrderStats() {
        LocalDate dateFrom = LocalDate.of(2020, 3, 14);
        LocalDate dateTo = LocalDate.of(2020, 3, 16);
        List<DistributionGraphsInfo> infos =
                distributionClickHouseDao.getDistributionOrderStatsGraphsInfo(List.of(1L), null, null,
                dateFrom, dateTo);
        infos.forEach(System.out::println);
        assertEquals(2, infos.size());
        var actual15 = infos.get(1);
        assertEquals(dateFrom.plusDays(1), actual15.getDate());
        assertEquals(0, actual15.getApproved());
        assertEquals(0, actual15.getPending());
        assertEquals(BigDecimal.ZERO, actual15.getConversion());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/graph_missing_clicks_distribution_order_stats.csv")
    public void testGraphWrongDatesDistributionOrderStats() {
        LocalDate dateFrom = LocalDate.of(2020, 3, 14);
        LocalDate dateTo = LocalDate.of(2020, 3, 13);
        List<DistributionGraphsInfo> infos =
                distributionClickHouseDao.getDistributionOrderStatsGraphsInfo(List.of(1L), null, null,
                dateFrom, dateTo);
        assertEquals(0, infos.size());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/graph_vid_distribution_order_stats.csv")
    public void testGraphVidFilterDistributionOrderStats() {
        LocalDate dateFrom = LocalDate.of(2020, 3, 14);
        LocalDate dateTo = LocalDate.of(2020, 3, 15);
        List<DistributionGraphsInfo> infos = distributionClickHouseDao.getDistributionOrderStatsGraphsInfo(
                List.of(1L), "aaa", null,
                dateFrom, dateTo);
        assertEquals(1, infos.size());
        var expected14 = DistributionGraphsInfo.builder()
                .setDate(dateFrom)
                .setClicks(371L)
                .setPayment(new BigDecimal("1.52"))
                .setConversion(BigDecimal.valueOf(0.3))
                .setPending(1)
                .setApproved(2)
                .build();
        assertEquals(expected14, infos.get(0));
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/graph_multiple_clids_distribution_order_stats.csv")
    public void testGraphMultipleClidsDistributionOrderStats() {
        LocalDate dateFrom = LocalDate.of(2020, 3, 14);
        LocalDate dateTo = LocalDate.of(2020, 3, 16);
        List<DistributionGraphsInfo> infos = distributionClickHouseDao.getDistributionOrderStatsGraphsInfo(
                List.of(1L, 2L), null, null, dateFrom, dateTo);
        assertEquals(2, infos.size());
        var expected14 = DistributionGraphsInfo.builder()
                .setDate(dateFrom)
                .setClicks(371L)
                .setPayment(new BigDecimal("0.53"))
                .setConversion(BigDecimal.valueOf(1.1))
                .setPending(4)
                .setApproved(2)
                .build();
        var expected15 = DistributionGraphsInfo.builder()
                .setDate(dateFrom.plusDays(1))
                .setClicks(217L)
                .setPayment(new BigDecimal("1.63"))
                .setConversion(BigDecimal.valueOf(0.9))
                .setPending(2)
                .setApproved(2)
                .build();
        assertEquals(expected14, infos.get(0));
        assertEquals(expected15, infos.get(1));
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/top_orders_complex_stats.csv")
    public void testTopComplexDistributionOrderStats() {
        List<DistributionTopOrder> topOrders =
                distributionClickHouseDao.getDistributionOrderStatsTopOrders(List.of(1L), null, null, 9);
        assertEquals(2, topOrders.size());
        assertEquals(20, topOrders.get(0).getOrdersCount());
        assertEquals("vid1", topOrders.get(0).getVid());
        assertEquals(10, topOrders.get(1).getOrdersCount());
        assertEquals("vid2", topOrders.get(1).getVid());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/top_orders_complex_stats.csv")
    public void testTopLimitDistributionOrderStats() {
        List<DistributionTopOrder> topOrders =
                distributionClickHouseDao.getDistributionOrderStatsTopOrders(List.of(1L), null, null, 1);
        assertEquals(1, topOrders.size());
        assertEquals(20, topOrders.get(0).getOrdersCount());
        assertEquals("vid1", topOrders.get(0).getVid());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/top_orders_complex_stats.csv")
    public void testTopNegativeLimitDistributionOrderStats() {
        assertThrows(IllegalArgumentException.class,
                () -> distributionClickHouseDao.getDistributionOrderStatsTopOrders(List.of(1L), null, null, -1));
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/top_dates_order_stats.csv")
    public void testTopDatesDistributionOrderStats() {
        LocalDate dateFrom = LocalDate.of(2021, 2, 3);
        LocalDate dateTo = LocalDate.of(2021, 2, 5);
        List<DistributionTopOrder> topOrders =
                distributionClickHouseDao.getDistributionOrderStatsTopOrders(List.of(1L), dateFrom, dateTo, 1);
        assertEquals(1, topOrders.size());
        assertEquals(2, topOrders.get(0).getOrdersCount());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/top_multiple_clids_order_stats.csv")
    public void testTopMultipleClidsDistributionOrderStats() {
        List<DistributionTopOrder> topOrders =
                distributionClickHouseDao.getDistributionOrderStatsTopOrders(List.of(1L, 2L), null, null, 1);
        assertEquals(1, topOrders.size());
        assertEquals(2, topOrders.get(0).getOrdersCount());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats.csv")
    public void testGetDistributionOrderStatsByCreationDate() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 9, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), null, null, 1, 5, 0,
                        null, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(DISTRIBUTION_ORDER_1_2);
    }


    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats.csv")
    public void testGetDistributionOrderStatsByOrderId() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 9, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), null, null, 1, 5, 0,
                        null, SortingOrder.ASC, false, 1001L);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(DISTRIBUTION_ORDER_1_2);
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats.csv")
    public void testGetDistributionOrderStatsAll() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), null, null, 1, 10, 0,
                        null, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        DISTRIBUTION_ORDER_1_2,
                        DISTRIBUTION_ORDER_2_2
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats.csv")
    public void testGetDistributionOrderStatsSortDesc() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), null, null, 1, 10, 0,
                        SortingField.CART, SortingOrder.DESC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        DISTRIBUTION_ORDER_2_2,
                        DISTRIBUTION_ORDER_1_2
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats.csv")
    public void testGetDistributionOrderStatsSortAsc() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), null, null, 1, 10, 0,
                        SortingField.CART, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        DISTRIBUTION_ORDER_1_2,
                        DISTRIBUTION_ORDER_2_2
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats_promocode.csv")
    public void testGetDistributionOrderStatsByPromocode() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), null, "BE_2", 1, 5, 0,
                        null, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        DistributionOrder.builder()
                                .setClid(1L)
                                .setCart(new BigDecimal("1.10"))
                                .setPayment(new BigDecimal("1.10"))
                                .setPromocode("BE_2")
                                .setDateCreated(LocalDateTime.parse("2021-02-01T02:00:00"))
                                .setDateUpdated(LocalDateTime.parse("2021-02-03T02:00:00"))
                                .setStatus(1)
                                .setVid("some_vid")
                                .setOrderId(1002L)
                                .setTariff("general")
                                .setItems(
                                        Arrays.asList(DistributionOrderItem.builder()
                                                .setItemId(0)
                                                .setCart(new BigDecimal("1.10"))
                                                .setPayment(new BigDecimal("1.10"))
                                                .setTariffRate(new BigDecimal("0.08600000"))
                                                .setTariffName("CEHAC")
                                                .setItemCount(0)
                                                .build())
                                )
                                .setAdditionalInfo(new ArrayList<>())
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats_vid.csv")
    public void testGetDistributionOrderStatsByVid() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), "some_vid_2", "BE_2", 1, 5, 0,
                        null, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        DistributionOrder.builder()
                                .setClid(1L)
                                .setCart(new BigDecimal("1.10"))
                                .setPayment(new BigDecimal("1.10"))
                                .setPromocode("BE_2")
                                .setDateCreated(LocalDateTime.parse("2021-02-01T02:00:00"))
                                .setDateUpdated(LocalDateTime.parse("2021-02-03T02:00:00"))
                                .setStatus(1)
                                .setVid("some_vid_2")
                                .setOrderId(1002L)
                                .setTariff("general")
                                .setItems(
                                        Arrays.asList(DistributionOrderItem.builder()
                                                .setItemId(0)
                                                .setCart(new BigDecimal("1.10"))
                                                .setPayment(new BigDecimal("1.10"))
                                                .setTariffRate(new BigDecimal("0.08600000"))
                                                .setTariffName("CEHAC")
                                                .setItemCount(0)
                                                .build())
                                )
                                .setAdditionalInfo(new ArrayList<>())
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats_vid_status.csv")
    public void testGetDistributionOrderStatsByCancelledStatus() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), "some_vid_1", "BE_1", 2, 5, 0,
                        null, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        DistributionOrder.builder()
                                .setClid(1L)
                                .setCart(new BigDecimal("1.10"))
                                .setPayment(new BigDecimal("1.10"))
                                .setPromocode("BE_1")
                                .setDateCreated(LocalDateTime.parse("2021-02-01T10:00:00"))
                                .setDateUpdated(LocalDateTime.parse("2021-02-02T10:00:00"))
                                .setStatus(2)
                                .setVid("some_vid_1")
                                .setOrderId(1001L)
                                .setTariff("general")
                                .setItems(
                                        Arrays.asList(DistributionOrderItem.builder()
                                                .setItemId(0)
                                                .setCart(new BigDecimal("1.10"))
                                                .setPayment(new BigDecimal("1.10"))
                                                .setTariffRate(new BigDecimal("0.08100000"))
                                                .setTariffName("CEHAC")
                                                .setItemCount(0)
                                                .build())
                                )
                                .setAdditionalInfo(new ArrayList<>())
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats.csv")
    public void testGetDistributionOrderStatsByUpdateDate() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 3, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 4, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(null, null, dateFrom, dateTo,
                        List.of(1L), null, null, 1, 5, 0,
                        null, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(DISTRIBUTION_ORDER_2_2);
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats.csv")
    public void testGetDistributionOrderStatsByUpdateDateStart() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 3, 0, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 4, 0, 0);
        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(null, null, dateFrom, null,
                        List.of(1L), null, null, 1, 5, 0,
                        null, SortingOrder.ASC, false, null);
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(DISTRIBUTION_ORDER_2_2);
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionClickHouseDaoTest/distribution_order_stats_diff.csv")
    public void testGetDistributionOrderStatsWithDifferentCountRecords() {
        LocalDateTime dateFrom = LocalDateTime.of(2021, Month.FEBRUARY, 1, 9, 0, 0);
        LocalDateTime dateTo = LocalDateTime.of(2021, Month.FEBRUARY, 9, 0, 0);

        List<DistributionOrder> orders =
                distributionClickHouseDao.getDistributionOrderStats(dateFrom, dateTo, null,
                        null, List.of(1L), "some_vid", "BE_1", 1, 5, 0,
                        SortingField.PAYMENT, SortingOrder.DESC, false, 1001L);

        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        DistributionOrder.builder()
                                .setClid(1L)
                                .setCart(BigDecimal.valueOf(1.02))
                                .setPayment(BigDecimal.valueOf(1.02))
                                .setPromocode("BE_1")
                                .setDateCreated(LocalDateTime.parse("2021-02-01T10:00:00"))
                                .setDateUpdated(LocalDateTime.parse("2021-02-03T02:00:00"))
                                .setStatus(1)
                                .setVid("some_vid")
                                .setOrderId(1001L)
                                .setTariff("general")
                                .setItems(
                                        Arrays.asList(
                                                DistributionOrderItem.builder()
                                                        .setItemId(0)
                                                        .setCart(BigDecimal.valueOf(1.02))
                                                        .setPayment(BigDecimal.valueOf(1.02))
                                                        .setTariffRate(new BigDecimal("0.08100000"))
                                                        .setTariffName("CEHAC")
                                                        .setItemCount(0)
                                                        .build(),
                                                DistributionOrderItem.builder()
                                                        .setItemId(1)
                                                        .setCart(BigDecimal.valueOf(1.02))
                                                        .setPayment(BigDecimal.valueOf(1.02))
                                                        .setTariffRate(new BigDecimal("0.08100000"))
                                                        .setTariffName("CEHAC")
                                                        .setItemCount(0)
                                                        .build()
                                        )
                                )
                                .setAdditionalInfo(new ArrayList<>())
                                .build()
                );
    }
}
