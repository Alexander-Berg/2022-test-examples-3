package ru.yandex.direct.intapi.statistic;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.statistic.StatisticController;
import ru.yandex.direct.intapi.fatconfiguration.FatIntApiTest;
import ru.yandex.direct.intapi.statistic.statutils.OrderInfoYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.OrderStatDayYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.StatTablesUtils;
import ru.yandex.direct.intapi.statistic.statutils.TaxHistoryYTRecord;
import ru.yandex.direct.utils.TimeProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.core.entity.statistics.service.OrderStatService.MILLION;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.grid.schema.yt.Tables.CAESARORDERINFO_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.ORDERSTATDAY_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.TAXHISTORY_BS;
import static ru.yandex.direct.intapi.utils.TablesUtils.generatePrefix;
import static ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL;

@FatIntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetSpentTodayTests {
    private TimeProvider timeProvider = new TimeProvider();

    private static final long BAD_ORDERID = 0L;
    private static final long RUB_ISO_CODE = 643L;
    private static final long EXPECTED_COST_CUR = 1100L;
    private static final long NDS_PERCENT = 20L;
    private static final long TAX_ID = 1L;
    private final Long todayTimestamp =
            timeProvider.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    private final Long yesterdayTimestamp =
            timeProvider.now().toLocalDate().minusDays(1L).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    private final Long tomorrowTimestamp =
            timeProvider.now().toLocalDate().plusDays(1L).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

    private Long orderId;
    private Long secondOrderId;


    @Autowired
    private StatisticController statisticController;

    @Autowired
    private StatTablesUtils statTablesUtils;

    @Autowired
    private Steps steps;


    @Before
    public void before() {
        String prefix = generatePrefix();

        //Подготовим данные
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        orderId = campaignInfo.getCampaign().getOrderId();

        secondOrderId = orderId + 42;
        Campaign campaign2 = activeTextCampaign(null, null).withOrderId(secondOrderId);
        steps.campaignSteps().createCampaign(campaign2);

        TaxHistoryYTRecord taxHistory = new TaxHistoryYTRecord()
                .withTaxID(TAX_ID)
                .withStartDate(yesterdayTimestamp)
                .withPercent(NDS_PERCENT * 10000);
        statTablesUtils.bindTableToTmp(TAXHISTORY_BS, prefix);
        statTablesUtils.createTaxHistoryTable(YT_LOCAL, singleton(taxHistory));

        OrderInfoYTRecord orderInfo = new OrderInfoYTRecord()
                .withOrderID(orderId)
                .withCurrencyID(RUB_ISO_CODE)
                .withTaxID(TAX_ID);
        OrderInfoYTRecord orderInfo2 = new OrderInfoYTRecord()
                .withOrderID(secondOrderId)
                .withCurrencyID(RUB_ISO_CODE)
                .withTaxID(TAX_ID);
        statTablesUtils.bindTableToTmp(CAESARORDERINFO_BS, prefix);
        statTablesUtils.createOrderInfoTable(YT_LOCAL, List.of(orderInfo, orderInfo2));

        List<OrderStatDayYTRecord> stats = asList(
                new OrderStatDayYTRecord().withOrderID(secondOrderId).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withCostCur(null),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(yesterdayTimestamp)
                        .withIsSearch(true).withCostCur(700L),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withCostCur(EXPECTED_COST_CUR),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(tomorrowTimestamp)
                        .withIsSearch(true).withCostCur(300L)
        );
        statTablesUtils.bindTableToTmp(ORDERSTATDAY_BS, prefix);
        statTablesUtils.createOrderStatDayTable(YT_LOCAL, stats);
    }

    @Test
    public void getSpentTodayNoNdsTest() {
        Map<Long, BigDecimal> result = statisticController.getOrdersSpentToday(List.of(orderId), false);
        BigDecimal expectedSpentToday =
                applyRatio(BigDecimal.valueOf(EXPECTED_COST_CUR), CurrencyCode.RUB, NDS_PERCENT);
        assertThat(result).containsEntry(orderId, expectedSpentToday);
    }

    @Test
    public void getSpentTodayWithNdsTest() {
        Map<Long, BigDecimal> result = statisticController.getOrdersSpentToday(List.of(orderId), true);
        BigDecimal expectedSpentToday =
                applyRatio(BigDecimal.valueOf(EXPECTED_COST_CUR), CurrencyCode.RUB);
        assertThat(result).containsEntry(orderId, expectedSpentToday);
    }

    @Test
    public void getSpentTodayNullTest() {
        long unexistedOrderId = orderId + 1;
        Map<Long, BigDecimal> result = statisticController.getOrdersSpentToday(List.of(unexistedOrderId), true);
        assertThat(result).isEmpty();
    }

    @Test
    public void getSpentTodayYtNullValueNdsTest() {
        Map<Long, BigDecimal> result = statisticController.getOrdersSpentToday(List.of(secondOrderId), true);
        assertThat(result).containsEntry(secondOrderId, BigDecimal.valueOf(0, 2));
    }

    @Test
    public void getSpentTodayYtNullValueNoNdsTest() {
        Map<Long, BigDecimal> result = statisticController.getOrdersSpentToday(List.of(secondOrderId), false);
        assertThat(result).containsEntry(secondOrderId, BigDecimal.valueOf(0, 2));
    }

    @Test
    public void getRoughForecastErrorTest() {
        assertThatThrownBy(() -> statisticController.getOrdersSpentToday(List.of(BAD_ORDERID), true))
                .isInstanceOf(IntApiException.class)
                .hasMessageContaining("\"success\":false")
                .hasMessageContaining("\"code\":\"DefectIds.MUST_BE_VALID_ID\"");
    }

    private BigDecimal applyRatio(BigDecimal value, CurrencyCode currencyCode) {
        return Money.valueOf(value, currencyCode)
                .multiply(currencyCode.getCurrency().getYabsRatio())
                .divide(MILLION)
                .roundToCentDown()
                .bigDecimalValue();
    }

    private BigDecimal applyRatio(BigDecimal value, CurrencyCode currencyCode, Long nsdPercent) {
        return Money.valueOf(value, currencyCode)
                .multiply(currencyCode.getCurrency().getYabsRatio())
                .divide(MILLION)
                .roundToCentDown()
                .subtractNds(Percent.fromPercent(BigDecimal.valueOf(nsdPercent)))
                .bigDecimalValue();
    }
}
