package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddRetargetingsOperationAutoPricesTest {

    @Autowired
    private Steps steps;
    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private RetargetingRepository retargetingRepository;
    @Autowired
    ClientService clientService;

    private ClientInfo clientInfo;
    private Currency clientCurrency;
    private Long retCondId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        retCondId = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null).withType(ConditionType.interests),
                        clientInfo
                ).getRetConditionId();

        clientCurrency = clientService.getWorkCurrency(clientInfo.getClientId());
    }

    @Test
    public void cpmBannerAdGroupInAutobudgetCampaign() {
        CampaignInfo autobudgetCampaign = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(autobudgetCampaign);

        Retargeting retargeting = addDefaultRetargeting(adGroup);
        assertThat("цена в сетях корректная",
                moneyOf(retargeting.getPriceContext().doubleValue()),
                is(Money.valueOf(clientCurrency.getMinCpmPrice(), clientCurrency.getCode())));
        assertThat("приоритет автобюджета не должен выставляться", retargeting.getAutobudgetPriority(),
                nullValue());
    }

    @Test
    public void cpmVideoAdGroupInAutobudgetCampaign() {
        CampaignInfo autobudgetCampaign = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmVideoAdGroup(autobudgetCampaign);

        Retargeting retargeting = addDefaultRetargeting(adGroup);
        assertThat("цена в сетях корректная",
                moneyOf(retargeting.getPriceContext().doubleValue()),
                is(Money.valueOf(clientCurrency.getMinCpmPrice(), clientCurrency.getCode())));
        assertThat("приоритет автобюджета не должен выставляться", retargeting.getAutobudgetPriority(),
                nullValue());
    }

    @Test
    public void cpmBannerAdGroupInManualStrategyCampaign() {
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);

        double commonPrice = 100;
        Retargeting retargeting = addDefaultRetargeting(adGroup, BigDecimal.valueOf(commonPrice));
        assertThat("цена в сетях корректная", moneyOf(retargeting.getPriceContext().doubleValue()),
                equalTo(moneyOf(commonPrice)));
        assertThat("приоритет автобюджета не должен выставляться", retargeting.getAutobudgetPriority(),
                nullValue());
    }

    @Test
    public void cpmVideoAdGroupInManualStrategyCampaign() {
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo);

        double commonPrice = 100;
        Retargeting retargeting = addDefaultRetargeting(adGroup, BigDecimal.valueOf(commonPrice));
        assertThat("цена в сетях корректная", moneyOf(retargeting.getPriceContext().doubleValue()),
                equalTo(moneyOf(commonPrice)));
        assertThat("приоритет автобюджета не должен выставляться", retargeting.getAutobudgetPriority(),
                nullValue());
    }

    private Retargeting addDefaultRetargeting(AdGroupInfo adGroupInfo, BigDecimal priceContext) {
        List<TargetInterest> targetInterests = singletonList(defaultTargetInterest(adGroupInfo));

        AddRetargetingsOperation addOperation = retargetingService
                .createAddOperation(Applicability.PARTIAL, false, false, targetInterests, true,
                        ShowConditionFixedAutoPrices.ofGlobalFixedPrice(priceContext), clientInfo.getUid(),
                        clientInfo.getClientId(), clientInfo.getUid());
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        List<Retargeting> actualRetargetings = retargetingRepository
                .getRetargetingsByAdGroups(clientInfo.getShard(), singletonList(adGroupInfo.getAdGroupId()));
        assertThat("в группе один ретаргетинг", actualRetargetings, hasSize(1));

        return actualRetargetings.get(0);
    }

    private Retargeting addDefaultRetargeting(AdGroupInfo adGroupInfo) {
        return addDefaultRetargeting(adGroupInfo, null);
    }

    private TargetInterest defaultTargetInterest(AdGroupInfo adGroupInfo) {
        return new TargetInterest()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withRetargetingConditionId(retCondId);
    }

    private Money moneyOf(double price) {
        return Money.valueOf(price, clientCurrency.getCode());
    }
}
