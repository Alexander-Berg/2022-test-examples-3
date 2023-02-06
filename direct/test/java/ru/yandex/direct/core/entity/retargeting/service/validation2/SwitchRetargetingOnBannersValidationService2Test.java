package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.retargeting.container.SwitchRetargeting;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.core.testing.data.TestPricePackages.LTV;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SwitchRetargetingOnBannersValidationService2Test {

    private static final long NON_EXISTING_RET_COND_ID = Long.MAX_VALUE;

    @Autowired
    SwitchRetargetingOnBannersValidationService2 switchRetargetingOnBannersValidationService2;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    private ClientId clientId;
    private ClientInfo clientInfo;
    private int shard;
    private RetConditionInfo retCond;

    @Before
    public void before() {
        retCond = steps.retConditionSteps().createDefaultRetCondition();
        clientInfo = retCond.getClientInfo();
        clientId = retCond.getClientId();
        shard = retCond.getShard();
    }

    @Test
    public void success() {
        List<SwitchRetargeting> switchRetargetings =
                Collections
                        .singletonList(
                                new SwitchRetargeting().withRetCondId(retCond.getRetConditionId()).withSuspended(true));
        ValidationResult<List<SwitchRetargeting>, Defect>
                result = switchRetargetingOnBannersValidationService2.validate(switchRetargetings, clientId);
        assertThat("Ошибок нет в соответствии с ожиданиями", result, hasNoDefectsDefinitions());
    }

    @Test
    public void notExistRetargetingCondition() {
        List<SwitchRetargeting> switchRetargetings =
                Collections.singletonList(new SwitchRetargeting().withRetCondId(1111111L).withSuspended(true));
        ValidationResult<List<SwitchRetargeting>, Defect>
                result = switchRetargetingOnBannersValidationService2.validate(switchRetargetings, clientId);
        assertThat("Ошибка соответствует ожиданиям",
                result,
                hasDefectDefinitionWith(validationError(DefectIds.OBJECT_NOT_FOUND))
        );
    }

    @Test
    public void bothRetargetingConditionsNotExist_secondHasError() {
        List<SwitchRetargeting> switchRetargetings = List.of(
                new SwitchRetargeting().withRetCondId(null).withSuspended(true),
                new SwitchRetargeting().withRetCondId(NON_EXISTING_RET_COND_ID).withSuspended(true)
        );

        ValidationResult<List<SwitchRetargeting>, Defect>
                result = switchRetargetingOnBannersValidationService2.validate(switchRetargetings, clientId);
        assertThat("Ошибка соответствует ожиданиям",
                result.flattenErrors(),
                hasSize(2)
        );
    }

    @Test
    public void retargetingConditionInCpmPriceCampaign() {
        PricePackage pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(clientInfo)
                .getPricePackage();
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);
        RetConditionInfo retConditionInfo = createRetargetingConditionWithRules(validRulesForCampaign(campaign));

        Retargeting retargeting = defaultRetargeting(campaign.getId(), adGroup.getId(),
                retConditionInfo.getRetConditionId())
                .withPriceContext(pricePackage.getPrice());
        retargetingRepository.add(shard, singletonList(retargeting));

        List<SwitchRetargeting> switchRetargetings =
                Collections
                        .singletonList(new SwitchRetargeting().withRetCondId(retConditionInfo.getRetConditionId())
                                .withSuspended(true));
        ValidationResult<List<SwitchRetargeting>, Defect>
                result = switchRetargetingOnBannersValidationService2.validate(switchRetargetings, clientId);
        assertThat("Ошибка соответствует ожиданиям",
                result,
                hasDefectDefinitionWith(validationError(path(index(0), field(SwitchRetargeting.IS_SUSPENDED)),
                        DefectIds.INVALID_VALUE))
        );
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
