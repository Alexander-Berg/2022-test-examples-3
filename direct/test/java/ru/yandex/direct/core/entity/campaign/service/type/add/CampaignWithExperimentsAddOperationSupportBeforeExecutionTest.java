package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignExperiment;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithExperiments;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.ExperimentRetargetingConditions;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(Parameterized.class)
public class CampaignWithExperimentsAddOperationSupportBeforeExecutionTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private RetargetingConditionService retargetingConditionService;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private CampaignWithExperimentsAddOperationSupport operationSupport;

    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    private static ClientId clientId;
    private static Long uid;
    private static Long campaignId;
    private static CampaignWithExperiments campaign;
    private static int shard;
    private static RestrictedCampaignsAddOperationContainer addParameters;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.DYNAMIC},
                {CampaignType.MCBANNER},
                {CampaignType.PERFORMANCE}
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        shard = 1;
        campaignId = RandomNumberUtils.nextPositiveLong();
        campaign = createCampaign();

        addParameters = new RestrictedCampaignsAddOperationContainerImpl(
                shard,
                uid,
                clientId,
                uid,
                uid,
                null,
                new CampaignOptions(),
                metrikaClientAdapter,
                emptyMap());
    }

    @Test
    public void beforeExecution_onlySectionIds_featureEnabled() {
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA);

        List<Long> sectionIds = List.of(RandomNumberUtils.nextPositiveLong());
        List<Long> abSegmentIds = null;
        Long abStatisticRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long abRetargetingConditionId = null;

        mockRetargetingConditionService(sectionIds, abSegmentIds, abStatisticRetargetingConditionId,
                abRetargetingConditionId);

        campaign
                .withAbSegmentRetargetingConditionId(RandomNumberUtils.nextPositiveLong())
                .withAbSegmentStatisticRetargetingConditionId(RandomNumberUtils.nextPositiveLong())
                .withSectionIds(sectionIds);

        operationSupport.beforeExecution(addParameters, List.of(campaign));

        assertThat(campaign.getAbSegmentStatisticRetargetingConditionId()).isEqualTo(abStatisticRetargetingConditionId);
        assertThat(campaign.getAbSegmentRetargetingConditionId()).isEqualTo(abRetargetingConditionId);
    }

    @Test
    public void beforeExecution_onlySectionIds_featureDisabled() {
        doReturn(false).when(featureService).isEnabledForClientId(clientId,
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA);

        List<Long> sectionIds = List.of(RandomNumberUtils.nextPositiveLong());
        List<Long> abSegmentIds = null;

        Long oldAbStatisticRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long newAbStatisticRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long oldAbRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long newAbRetargetingConditionId = null;

        mockRetargetingConditionService(sectionIds, abSegmentIds, newAbStatisticRetargetingConditionId,
                newAbRetargetingConditionId);

        campaign
                .withAbSegmentRetargetingConditionId(oldAbRetargetingConditionId)
                .withAbSegmentStatisticRetargetingConditionId(oldAbStatisticRetargetingConditionId)
                .withSectionIds(sectionIds);

        operationSupport.beforeExecution(addParameters, List.of(campaign));

        assertThat(campaign.getAbSegmentStatisticRetargetingConditionId()).isEqualTo(oldAbStatisticRetargetingConditionId);
        assertThat(campaign.getAbSegmentRetargetingConditionId()).isEqualTo(oldAbRetargetingConditionId);
    }

    @Test
    public void beforeExecution_sectionIdsAndAbSegmentIds_featureEnabled() {
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA);

        List<Long> sectionIds = List.of(RandomNumberUtils.nextPositiveLong());
        List<Long> abSegmentIds = List.of(RandomNumberUtils.nextPositiveLong());

        Long abStatisticRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long abRetargetingConditionId = RandomNumberUtils.nextPositiveLong();

        mockRetargetingConditionService(sectionIds, abSegmentIds, abStatisticRetargetingConditionId,
                abRetargetingConditionId);

        campaign
                .withAbSegmentRetargetingConditionId(RandomNumberUtils.nextPositiveLong())
                .withAbSegmentStatisticRetargetingConditionId(RandomNumberUtils.nextPositiveLong())
                .withSectionIds(sectionIds)
                .withAbSegmentGoalIds(abSegmentIds);

        operationSupport.beforeExecution(addParameters, List.of(campaign));

        assertThat(campaign.getAbSegmentStatisticRetargetingConditionId()).isEqualTo(abStatisticRetargetingConditionId);
        assertThat(campaign.getAbSegmentRetargetingConditionId()).isEqualTo(abRetargetingConditionId);
    }

    @Test
    public void beforeExecution_sectionIdsAndAbSegmentIds_featureDisabled() {
        doReturn(false).when(featureService).isEnabledForClientId(clientId,
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA);

        List<Long> sectionIds = List.of(RandomNumberUtils.nextPositiveLong());
        List<Long> abSegmentIds = List.of(RandomNumberUtils.nextPositiveLong());

        Long oldAbStatisticRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long newAbStatisticRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long oldAbRetargetingConditionId = RandomNumberUtils.nextPositiveLong();
        Long newAbRetargetingConditionId = RandomNumberUtils.nextPositiveLong();

        mockRetargetingConditionService(sectionIds, abSegmentIds, newAbStatisticRetargetingConditionId,
                newAbRetargetingConditionId);

        campaign
                .withAbSegmentRetargetingConditionId(oldAbRetargetingConditionId)
                .withAbSegmentStatisticRetargetingConditionId(oldAbStatisticRetargetingConditionId)
                .withSectionIds(sectionIds)
                .withAbSegmentGoalIds(abSegmentIds);

        operationSupport.beforeExecution(addParameters, List.of(campaign));

        assertThat(campaign.getAbSegmentStatisticRetargetingConditionId()).isEqualTo(oldAbStatisticRetargetingConditionId);
        assertThat(campaign.getAbSegmentRetargetingConditionId()).isEqualTo(oldAbRetargetingConditionId);
    }

    private void mockRetargetingConditionService(List<Long> sectionIds, List<Long> abSegmentIds,
                                                 Long abStatisticRetargetingConditionId,
                                                 Long abRetargetingConditionId) {
        CampaignExperiment campaignExperiment = new CampaignExperiment()
                .withSectionIds(sectionIds)
                .withAbSegmentGoalIds(abSegmentIds);

        ExperimentRetargetingConditions experimentRetargetingConditions = new ExperimentRetargetingConditions()
                .withStatisticRetargetingConditionId(abStatisticRetargetingConditionId)
                .withRetargetingConditionId(abRetargetingConditionId);

        var goals = Map.of(1L, List.of(new RetargetingCondition()));

        doReturn(goals).when(metrikaClientAdapter).getAbSegmentGoals();
        doReturn(List.of(experimentRetargetingConditions)).when(retargetingConditionService)
                .findOrCreateExperimentsRetargetingConditions(clientId, List.of(campaignExperiment), goals, null);
    }

    private CampaignWithExperiments createCampaign() {
        return (CampaignWithExperiments) TestCampaigns.newCampaignByCampaignType(campaignType)
                .withClientId(clientId.asLong())
                .withName("campaign")
                .withType(CampaignType.valueOf(campaignType.name()))
                .withId(campaignId)
                .withUid(uid);
    }
}
