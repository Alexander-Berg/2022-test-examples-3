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
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.audience.client.YaAudienceClient;
import ru.yandex.direct.audience.client.model.CreateExperimentResponse;
import ru.yandex.direct.audience.client.model.CreateExperimentResponseEnvelope;
import ru.yandex.direct.audience.client.model.ExperimentSegmentResponse;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignExperiment;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.ExperimentRetargetingConditions;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.CpmBannerCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.metrika.client.model.request.GoalType;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.brandLiftExperimentSegmentsCantBeChanged;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.feature.FeatureName.AB_SEGMENTS;
import static ru.yandex.direct.feature.FeatureName.BRAND_LIFT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class RestrictedCampaignsUpdateOperationBrandLiftTest {
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
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    private CampaignAdditionalActionsService campaignAdditionalActionsService;
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
    private RetargetingConditionService retargetingConditionService;

    @Autowired
    private Steps steps;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    private FeatureService featureService;

    private CpmBannerCampaignInfo cpmBannerCampaign;
    private ClientInfo clientInfo;
    private List<Goal> goals;
    private Long campaignId;
    private Integer shard;
    private UserInfo defaultUser;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        clientInfo = defaultUser.getClientInfo();
        createCampaign();

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), BRAND_LIFT, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), AB_SEGMENTS, true);

        goals = List.of(defaultABSegmentGoal(), defaultABSegmentGoal(), defaultABSegmentGoal());

        initGoalsInMetrika(goals);
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);
    }

    @Test
    public void update_BrandLift() {
        Goal goal = goals.get(0);
        Long experimentId = goal.getSectionId();
        Long abSegmentGoalId = goal.getId();
        String newBrandSurveyId = "brand_survey_id1";
        String newBrandSurveyName = "brand_survey_Name123";

        mockYaAudienceClient(experimentId, abSegmentGoalId, abSegmentGoalId);

        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges.process(newBrandSurveyId, CpmBannerCampaign.BRAND_SURVEY_ID);
        campaignModelChanges.process(newBrandSurveyName, CpmBannerCampaign.BRAND_SURVEY_NAME);
        campaignModelChanges.process(List.of(METRIKA_COUNTER.longValue()),
                CpmBannerCampaign.METRIKA_COUNTERS);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkBrandSurveyInCampaign(newBrandSurveyId, newBrandSurveyName, campaignId);
        checkSectionIdsMatchedByRetCond(Set.of(experimentId), campaignId);
        checkAbGoalIdsMatchedByRetCond(List.of(abSegmentGoalId), campaignId);
    }

    @Test
    public void update_RenameExistedBrandLift() {
        Goal goal = goals.get(0);
        Long experimentId = goal.getSectionId();
        Long abSegmentGoalId = goal.getId();
        String newBrandSurveyId = "brand_survey_id6";
        String oldBrandSurveyName = "brand_survey_old_Name123";
        String newBrandSurveyName = "brand_survey_Name123";

        mockYaAudienceClient(experimentId, abSegmentGoalId, abSegmentGoalId);

        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges.process(newBrandSurveyId, CpmBannerCampaign.BRAND_SURVEY_ID);
        campaignModelChanges.process(oldBrandSurveyName, CpmBannerCampaign.BRAND_SURVEY_NAME);
        campaignModelChanges.process(List.of(METRIKA_COUNTER.longValue()),
                CpmBannerCampaign.METRIKA_COUNTERS);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        createCampaign();

        ModelChanges<CpmBannerCampaign> campaignModelChanges2 = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges2.process(newBrandSurveyId, CpmBannerCampaign.BRAND_SURVEY_ID);
        campaignModelChanges2.process(newBrandSurveyName, CpmBannerCampaign.BRAND_SURVEY_NAME);
        campaignModelChanges2.process(List.of(METRIKA_COUNTER.longValue()),
                CpmBannerCampaign.METRIKA_COUNTERS);

        MassResult<Long> result2 = createOperationAndApply(campaignModelChanges2);
        assertThat(result2.getValidationResult().flattenErrors()).isEmpty();

        checkBrandSurveyInCampaign(newBrandSurveyId, newBrandSurveyName, campaignId);
        checkSectionIdsMatchedByRetCond(Set.of(experimentId), campaignId);
        checkAbGoalIdsMatchedByRetCond(List.of(abSegmentGoalId), campaignId);
    }

    @Test
    public void update_BrandLiftAndExperimentsTogether() {
        Goal brandLiftGoal = goals.get(0);
        Long brandSurveySectionId = brandLiftGoal.getSectionId();
        Long brandSurveyAbSegmentGoalId = brandLiftGoal.getId();
        String newBrandSurveyId = "brand_survey_id2";
        String newBrandSurveyName = "brand_survey_Name123";

        mockYaAudienceClient(brandSurveySectionId, brandSurveyAbSegmentGoalId, brandSurveyAbSegmentGoalId);

        Goal experimentGoal = goals.get(1);
        Long experimentSectionId = experimentGoal.getSectionId();
        Long experimentAbGoalSegmentId = experimentGoal.getId();

        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges.process(newBrandSurveyId, CpmBannerCampaign.BRAND_SURVEY_ID);
        campaignModelChanges.process(newBrandSurveyName, CpmBannerCampaign.BRAND_SURVEY_NAME);

        campaignModelChanges.process(List.of(METRIKA_COUNTER.longValue()),
                CpmBannerCampaign.METRIKA_COUNTERS);
        campaignModelChanges.process(new ArrayList<>(List.of(experimentSectionId)), CpmBannerCampaign.SECTION_IDS);
        campaignModelChanges.process(new ArrayList<>(List.of(experimentAbGoalSegmentId)),
                CpmBannerCampaign.AB_SEGMENT_GOAL_IDS);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkBrandSurveyInCampaign(newBrandSurveyId, newBrandSurveyName, campaignId);
        checkSectionIdsMatchedByRetCond(new HashSet<>(List.of(brandSurveySectionId, experimentSectionId)), campaignId);
        checkAbGoalIdsMatchedByRetCond(List.of(brandSurveyAbSegmentGoalId, experimentAbGoalSegmentId), campaignId);
    }

    @Test
    public void update_AddBrandLiftToCampaignWithExperiments() {
        Goal brandLiftGoal = goals.get(0);
        Long brandSurveySectionId = brandLiftGoal.getSectionId();
        Long brandSurveyAbSegmentGoalId = brandLiftGoal.getId();
        String newBrandSurveyId = "brand_survey_id3";
        String newBrandSurveyName = "brand_survey_Name123";

        mockYaAudienceClient(brandSurveySectionId, brandSurveyAbSegmentGoalId, brandSurveyAbSegmentGoalId);

        Goal experimentGoal = goals.get(1);
        Long experimentSectionId = experimentGoal.getSectionId();
        Long experimentAbGoalSegmentId = experimentGoal.getId();

        createCampaignWithExperiment(new ArrayList<>(List.of(experimentSectionId)),
                new ArrayList<>(List.of(experimentAbGoalSegmentId)));

        // brandlift
        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges.process(newBrandSurveyId, CpmBannerCampaign.BRAND_SURVEY_ID);
        campaignModelChanges.process(newBrandSurveyName, CpmBannerCampaign.BRAND_SURVEY_NAME);

        campaignModelChanges.process(List.of(METRIKA_COUNTER.longValue()),
                CpmBannerCampaign.METRIKA_COUNTERS);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkBrandSurveyInCampaign(newBrandSurveyId, newBrandSurveyName, campaignId);
        checkSectionIdsMatchedByRetCond(new HashSet<>(List.of(brandSurveySectionId, experimentSectionId)), campaignId);
        checkAbGoalIdsMatchedByRetCond(List.of(brandSurveyAbSegmentGoalId, experimentAbGoalSegmentId), campaignId);
    }

    @Test
    public void update_AddExperimentToCampaignWithBrandLift() {
        Goal brandLiftGoal = goals.get(0);
        Long brandSurveySectionId = brandLiftGoal.getSectionId();
        Long brandSurveyAbSegmentGoalId = brandLiftGoal.getId();
        String newBrandSurveyId = "brand_survey_id4";
        String newBrandSurveyName = "brand_survey_Name123";

        mockYaAudienceClient(brandSurveySectionId, brandSurveyAbSegmentGoalId, brandSurveyAbSegmentGoalId);

        Goal experimentGoal = goals.get(1);
        Long experimentSectionId = experimentGoal.getSectionId();
        Long experimentAbGoalSegmentId = experimentGoal.getId();

        // brandlift
        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges.process(newBrandSurveyId, CpmBannerCampaign.BRAND_SURVEY_ID);
        campaignModelChanges.process(newBrandSurveyName, CpmBannerCampaign.BRAND_SURVEY_NAME);
        campaignModelChanges.process(List.of(METRIKA_COUNTER.longValue()),
                CpmBannerCampaign.METRIKA_COUNTERS);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkBrandSurveyInCampaign(newBrandSurveyId, newBrandSurveyName, campaignId);
        checkSectionIdsMatchedByRetCond(Set.of(brandSurveySectionId), campaignId);
        checkAbGoalIdsMatchedByRetCond(List.of(brandSurveyAbSegmentGoalId), campaignId);

        // experiments
        campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges.process(new ArrayList<>(List.of(experimentSectionId)), CpmBannerCampaign.SECTION_IDS);
        campaignModelChanges.process(new ArrayList<>(List.of(experimentAbGoalSegmentId)),
                CpmBannerCampaign.AB_SEGMENT_GOAL_IDS);

        result = createOperationAndApply(campaignModelChanges);

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(path(index(0), field(CpmBannerCampaign.AB_SEGMENT_GOAL_IDS)),
                                brandLiftExperimentSegmentsCantBeChanged()))));
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "parametersForUpdate_SaveExperiments_SavedSuccessfully")
    public void update_PreserveSavedExperiments_WhenSaving(@SuppressWarnings("unused") String testName,
                                                           List<Goal> goals,
                                                           List<Long> newSectionIds,
                                                           List<Long> newAbSegmentGoalIds,
                                                           List<Long> expectedSectionIds,
                                                           List<Long> expectedAbSegmentGoalIds) {
        initGoalsInMetrika(goals);
        createCampaignWithExperiment(newSectionIds, newAbSegmentGoalIds);

        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkSectionIdsMatchedByRetCond(listToSet(expectedSectionIds), campaignId);
        checkAbGoalIdsMatchedByRetCond(expectedAbSegmentGoalIds, campaignId);
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "parametersForUpdate_SaveExperiments_SavedSuccessfully")
    public void update_SaveExperiments_SavedSuccessfully(@SuppressWarnings("unused") String testName,
                                                         List<Goal> goals,
                                                         List<Long> newSectionIds,
                                                         List<Long> newAbSegmentGoalIds,
                                                         List<Long> expectedSectionIds,
                                                         List<Long> expectedAbSegmentGoalIds) {
        initGoalsInMetrika(goals);

        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(campaignId, CpmBannerCampaign.class);
        campaignModelChanges.process(ifNotNull(newSectionIds, ArrayList::new), CpmBannerCampaign.SECTION_IDS);
        campaignModelChanges.process(ifNotNull(newAbSegmentGoalIds, ArrayList::new),
                CpmBannerCampaign.AB_SEGMENT_GOAL_IDS);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkSectionIdsMatchedByRetCond(listToSet(expectedSectionIds), campaignId);
        checkAbGoalIdsMatchedByRetCond(expectedAbSegmentGoalIds, campaignId);
    }

    @Parameterized.Parameters(name = "Successful validation, featureEnabled = {0}, valueReceived = {1}")
    private static Object[][] parametersForUpdate_SaveExperiments_SavedSuccessfully() {
        List<Goal> goals = List.of(
                defaultABSegmentGoal(),
                defaultABSegmentGoal()
        );

        Long sectionId = goals.get(0).getSectionId();
        Long abSegmentGoalId1 = goals.get(0).getId();
        Long abSegmentGoalId2 = goals.get(1).getId();

        List<Long> sectionIds = List.of(sectionId);
        List<Long> abSegmentGoalIds = List.of(abSegmentGoalId1, abSegmentGoalId2);

        return new Object[][]{
                {"null -> null", goals, null, null, null, null},
                {"[] -> []", goals, emptyList(), emptyList(), emptyList(), emptyList()},
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
                retargetingConditionRepository.getRetConditionsByCampaignIds(cpmBannerCampaign.getShard(),
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
                retargetingConditionRepository.getStatisticRetConditionsByCampaignIds(cpmBannerCampaign.getShard(),
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
        createCampaign(TestCpmBannerCampaigns.fullDraftCpmBannerCampaign(), null, null);
    }

    private void createCampaign(CpmBannerCampaign campaign, Long abSegmentStatisticRetargetingConditionId,
                                Long abSegmentRetargetingConditionId) {
        campaign
                .withAbSegmentStatisticRetargetingConditionId(abSegmentStatisticRetargetingConditionId)
                .withAbSegmentRetargetingConditionId(abSegmentRetargetingConditionId)
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());
        cpmBannerCampaign = steps.cpmBannerCampaignSteps().createCampaign(clientInfo, campaign);
        campaignId = cpmBannerCampaign.getId();
        shard = cpmBannerCampaign.getShard();
    }

    private void createCampaignWithExperiment(List<Long> sectionIds,
                                              List<Long> abSegmentGoalIds) {
        var goals = metrikaClientStub.getGoals(List.of(clientInfo.getUid()), GoalType.AB_SEGMENT)
                .getUidToConditions();
        ExperimentRetargetingConditions experimentRetargetingCondition =
                retargetingConditionService.findOrCreateExperimentsRetargetingConditions(clientInfo.getClientId(),
                        List.of(new CampaignExperiment()
                                .withSectionIds(sectionIds)
                                .withAbSegmentGoalIds(abSegmentGoalIds)),
                        goals,
                        null).get(0);

        CpmBannerCampaign campaign = TestCpmBannerCampaigns.fullDraftCpmBannerCampaign();
        createCampaign(campaign, experimentRetargetingCondition.getStatisticRetargetingConditionId(),
                experimentRetargetingCondition.getRetargetingConditionId());
    }

    private MassResult<Long> createOperationAndApply(ModelChanges<CpmBannerCampaign> campaignModelChanges) {
        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation operation =
                new RestrictedCampaignsUpdateOperation(List.of(campaignModelChanges),
                        cpmBannerCampaign.getUid(), UidClientIdShard.of(cpmBannerCampaign.getUid(),
                        cpmBannerCampaign.getClientId(),
                        cpmBannerCampaign.getShard()), campaignModifyRepository, campaignTypedRepository,
                        strategyTypedRepository, updateRestrictedCampaignValidationService,
                        campaignUpdateOperationSupportFacade,
                        campaignAdditionalActionsService, dslContextProvider, rbacService, metrikaClientFactory,
                        featureService, Applicability.PARTIAL, options);
        return operation.apply();
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
