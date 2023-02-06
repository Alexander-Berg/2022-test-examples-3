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
import ru.yandex.direct.core.entity.campaign.model.CampaignExperiment;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
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
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TypedCampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.feature.FeatureName.AB_SEGMENTS;
import static ru.yandex.direct.feature.FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class RestrictedCampaignsUpdateOperationExperimentsTest {
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
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

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

    private TypedCampaignInfo textCampaign;
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

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), AB_SEGMENTS, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA, true);

        goals = List.of(defaultABSegmentGoal(), defaultABSegmentGoal(), defaultABSegmentGoal());

        initGoalsInMetrika(goals);
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

        ModelChanges<TextCampaign> campaignModelChanges = new ModelChanges<>(campaignId, TextCampaign.class);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkSectionIdsMatchedByRetCond(expectedSectionIds, campaignId);
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

        ModelChanges<TextCampaign> campaignModelChanges = new ModelChanges<>(campaignId, TextCampaign.class);
        campaignModelChanges.process(ifNotNull(newSectionIds, ArrayList::new), TextCampaign.SECTION_IDS);
        campaignModelChanges.process(ifNotNull(newAbSegmentGoalIds, ArrayList::new),
                TextCampaign.AB_SEGMENT_GOAL_IDS);

        MassResult<Long> result = createOperationAndApply(campaignModelChanges);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        checkSectionIdsMatchedByRetCond(expectedSectionIds, campaignId);
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
                {"[null, null] -> [null, null]", goals, null, null, null, null},
                {"[empty, empty] -> [null, null]", goals, emptyList(), emptyList(), null, null},
                {"[id, null] -> [id, null]", goals, sectionIds, null, sectionIds, null},
                {"[null, id] -> [null, id]", goals, null, abSegmentGoalIds, null, abSegmentGoalIds},
                {"[id, id] -> [id, id]", goals, sectionIds, abSegmentGoalIds, sectionIds, abSegmentGoalIds}
        };
    }

    private void checkAbGoalIdsMatchedByRetCond(List<Long> newAbSegmentGoalIds, Long campaignId) {
        RetargetingCondition abRetCond =
                retargetingConditionRepository.getRetConditionsByCampaignIds(textCampaign.getShard(),
                        List.of(campaignId), ConditionType.ab_segments).get(campaignId);

        if (newAbSegmentGoalIds == null) {
            assertThat(abRetCond).isNull();
        } else {
            Set<Long> actualAbSegmentGoalIds = listToSet(abRetCond.collectGoals(), GoalBase::getId);
            assertThat(actualAbSegmentGoalIds).containsExactlyInAnyOrder(newAbSegmentGoalIds.toArray(new Long[0]));
        }
    }

    private void checkSectionIdsMatchedByRetCond(List<Long> newSectionIds, Long campaignId) {
        RetargetingCondition abStatRetCond =
                retargetingConditionRepository.getStatisticRetConditionsByCampaignIds(textCampaign.getShard(),
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
        createCampaign(defaultTextCampaignWithSystemFields(), null, null);
    }

    private void createCampaign(TextCampaign campaign, Long abSegmentStatisticRetargetingConditionId,
                                Long abSegmentRetargetingConditionId) {
        campaign
                .withAbSegmentStatisticRetargetingConditionId(abSegmentStatisticRetargetingConditionId)
                .withAbSegmentRetargetingConditionId(abSegmentRetargetingConditionId)
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong());
        textCampaign = steps.typedCampaignSteps().createTextCampaign(defaultUser, clientInfo,
                campaign);
        campaignId = textCampaign.getId();
        shard = textCampaign.getShard();
    }

    private void createCampaignWithExperiment(List<Long> sectionIds,
                                              List<Long> abSegmentGoalIds) {
        ExperimentRetargetingConditions experimentRetargetingCondition =
                retargetingConditionService.findOrCreateExperimentsRetargetingConditions(clientInfo.getClientId(),
                        List.of(new CampaignExperiment()
                                .withSectionIds(sectionIds)
                                .withAbSegmentGoalIds(abSegmentGoalIds))).get(0);

        TextCampaign campaign = defaultTextCampaignWithSystemFields();
        createCampaign(campaign, experimentRetargetingCondition.getStatisticRetargetingConditionId(),
                experimentRetargetingCondition.getRetargetingConditionId());
    }

    private MassResult<Long> createOperationAndApply(ModelChanges<TextCampaign> campaignModelChanges) {
        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation operation =
                new RestrictedCampaignsUpdateOperation(List.of(campaignModelChanges),
                        textCampaign.getUid(), UidClientIdShard.of(textCampaign.getUid(),
                        textCampaign.getClientId(),
                        textCampaign.getShard()), campaignModifyRepository, campaignTypedRepository,
                        strategyTypedRepository,
                        updateRestrictedCampaignValidationService, campaignUpdateOperationSupportFacade,
                        campaignAdditionalActionsService, dslContextProvider, rbacService, metrikaClientFactory,
                        featureService, Applicability.PARTIAL, options);
        return operation.apply();
    }
}
