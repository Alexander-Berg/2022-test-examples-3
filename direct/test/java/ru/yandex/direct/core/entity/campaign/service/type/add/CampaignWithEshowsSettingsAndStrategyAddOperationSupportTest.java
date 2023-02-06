package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.time.LocalDateTime;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithEshowsSettingsAndStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings;
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAvgCpvCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAvgCpvStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithEshowsSettingsAndStrategyAddOperationSupportTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignWithEshowsSettingsAndStrategyAddOperationSupport support;

    @Autowired
    protected DslContextProvider dslContextProvider;

    private ClientInfo defaultClient;
    private CampaignInfo activeCampaignAutoStrategy;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.CPM_BANNER}
        });
    }

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        activeCampaignAutoStrategy = steps.campaignSteps()
                .createActiveCampaignAutoStrategyByCampaignType(defaultClient, campaignType);
    }

    @Test
    public void successTest() {
        checkVideoType(
                EshowsVideoType.LONG_CLICKS,
                EshowsVideoType.LONG_CLICKS,
                defaultAutobudgetMaxImpressionsCustomPeriodDbStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void successWithAvgCpvTest() {
        checkVideoType(
                null,
                null,
                defaultAvgCpvStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void dropVideoTypeToNullWithAvgCpvStrategyTest() {
        checkVideoType(
                EshowsVideoType.LONG_CLICKS,
                null,
                defaultAvgCpvStrategy(LocalDateTime.now())
        );
    }

    @Test
    public void dropVideoTypeToNullWithAvgCpvCustomPeriodStrategyTest() {
        checkVideoType(
                EshowsVideoType.LONG_CLICKS,
                null,
                defaultAvgCpvCustomPeriodStrategy(LocalDateTime.now())
        );
    }

    public void checkVideoType(
            EshowsVideoType videoType,
            EshowsVideoType expected,
            DbStrategy strategy
    ) {
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                defaultClient.getShard(),
                defaultClient.getClient().getChiefUid(),
                defaultClient.getClientId(),
                defaultClient.getClient().getChiefUid(),
                defaultClient.getClient().getChiefUid());

        CampaignWithEshowsSettingsAndStrategy campaign =
                ((CampaignWithEshowsSettingsAndStrategy) newCampaignByCampaignType(campaignType))
                        .withId(activeCampaignAutoStrategy.getCampaignId())
                        .withStrategy(strategy)
                        .withEshowsSettings((new EshowsSettings()).withVideoType(videoType));

        var campaigns = List.of(campaign);
        support.beforeExecution(addCampaignParametersContainer, campaigns);
        assertThat(
                campaigns.get(0).getEshowsSettings().getVideoType(),
                equalTo(expected)
        );
    }
}
