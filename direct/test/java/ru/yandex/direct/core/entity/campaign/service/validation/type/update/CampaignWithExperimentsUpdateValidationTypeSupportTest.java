package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithExperiments;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFullGoals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.core.validation.defects.RightsDefects;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithExperimentsUpdateValidationTypeSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignWithExperimentsUpdateValidationTypeSupport validationTypeSupport;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    private List<Goal> goals;
    private ClientInfo defaultClient;
    private List<Long> sectionIds;
    private List<Long> unexistedSectionIds;
    private List<Long> unexistedAbSegmentIds;
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;
    private RestrictedCampaignsUpdateOperationContainerImpl container;

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
        sectionIds = List.of(1L, 2L);
        unexistedSectionIds = List.of(10000L);
        unexistedAbSegmentIds = List.of(20000L);

        metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultClient.getUid()), Set.of());
        container = new RestrictedCampaignsUpdateOperationContainerImpl(
                defaultClient.getShard(), defaultClient.getUid(), defaultClient.getClientId(), defaultClient.getUid(),
                defaultClient.getChiefUserInfo().getUid(), metrikaClientAdapter, new CampaignOptions(), null,
                emptyMap());

        goals = List.of((Goal) TestFullGoals.defaultABSegmentGoal().withSectionId(sectionIds.get(0)),
                (Goal) TestFullGoals.defaultABSegmentGoal().withSectionId(sectionIds.get(0)),
                (Goal) TestFullGoals.defaultABSegmentGoal().withSectionId(sectionIds.get(1)));

        steps.featureSteps().addClientFeature(defaultClient.getClientId(),
                FeatureName.AB_SEGMENTS, true);

        metrikaClientStub.addGoals(defaultClient.getUid(), new HashSet<>(goals));
        metrikaHelperStub.addGoalIds(defaultClient.getUid(), listToSet(goals, GoalBase::getId));
    }

    @Test
    public void validate_experimentRetCondCreation_featureEnabled_success() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(),
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA, true);

        List<Long> abSegmentGoalIds = List.of(goals.get(0).getId());
        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType))
                .withSectionIds(sectionIds)
                .withAbSegmentGoalIds(abSegmentGoalIds);

        AppliedChanges<CampaignWithExperiments> appliedChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithExperiments.class)
                        .process(sectionIds, CampaignWithExperiments.SECTION_IDS)
                        .process(abSegmentGoalIds, CampaignWithExperiments.AB_SEGMENT_GOAL_IDS)
                        .applyTo(campaign);
        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                validationTypeSupport.validate(container, new ValidationResult<>(List.of(campaign)),
                        Map.of(0, appliedChanges));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_experimentRetCondCreation_featureEnabled_unexistedSectionIds_error() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(),
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA, true);

        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType));

        AppliedChanges<CampaignWithExperiments> appliedChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithExperiments.class)
                        .process(unexistedSectionIds, CampaignWithExperiments.SECTION_IDS)
                        .applyTo(campaign);

        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                validationTypeSupport.validate(container, new ValidationResult<>(List.of(campaign)),
                        Map.of(0, appliedChanges));

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithExperiments.SECTION_IDS), index(0)), CollectionDefects.inCollection())));
    }

    @Test
    public void validate_experimentRetCondCreation_featureEnabled_unexistedAbSegmentIds_error() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(),
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA, true);

        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType));

        AppliedChanges<CampaignWithExperiments> appliedChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithExperiments.class)
                        .process(unexistedAbSegmentIds, CampaignWithExperiments.AB_SEGMENT_GOAL_IDS)
                        .applyTo(campaign);

        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                validationTypeSupport.validate(container, new ValidationResult<>(List.of(campaign)),
                        Map.of(0, appliedChanges));

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithExperiments.AB_SEGMENT_GOAL_IDS), index(0)), CollectionDefects.inCollection())));
    }

    @Test
    public void validate_experimentRetCondCreation_featureDisabled_noSectionId_success() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(),
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA, false);

        CampaignWithExperiments campaign = (CampaignWithExperiments) newCampaignByCampaignType(campaignType);

        AppliedChanges<CampaignWithExperiments> appliedChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithExperiments.class)
                        .applyTo(campaign);

        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                validationTypeSupport.validate(container, new ValidationResult<>(List.of(campaign)),
                        Map.of(0, appliedChanges));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_experimentRetCondCreation_featureDisabled_existedSectionId_error() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(),
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA, false);

        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType));

        ValidationResult<List<ModelChanges<CampaignWithExperiments>>, Defect> vr = new ValidationResult<>(List.of(
                ModelChanges.build(campaign, CampaignWithExperiments.SECTION_IDS, sectionIds)
        ));

        vr = validationTypeSupport.preValidate(CampaignValidationContainer.create(defaultClient.getShard(),
                defaultClient.getUid(), defaultClient.getClientId()), vr);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithExperiments.SECTION_IDS)), RightsDefects.noRights())));
    }

    @Test
    public void preValidate_experimentRetCondCreation_featureDisabled_existedAbSegmentId_error() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(),
                FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA, false);

        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType));


        ValidationResult<List<ModelChanges<CampaignWithExperiments>>, Defect> vr = new ValidationResult<>(List.of(
                ModelChanges.build(campaign, CampaignWithExperiments.AB_SEGMENT_GOAL_IDS, List.of(goals.get(0).getId()))
        ));

        vr = validationTypeSupport.preValidate(CampaignValidationContainer.create(defaultClient.getShard(),
                defaultClient.getUid(), defaultClient.getClientId()), vr);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
                field(CampaignWithExperiments.AB_SEGMENT_GOAL_IDS)), RightsDefects.noRights())));
    }
}
