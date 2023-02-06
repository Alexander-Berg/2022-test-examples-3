package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;
import static ru.yandex.direct.dbschema.ppc.Tables.AUTOBUDGET_FORECAST;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomStrategyAddOperationSupportTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignWithCustomStrategyAddOperationSupport support;

    @Autowired
    protected DslContextProvider dslContextProvider;

    private ClientInfo defaultClient;
    private CampaignInfo activeCampaignAutoStrategy;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        activeCampaignAutoStrategy = steps.campaignSteps()
                .createActiveCampaignAutoStrategyByCampaignType(defaultClient, campaignType);
    }

    @Test
    public void test() {
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                defaultClient.getShard(),
                defaultClient.getClient().getChiefUid(),
                defaultClient.getClientId(),
                defaultClient.getClient().getChiefUid(),
                defaultClient.getClient().getChiefUid());

        CampaignWithCustomStrategy campaignWithCustomStrategy =
                ((CampaignWithCustomStrategy) newCampaignByCampaignType(campaignType))
                        .withId(activeCampaignAutoStrategy.getCampaignId())
                        .withStrategy(defaultAutobudgetMaxImpressionsDbStrategy());

        support.afterExecution(addCampaignParametersContainer, List.of(campaignWithCustomStrategy));

        checkAutobudgetForecastWasInsertIfAutobudgetEnabled(activeCampaignAutoStrategy);
    }

    private void checkAutobudgetForecastWasInsertIfAutobudgetEnabled(CampaignInfo campaignInfo) {
        int count = dslContextProvider.ppc(campaignInfo.getShard())
                .selectCount()
                .from(AUTOBUDGET_FORECAST)
                .where(AUTOBUDGET_FORECAST.CID.eq(campaignInfo.getCampaignId()))
                .fetchOne()
                .value1();
        assertThat("autobudget_forecast должен был дополниться", count, greaterThan(0));
    }
}
