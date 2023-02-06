package ru.yandex.market.mbi.partner_stat.service.distribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.logging.log4j.util.Strings;
import org.dbunit.database.DatabaseConfig;
import org.jetbrains.annotations.NotNull;
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
import ru.yandex.market.mbi.partner_stat.config.FunctionalTestConfig;
import ru.yandex.market.mbi.partner_stat.mvc.distribution.model.SortingField;
import ru.yandex.market.mbi.partner_stat.repository.ClickhouseFunctionalTest;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionGraphIntervalsDiff;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionGraphsInfo;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionGraphsStat;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionOrder;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionOrderItem;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionStatsClicks;
import ru.yandex.market.mbi.partner_stat.service.distribution.model.DistributionStatsFlags;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(
        classes = {
                ClickHouseTestConfig.class,
                DistributionServiceConfig.class
        }
)
@ActiveProfiles("clickHouseTest")
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class DistributionStatServiceClickhouseTest extends ClickhouseFunctionalTest {
    private static final long CLID = 1L;
    private static final String VID = "1A";
    private static final LocalDateTime NOW =
            LocalDateTime.ofInstant(FunctionalTestConfig.TEST_CLOCK_TIME, ZoneId.systemDefault());

    @Autowired
    private DistributionStatService distributionStatService;

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionStatServiceClickhouseTest/graph_vids_distribution_order_stats.csv"
    )
    public void getV2DistributionGraphsInfoNoVid() {
        DistributionGraphsStat actual = distributionStatService.getDistributionOrderStatsGraphsInfo(
                List.of(700L, 701L),
                null,
                null,
                LocalDate.of(2020, 2, 4),
                LocalDate.of(2020, 2, 7)
        );

        assertThat(actual).usingRecursiveComparison().ignoringAllOverriddenEquals()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(
                        DistributionGraphsStat.builder().setCurrentInterval(List.of(
                                DistributionGraphsInfo.builder()
                                        .setDate(LocalDate.of(2020, 2, 4))
                                        .setPayment(BigDecimal.valueOf(1.11))
                                        .setConversion(BigDecimal.valueOf(0))
                                        .setClicks(2L)
                                        .setPending(0)
                                        .setApproved(1)
                                        .build(),
                                DistributionGraphsInfo.builder()
                                        .setDate(LocalDate.of(2020, 2, 5))
                                        .setPayment(BigDecimal.valueOf(1.11))
                                        .setConversion(BigDecimal.valueOf(0))
                                        .setClicks(2L)
                                        .setPending(0)
                                        .setApproved(1)
                                        .build(),
                                DistributionGraphsInfo.builder()
                                        .setDate(LocalDate.of(2020, 2, 6))
                                        .setPayment(BigDecimal.valueOf(1.11))
                                        .setConversion(BigDecimal.valueOf(0))
                                        .setClicks(2L)
                                        .setPending(0)
                                        .setApproved(1)
                                        .build()
                                )
                        )
                                .setPreviousInterval(List.of(
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 1))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(200))
                                                .setClicks(2L)
                                                .setPending(4)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 2))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(50))
                                                .setClicks(2L)
                                                .setPending(1)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 3))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(50))
                                                .setClicks(2L)
                                                .setPending(1)
                                                .setApproved(0)
                                                .build()
                                        )
                                )
                                .setSummaryCurrentInterval(
                                        DistributionGraphsInfo.builder()
                                                .setDate(null)
                                                .setPayment(BigDecimal.valueOf(3.33))
                                                .setConversion(BigDecimal.valueOf(0L))
                                                .setClicks(6L)
                                                .setPending(0)
                                                .setApproved(3)
                                                .build()
                                )
                                .setSummaryPreviousInterval(
                                        DistributionGraphsInfo.builder()
                                                .setDate(null)
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(100))
                                                .setClicks(6L)
                                                .setPending(6)
                                                .setApproved(0)
                                                .build()
                                )
                                .setIntervalsDiff(
                                        DistributionGraphIntervalsDiff.builder()
                                                .setConversion(-100)
                                                .setPayment(null)
                                                .setClicks(BigDecimal.valueOf(0L))
                                                .setCreatedOrders(BigDecimal.valueOf(-100L))
                                                .build()
                                )
                                .setStatsFlags(
                                        DistributionStatsFlags.builder()
                                                .setHasClicks(true)
                                                .setHasOrders(true)
                                                .setHasPayments(true)
                                                .build()
                                )
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionStatServiceClickhouseTest/graph_vids_distribution_order_stats.csv"
    )
    public void getV2DistributionGraphsInfoVid() {
        DistributionGraphsStat actual = distributionStatService.getDistributionOrderStatsGraphsInfo(
                List.of(700L, 701L),
                "vid2", null,
                LocalDate.of(2020, 2, 4),
                LocalDate.of(2020, 2, 7)
        );

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(
                        DistributionGraphsStat.builder()
                                .setCurrentInterval(List.of(
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 4))
                                                .setPayment(BigDecimal.valueOf(1.11))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(1L)
                                                .setPending(0)
                                                .setApproved(1)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 5))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(1L)
                                                .setPending(0)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 6))
                                                .setPayment(BigDecimal.valueOf(1.11))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(1L)
                                                .setPending(0)
                                                .setApproved(1)
                                                .build()
                                ))
                                .setPreviousInterval(List.of(
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 1))
                                                .setPayment(BigDecimal.valueOf(0.00))
                                                .setConversion(BigDecimal.valueOf(200))
                                                .setClicks(1L)
                                                .setPending(2)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 2))
                                                .setPayment(BigDecimal.valueOf(0.00))
                                                .setConversion(BigDecimal.valueOf(100))
                                                .setClicks(1L)
                                                .setPending(1)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 3))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(1L)
                                                .setPending(0)
                                                .setApproved(0)
                                                .build()
                                ))
                                .setSummaryCurrentInterval(
                                        DistributionGraphsInfo.builder()
                                                .setDate(null)
                                                .setPayment(BigDecimal.valueOf(2.22))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(3L)
                                                .setPending(0)
                                                .setApproved(2)
                                                .build()
                                )
                                .setSummaryPreviousInterval(
                                        DistributionGraphsInfo.builder()
                                                .setDate(null)
                                                .setPayment(BigDecimal.valueOf(0.00))
                                                .setConversion(BigDecimal.valueOf(100))
                                                .setClicks(3L)
                                                .setPending(3)
                                                .setApproved(0)
                                                .build()
                                )
                                .setIntervalsDiff(
                                        DistributionGraphIntervalsDiff.builder()
                                                .setConversion(-100)
                                                .setPayment(null)
                                                .setClicks(BigDecimal.valueOf(0L))
                                                .setCreatedOrders(BigDecimal.valueOf(-100L))
                                                .build()
                                )
                                .setStatsFlags(
                                        DistributionStatsFlags.builder()
                                                .setHasClicks(true)
                                                .setHasOrders(true)
                                                .setHasPayments(true)
                                                .build()
                                )
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionStatServiceClickhouseTest/graph_vids_distribution_order_stats.csv"
    )
    public void getV2DistributionGraphsInfoNoEndDate() {
        DistributionGraphsStat actual = distributionStatService.getDistributionOrderStatsGraphsInfo(
                List.of(700L, 701L),
                null, null,
                LocalDate.of(2020, 2, 4),
                null
        );

        assertThat(actual.getCurrentInterval().get(0)).usingRecursiveComparison()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(DistributionGraphsInfo.builder()
                        .setDate(LocalDate.of(2020, 2, 4))
                        .setPayment(BigDecimal.valueOf(1.11))
                        .setConversion(BigDecimal.valueOf(0))
                        .setClicks(2L)
                        .setPending(0)
                        .setApproved(1)
                        .build()
                );
        assertThat(actual.getCurrentInterval().get(1)).usingRecursiveComparison()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(DistributionGraphsInfo.builder()
                        .setDate(LocalDate.of(2020, 2, 5))
                        .setPayment(BigDecimal.valueOf(1.11))
                        .setConversion(BigDecimal.valueOf(0))
                        .setClicks(2L)
                        .setPending(0)
                        .setApproved(1)
                        .build()
                );

        assertThat(actual.getCurrentInterval().get(2)).usingRecursiveComparison()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(DistributionGraphsInfo.builder()
                        .setDate(LocalDate.of(2020, 2, 6))
                        .setPayment(BigDecimal.valueOf(1.11))
                        .setConversion(BigDecimal.valueOf(0))
                        .setClicks(2L)
                        .setPending(0)
                        .setApproved(1)
                        .build()
                );

        assertThat(actual.getPreviousInterval()).isEmpty();

        assertThat(actual.getSummaryCurrentInterval()).usingRecursiveComparison()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(DistributionGraphsInfo.builder()
                        .setDate(null)
                        .setPayment(BigDecimal.valueOf(3.33))
                        .setConversion(BigDecimal.valueOf(0L))
                        .setClicks(6L)
                        .setPending(0)
                        .setApproved(3)
                        .build()
                );

        assertThat(actual.getStatsFlags()).usingRecursiveComparison()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(DistributionStatsFlags.builder()
                        .setHasClicks(true)
                        .setHasOrders(true)
                        .setHasPayments(true)
                        .build()
                );
    }

    @Test
    void testDistributionOrderStatsIntervalDates() {
        LocalDate dateFrom = NOW.toLocalDate().minusDays(9);
        LocalDate dateTo = NOW.toLocalDate().minusDays(2);

        var actual = distributionStatService.getDistributionOrderStatsGraphsInfo(
                List.of(CLID), VID, null, dateFrom, dateTo);

        var current = actual.getCurrentInterval();
        assertThat(current.size()).isEqualTo(7);
        assertThat(current.get(0).getDate()).isEqualTo(dateFrom);
        assertThat(current.get(6).getDate()).isEqualTo(dateTo.minusDays(1));

        var previous = actual.getPreviousInterval();
        assertThat(previous.size()).isEqualTo(7);
        assertThat(previous.get(0).getDate()).isEqualTo(dateFrom.minusDays(7));
        assertThat(previous.get(6).getDate()).isEqualTo(dateFrom.minusDays(1));
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionStatServiceClickhouseTest/graph_distribution_order_stats.csv")
    void testDistributionOrderStatsGraphInfoNoDateLimits() {
        var actual = distributionStatService.getDistributionOrderStatsGraphsInfo(
                List.of(CLID), VID, null, null, null);
        assertThat(actual.getSummaryCurrentInterval())
                .usingRecursiveComparison()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(DistributionGraphsInfo.builder()
                        .setApproved(3)
                        .setPending(9)
                        .setPayment(BigDecimal.valueOf(0.3))
                        .setClicks(26L)
                        .setConversion(BigDecimal.valueOf(34.6))
                        .build()
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionStatServiceClickhouseTest/graph_gaps_distribution_order_stats.csv")
    void testDistributionOrderStatsGraphInfoFillGapsNoInterval() {
        var actual = distributionStatService.getDistributionOrderStatsGraphsInfo(
                List.of(CLID), VID, null, null, null);
        assertThat(actual.getSummaryCurrentInterval())
                .usingRecursiveComparison()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(DistributionGraphsInfo.builder()
                        .setApproved(3)
                        .setPending(9)
                        .setPayment(BigDecimal.valueOf(0.3))
                        .setClicks(26L)
                        .setConversion(BigDecimal.valueOf(34.6))
                        .build()
                );
        assertThat(actual.getCurrentInterval().size()).isEqualTo(21);
        assertThat(actual.getCurrentInterval()).contains(
                DistributionGraphsInfo.builder()
                        .setDate(LocalDate.of(2021, 1, 31))
                        .setApproved(0)
                        .setPending(0)
                        .setPayment(BigDecimal.ZERO)
                        .setClicks(0L)
                        .setConversion(null)
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionStatServiceClickhouseTest/graph_promocode_distribution_order_stats.csv")
    void testDistributionOrderStatsGraphInfoPromocode() {
        DistributionGraphsStat actual = distributionStatService.getDistributionOrderStatsGraphsInfo(
                List.of(700L, 701L),
                null, "TEST-PROMO-AF",
                LocalDate.of(2020, 2, 4),
                LocalDate.of(2020, 2, 7)
        );
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .withComparatorForType(bigDecimalComparator(), BigDecimal.class)
                .isEqualTo(
                        DistributionGraphsStat.builder()
                                .setCurrentInterval(List.of(
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 4))
                                                .setPayment(BigDecimal.valueOf(1.11))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(2L)
                                                .setPending(0)
                                                .setApproved(1)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 5))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(2L)
                                                .setPending(0)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 6))
                                                .setPayment(BigDecimal.valueOf(1.11))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(2L)
                                                .setPending(0)
                                                .setApproved(1)
                                                .build()
                                ))
                                .setPreviousInterval(List.of(
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 1))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(100))
                                                .setClicks(2L)
                                                .setPending(2)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 2))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(50))
                                                .setClicks(2L)
                                                .setPending(1)
                                                .setApproved(0)
                                                .build(),
                                        DistributionGraphsInfo.builder()
                                                .setDate(LocalDate.of(2020, 2, 3))
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(2L)
                                                .setPending(0)
                                                .setApproved(0)
                                                .build()
                                ))
                                .setSummaryCurrentInterval(
                                        DistributionGraphsInfo.builder()
                                                .setDate(null)
                                                .setPayment(BigDecimal.valueOf(2.22))
                                                .setConversion(BigDecimal.valueOf(0))
                                                .setClicks(6L)
                                                .setPending(0)
                                                .setApproved(2)
                                                .build()
                                )
                                .setSummaryPreviousInterval(
                                        DistributionGraphsInfo.builder()
                                                .setDate(null)
                                                .setPayment(BigDecimal.valueOf(0))
                                                .setConversion(BigDecimal.valueOf(50))
                                                .setClicks(6L)
                                                .setPending(3)
                                                .setApproved(0)
                                                .build()
                                )
                                .setIntervalsDiff(
                                        DistributionGraphIntervalsDiff.builder()
                                                .setConversion(-100)
                                                .setPayment(null)
                                                .setClicks(BigDecimal.valueOf(0L))
                                                .setCreatedOrders(BigDecimal.valueOf(-100L))
                                                .build()
                                )
                                .setStatsFlags(
                                        DistributionStatsFlags.builder()
                                                .setHasClicks(true)
                                                .setHasOrders(true)
                                                .setHasPayments(true)
                                                .build()
                                )
                                .build()
                );
    }



    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionStatServiceClickhouseTest/stats.csv")
    void testGetDistributionStatsClicksWithoutCurrentDay() {
        var result = distributionStatService.getDistributionStatsClicks(CLID, List.of(VID),
                NOW.toLocalDate().minusDays(10), NOW.toLocalDate());
        var dates = result.stream().map(DistributionStatsClicks::getDate).collect(Collectors.toList());
        assertThat(dates).doesNotContain(NOW.toLocalDate());
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            before = "DistributionStatServiceClickhouseTest/stats.csv")
    void testGetDistributionStatsClicksWithCurrentDay() {
        var list = distributionStatService.getDistributionStatsClicks(CLID, List.of(VID),
                NOW.toLocalDate(), NOW.plusDays(1).toLocalDate());
        assertThat(list.size()).isEqualTo(1);
        var statData = list.get(0);
        assertThat(statData.getClicks()).isEqualTo(70000L);
        assertThat(statData.getDate()).isEqualTo(NOW.toLocalDate());
    }

    @Test
    void testDivideByZero() {
        assertThat(
                DistributionStatService.calcPercentDiff(
                        BigDecimal.valueOf(1),
                        new BigDecimal("0.0"),
                        1
                )).isNull();
    }

    @Test
    void testGetSummaryIntervalsDiff() {
        DistributionGraphsInfo info1 =
                DistributionGraphsInfo.builder()
                        .setApproved(3)
                        .setPending(1)
                        .setClicks(10L)
                        .setPayment(BigDecimal.valueOf(12.0))
                        .setConversion(BigDecimal.valueOf(35.0))
                        .build();
        DistributionGraphsInfo info2 =
                DistributionGraphsInfo.builder()
                        .setApproved(7)
                        .setPending(2)
                        .setClicks(19L)
                        .setPayment(BigDecimal.valueOf(19.8))
                        .setConversion(BigDecimal.valueOf(37.0))
                        .build();
        var result = DistributionStatService.getSummaryIntervalDifference(info2, info1);
        assertThat(result.getConversion()).isEqualTo(6);
        assertThat(result.getPayment()).usingComparator(bigDecimalComparator())
                .isEqualTo(BigDecimal.valueOf(65));
        assertThat(result.getClicks()).usingComparator(bigDecimalComparator())
                .isEqualTo(BigDecimal.valueOf(90));
        assertThat(result.getCreatedOrders()).usingComparator(bigDecimalComparator())
                .isEqualTo(BigDecimal.valueOf(100));
    }

    @NotNull
    private static Comparator<BigDecimal> bigDecimalComparator() {
        return Comparator.nullsFirst(Comparator.comparingDouble(BigDecimal::doubleValue));
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionStatServiceClickhouseTest/distributionOrderStatsCount.csv"
    )
    public void getDistributionOrderStatsCount() {
        var count = distributionStatService.getDistributionOrderStatsCount(
                null,
                null,
                LocalDateTime.parse("2020-01-01T02:00:01"),
                LocalDateTime.parse("2020-01-04T02:00:01"),
                Arrays.asList(123L),
                "vid_1_1",
                null,
                1
        );
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionStatServiceClickhouseTest/distributionOrderStats.csv"
    )
    public void getDistributionOrdersStatsEmpty() {
        var orders = distributionStatService.getDistributionOrderStats(
                null,
                null,
                LocalDateTime.parse("2020-01-01T02:00:01"),
                LocalDateTime.parse("2020-01-04T02:00:01"),
                Arrays.asList(1234L),
                null,
                null,
                1,
                10,
                0,
                SortingField.VID,
                SortingOrder.DESC,
                false
        );
        assertThat(orders).isEmpty();
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionStatServiceClickhouseTest/distributionOrderStats.csv"
    )
    public void getDistributionOrderStatsById() {
        var order = distributionStatService.getDistributionOrderStatsById(
                Arrays.asList(123L),
                1L,
                false
        );
        assertThat(order.isPresent()).isTrue();
        assertThat(order.get())
                .usingRecursiveComparison()
                .isEqualTo(
                        DistributionOrder.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setOrderId(1L)
                                .setPromocode(Strings.EMPTY)
                                .setDateCreated(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setDateUpdated(LocalDateTime.parse("2020-01-02T02:00:01"))
                                .setStatus(1)
                                .setTariff("general")
                                .setPayment(new BigDecimal("0.03"))
                                .setCart(new BigDecimal("0.10"))
                                .setItems(Arrays.asList(DistributionOrderItem.builder()
                                        .setItemId(0)
                                        .setItemCount(2)
                                        .setCart(new BigDecimal("0.10"))
                                        .setPayment(new BigDecimal("0.03"))
                                        .setTariffRate(new BigDecimal("0.05000000"))
                                        .setTariffName("CEHAC")
                                        .build()))
                                .setAdditionalInfo(new ArrayList<>())
                                .build()
                );
    }

    @Test
    @DbUnitDataSet(
            dataSource = ClickHouseTestConfig.DATA_SOURCE,
            type = DataSetType.SINGLE_CSV,
            before = "DistributionStatServiceClickhouseTest/distributionOrderStats.csv"
    )
    public void getDistributionOrderStats() {
        var orders = distributionStatService.getDistributionOrderStats(
                null,
                null,
                LocalDateTime.parse("2020-01-01T02:00:01"),
                LocalDateTime.parse("2020-01-04T02:00:01"),
                Arrays.asList(123L),
                null,
                null,
                1,
                10,
                0,
                SortingField.VID,
                SortingOrder.DESC,
                false
        );
        assertThat(orders)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        DistributionOrder.builder()
                                .setClid(123L)
                                .setVid("vid_1_2")
                                .setOrderId(4L)
                                .setPromocode(Strings.EMPTY)
                                .setDateCreated(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setDateUpdated(LocalDateTime.parse("2020-01-02T02:00:01"))
                                .setStatus(1)
                                .setTariff("general")
                                .setPayment(new BigDecimal("0.03"))
                                .setCart(new BigDecimal("0.10"))
                                .setItems(Arrays.asList(DistributionOrderItem.builder()
                                        .setItemId(0)
                                        .setItemCount(2)
                                        .setCart(new BigDecimal("0.10"))
                                        .setPayment(new BigDecimal("0.03"))
                                        .setTariffRate(new BigDecimal("0.05000000"))
                                        .setTariffName("CEHAC")
                                        .build()))
                                .setAdditionalInfo(new ArrayList<>())
                                .build(),
                        DistributionOrder.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setOrderId(3L)
                                .setPromocode(Strings.EMPTY)
                                .setDateCreated(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setDateUpdated(LocalDateTime.parse("2020-01-02T02:00:01"))
                                .setStatus(1)
                                .setTariff("general")
                                .setPayment(new BigDecimal("0.03"))
                                .setCart(new BigDecimal("0.10"))
                                .setItems(Arrays.asList(DistributionOrderItem.builder()
                                        .setItemId(0)
                                        .setItemCount(2)
                                        .setCart(new BigDecimal("0.10"))
                                        .setPayment(new BigDecimal("0.03"))
                                        .setTariffRate(new BigDecimal("0.05000000"))
                                        .setTariffName("CEHAC")
                                        .build()))
                                .setAdditionalInfo(new ArrayList<>())
                                .build(),
                        DistributionOrder.builder()
                                .setClid(123L)
                                .setVid("vid_1_1")
                                .setOrderId(1L)
                                .setPromocode(Strings.EMPTY)
                                .setDateCreated(LocalDateTime.parse("2020-01-01T02:00:01"))
                                .setDateUpdated(LocalDateTime.parse("2020-01-02T02:00:01"))
                                .setStatus(1)
                                .setTariff("general")
                                .setPayment(new BigDecimal("0.03"))
                                .setCart(new BigDecimal("0.10"))
                                .setItems(Arrays.asList(DistributionOrderItem.builder()
                                        .setItemId(0)
                                        .setItemCount(2)
                                        .setCart(new BigDecimal("0.10"))
                                        .setPayment(new BigDecimal("0.03"))
                                        .setTariffRate(new BigDecimal("0.05000000"))
                                        .setTariffName("CEHAC")
                                        .build()))
                                .setAdditionalInfo(new ArrayList<>())
                                .build()
                );
    }
}
