package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingConditionValidationServiceCpmPriceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private UpdateRetargetingConditionValidationService2 validationUnderTest;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private long operatorId;
    private int shard;

    @Before
    public void setup() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        operatorId = clientInfo.getUid();
        shard = clientInfo.getShard();
    }

    @Test
    public void validGoalType() {
        RetargetingCondition retCondition = createRetargetingCondition();
        ModelChanges<RetargetingCondition> changes = withGoalsModelChanges(retCondition.getId(),
                defaultGoalByType(GoalType.FAMILY));

        ValidationResult<List<RetargetingCondition>, Defect> result = validate(retCondition, changes);

        assertThat(result, hasNoDefectsDefinitions());
    }

    private RetargetingCondition createRetargetingCondition() {
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);

        RetargetingCondition retargetingCondition = defaultRetCondition(clientId);
        retargetingCondition
                .withRules(List.of(new Rule().withType(RuleType.OR).withGoals(emptyList())))
                .withType(interests);
        steps.retConditionSteps().createRetCondition(retargetingCondition, clientInfo);

        Retargeting retargeting = defaultRetargeting(campaign.getId(), adGroup.getId(),
                retargetingCondition.getId());
        retargetingRepository.add(shard, singletonList(retargeting));

        return retargetingCondition;
    }

    private ModelChanges<RetargetingCondition> withGoalsModelChanges(Long id, Goal... goals) {
        Rule rule = new Rule()
                .withType(RuleType.OR)
                .withGoals(Arrays.asList(goals));
        var cryptaGoals = filterList(rule.getGoals(), g -> g.getType().isCrypta());
        cryptaGoals.forEach(g -> g.setCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD)));
        testCryptaSegmentRepository.addAll(cryptaGoals);
        metrikaHelperStub.addGoalsFromRules(operatorId, singletonList(rule));
        return new ModelChanges<>(id, RetargetingCondition.class)
                .process(singletonList(rule), RetargetingCondition.RULES);
    }

    private ValidationResult<List<RetargetingCondition>, Defect> validate(RetargetingCondition retargetingCondition,
                                                                          ModelChanges<RetargetingCondition> changes) {
        ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> preMassValidation =
                new ValidationResult<>(singletonList(changes));
        AppliedChanges<RetargetingCondition> appliedChanges = changes.applyTo(retargetingCondition);

        return validationUnderTest.validate(preMassValidation, singletonList(retargetingCondition),
                singletonList(appliedChanges), clientId, shard);
    }
}
