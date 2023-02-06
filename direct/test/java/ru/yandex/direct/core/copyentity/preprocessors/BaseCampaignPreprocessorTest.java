package ru.yandex.direct.core.copyentity.preprocessors;

import java.math.BigDecimal;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.core.copyentity.CopyOperationContainer;
import ru.yandex.direct.core.copyentity.preprocessors.campaign.CampaignWithStrategyCopyPreprocessor;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.currency.service.CurrencyConverterFactory;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.Currencies;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.copyentity.CopyEntityTestUtils.defaultBetweenShardsCopyContainer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.currency.CurrencyCode.USD;

@CoreTest
@ParametersAreNonnullByDefault
public class BaseCampaignPreprocessorTest {
    private static final Currency RUB_CURRENCY = Currencies.getCurrency(RUB);

    private CopyOperationContainer copyOperationContainer;
    private CampaignWithStrategyCopyPreprocessor preprocessor;
    private TextCampaign campaign;

    @Before
    public void setUp() {
        CurrencyRateService currencyRateService = mock(CurrencyRateService.class);
        var currencyConverterFactory = new CurrencyConverterFactory(currencyRateService);

        when(currencyRateService.convertMoney(any(Money.class), any(CurrencyCode.class)))
                .thenAnswer(this::fakeConvertMoney);

        copyOperationContainer = defaultBetweenShardsCopyContainer(USD, RUB);
        preprocessor = new CampaignWithStrategyCopyPreprocessor(currencyConverterFactory);
        campaign = campaign().withCurrency(USD);
    }

    private Money fakeConvertMoney(InvocationOnMock i) {
        Money money = i.getArgument(0);
        return Money.valueOf(
                money.bigDecimalValue().multiply(BigDecimal.TEN),
                RUB);
    }

    @Test
    public void copyChangingCurrency_strategyAvgBidIsOkay_isConverted() {
        var avgBid = BigDecimal.valueOf(30);
        campaign.getStrategy().getStrategyData().setAvgBid(avgBid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgBid()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    public void copyChangingCurrency_strategyAvgBidIsLessThanMin_isNotLessThanMinAfterConverting() {
        var avgBid = BigDecimal.valueOf(0.029);
        campaign.getStrategy().getStrategyData().setAvgBid(avgBid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMinAutobudgetAvgPrice());
    }

    @Test
    public void copySmartChangingCurrency_strategyAvgBidIsLessThanMin_isNotLessThanMinAfterConverting() {
        var avgBid = BigDecimal.valueOf(0.0089);
        campaign.getStrategy().getStrategyData().setAvgBid(avgBid);
        campaign.setType(CampaignType.PERFORMANCE);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMinCpcCpaPerformance());
    }

    @Test
    public void copyChangingCurrency_strategyAvgBidIsMoreThanMax_isNotMoreThanMaxAfterConverting() {
        var avgBid = BigDecimal.valueOf(2_501);
        campaign.getStrategy().getStrategyData().setAvgBid(avgBid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMaxAutobudgetBid());
    }

    @Test
    public void copyChangingCurrency_strategyAvgCpaIsOkay_isConverted() {
        var avgCpa = BigDecimal.valueOf(30);
        campaign.getStrategy().getStrategyData().setAvgCpa(avgCpa);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpa()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    public void copyChangingCurrency_strategyAvgCpaIsLessThanMin_isNotLessThanMinAfterConverting() {
        var avgCpa = BigDecimal.valueOf(0.089);
        campaign.getStrategy().getStrategyData().setAvgCpa(avgCpa);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpa())
                .isEqualByComparingTo(RUB_CURRENCY.getMinAutobudgetAvgCpa());
    }

    @Test
    public void copySmartChangingCurrency_strategyAvgCpaIsLessThanMin_isNotLessThanMinAfterConverting() {
        var avgCpa = BigDecimal.valueOf(0.029);
        campaign.getStrategy().getStrategyData().setAvgCpa(avgCpa);
        campaign.setType(CampaignType.PERFORMANCE);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpa())
                .isEqualByComparingTo(RUB_CURRENCY.getMinCpcCpaPerformance());
    }

    @Test
    public void copyChangingCurrency_strategyAvgCpiIsOkay_isConverted() {
        var avgCpi = BigDecimal.valueOf(30);
        campaign.getStrategy().getStrategyData().setAvgCpi(avgCpi);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpi()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    public void copyChangingCurrency_strategyAvgCpiIsLessThanMin_isNotLessThanMinAfterConverting() {
        var avgCpi = BigDecimal.valueOf(0.089);
        campaign.getStrategy().getStrategyData().setAvgCpi(avgCpi);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpi())
                .isEqualByComparingTo(RUB_CURRENCY.getMinAutobudgetAvgCpa());
    }

    @Test
    public void copyChangingCurrency_strategyAvgCpmIsOkay_isConverted() {
        var avgCpm = BigDecimal.valueOf(30);
        campaign.getStrategy().getStrategyData().setAvgCpm(avgCpm);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpm()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    public void copyChangingCurrency_strategyAvgCpmIsLessThanMin_isNotLessThanMinAfterConverting() {
        var avgCpm = BigDecimal.valueOf(0.49);
        campaign.getStrategy().getStrategyData().setAvgCpm(avgCpm);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpm())
                .isEqualByComparingTo(RUB_CURRENCY.getMinAutobudgetAvgCpm());
    }

    @Test
    public void copyChangingCurrency_strategyAvgCpmIsMoreThanMax_isNotMoreThanMaxAfterConverting() {
        var avgCpm = BigDecimal.valueOf(301);
        campaign.getStrategy().getStrategyData().setAvgCpm(avgCpm);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getAvgCpm())
                .isEqualByComparingTo(RUB_CURRENCY.getMaxCpmPrice());
    }

    @Test
    public void copyChangingCurrency_strategyFilterAvgBidIsOkay_isConverted() {
        var filterAvgBid = BigDecimal.valueOf(30);
        campaign.getStrategy().getStrategyData().setFilterAvgBid(filterAvgBid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getFilterAvgBid())
                .isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    public void copyChangingCurrency_strategyFilterAvgBidIsLessThanMin_isNotLessThanMinAfterConverting() {
        var filterAvgBid = BigDecimal.valueOf(0.029);
        campaign.getStrategy().getStrategyData().setFilterAvgBid(filterAvgBid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getFilterAvgBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMinCpcCpaPerformance());
    }

    @Test
    public void copyChangingCurrency_strategyFilterAvgBidIsMoreThanMax_isNotMoreThanMaxAfterConverting() {
        var filterAvgBid = BigDecimal.valueOf(2_501);
        campaign.getStrategy().getStrategyData().setFilterAvgBid(filterAvgBid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getFilterAvgBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMaxAutobudgetBid());
    }

    @Test
    public void copyChangingCurrency_strategyFilterAvgCpaIsOkay_isConverted() {
        var filterAvgCpa = BigDecimal.valueOf(30);
        campaign.getStrategy().getStrategyData().setFilterAvgCpa(filterAvgCpa);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getFilterAvgCpa())
                .isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    public void copyChangingCurrency_strategyFilterAvgCpaIsLessThanMin_isNotLessThanMinAfterConverting() {
        var filterAvgCpa = BigDecimal.valueOf(0.0089);
        campaign.getStrategy().getStrategyData().setFilterAvgCpa(filterAvgCpa);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getFilterAvgCpa())
                .isEqualByComparingTo(RUB_CURRENCY.getMinCpcCpaPerformance());
    }

    @Test
    public void copyChangingCurrency_strategyBidIsOkay_isConverted() {
        var bid = BigDecimal.valueOf(30);
        campaign.getStrategy().getStrategyData().setBid(bid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBid())
                .isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    public void copyChangingCurrency_strategyBidIsLessThanMin_isNotLessThanMinAfterConverting() {
        var bid = BigDecimal.valueOf(0.029);
        campaign.getStrategy().getStrategyData().setBid(bid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMinAutobudgetBid());
    }

    @Test
    public void copyChangingCurrency_strategyBidIsMoreThanMax_isNotMoreThanMaxAfterConverting() {
        var bid = BigDecimal.valueOf(2_501);
        campaign.getStrategy().getStrategyData().setBid(bid);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMaxAutobudgetBid());
    }

    @Test
    public void copySmartChangingCurrency_strategyBidIsLessThanMin_isNotLessThanMinAfterConverting() {
        var bid = BigDecimal.valueOf(0.029);
        campaign.getStrategy().getStrategyData().setBid(bid);
        campaign.setType(CampaignType.PERFORMANCE);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMinCpcCpaPerformance());
    }

    @Test
    public void copyAvgCpiChangingCurrency_strategyBidIsLessThanMin_isNotLessThanMinAfterConverting() {
        var bid = BigDecimal.valueOf(0.089);
        campaign.getStrategy().getStrategyData().setBid(bid);
        campaign.getStrategy().getStrategyData().setAvgCpi(BigDecimal.ONE);
        campaign.getStrategy().getStrategyData().setAvgCpa(BigDecimal.ONE);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBid())
                .isEqualByComparingTo(RUB_CURRENCY.getMinAutobudgetAvgCpa());
    }

    @Test
    public void copyChangingCurrency_strategySumIsOkay_isConverted() {
        var sum = BigDecimal.valueOf(300);
        campaign.getStrategy().getStrategyData().setSum(sum);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getSum())
                .isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    public void copyChangingCurrency_strategySumIsLessThanMin_isNotLessThanMinAfterConverting() {
        var sum = BigDecimal.valueOf(29);
        campaign.getStrategy().getStrategyData().setSum(sum);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getSum())
                .isEqualByComparingTo(RUB_CURRENCY.getMinAutobudget());
    }

    @Test
    public void copyChangingCurrency_strategySumIsMoreThanMax_isNotMoreThanMaxAfterConverting() {
        var sum = BigDecimal.valueOf(30000001);
        campaign.getStrategy().getStrategyData().setSum(sum);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getSum())
                .isEqualByComparingTo(RUB_CURRENCY.getMaxAutobudget());
    }

    @Test
    public void copyChangingCurrency_strategyBudgetIsOkay_isConverted() {
        var budget = BigDecimal.valueOf(300);
        campaign.getStrategy().getStrategyData().setBudget(budget);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBudget())
                .isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    public void copyChangingCurrency_strategyBudgetIsLessThanMin_isNotLessThanMinAfterConverting() {
        var budget = BigDecimal.valueOf(29);
        campaign.getStrategy().getStrategyData().setBudget(budget);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBudget())
                .isEqualByComparingTo(RUB_CURRENCY.getMinDailyBudgetForPeriod());
    }

    @Test
    public void copyChangingCurrency_strategyBudgetIsMoreThanMax_isNotMoreThanMaxAfterConverting() {
        var budget = BigDecimal.valueOf(10000001);
        campaign.getStrategy().getStrategyData().setBudget(budget);

        preprocessor.preprocess(campaign, copyOperationContainer);

        assertThat(campaign.getStrategy().getStrategyData().getBudget())
                .isEqualByComparingTo(RUB_CURRENCY.getMaxDailyBudgetForPeriod());
    }

    private static TextCampaign campaign() {
        return defaultTextCampaignWithSystemFields();
    }
}
