package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRule;
import static ru.yandex.direct.feature.FeatureName.TEXT_BANNER_INTERESTS_RET_COND_ENABLED;
import static ru.yandex.direct.feature.FeatureName.TGO_ALL_INTERESTS_IN_USER_PROFILE;
import static ru.yandex.direct.feature.FeatureName.TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE;
import static ru.yandex.direct.feature.FeatureName.TGO_METRIKA_AND_AUDIENCE_IN_USER_PROFILE;
import static ru.yandex.direct.feature.FeatureName.TGO_SOCIAL_DEMO_IN_USER_PROFILE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class AddRetargetingValidationServiceInUserProfileForTgoTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AddRetargetingValidationService addRetargetingValidationService;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public GoalType goalType1;

    @Parameterized.Parameter(2)
    public CryptaInterestType cryptaInterestType1;

    @Parameterized.Parameter(3)
    public GoalType goalType2;

    @Parameterized.Parameter(4)
    public CryptaInterestType cryptaInterestType2;

    @Parameterized.Parameter(5)
    public Boolean tgoSocialDemoInUserProfileFeature;

    @Parameterized.Parameter(6)
    public Boolean tgoFamilyAndBehaviorsInUserProfileFeature;

    @Parameterized.Parameter(7)
    public Boolean tgoAllInterestsInUserProfileFeature;

    @Parameterized.Parameter(8)
    public Boolean tgoMetrikaAndAudienceInUserProfileFeature;

    @Parameterized.Parameter(9)
    public Matcher<DefectInfo<Defect>> error;

    private AdGroupInfo defaultAdGroupInfo;
    private int shard;
    private long operatorId;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Map<Long, AdGroupSimple> existingAdGroups;

    @Parameterized.Parameters(name = "{0} {1} {2} {3}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Позитивный тест: short_term INTERESTS разрешены всегда",
                        GoalType.INTERESTS, CryptaInterestType.short_term, null, null, false, false, false, false, null},
                {"SOCIAL_DEMO при выключенной TGO_SOCIAL_DEMO_IN_USER_PROFILE и включенной TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE",
                        GoalType.SOCIAL_DEMO, null, null, null, false, true, false, false,
                        validationError(path(field("retargetingConditionId")),
                                RetargetingDefects.invalidRetargetingConditionInUserProfileInTgo())},
                {"SOCIAL_DEMO при включенной TGO_SOCIAL_DEMO_IN_USER_PROFILE",
                        GoalType.SOCIAL_DEMO, null, null, null, true, false, false, false, null},
                {"FAMILY при выключенной фиче TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE и включенной TGO_SOCIAL_DEMO_IN_USER_PROFILE",
                        GoalType.FAMILY, null, null, null, true, false, false, false,
                        validationError(path(field("retargetingConditionId")),
                                RetargetingDefects.invalidRetargetingConditionInUserProfileInTgo())},
                {"FAMILY при включенной фиче TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE",
                        GoalType.FAMILY, null, null, null, false, true, false, false, null},
                {"BEHAVIORS при выключенной фичу TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE",
                        GoalType.BEHAVIORS, null, null, null, false, false, false, false,
                        validationError(path(field("retargetingConditionId")),
                                RetargetingDefects.invalidRetargetingConditionInUserProfileInTgo())},
                {"BEHAVIORS при включенной фичу TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE",
                        GoalType.BEHAVIORS, null, null, null, false, true, false, false, null},
                {"Разрешены только цели с типом INTERESTS, FAMILY, BEHAVIORS и SOCIAL_DEMO",
                        GoalType.AUDIO_GENRES, null, null, null, true, true, false, false,
                        validationError(path(field("retargetingConditionId")),
                                RetargetingDefects.invalidRetargetingConditionInUserProfileInTgo())},
                {"Разрешены только краткосрочные INTERESTS цели",
                        GoalType.INTERESTS, CryptaInterestType.long_term, null, null, true, true, false, false,
                        validationError(path(field("retargetingConditionId")),
                                RetargetingDefects.invalidRetargetingConditionInUserProfileInTgo())},
                {"Разрешено только одно условие с типом ConditionType.interests short_term",
                        GoalType.INTERESTS, CryptaInterestType.short_term,
                        GoalType.INTERESTS, CryptaInterestType.short_term, true, true, false, false,
                        validationError(path(), RetargetingDefects.maxCollectionSizeUserProfileInAdGroup())},
                {"Разрешено только одно условие с типом ConditionType.interests long_term",
                        GoalType.INTERESTS, CryptaInterestType.long_term,
                        GoalType.INTERESTS, CryptaInterestType.long_term, true, true, true, false,
                        validationError(path(), RetargetingDefects.maxCollectionSizeUserProfileInAdGroup())},
                {"GOAL при включенной фиче TGO_METRIKA_AND_AUDIENCE_IN_USER_PROFILE",
                        GoalType.GOAL, null, null, null, false, true, false, true, null},
                {"GOAL при включенной фиче TGO_METRIKA_AND_AUDIENCE_IN_USER_PROFILE",
                        GoalType.GOAL, null, null, null, false, true, false, false,
                        validationError(path(field("retargetingConditionId")),
                                RetargetingDefects.invalidRetargetingConditionInUserProfileInTgo())},
        };
        return Arrays.asList(data);
    }

    @Before
    public void setup() {
        defaultAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        clientInfo = defaultAdGroupInfo.getClientInfo();
        shard = defaultAdGroupInfo.getShard();
        clientId = defaultAdGroupInfo.getClientId();
        operatorId = defaultAdGroupInfo.getUid();
        long adGroupId = defaultAdGroupInfo.getAdGroupId();
        existingAdGroups = adGroupRepository.getAdGroupSimple(shard, clientId, ImmutableList.of(adGroupId));

        steps.featureSteps().addClientFeature(clientId, TEXT_BANNER_INTERESTS_RET_COND_ENABLED, true);
    }

    private TargetInterest createTargetInterest(GoalType goalType, CryptaInterestType cryptaInterestType) {
        RetargetingCondition cryptaRetCondition = (RetargetingCondition) defaultCpmRetCondition()
                .withRules(singletonList(defaultRule(singletonList(defaultGoalByType(goalType)), cryptaInterestType)
                ));

        long cryptaRetConditionId = steps.retConditionSteps().createRetCondition(cryptaRetCondition, clientInfo)
                .getRetConditionId();

        return new TargetInterest()
                .withAdGroupId(defaultAdGroupInfo.getAdGroupId())
                .withCampaignId(defaultAdGroupInfo.getCampaignId())
                .withRetargetingConditionId(cryptaRetConditionId);
    }

    @Test
    public void validate() {
        steps.featureSteps().addClientFeature(clientId, TGO_SOCIAL_DEMO_IN_USER_PROFILE,
                tgoSocialDemoInUserProfileFeature);
        steps.featureSteps().addClientFeature(clientId, TGO_FAMILY_AND_BEHAVIORS_IN_USER_PROFILE,
                tgoFamilyAndBehaviorsInUserProfileFeature);
        steps.featureSteps().addClientFeature(clientId, TGO_ALL_INTERESTS_IN_USER_PROFILE,
                tgoAllInterestsInUserProfileFeature);
        steps.featureSteps().addClientFeature(clientId, TGO_METRIKA_AND_AUDIENCE_IN_USER_PROFILE,
                tgoMetrikaAndAudienceInUserProfileFeature);

        TargetInterest targetInterest1 = createTargetInterest(goalType1, cryptaInterestType1);

        List<TargetInterest> retargetings;
        if (goalType2 == null) {
            retargetings = ImmutableList.of(targetInterest1);
        } else {
            TargetInterest targetInterest2 = createTargetInterest(goalType2, cryptaInterestType2);
            retargetings = ImmutableList.of(targetInterest1, targetInterest2);
        }

        ValidationResult<List<TargetInterest>, Defect> actual = addRetargetingValidationService
                .validate(new ValidationResult<>(retargetings), emptyList(),
                        existingAdGroups, operatorId, clientId, shard);

        if (error == null) {
            assertThat(actual, hasNoErrors());
        } else {
            assertThat(actual.getSubResults().get(index(0)).flattenErrors(), contains(error));
        }
    }
}
