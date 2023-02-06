package ru.yandex.direct.core.copyentity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.BaseCampaignService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationCampaignBetweenClientsStrategyCurrencyConversionTest {
    public static final CurrencyCode TARGET_CURRENCY = CurrencyCode.USD;
    public static final BigDecimal RATE = BigDecimal.valueOf(60);
    public static final CurrencyCode SOURCE_CURRENCY = CurrencyCode.RUB;

    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private BaseCampaignService baseCampaignService;

    @Autowired
    private CopyOperationFactory copyOperationFactory;

    private Long uid;
    private ClientId clientId;
    private ClientId clientIdTo;

    private CampaignInfo campaignInfo;
    private CopyOperation xerox;

    private CopyResult copyResult;

    private Set<Long> newCampaignIds;

    @Before
    public void setUp() {
        steps.currencySteps().createCurrencyRate(TARGET_CURRENCY, LocalDate.now(), RATE);

        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(SOURCE_CURRENCY));
        clientId = clientInfo.getClientId();
        steps.featureSteps().setCurrentClient(clientId);

        ClientInfo clientInfoTo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(TARGET_CURRENCY));
        clientIdTo = clientInfoTo.getClientId();

        campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru")
                        .withStartTime(LocalDate.now().plusDays(1L)),
                clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        asserts.init(clientId, clientIdTo, uid);

        xerox = copyOperationFactory.build(clientInfo.getShard(), clientInfo.getClient(),
                clientInfoTo.getShard(), clientInfoTo.getClient(),
                uid,
                BaseCampaign.class, List.of(campaignId),
                new CopyCampaignFlags());
    }

    @Test
    public void campaignIsCopied() {
        copyResult = xerox.copy();

        asserts.checkErrors(copyResult);
        newCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());

        assertThat(newCampaignIds).hasSize(1);
    }

    @Test
    public void campaignCurrencyIsChanged() {
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getCurrency()).isEqualTo(TARGET_CURRENCY);
    }

    @Test
    public void campaignDayBudgetIsConverted() {
        var dayBudgetRub = BigDecimal.valueOf(6000);

        steps.campaignSteps().setDayBudget(campaignInfo, dayBudgetRub, null, null);
        TextCampaign newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getDayBudget()).isEqualTo(getExpectedConvertedValue(dayBudgetRub, 2));
    }

    @Test
    public void strategyAvgBidIsConverted() {
        var avgBid = BigDecimal.valueOf(3000);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withAvgBid(avgBid));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getAvgBid())
                .isEqualTo(getExpectedConvertedValue(avgBid));
    }

    @Test
    public void strategyAvgCpaIsConverted() {
        var avgCpa = BigDecimal.valueOf(3000);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withAvgCpa(avgCpa));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getAvgCpa())
                .isEqualTo(getExpectedConvertedValue(avgCpa));
    }

    @Test
    public void strategyAvgCpiIsConverted() {
        var avgCpi = BigDecimal.valueOf(3000);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withAvgCpi(avgCpi));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getAvgCpi())
                .isEqualTo(getExpectedConvertedValue(avgCpi));
    }

    @Test
    public void strategyAvgCpmIsConverted() {
        var avgCpm = BigDecimal.valueOf(3000);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withAvgCpm(avgCpm));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getAvgCpm())
                .isEqualTo(getExpectedConvertedValue(avgCpm));
    }

    @Test
    public void strategyFilterAvgBidIsConverted() {
        var filterAvgBid = BigDecimal.valueOf(9000);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withFilterAvgBid(filterAvgBid));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getFilterAvgBid())
                .isEqualTo(getExpectedConvertedValue(filterAvgBid));
    }

    @Test
    public void strategyFilterAvgCpaIsConverted() {
        var filterAvgCpa = BigDecimal.valueOf(9000);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withFilterAvgCpa(filterAvgCpa));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getFilterAvgCpa())
                .isEqualTo(getExpectedConvertedValue(filterAvgCpa));
    }

    @Test
    public void strategyBidIsConverted() {
        var bid = BigDecimal.valueOf(4500);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withBid(bid));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getBid())
                .isEqualTo(getExpectedConvertedValue(bid));
    }

    @Test
    public void strategySumIsConverted() {
        var sum = BigDecimal.valueOf(4500);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withSum(sum));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getSum())
                .isEqualTo(getExpectedConvertedValue(sum));
    }

    @Test
    public void strategyBudgetIsConverted() {
        var budget = BigDecimal.valueOf(4500);

        var sourceCampaign = getSourceCampaign();
        steps.campaignSteps().setStrategyData(campaignInfo,
                sourceCampaign.getStrategy()
                        .getStrategyData()
                        .withBudget(budget));
        var newCampaign = copyAndGetNewCampaign();

        assertThat(newCampaign.getStrategy().getStrategyData().getBudget())
                .isEqualTo(getExpectedConvertedValue(budget));
    }

    private TextCampaign getSourceCampaign() {
        return (TextCampaign) baseCampaignService.get(clientId, uid, Set.of(campaignInfo.getCampaignId())).get(0);
    }

    private TextCampaign copyAndGetNewCampaign() {
        copyResult = xerox.copy();

        asserts.checkErrors(copyResult);
        newCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        return (TextCampaign) baseCampaignService.get(clientIdTo, uid, newCampaignIds).get(0);
    }

    @NotNull
    private BigDecimal getExpectedConvertedValue(BigDecimal dayBudgetRub, int scale) {
        return dayBudgetRub.divide(RATE, scale, RoundingMode.HALF_UP);
    }

    @NotNull
    private BigDecimal getExpectedConvertedValue(BigDecimal dayBudgetRub) {
        return getExpectedConvertedValue(dayBudgetRub, 0);
    }
}
