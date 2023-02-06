package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandLift;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.brandLiftCantBeChanged;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.brandLiftExperimentSegmentsCantBeChanged;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultABSegmentRetCondition;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithBrandLiftUpdateValidationTypeSupportTest {
    private static final int SHARD = 1;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    //@Autowired
    private MetrikaClientStub metrikaClientStub = new MetrikaClientStub();
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    //@Autowired
    private MetrikaHelperStub metrikaHelperStub = spy(new MetrikaHelperStub());

    @Autowired
    private CampaignWithBrandLiftUpdateValidationTypeSupport typeSupport;

    @Autowired
    private Steps steps;

    private static final List<Long> METRIKA_COUNTERS = List.of(nextPositiveLong());

    private ClientInfo defaultClient;
    private ClientId clientId;
    private long campaignId;
    private long operatorUid;
    private CampaignValidationContainer container;
    private List<Long> sectionIds;
    private List<Long> abSegmentGoalIds;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();

        clientId = defaultClient.getClientId();
        campaignId = nextPositiveLong();
        operatorUid = defaultClient.getUid();

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub, List.of(defaultClient.getUid()), Set.of());

        container = new RestrictedCampaignsUpdateOperationContainerImpl(SHARD, operatorUid, clientId,
                defaultClient.getUid(), defaultClient.getChiefUserInfo().getUid(),
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());

        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT, true);
        steps.featureSteps().addClientFeature(clientId, FeatureName.AB_SEGMENTS, true);
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT_HIDDEN, false);

        List<Goal> goals = List.of(defaultABSegmentGoal(), defaultABSegmentGoal());

        sectionIds = mapList(goals, Goal::getSectionId);
        abSegmentGoalIds = mapList(goals, Goal::getId);

        metrikaClientStub.addGoals(defaultClient.getUid(), new HashSet<>(goals));
        metrikaHelperStub.addGoalIds(defaultClient.getUid(), listToSet(goals, GoalBase::getId));
    }

    @Test
    public void preValitate_Success() {
        var campaign = createCampaign();

        ValidationResult<List<ModelChanges<CampaignWithBrandLift>>, Defect> vr = new ValidationResult<>(List.of(
                ModelChanges.build(campaign, CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS, List.of(nextPositiveLong())),
                ModelChanges.build(campaign, CampaignWithBrandLift.SECTION_IDS, List.of(nextPositiveLong()))
        ));

        vr = typeSupport.preValidate(container, vr);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValitate_ForbiddenToChange() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.AB_SEGMENTS, false);

        var campaign = createCampaign();

        ValidationResult<List<ModelChanges<CampaignWithBrandLift>>, Defect> vr = new ValidationResult<>(List.of(
                ModelChanges.build(campaign, CampaignWithBrandLift.SECTION_IDS, List.of(nextPositiveLong())),
                ModelChanges.build(campaign, CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS, List.of(nextPositiveLong())),
                ModelChanges.build(campaign, CampaignWithBrandLift.IS_BRAND_LIFT_HIDDEN, false)
        ));

        vr = typeSupport.preValidate(container, vr);

        assertThat(vr, allOf(
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CampaignWithBrandLift.SECTION_IDS)), forbiddenToChange())),
                hasDefectWithDefinition(validationError(path(index(1),
                        field(CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)),
                        forbiddenToChange())),
                hasDefectWithDefinition(validationError(path(index(2),
                        field(CampaignWithBrandLift.IS_BRAND_LIFT_HIDDEN)),
                        forbiddenToChange()))
        ));
    }

    @Test
    public void validateBeforeApply_EmptyBrandLiftSurveyId_NoChange_Success() {
        CampaignWithBrandLift campaign = createCampaign();

        var modelChanges = new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class);
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_EmptyBrandLiftSurveyId_ChangeToNull_Success() {
        CampaignWithBrandLift campaign = createCampaign();

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, null);
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_EmptyBrandLiftSurveyId_FeatureEnabled_Change_Success() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_EmptyBrandLiftSurveyId_FeatureDisabled_Change_Error() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW);


        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT, false);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithBrandLift.BRAND_SURVEY_ID)), noRights())));
    }

    @Test
    public void validateBeforeApply_ExistedBrandLiftSurveyId_ChangeToNull_Success() {
        CampaignWithBrandLift campaign = createCampaign()
                .withBrandSurveyId("newBrandSurveyId")
                .withStatusModerate(CampaignStatusModerate.NEW);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, null);
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_ExistedBrandLiftSurveyId_ChangeToNull_isBrandLiftHidden_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT_HIDDEN, true);
        CampaignWithBrandLift campaign = createCampaign()
                .withBrandSurveyId("newBrandSurveyId")
                .withStatusModerate(CampaignStatusModerate.NEW);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, null);
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_ExistedBrandLiftSurveyId_ChangeToEqual_Success() {
        CampaignWithBrandLift campaign = createCampaign()
                .withBrandSurveyId("newBrandSurveyId");

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_ExistedBrandLiftSurveyId_ChangeToEqual_isBrandLiftHidden_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT_HIDDEN, true);
        CampaignWithBrandLift campaign = createCampaign()
                .withBrandSurveyId("newBrandSurveyId");

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }


    @Test
    public void validateBeforeApply_ExistedBrandLiftSurveyId_ChangeToAnother_Success() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withBrandSurveyId("newBrandSurveyId")
                .withIsBrandLiftHidden(true);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "anotherBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_CampaignShouldBeDraft_isBrandLiftHidden_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT_HIDDEN, true);
        CampaignWithBrandLift campaign = createCampaign();

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_BrandLiftWasHiddenAndChangedByClient_Success() {
        CampaignWithBrandLift campaign = createCampaign()
                .withBrandSurveyId("newBrandSurveyId")
                .withIsBrandLiftHidden(true);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS, List.of(10L));
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_BrandLiftWasHiddenAndChangedAbSegmentsByOperator_Error() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT_HIDDEN, true);
        CampaignWithBrandLift campaign = createCampaign()
                .withBrandSurveyId("newBrandSurveyId")
                .withIsBrandLiftHidden(true);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS, List.of(10L));
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)),
                brandLiftExperimentSegmentsCantBeChanged())));
    }

    @Test
    public void validateBeforeApply_BrandLiftWasHiddenAndChangedSectionIdsByOperator_Error() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT_HIDDEN, true);
        CampaignWithBrandLift campaign = createCampaign()
                .withBrandSurveyId("newBrandSurveyId")
                .withIsBrandLiftHidden(true);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.SECTION_IDS, List.of(10L));
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithBrandLift.SECTION_IDS)),
                brandLiftExperimentSegmentsCantBeChanged())));
    }

    @Test
    public void validateBeforeApply_AddBrandLiftToShownCampaign_Error() {
        CampaignWithBrandLift campaign = createCampaign().withShows(1L);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                        field(CampaignWithBrandLift.BRAND_SURVEY_ID)),
                brandLiftCantBeChanged())));
    }

    @Test
    public void validateBeforeApply_ChangeBrandLiftInShownCampaign_Error() {
        CampaignWithBrandLift campaign = createCampaign()
                .withShows(1L)
                .withBrandSurveyId("existingBrandSurveyId");

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                        field(CampaignWithBrandLift.BRAND_SURVEY_ID)),
                brandLiftCantBeChanged())));
    }

    @Test
    public void validateBeforeApply_BrandLiftCanBeAddedToNotDraftCampaign() {
        CampaignWithBrandLift campaign = createCampaign().withStatusModerate(CampaignStatusModerate.SENT);

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_BrandLiftCanBeChangedInNotDraftCampaign() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.SENT)
                .withBrandSurveyId("existingBrandSurveyId");

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, "newBrandSurveyId");
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_BrandLiftCanBeDeletedInNotDraftCampaign() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.SENT)
                .withBrandSurveyId("existingBrandSurveyId");

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, null);
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_BrandLiftCanBeDeletedInShownCampaign() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.SENT)
                .withShows(1L)
                .withBrandSurveyId("existingBrandSurveyId");

        var modelChanges = ModelChanges.build(campaign, CampaignWithBrandLift.BRAND_SURVEY_ID, null);
        var vr = validateBeforeApply(campaign, modelChanges);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_Simple_Successfully() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withBrandSurveyId("newBrandSurveyId")
                .withMetrikaCounters(METRIKA_COUNTERS);

        ValidationResult<List<CampaignWithBrandLift>, Defect> vr = validate(campaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_Simple_IsBrandLiftHidden_Successfully() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT_HIDDEN, true);
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withBrandSurveyId("newBrandSurveyId")
                .withMetrikaCounters(METRIKA_COUNTERS)
                .withIsBrandLiftHidden(true);

        ValidationResult<List<CampaignWithBrandLift>, Defect> vr = validate(campaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_WithSectionAndSegments_Successfully() {
        RetargetingCondition retargetingCondition = defaultABSegmentRetCondition(clientId);
        Rule rule = retargetingCondition.getRules().get(0);
        Goal goal = rule.getGoals().get(0);

        rule.setSectionId(sectionIds.get(0));
        goal.setId(abSegmentGoalIds.get(0));

        RetConditionInfo retConditionInfo = steps.retConditionSteps().createRetCondition(retargetingCondition,
                defaultClient);

        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withBrandSurveyId("newBrandSurveyId")
                .withSectionIds(List.of(sectionIds.get(0)))
                .withAbSegmentGoalIds(List.of(abSegmentGoalIds.get(0)))
                .withAbSegmentRetargetingConditionId(retConditionInfo.getRetConditionId())
                .withMetrikaCounters(METRIKA_COUNTERS);

        ValidationResult<List<CampaignWithBrandLift>, Defect> vr = validate(campaign);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_WithSectionAndSegments_UnknownSectionId_Error() {
        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withBrandSurveyId("newBrandSurveyId")
                .withSectionIds(List.of(sectionIds.get(0), nextPositiveLong()))
                .withAbSegmentGoalIds(List.of(abSegmentGoalIds.get(0)))
                .withMetrikaCounters(METRIKA_COUNTERS);

        ValidationResult<List<CampaignWithBrandLift>, Defect> vr = validate(campaign);
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithBrandLift.SECTION_IDS), index(1)),
                inCollection())));
    }

    @Test
    public void validate_WithSectionAndSegments_TurnOffAbSegment_Error() {
        RetargetingCondition retargetingCondition = defaultABSegmentRetCondition(clientId);
        Rule rule = retargetingCondition.getRules().get(0);
        Goal goal = rule.getGoals().get(0);

        rule.setSectionId(sectionIds.get(0));
        goal.setId(abSegmentGoalIds.get(0));

        RetConditionInfo retConditionInfo = steps.retConditionSteps().createRetCondition(retargetingCondition,
                defaultClient);

        CampaignWithBrandLift campaign = createCampaign()
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withBrandSurveyId("newBrandSurveyId")
                .withSectionIds(List.of(sectionIds.get(0)))
                .withAbSegmentGoalIds(List.of(abSegmentGoalIds.get(1)))
                .withAbSegmentRetargetingConditionId(retConditionInfo.getRetConditionId())
                .withMetrikaCounters(METRIKA_COUNTERS);

        ValidationResult<List<CampaignWithBrandLift>, Defect> vr = validate(campaign);
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)),
                brandLiftExperimentSegmentsCantBeChanged())));
    }

    private ValidationResult<List<ModelChanges<CampaignWithBrandLift>>, Defect> validateBeforeApply(
            CampaignWithBrandLift campaign,
            ModelChanges<CampaignWithBrandLift> modelChanges) {
        Map<Long, CampaignWithBrandLift> campaignsById = listToMap(List.of(campaign),
                CampaignWithBrandLift::getId);

        return typeSupport.validateBeforeApply(
                container,
                ListValidationBuilder.of(singletonList(modelChanges), Defect.class).getResult(),
                campaignsById);
    }

    private ValidationResult<List<CampaignWithBrandLift>, Defect> validate(CampaignWithBrandLift campaign) {
        return typeSupport.validate(
                container,
                ListValidationBuilder.of(singletonList(campaign), Defect.class).getResult(),
                emptyMap());
    }

    private CampaignWithBrandLift createCampaign() {
        return new CpmBannerCampaign()
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withUid(operatorUid)
                .withIsBrandLiftHidden(false)
                .withSumSpent(BigDecimal.ZERO);
    }
}
