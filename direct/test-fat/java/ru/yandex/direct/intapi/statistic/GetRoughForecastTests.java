package ru.yandex.direct.intapi.statistic;

import java.math.BigDecimal;
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
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.entity.statistic.StatisticController;
import ru.yandex.direct.intapi.fatconfiguration.FatIntApiTest;
import ru.yandex.direct.intapi.statistic.statutils.OrderInfoYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.OrderStatDayYTRecord;
import ru.yandex.direct.intapi.statistic.statutils.StatTablesUtils;
import ru.yandex.direct.utils.TimeProvider;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.core.entity.statistics.service.OrderStatService.MILLION;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.grid.schema.yt.Tables.CAESARORDERINFO_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.ORDERSTATDAY_BS;
import static ru.yandex.direct.intapi.utils.TablesUtils.generatePrefix;
import static ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL;

@FatIntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetRoughForecastTests {
    private TimeProvider timeProvider = new TimeProvider();

    private static final long BAD_CID = 0L;
    private static final long RUB_ISO_CODE = 643L;
    private static final long EXPECTED_COST_CUR = 5L;
    private static final long SECOND_EXPECTED_COST_CUR = 12L;
    private final Long todayTimestamp = timeProvider.instantNow().getEpochSecond();
    private final Long yesterdayTimestamp = timeProvider.instantNow().minus(1, DAYS).getEpochSecond();
    private final Long twoDaysAgoTimestamp = timeProvider.instantNow().minus(2, DAYS).getEpochSecond();
    private final Long threeDaysAgoTimestamp = timeProvider.instantNow().minus(3, DAYS).getEpochSecond();
    private final Long sevenDaysAgoTimestamp = timeProvider.instantNow().minus(7, DAYS).getEpochSecond();
    private final Long eightDaysAgoTimestamp = timeProvider.instantNow().minus(8, DAYS).getEpochSecond();

    private Long orderId;
    private Long secondOrderID;
    private Long cid;
    private Long secondCid;


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
        cid = campaignInfo.getCampaignId();
        orderId = campaignInfo.getCampaign().getOrderId();

        Campaign campaign2 = activeTextCampaign(null, null).withOrderId(256L);
        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaign(campaign2);
        secondCid = campaignInfo2.getCampaignId();
        secondOrderID = campaignInfo2.getCampaign().getOrderId();

        OrderInfoYTRecord orderInfo = new OrderInfoYTRecord()
                .withOrderID(orderId)
                .withCurrencyID(RUB_ISO_CODE)
                .withTaxID(1);
        OrderInfoYTRecord orderInfo2 = new OrderInfoYTRecord()
                .withOrderID(secondOrderID)
                .withCurrencyID(RUB_ISO_CODE)
                .withTaxID(1);
        statTablesUtils.bindTableToTmp(CAESARORDERINFO_BS, prefix);
        statTablesUtils.createOrderInfoTable(YT_LOCAL, List.of(orderInfo, orderInfo2));

        List<OrderStatDayYTRecord> stats = asList(
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(eightDaysAgoTimestamp)
                        .withIsSearch(true).withCostCur(13L),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(sevenDaysAgoTimestamp)
                        .withIsSearch(true).withCostCur(3L),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(twoDaysAgoTimestamp)
                        .withIsSearch(true).withCostCur(EXPECTED_COST_CUR),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(yesterdayTimestamp)
                        .withIsSearch(true).withCostCur(7L),
                new OrderStatDayYTRecord().withOrderID(orderId).withUpdateTime(todayTimestamp)
                        .withIsSearch(true).withCostCur(11L),
                new OrderStatDayYTRecord().withOrderID(secondOrderID).withUpdateTime(threeDaysAgoTimestamp)
                        .withIsSearch(false).withCostCur(SECOND_EXPECTED_COST_CUR),
                new OrderStatDayYTRecord().withOrderID(secondOrderID).withUpdateTime(twoDaysAgoTimestamp)
                        .withIsSearch(true).withCostCur(null)
        );
        statTablesUtils.bindTableToTmp(ORDERSTATDAY_BS, prefix);
        statTablesUtils.createOrderStatDayTable(YT_LOCAL, stats);
    }

    @Test
    public void getRoughForecastTest() {
        Map<Long, BigDecimal> result = statisticController.getRoughForecast(List.of(cid), CurrencyCode.RUB);
        BigDecimal expectedForecastValue =
                applyRatio(BigDecimal.valueOf(EXPECTED_COST_CUR), CurrencyCode.RUB);
        assertThat(result).containsEntry(cid, expectedForecastValue);
    }

    @Test
    public void getRoughForecastDuplicateRequestTest() {
        Map<Long, BigDecimal> result = statisticController.getRoughForecast(List.of(cid, cid), CurrencyCode.RUB);
        BigDecimal expectedForecastValue =
                applyRatio(BigDecimal.valueOf(EXPECTED_COST_CUR), CurrencyCode.RUB);
        assertThat(result).containsEntry(cid, expectedForecastValue);
    }

    @Test
    public void getRoughForecastYtResponseWithNullsTest() {
        Map<Long, BigDecimal> result = statisticController.getRoughForecast(List.of(secondCid), CurrencyCode.RUB);
        BigDecimal expected = applyRatio(BigDecimal.valueOf(SECOND_EXPECTED_COST_CUR), CurrencyCode.RUB);
        assertThat(result).containsEntry(secondCid, expected);
    }

    @Test
    public void getRoughForecastTwoCampaignsTest() {
        Map<Long, BigDecimal> result = statisticController.getRoughForecast(List.of(cid, secondCid), CurrencyCode.RUB);
        BigDecimal expected1 = applyRatio(BigDecimal.valueOf(EXPECTED_COST_CUR), CurrencyCode.RUB);
        BigDecimal expected2 = applyRatio(BigDecimal.valueOf(SECOND_EXPECTED_COST_CUR), CurrencyCode.RUB);
        assertThat(result).containsExactly(Map.entry(cid, expected1), Map.entry(secondCid, expected2));
    }

    @Test
    public void getRoughForecastNullTest() {
        long unexistedCid = cid + 999999;
        Map<Long, BigDecimal> result = statisticController.getRoughForecast(List.of(unexistedCid), CurrencyCode.RUB);
        BigDecimal expectedForecastValue = applyRatio(BigDecimal.ZERO, CurrencyCode.RUB);
        assertThat(result).containsEntry(unexistedCid, expectedForecastValue);
    }

    @Test
    public void getRoughForecastWrongCurrencyTest() {
        assertThatThrownBy(() -> statisticController.getRoughForecast(List.of(cid), CurrencyCode.USD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("wrong currency");
    }

    @Test
    public void getRoughForecastErrorTest() {
        assertThatThrownBy(() -> statisticController.getRoughForecast(List.of(BAD_CID), CurrencyCode.RUB))
                .isInstanceOf(IntApiException.class)
                .hasMessageContaining("\"success\":false")
                .hasMessageContaining("\"code\":\"DefectIds.MUST_BE_VALID_ID\"");
    }

    private BigDecimal applyRatio(BigDecimal value, CurrencyCode currencyCode) {
        return Money.valueOf(value, currencyCode)
                .multiply(currencyCode.getCurrency().getYabsRatio())
                .divide(MILLION).bigDecimalValue();
    }
}
