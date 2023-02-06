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
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
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
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.feature.FeatureName.AB_SEGMENTS;
import static ru.yandex.direct.feature.FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class RestrictedCampaignsAddOperationExperimentsTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @org.junit.Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;
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
    private RetargetingConditionService retargetingConditionService;

    @Autowired
    private Steps steps;

    private TextCampaign textCampaign;
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
    public void add_SaveExperiments_SavedSuccessfully(@SuppressWarnings("unused") String testName,
                                                      List<Goal> goals,
                                                      List<Long> newSectionIds,
                                                      List<Long> newAbSegmentGoalIds,
                                                      List<Long> expectedSectionIds,
                                                      List<Long> expectedAbSegmentGoalIds) {
        initGoalsInMetrika(goals);

        textCampaign
                .withSectionIds(ifNotNull(newSectionIds, ArrayList::new))
                .withAbSegmentGoalIds(ifNotNull(newAbSegmentGoalIds, ArrayList::new));

        MassResult<Long> result = createOperationAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        TextCampaign actualCampaign = getCampaignFromResult(result);

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
                retargetingConditionRepository.getRetConditionsByCampaignIds(clientInfo.getShard(),
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
        createCampaign(defaultTextCampaign());
    }

    private void createCampaign(TextCampaign campaign) {
        this.textCampaign = campaign;
    }

    private TextCampaign getCampaignFromResult(MassResult<Long> result) {
        TextCampaign campaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                        List.of(result.get(0).getResult())).get(0);
        campaignId = campaign.getId();
        return campaign;
    }

    private MassResult<Long> createOperationAndApply() {

        RestrictedCampaignsAddOperation operation = new RestrictedCampaignsAddOperation(
                List.of(textCampaign),
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
}
