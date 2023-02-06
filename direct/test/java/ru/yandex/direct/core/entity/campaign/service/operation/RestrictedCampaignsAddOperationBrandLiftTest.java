package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
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
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.feature.FeatureName.AB_SEGMENTS;
import static ru.yandex.direct.feature.FeatureName.BRAND_LIFT;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class RestrictedCampaignsAddOperationBrandLiftTest {
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
    private CampaignRepository campaignRepository;
    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;
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

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
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
    private UserInfo defaultUser;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
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
    public void add_BrandLift() {
        Goal goal = goals.get(0);
        Long experimentId = goal.getSectionId();
        Long abSegmentGoalId = goal.getId();
        String newBrandSurveyId = "brand_survey_id";
        String brandSurveyName = "newBrandSurveyName123";

        mockYaAudienceClient(experimentId, abSegmentGoalId, abSegmentGoalId);

        cpmBannerCampaign
                .withBrandSurveyId(newBrandSurveyId)
                .withBrandSurveyName(brandSurveyName)
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));

        MassResult<Long> result = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign = getCampaignFromResult(result);

        checkBrandSurveyInCampaign(newBrandSurveyId, brandSurveyName, campaignId);
        checkSectionIdsMatchedByRetCond(Set.of(experimentId), campaignId);
        checkAbGoalIdsMatchedByRetCond(List.of(abSegmentGoalId), campaignId);
    }

    @Test
    public void add_campaignsWithSameBrandLift() {
        Goal goal = goals.get(0);
        Long experimentId = goal.getSectionId();
        Long abSegmentGoalId = goal.getId();
        String newBrandSurveyId = "brand_survey_id197";
        String brandSurveyName = "newBrandSurveyName123";

        mockYaAudienceClient(experimentId, abSegmentGoalId, abSegmentGoalId);

        cpmBannerCampaign
                .withBrandSurveyId(newBrandSurveyId)
                .withBrandSurveyName(brandSurveyName)
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));

        MassResult<Long> result = createOperationAndApply();

        createCampaign();
        cpmBannerCampaign
                .withBrandSurveyId(newBrandSurveyId)
                .withBrandSurveyName(brandSurveyName)
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()));

        MassResult<Long> result2 = createOperationAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        assertThat(result2.getValidationResult().flattenErrors()).isEmpty();

        CpmBannerCampaign actualCampaign = getCampaignFromResult(result);
        CpmBannerCampaign actualCampaign2 = getCampaignFromResult(result2);

        checkBrandSurveyInCampaign(newBrandSurveyId, brandSurveyName, actualCampaign.getId());
        checkBrandSurveyInCampaign(newBrandSurveyId, brandSurveyName, actualCampaign2.getId());

        checkSectionIdsMatchedByRetCond(Set.of(experimentId), actualCampaign.getId());
        checkSectionIdsMatchedByRetCond(Set.of(experimentId), actualCampaign2.getId());

        checkAbGoalIdsMatchedByRetCond(List.of(abSegmentGoalId), actualCampaign.getId());
        checkAbGoalIdsMatchedByRetCond(List.of(abSegmentGoalId), actualCampaign2.getId());
    }

    @Test
    public void add_BrandLiftAndExperimentsTogether() {
        Goal brandLiftGoal = goals.get(0);
        Long brandSurveySectionId = brandLiftGoal.getSectionId();
        Long brandSurveyAbSegmentGoalId = brandLiftGoal.getId();
        String newBrandSurveyId = "brand_survey_id267";
        String brandSurveyName = "newBrandSurveyName123";

        mockYaAudienceClient(brandSurveySectionId, brandSurveyAbSegmentGoalId, brandSurveyAbSegmentGoalId);

        Goal experimentGoal = goals.get(1);
        Long experimentSectionId = experimentGoal.getSectionId();
        Long experimentAbGoalSegmentId = experimentGoal.getId();

        cpmBannerCampaign
                .withBrandSurveyId(newBrandSurveyId)
                .withBrandSurveyName(brandSurveyName)
                .withMetrikaCounters(List.of(METRIKA_COUNTER.longValue()))
                .withSectionIds(new ArrayList<>(List.of(experimentSectionId)))
                .withAbSegmentGoalIds(new ArrayList<>(List.of(experimentAbGoalSegmentId)));

        MassResult<Long> result = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign = getCampaignFromResult(result);

        checkBrandSurveyInCampaign(newBrandSurveyId, brandSurveyName, campaignId);
        checkSectionIdsMatchedByRetCond(new HashSet<>(List.of(brandSurveySectionId, experimentSectionId)), campaignId);
        checkAbGoalIdsMatchedByRetCond(List.of(brandSurveyAbSegmentGoalId, experimentAbGoalSegmentId), campaignId);
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "parametersForAdd_SaveExperiments_SavedSuccessfully")
    public void add_SaveExperiments_SavedSuccessfully(@SuppressWarnings("unused") String testName,
                                                      List<Goal> goals,
                                                      Set<Long> newSectionIds,
                                                      List<Long> newAbSegmentGoalIds,
                                                      Set<Long> expectedSectionIds,
                                                      List<Long> expectedAbSegmentGoalIds) {
        initGoalsInMetrika(goals);

        cpmBannerCampaign
                .withSectionIds(ifNotNull(newSectionIds, ArrayList::new))
                .withAbSegmentGoalIds(ifNotNull(newAbSegmentGoalIds, ArrayList::new));

        MassResult<Long> result = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmBannerCampaign actualCampaign = getCampaignFromResult(result);

        checkSectionIdsMatchedByRetCond(expectedSectionIds, campaignId);
        checkAbGoalIdsMatchedByRetCond(expectedAbSegmentGoalIds, campaignId);
    }

    @SuppressWarnings("unused")
    private static Object[][] parametersForAdd_SaveExperiments_SavedSuccessfully() {
        List<Goal> goals = List.of(
                defaultABSegmentGoal(),
                defaultABSegmentGoal()
        );

        Long sectionId = goals.get(0).getSectionId();
        Long abSegmentGoalId1 = goals.get(0).getId();
        Long abSegmentGoalId2 = goals.get(1).getId();

        Set<Long> sectionIds = Set.of(sectionId);
        List<Long> abSegmentGoalIds = List.of(abSegmentGoalId1, abSegmentGoalId2);

        return new Object[][]{
                {"null -> null", goals, null, null, null, null},
                {"[] -> []", goals, emptySet(), emptyList(), emptySet(), emptyList()},
                {"[id, null] -> [id, null]", goals, sectionIds, null, sectionIds, null},
                {"[null, id] -> [null, id]", goals, null, abSegmentGoalIds, null, abSegmentGoalIds},
                {"[id, id] -> [id, id]", goals, sectionIds, abSegmentGoalIds, sectionIds, abSegmentGoalIds}
        };
    }

    private void checkBrandSurveyInCampaign(String newBrandSurveyId, String brandSurveyName, Long campaignId) {
        var brandSurveyIdsForCampaigns = campaignRepository.getBrandSurveyIdsForCampaigns(shard,
                List.of(campaignId));

        var actualBrandSurveyId = brandSurveyIdsForCampaigns.get(campaignId);
        var actualBrandSurveyName = brandSurveyRepository
                .getBrandSurvey(shard, brandSurveyIdsForCampaigns.get(campaignId)).get(0).getName();

        assertThat(actualBrandSurveyId).isEqualTo(newBrandSurveyId);
        assertThat(actualBrandSurveyName).isEqualTo(brandSurveyName);
    }

    private void checkAbGoalIdsMatchedByRetCond(List<Long> newAbSegmentGoalIds, Long campaignId) {
        RetargetingCondition abRetCond =
                retargetingConditionRepository.getRetConditionsByCampaignIds(clientInfo.getShard(),
                        List.of(campaignId), ConditionType.ab_segments).get(campaignId);

        if (newAbSegmentGoalIds == null) {
            assertThat(abRetCond).isNull();
        } else {
            Set<Long> actualAbSegmentGoalIds = listToSet(abRetCond.collectGoals(), GoalBase::getId);
            assertThat(actualAbSegmentGoalIds).containsExactlyInAnyOrder(newAbSegmentGoalIds.toArray(new Long[0]));
        }
    }

    private void checkSectionIdsMatchedByRetCond(Set<Long> newSectionIds, Long campaignId) {
        RetargetingCondition abStatRetCond =
                retargetingConditionRepository.getStatisticRetConditionsByCampaignIds(clientInfo.getShard(),
                        List.of(campaignId), ConditionType.ab_segments).get(campaignId);

        if (newSectionIds == null) {
            assertThat(abStatRetCond).isNull();
        } else {
            Set<Long> actualSectionIds = listToSet(abStatRetCond.getRules(), Rule::getSectionId);
            assertThat(actualSectionIds).containsExactlyInAnyOrder(newSectionIds.toArray(new Long[0]));
        }
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
