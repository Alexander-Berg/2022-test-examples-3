package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.HashSet;
import java.util.List;
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
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl;
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
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultABSegmentRetCondition;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithBrandLiftAddValidationTypeSupportTest {
    private static final int SHARD = 1;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private CampaignWithBrandLiftAddValidationTypeSupport typeSupport;

    @Autowired
    private Steps steps;

    private static final List<Long> METRIKA_COUNTERS =
            List.of(nextPositiveLong());

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
        operatorUid = nextPositiveLong();

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultClient.getUid()), Set.of());

        container = new RestrictedCampaignsAddOperationContainerImpl(SHARD, operatorUid, clientId,
                defaultClient.getUid(), defaultClient.getChiefUserInfo().getUid(), null,
                new CampaignOptions(), metrikaClientAdapter, emptyMap());

        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT, true);
        steps.featureSteps().addClientFeature(clientId, FeatureName.AB_SEGMENTS, true);

        sectionIds = List.of(nextPositiveLong(), nextPositiveLong());
        abSegmentGoalIds = List.of(nextPositiveLong(), nextPositiveLong(), nextPositiveLong());

        List<Goal> goals = List.of(defaultABSegmentGoal(), defaultABSegmentGoal());

        sectionIds = mapList(goals, Goal::getSectionId);
        abSegmentGoalIds = mapList(goals, Goal::getId);

        metrikaClientStub.addGoals(defaultClient.getUid(), new HashSet<>(goals));
        metrikaHelperStub.addGoalIds(defaultClient.getUid(), listToSet(goals, GoalBase::getId));
    }

    @Test
    public void preValitate_Success() {
        var campaign = createCampaign();

        campaign
                .withAbSegmentGoalIds(List.of(nextPositiveLong()))
                .withSectionIds(List.of(nextPositiveLong()))
                .withBrandSurveyId("brand_survey_id");
        ValidationResult<List<CampaignWithBrandLift>, Defect> vr = new ValidationResult<>(List.of(campaign));

        vr = typeSupport.preValidate(container, vr);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValitate_NonNull_WhenFeaturesOff_Error() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.AB_SEGMENTS, false);
        steps.featureSteps().addClientFeature(clientId, FeatureName.BRAND_LIFT, false);

        var campaign = createCampaign();

        campaign
                .withAbSegmentGoalIds(List.of(nextPositiveLong()))
                .withSectionIds(List.of(nextPositiveLong()))
                .withBrandSurveyId("brand_survey_id")
                .withIsBrandLiftHidden(true);

        ValidationResult<List<CampaignWithBrandLift>, Defect> vr = new ValidationResult<>(List.of(campaign));

        vr = typeSupport.preValidate(container, vr);

        assertThat(vr, allOf(
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CampaignWithBrandLift.SECTION_IDS)), isNull())),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)), isNull())),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CampaignWithBrandLift.BRAND_SURVEY_ID)), isNull())),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CampaignWithBrandLift.IS_BRAND_LIFT_HIDDEN)), invalidValue()))
        ));
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

    private ValidationResult<List<CampaignWithBrandLift>, Defect> validate(CampaignWithBrandLift campaign) {
        return typeSupport.validate(
                container,
                ListValidationBuilder.of(singletonList(campaign), Defect.class).getResult());
    }

    private CampaignWithBrandLift createCampaign() {
        return new CpmBannerCampaign()
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withUid(operatorUid);
    }
}
