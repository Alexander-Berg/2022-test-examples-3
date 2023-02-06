package ru.yandex.market.core.stats.ng.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.click.ClickType;
import ru.yandex.market.core.stats.ng.model.CampaignStatsItem;
import ru.yandex.market.core.stats.ng.model.CampaignStatsRequest;
import ru.yandex.market.core.stats.ng.model.OrdersStats;
import ru.yandex.market.core.stats.ng.model.PpGrouping;
import ru.yandex.market.core.stats.ng.model.StatsContainer;
import ru.yandex.market.core.stats.ng.model.StatsDateType;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author zoom
 */
public class DbCampaignStatsServiceTest extends FunctionalTest {

    private static Instant NOW = OffsetDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

    @Autowired
    private DbCampaignStatsService service;

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldReturnEmptyListWhenCampaignHasNoClicks() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(1, req);
        assertThat(new HashSet<>(result), equalTo(emptySet()));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldReturnOneElementWhenCampaignHasOneClickGroup() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(2, req);
        CampaignStatsItem item =
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        6,
                        BigInteger.valueOf(30),
                        null,
                        emptyMap(),
                        null);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(item)));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldZeroCpaSectionExistsInResponseWhenCpaFieldExistsInRequest() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(2, req);
        CampaignStatsItem item =
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        6,
                        BigInteger.valueOf(30),
                        null,
                        emptyMap(),
                        new OrdersStats(
                                0L,
                                BigDecimal.valueOf(0),
                                BigDecimal.valueOf(0L),
                                0L,
                                BigDecimal.valueOf(0),
                                BigDecimal.valueOf(0L)));
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(item)));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldZeroMainSectionWhenNoClicksButCpaFeeExists() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(10, req);
        CampaignStatsItem item =
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        0,
                        BigInteger.ZERO,
                        null,
                        emptyMap(),
                        new OrdersStats(
                                1L,
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(4L),
                                1L,
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(4L)));

        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(item)));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldZeroMainSectionWhenNoClicksButTwoCpaFeeExists() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(11, req);
        CampaignStatsItem item =
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        0,
                        BigInteger.ZERO,
                        null,
                        emptyMap(),
                        new OrdersStats(
                                2L,
                                BigDecimal.valueOf(200),
                                BigDecimal.valueOf(7L),
                                1L,
                                BigDecimal.valueOf(100),
                                BigDecimal.valueOf(3L)));

        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(item)));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldZeroMainSectionWhenNoClicksButCpaFeeExistsForTwoDays() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(12, req);
        assertThat(
                new HashSet<>(result),
                equalTo(ImmutableSet.of(
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 1)),
                                0,
                                0,
                                BigInteger.ZERO,
                                null,
                                emptyMap(),
                                new OrdersStats(
                                        2L,
                                        BigDecimal.valueOf(200),
                                        BigDecimal.valueOf(7L),
                                        1L,
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(3L))),
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 2)),
                                0,
                                0,
                                BigInteger.ZERO,
                                null,
                                emptyMap(),
                                new OrdersStats(
                                        1L,
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(5L),
                                        1L,
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(5L))))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHasOneDayWithCpaAndClickAndOneDayWithOnlyCpaFee() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(13, req);
        assertThat(
                result,
                equalTo(ImmutableList.of(
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 1)),
                                0,
                                60,
                                BigInteger.valueOf(300),
                                null,
                                emptyMap(),
                                new OrdersStats(
                                        2L,
                                        BigDecimal.valueOf(200),
                                        BigDecimal.valueOf(77L),
                                        0L,
                                        BigDecimal.valueOf(0),
                                        BigDecimal.valueOf(0L))),
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 2)),
                                0,
                                0,
                                BigInteger.ZERO,
                                null,
                                emptyMap(),
                                new OrdersStats(
                                        1L,
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(55L),
                                        1L,
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(55L))))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHasOneDayWithCpaAndClickAnd400ShowsAndOneDayWithOnlyCpaFeeAnd0Shows() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA, CampaignStatsRequest.Field.SHOWS));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(13, req);
        assertThat(
                result,
                equalTo(ImmutableList.of(
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 1)),
                                0,
                                60,
                                BigInteger.valueOf(300),
                                BigInteger.valueOf(400),
                                emptyMap(),
                                new OrdersStats(
                                        2L,
                                        BigDecimal.valueOf(200),
                                        BigDecimal.valueOf(77L),
                                        0L,
                                        BigDecimal.valueOf(0),
                                        BigDecimal.valueOf(0L))),
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 2)),
                                0,
                                0,
                                BigInteger.ZERO,
                                BigInteger.ZERO,
                                emptyMap(),
                                new OrdersStats(
                                        1L,
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(55L),
                                        1L,
                                        BigDecimal.valueOf(100),
                                        BigDecimal.valueOf(55L))))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldReturnEmptyListWhenCpaFeeExistsButGroupingByPP() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.BY_PP);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(11, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of()));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldNotShowCpaFeeWhenGroupingByPP() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.BY_PP);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.CPA));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(13, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        3,
                        60,
                        BigInteger.valueOf(300),
                        null,
                        emptyMap(),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHave120PhoneClicksAndNoCpaStatsWhileFilteringByClickType() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setClickTypeId(ClickType.PHONE.id());
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        120,
                        BigInteger.valueOf(600),
                        null,
                        emptyMap(),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHaveMobileStatsWhileFilteringByClickType() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setClickTypeId(ClickType.PHONE.id());
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.MOBILE));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        120,
                        BigInteger.valueOf(600),
                        null,
                        ImmutableMap.of("mobile", new StatsContainer(60, BigInteger.valueOf(300), null)),
                        null))));
    }


    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHaveNoSpendingWhileFilteringByDiscounts() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setSpendingFilter(CampaignStatsRequest.SpendingFilter.DISCOUNTS);
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        3,
                        BigInteger.ONE,
                        null,
                        emptyMap(),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHaveMobileSpendingWhileFilteringByDiscounts() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setSpendingFilter(CampaignStatsRequest.SpendingFilter.DISCOUNTS);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.MOBILE));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        3,
                        BigInteger.ONE,
                        null,
                        ImmutableMap.of("mobile", new StatsContainer(2, BigInteger.ZERO, null)),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHaveShowsAndMobileStatsWithSpendingFilterApplied() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setSpendingFilter(CampaignStatsRequest.SpendingFilter.DISCOUNTS);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.MOBILE, CampaignStatsRequest.Field.SHOWS));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        3,
                        BigInteger.ONE,
                        BigInteger.valueOf(500),
                        ImmutableMap.of("mobile", new StatsContainer(2, BigInteger.ZERO, BigInteger.valueOf(100))),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHave60ClicksAndNoCpaWhileFilteringByEventtime() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setDateType(StatsDateType.EVENTTIME);
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(15, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        60,
                        BigInteger.valueOf(300),
                        null,
                        emptyMap(),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldHaveNoShowsWhenEventTypeIsTranTime() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setDateType(StatsDateType.TRANTIME);
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(15, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        120,
                        BigInteger.valueOf(600),
                        null,
                        emptyMap(),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldReturnMobileStatsAndClicksWithoutDiscounts() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.NONE);
        req.setSpendingFilter(CampaignStatsRequest.SpendingFilter.WITHOUT_DISCOUNTS);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.MOBILE, CampaignStatsRequest.Field.SHOWS));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        0,
                        117,
                        BigInteger.valueOf(599),
                        BigInteger.valueOf(500),
                        ImmutableMap.of("mobile", new StatsContainer(58, BigInteger.valueOf(300), BigInteger.valueOf(100))),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldReturnClickAndCpaStatsGroupedByPpForOneDay() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.BY_PP);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.MOBILE, CampaignStatsRequest.Field.SHOWS));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(
                new HashSet<>(result),
                equalTo(ImmutableSet.of(
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 1)),
                                14,
                                60,
                                BigInteger.valueOf(300),
                                BigInteger.valueOf(400),
                                ImmutableMap.of("mobile", new StatsContainer(0, BigInteger.ZERO, BigInteger.ZERO)),
                                null),
                        new CampaignStatsItem(
                                Date.valueOf(LocalDate.of(2016, 1, 1)),
                                38,
                                60,
                                BigInteger.valueOf(300),
                                BigInteger.valueOf(100),
                                ImmutableMap.of("mobile", new StatsContainer(60, BigInteger.valueOf(300), BigInteger.valueOf(100))),
                                null))));

    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldReturnClicksOnlyForFourthPpGroup() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.BY_PP_GROUP);
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        4,
                        60,
                        BigInteger.valueOf(300),
                        null,
                        emptyMap(),
                        null))));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "DbCampaignStatsServiceTest.csv")
    public void shouldReturnClickStatsGroupedByPpGroupForOneDay() {
        CampaignStatsRequest req = new CampaignStatsRequest();
        req.setPpGrouping(PpGrouping.BY_PP_GROUP);
        req.setFields(ImmutableSet.of(CampaignStatsRequest.Field.MODEL));
        req.setFromDate(Date.from(NOW.minus(30, ChronoUnit.DAYS)));
        List<CampaignStatsItem> result = service.getMainStats(14, req);
        assertThat(new HashSet<>(result), equalTo(ImmutableSet.of(
                new CampaignStatsItem(
                        Date.valueOf(LocalDate.of(2016, 1, 1)),
                        7,
                        60,
                        BigInteger.valueOf(300),
                        null,
                        emptyMap(),
                        null))));
    }

}
