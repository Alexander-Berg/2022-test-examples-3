package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.audience.client.YaAudienceClient;
import ru.yandex.direct.audience.client.model.CreateExperimentResponse;
import ru.yandex.direct.audience.client.model.CreateExperimentResponseEnvelope;
import ru.yandex.direct.audience.client.model.ExperimentSegmentResponse;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.client.model.ClientExperiment;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.feature.FeatureName.AB_SEGMENTS;
import static ru.yandex.direct.feature.FeatureName.BRAND_LIFT;
import static ru.yandex.direct.feature.FeatureName.CPM_GLOBAL_AB_SEGMENT;
import static ru.yandex.direct.feature.FeatureName.SEARCH_LIFT;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class RestrictedCampaignsAddOperationGlobalABSegmentWithLiftsTest {
    private static final Integer METRIKA_COUNTER = RandomNumberUtils.nextPositiveInteger();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @org.junit.Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private BrandSurveyRepository brandSurveyRepository;
    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;
    @Autowired
    private CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;
    @Autowired
    public YaAudienceClient yaAudienceClient;

    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    private GoalUtilsService goalUtilsService;

    @Autowired
    private Steps steps;

    private CpmBannerCampaign cpmBannerCampaign;
    private ClientInfo clientInfo;
    private List<Goal> goals;
    private Long campaignId;
    private Integer shard;

    @Before
    public void before() {
        var defaultUser = steps.userSteps().createDefaultUser();
        steps.featureSteps().addClientFeature(defaultUser.getClientId(), FeatureName.DISABLE_BILLING_AGGREGATES, true);
        clientInfo = defaultUser.getClientInfo();
        shard = clientInfo.getShard();
        createCampaign();

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), BRAND_LIFT, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), AB_SEGMENTS, true);

        goals = List.of(defaultABSegmentGoal(), defaultABSegmentGoal(), defaultABSegmentGoal());

        initGoalsInMetrika(goals);
    }

    @Test
    public void add_withSearchLift() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), SEARCH_LIFT, true);
        Goal goal = goals.get(0);
        Long experimentId = goal.getSectionId();
        Long abSegmentGoalId = goal.getId();

        mockYaAudienceClient(experimentId, abSegmentGoalId, abSegmentGoalId);

        cpmBannerCampaign
                .withIsSearchLiftEnabled(true)
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));

        MassResult<Long> result = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign = getCampaignFromResult(result);

        Optional<ClientExperiment> experiment = clientRepository.getExperiment(shard,
                clientInfo.getClientId().asLong());
        assertThat(actualCampaign.getSectionIds()).containsExactly(experimentId);
        assertThat(actualCampaign.getAbSegmentGoalIds()).containsExactly(abSegmentGoalId);
        assertThat(experiment.isPresent()).isTrue();
        assertThat(experiment.get().getExperimentId()).isEqualTo(experimentId);
        assertThat(experiment.get().getSegmentId()).isEqualTo(abSegmentGoalId);
    }


    @Test
    public void add_WithoutBrandLift_withGlobalAbSegment() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CPM_GLOBAL_AB_SEGMENT, true);
        Goal goal = goals.get(0);
        Long experimentId = goal.getSectionId();
        Long abSegmentGoalId = goal.getId();
        String newBrandSurveyId = "brand_survey_id";
        String brandSurveyName = "newBrandSurveyName123";

        mockYaAudienceClient(experimentId, abSegmentGoalId, abSegmentGoalId);

        cpmBannerCampaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));

        MassResult<Long> result = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign = getCampaignFromResult(result);

        Optional<ClientExperiment> experiment = clientRepository.getExperiment(shard,
                clientInfo.getClientId().asLong());
        assertThat(actualCampaign.getSectionIds()).containsExactly(experimentId);
        assertThat(actualCampaign.getAbSegmentGoalIds()).containsExactly(abSegmentGoalId);
        assertThat(experiment.isPresent()).isTrue();
        assertThat(experiment.get().getExperimentId()).isEqualTo(experimentId);
        assertThat(experiment.get().getSegmentId()).isEqualTo(abSegmentGoalId);



        mockYaAudienceClient(experimentId + 1, abSegmentGoalId + 1, abSegmentGoalId + 1);
        createCampaign();
        cpmBannerCampaign
                .withBrandSurveyId(newBrandSurveyId)
                .withBrandSurveyName(brandSurveyName)
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));
        MassResult<Long> result2 = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign2 = getCampaignFromResult(result2);

        experiment = clientRepository.getExperiment(shard,
                clientInfo.getClientId().asLong());
        var brandSurvey = brandSurveyRepository
                .getBrandSurvey(shard, newBrandSurveyId).get(0);

        assertThat(actualCampaign2.getSectionIds()).containsExactly(experimentId);
        assertThat(actualCampaign2.getAbSegmentGoalIds()).containsExactly(abSegmentGoalId);
        assertThat(experiment.isPresent()).isTrue();
        assertThat(experiment.get().getExperimentId()).isEqualTo(experimentId);
        assertThat(experiment.get().getSegmentId()).isEqualTo(abSegmentGoalId);
        assertThat(brandSurvey.getExperimentId()).isEqualTo(experimentId);
        assertThat(brandSurvey.getSegmentId()).isEqualTo(abSegmentGoalId);
    }

    @Test
    public void add_withoutFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CPM_GLOBAL_AB_SEGMENT, false);
        Goal goal = goals.get(0);
        Long experimentId = goal.getSectionId();
        Long abSegmentGoalId = goal.getId();
        String newBrandSurveyId = "brand_survey_id_1111";
        String brandSurveyName = "newBrandSurveyName123";

        mockYaAudienceClient(experimentId, abSegmentGoalId, abSegmentGoalId);

        cpmBannerCampaign
                .withBrandSurveyId(newBrandSurveyId)
                .withBrandSurveyName(brandSurveyName)
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));

        MassResult<Long> result = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign = getCampaignFromResult(result);

        Optional<ClientExperiment> experiment = clientRepository.getExperiment(shard,
                clientInfo.getClientId().asLong());
        var brandSurvey = brandSurveyRepository
                .getBrandSurvey(shard, newBrandSurveyId).get(0);
        assertThat(actualCampaign.getSectionIds()).containsExactly(experimentId);
        assertThat(actualCampaign.getAbSegmentGoalIds()).containsExactly(abSegmentGoalId);
        assertThat(experiment.isPresent()).isTrue();
        assertThat(experiment.get().getExperimentId()).isEqualTo(experimentId);
        assertThat(experiment.get().getSegmentId()).isEqualTo(abSegmentGoalId);
        assertThat(brandSurvey.getExperimentId()).isEqualTo(experimentId);
        assertThat(brandSurvey.getSegmentId()).isEqualTo(abSegmentGoalId);


        mockYaAudienceClient(experimentId + 1, abSegmentGoalId + 1, abSegmentGoalId + 1);
        createCampaign();
        cpmBannerCampaign
                .withBrandSurveyId(newBrandSurveyId + "2")
                .withBrandSurveyName(brandSurveyName + "2")
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));
        MassResult<Long> result2 = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign2 = getCampaignFromResult(result2);

        experiment = clientRepository.getExperiment(shard,
                clientInfo.getClientId().asLong());
        var brandSurvey2 = brandSurveyRepository
                .getBrandSurvey(shard, newBrandSurveyId+ "2").get(0);

        assertThat(actualCampaign2.getSectionIds()).containsExactly(experimentId);
        assertThat(actualCampaign2.getAbSegmentGoalIds()).containsExactly(abSegmentGoalId);
        assertThat(experiment.isPresent()).isTrue();
        assertThat(experiment.get().getExperimentId()).isEqualTo(experimentId);
        assertThat(experiment.get().getSegmentId()).isEqualTo(abSegmentGoalId);
        assertThat(brandSurvey2.getExperimentId()).isEqualTo(experimentId);
        assertThat(brandSurvey2.getSegmentId()).isEqualTo(abSegmentGoalId);
    }


    private void initGoalsInMetrika(List<Goal> goals) {
        metrikaClientStub.addGoals(clientInfo.getUid(), new HashSet<>(goals));
        metrikaHelperStub.addGoalIds(clientInfo.getUid(), listToSet(goals, GoalBase::getId));
    }

    private void createCampaign() {
        createCampaign(defaultCpmBannerCampaign());
    }

    private void createCampaign(CpmBannerCampaign campaign) {
        this.cpmBannerCampaign = campaign;
    }

    private MassResult<Long> createOperationAndApply() {
        RestrictedCampaignsAddOperation operation = new RestrictedCampaignsAddOperation(
                List.of(cpmBannerCampaign),
                clientInfo.getShard(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                clientInfo.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);
        return operation.prepareAndApply();
    }

    private CpmBannerCampaign getCampaignFromResult(MassResult<Long> result) {
        CpmBannerCampaign cpmBannerCampaign =
                (CpmBannerCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                        List.of(result.get(0).getResult())).get(0);
        campaignId = cpmBannerCampaign.getId();
        return cpmBannerCampaign;
    }

    private void mockYaAudienceClient(Long sectionId, Long abSegmentGoalIdA, Long abSegmentGoalIdB) {
        doReturn(createExperimentResponse(
                sectionId,
                abSegmentGoalIdA - CampaignConstants.SEGMENT_GOAL_ID_SHIFT,
                abSegmentGoalIdB - CampaignConstants.SEGMENT_GOAL_ID_SHIFT))
                .when(yaAudienceClient).createExperiment(any(), any());
    }

    private static CreateExperimentResponseEnvelope createExperimentResponse(
            Long experimentId, Long segmentAId, Long segmentBId) {
        return new CreateExperimentResponseEnvelope()
                .withCreateExperimentResponse(new CreateExperimentResponse()
                        .withExperimentId(experimentId)
                        .withExperimentSegments(List.of(
                                new ExperimentSegmentResponse().withSegmentId(segmentAId),
                                new ExperimentSegmentResponse().withSegmentId(segmentBId))
                        )
                );
    }
}
