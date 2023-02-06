package ru.yandex.direct.intapi.statistic;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.statistics.model.Period;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.statistic.StatisticController;
import ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersSumSpentRequest;
import ru.yandex.direct.intapi.fatconfiguration.FatIntApiTest;
import ru.yandex.direct.intapi.statistic.statutils.OrderInfoYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.OrderStatDayYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.StatTablesUtils;
import ru.yandex.direct.intapi.statistic.statutils.TaxHistoryYTRecord;
import ru.yandex.direct.utils.TimeProvider;

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
public class GetSumSpentTests {
    private TimeProvider timeProvider = new TimeProvider();

    private static final long BAD_ORDER_ID = 0L;
    private static final long RUB_ISO_CODE = 643L;
    private static final long YESTERDAY_COST_CUR = 700L;
    private static final long TODAY_COST_CUR = 1100L;
    private static final long NDS_PERCENT = 20L;
    private static final long TAX_ID = 1L;
    private final Long todayTimestamp =
            timeProvider.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    private final Long yesterdayTimestamp =
            timeProvider.now().toLocalDate().minusDays(1L).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
    private final Long twoDaysAgoTimestamp =
            timeProvider.now().toLocalDate().plusDays(2L).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
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

        List<OrderStatDayYTRecord> stats = List.of(
                new OrderStatDayYTRecord().withOrderID(secondOrderId).withUpdateTime(yesterdayTimestamp)
                        .withIsSearch(true).withCostCur(null),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(twoDaysAgoTimestamp)
                        .withIsSearch(true).withCostCur(300L),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(yesterdayTimestamp)
                        .withIsSearch(true).withCostCur(YESTERDAY_COST_CUR),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withCostCur(TODAY_COST_CUR)
        );
        statTablesUtils.bindTableToTmp(ORDERSTATDAY_BS, prefix);
        statTablesUtils.createOrderStatDayTable(YT_LOCAL, stats);
    }

    @Test
    public void getSumSpentTodayTest() {
        Period period = new Period("getSumSpentTodayTest", LocalDate.now(), LocalDate.now());
        GetOrdersSumSpentRequest request = new GetOrdersSumSpentRequest(List.of(orderId), List.of(period));
        Map<String, BigDecimal> result = statisticController.getOrdersSumSpent(request, CurrencyCode.RUB);
        BigDecimal expectedSpentToday =
                applyRatio(BigDecimal.valueOf(TODAY_COST_CUR), CurrencyCode.RUB);
        assertThat(result).containsEntry("getSumSpentTodayTest", expectedSpentToday);
    }

    @Test
    public void getSumSpentTwoDaysTest() {
        Period period = new Period("getSumSpentTwoDaysTest", LocalDate.now().minusDays(1L), LocalDate.now());
        GetOrdersSumSpentRequest request = new GetOrdersSumSpentRequest(List.of(orderId), List.of(period));
        Map<String, BigDecimal> result = statisticController.getOrdersSumSpent(request, CurrencyCode.RUB);
        BigDecimal expectedSpentToday =
                applyRatio(BigDecimal.valueOf(TODAY_COST_CUR + YESTERDAY_COST_CUR), CurrencyCode.RUB);
        assertThat(result).containsEntry("getSumSpentTwoDaysTest", expectedSpentToday);
    }

    @Test
    public void getSumSpentNullYtValueTest() {
        Period period = new Period("getSumSpentNullYtValueTest", LocalDate.now().minusMonths(1L), LocalDate.now());
        GetOrdersSumSpentRequest request = new GetOrdersSumSpentRequest(List.of(secondOrderId), List.of(period));
        Map<String, BigDecimal> result = statisticController.getOrdersSumSpent(request, CurrencyCode.RUB);

        BigDecimal expected = applyRatio(BigDecimal.ZERO, CurrencyCode.RUB);
        assertThat(result).containsEntry("getSumSpentNullYtValueTest", expected);
    }

    @Test
    public void getSumSpentWrongCurrencyTest() {
        Period period = new Period("getSumSpentNullTest", LocalDate.now(), LocalDate.now());
        GetOrdersSumSpentRequest request = new GetOrdersSumSpentRequest(List.of(orderId), List.of(period));
        Map<String, BigDecimal> result = statisticController.getOrdersSumSpent(request, CurrencyCode.USD);
        BigDecimal expectedSpentToday =
                applyRatio(BigDecimal.valueOf(TODAY_COST_CUR), CurrencyCode.USD);
        assertThat(result).containsEntry("getSumSpentNullTest", expectedSpentToday);
    }

    @Test
    public void getSumSpentNullTest() {
        long unexistedOrderId = orderId + 1;
        Period period = new Period("getSumSpentNullTest", LocalDate.now(), LocalDate.now());
        GetOrdersSumSpentRequest request = new GetOrdersSumSpentRequest(List.of(unexistedOrderId), List.of(period));
        Map<String, BigDecimal> result = statisticController.getOrdersSumSpent(request, CurrencyCode.RUB);
        BigDecimal expectedSpentToday =
                applyRatio(BigDecimal.ZERO, CurrencyCode.RUB);
        assertThat(result).containsEntry("getSumSpentNullTest", expectedSpentToday);
    }

    @Test
    public void getSumSpentErrorTest() {
        Period period = new Period("getSumSpentNullTest", LocalDate.now(), LocalDate.now());
        GetOrdersSumSpentRequest request = new GetOrdersSumSpentRequest(List.of(BAD_ORDER_ID), List.of(period));
        assertThatThrownBy(() -> statisticController.getOrdersSumSpent(request, CurrencyCode.RUB))
                .isInstanceOf(IntApiException.class)
                .hasMessageContaining("\"success\":false")
                .hasMessageContaining("\"code\":\"DefectIds.MUST_BE_VALID_ID\"");
    }

    private BigDecimal applyRatio(BigDecimal value, CurrencyCode currencyCode) {
        return Money.valueOf(value, currencyCode)
                .multiply(currencyCode.getCurrency().getYabsRatio())
                .divide(MILLION)
                .bigDecimalValue();
    }
}
