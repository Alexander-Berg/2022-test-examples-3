package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.cpmYndxFrontpageRetargetingsNotAllowed;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingConditionValidationServiceYndxFrontpageTest {

    @Autowired
    private Steps steps;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private UpdateRetargetingConditionValidationService2 validationUnderTest;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    private ClientInfo client;

    private RetConditionInfo createFrontpageRetargetingCondition() {
        CampaignInfo campaign = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign();
        client = campaign.getClientInfo();
        int shard = client.getShard();
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmYndxFrontpageAdGroup(campaign);

        RetConditionInfo retargetingCondition = createRetargetingCondition(new Rule().withType(RuleType.OR).withGoals(emptyList()));
        Retargeting retargeting = defaultRetargeting(campaign.getCampaignId(), adGroup.getAdGroupId(),
                retargetingCondition.getRetConditionId());
        retargetingRepository.add(shard, singletonList(retargeting));

        return retargetingCondition;
    }

    private void enableFrontpageProfile() {
        steps.featureSteps().addClientFeature(client.getClientId(), FeatureName.CPM_YNDX_FRONTPAGE_PROFILE, true);
    }

    private void disableFrontpageProfile() {
        steps.featureSteps().addClientFeature(client.getClientId(), FeatureName.CPM_YNDX_FRONTPAGE_PROFILE, false);
    }

    @Test
    public void cpmYndxFrontpageCampaign_WithRules_FeatureNoErrors() {
        RetConditionInfo retCondition = createFrontpageRetargetingCondition();
        enableFrontpageProfile();
        var demo = (Goal) defaultGoalByType(GoalType.SOCIAL_DEMO)
                .withCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
        testCryptaSegmentRepository.addAll(singletonList(demo));
        ModelChanges<RetargetingCondition> changes = withGoalsModelChanges(retCondition.getRetConditionId(),
                demo);

        ValidationResult<List<RetargetingCondition>, Defect> result = validate(retCondition.getRetCondition(), changes);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void cpmYndxFrontpageCampaign_WithRules_NoFeatureErrors() {
        RetConditionInfo retCondition = createFrontpageRetargetingCondition();
        disableFrontpageProfile();
        var demo = (Goal) defaultGoalByType(GoalType.SOCIAL_DEMO)
                .withCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
        testCryptaSegmentRepository.addAll(singletonList(demo));
        ModelChanges<RetargetingCondition> changes = withGoalsModelChanges(retCondition.getRetConditionId(),
                demo);

        ValidationResult<List<RetargetingCondition>, Defect> result = validate(retCondition.getRetCondition(), changes);
        assertThat(result.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(), cpmYndxFrontpageRetargetingsNotAllowed())));
    }

    private ModelChanges<RetargetingCondition> withGoalsModelChanges(Long id, Goal... goals) {
        Rule rule = new Rule()
                .withType(RuleType.OR)
                .withGoals(Arrays.asList(goals));
        metrikaHelperStub.addGoalsFromRules(client.getUid(), singletonList(rule));
        return new ModelChanges<>(id, RetargetingCondition.class)
                .process(singletonList(rule), RetargetingCondition.RULES);
    }

    private RetConditionInfo createRetargetingCondition(Rule rule) {
        RetargetingCondition retargetingCondition = defaultRetCondition(client.getClientId());
        retargetingCondition
                .withRules(singletonList(rule))
                .withType(interests);
        return steps.retConditionSteps().createRetCondition(retargetingCondition, client);
    }

    private ValidationResult<List<RetargetingCondition>, Defect> validate(RetargetingCondition retargetingCondition,
                                                                          ModelChanges<RetargetingCondition> changes) {
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> preMassValidation =
                new ValidationResult<>(singletonList(changes));
        AppliedChanges<RetargetingCondition> appliedChanges = changes.applyTo(retargetingCondition);

        return validationUnderTest.validate(preMassValidation, singletonList(retargetingCondition),
                singletonList(appliedChanges), client.getClientId(), client.getShard());
    }
}
