package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingAdGroupInfo;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.core.testing.data.TestGroups.activeSpecificAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestPricePackages.LTV;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddRetargetingValidationServiceCpmPriceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @Autowired
    private AddRetargetingValidationService validationService;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private long operatorId;
    private int shard;

    @Before
    public void setup() {
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        operatorId = clientInfo.getUid();
        shard = clientInfo.getShard();
    }

    @Test
    public void existentAdGroup_validGoalType() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign,
                clientInfo);

        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule()
                .withType(RuleType.OR)
                .withGoals(List.of(defaultGoalByType(GoalType.FAMILY))));
        rules.addAll(validRulesForCampaign(campaign));
        RetConditionInfo retConditionInfo = createRetargetingConditionWithRules(rules);

        TargetInterest retargeting = new TargetInterest()
                .withAdGroupId(adGroup.getId())
                .withRetargetingConditionId(retConditionInfo.getRetConditionId());

        ValidationResult<List<TargetInterest>, Defect> result = validationService
                .validate(new ValidationResult<>(List.of(retargeting)), emptyList(),
                        singletonMap(adGroup.getId(), adGroup),
                        operatorId, clientId, shard);

        Assert.assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void nonexistentAdGroup_validGoalType() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = activeSpecificAdGroupForPriceSales(campaign);
        long adGroupFakeId = -1L;

        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule()
                .withType(RuleType.OR)
                .withGoals(List.of(defaultGoalByType(GoalType.FAMILY))));
        rules.addAll(validRulesForCampaign(campaign));
        RetConditionInfo retConditionInfo = createRetargetingConditionWithRules(rules);

        TargetInterest retargeting = new TargetInterest()
                .withAdGroupId(adGroupFakeId)
                .withRetargetingConditionId(retConditionInfo.getRetConditionId());
        RetargetingAdGroupInfo retargetingAdGroupInfo =
                new RetargetingAdGroupInfo(adGroupFakeId, adGroup, campaign.getId(), campaign.getType(), null);

        ValidationResult<List<TargetInterest>, Defect> result = validationService.validateWithNonExistentAdGroups(
                new ValidationResult<>(List.of(retargeting)),
                Map.of(0, retargetingAdGroupInfo),
                List.of(retConditionInfo.getRetCondition()),
                false, clientId, shard);

        Assert.assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void suspendedRetargeting() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign,
                clientInfo);

        List<Rule> rules = validRulesForCampaign(campaign);
        RetConditionInfo retConditionInfo = createRetargetingConditionWithRules(rules);

        TargetInterest retargeting = new TargetInterest()
                .withIsSuspended(true)
                .withAdGroupId(adGroup.getId())
                .withRetargetingConditionId(retConditionInfo.getRetConditionId());

        ValidationResult<List<TargetInterest>, Defect> result = validationService
                .validate(new ValidationResult<>(List.of(retargeting)), emptyList(),
                        singletonMap(adGroup.getId(), adGroup),
                        operatorId, clientId, shard);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(index(0), field(TargetInterest.IS_SUSPENDED)),
                        invalidValue())
        )));
    }

    private List<Rule> validRulesForCampaign(CpmPriceCampaign campaign) {
        var rules = List.of(LTV).stream()
                .map(behaviorGoalId -> new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of(defaultGoalByTypeAndId(behaviorGoalId, GoalType.BEHAVIORS))))
                .collect(Collectors.toList());
        metrikaHelperStub.addGoalsFromRules(campaign.getUid(), rules);
        return rules;
    }

    private RetConditionInfo createRetargetingConditionWithRules(List<Rule> rules) {
        var retargetingCondition = (RetargetingCondition) defaultRetCondition(clientId)
                .withType(ConditionType.interests)
                .withRules(rules);
        return steps.retConditionSteps().createRetCondition(retargetingCondition, clientInfo);
    }
}
