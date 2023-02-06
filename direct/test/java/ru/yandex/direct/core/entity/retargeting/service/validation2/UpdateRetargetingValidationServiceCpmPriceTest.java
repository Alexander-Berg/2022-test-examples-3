package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.math.BigDecimal;
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

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.markupcondition.repository.MarkupConditionRepository;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.getPackagePriceFunctionByCampaignId;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.core.testing.data.TestPricePackages.LTV;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingValidationServiceCpmPriceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private PricePackageRepository pricePackageRepository;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private UpdateRetargetingValidationService updateRetargetingValidationService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private MarkupConditionRepository markupConditionRepository;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    @Test
    public void validGoalType() {
        PricePackage pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(clientInfo)
                .getPricePackage();
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);

        RetConditionInfo oldRetargetingCondition = createRetargetingConditionWithRules(validRulesForCampaign(campaign));

        List<Rule> newRules = new ArrayList<>();
        newRules.add(new Rule()
                .withType(RuleType.OR)
                .withGoals(List.of(defaultGoalByType(GoalType.FAMILY))));
        newRules.addAll(validRulesForCampaign(campaign));
        RetConditionInfo newRetargetingCondition = createRetargetingConditionWithRules(newRules);

        Retargeting retargeting = defaultRetargeting(campaign.getId(), adGroup.getId(),
                oldRetargetingCondition.getRetConditionId())
                .withPriceContext(pricePackage.getPrice());
        retargetingRepository.add(shard, singletonList(retargeting));

        retargeting.withRetargetingConditionId(newRetargetingCondition.getRetConditionId());

        ValidationResult<List<Retargeting>, Defect> result = validate(retargeting, campaign, adGroup,
                newRetargetingCondition);
        Assert.assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void suspendedRetargeting() {
        PricePackage pricePackage = steps.pricePackageSteps().createApprovedPricePackageWithClients(clientInfo)
                .getPricePackage();
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, clientInfo);

        RetConditionInfo retargetingCondition = createRetargetingConditionWithRules(validRulesForCampaign(campaign));

        Retargeting retargeting = defaultRetargeting(campaign.getId(), adGroup.getId(),
                retargetingCondition.getRetConditionId())
                .withPriceContext(pricePackage.getPrice());
        retargetingRepository.add(shard, singletonList(retargeting));

        retargeting.withIsSuspended(true);

        ValidationResult<List<Retargeting>, Defect> result = validate(retargeting, campaign, adGroup, retargetingCondition);
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

    private ValidationResult<List<Retargeting>, Defect> validate(Retargeting retargetings, CpmPriceCampaign campaign,
            CpmYndxFrontpageAdGroup adGroup, RetConditionInfo retCondition) {
        ValidationResult<List<Retargeting>, Defect> validationResult = new ValidationResult<>(List.of(retargetings));
        var campaignsType = campaignRepository.getCampaignsTypeMap(shard, List.of(campaign.getId()));
        var packagePriceFunctionByCampaignId = getPackagePriceFunctionByCampaignId(shard, campaignTypedRepository,
                pricePackageRepository, markupConditionRepository, campaignsType);
        Map<Long, AdGroupSimple> adGroupsById = adGroupRepository.getAdGroupSimple(shard, clientId,
                List.of(retargetings.getAdGroupId()));
        BigDecimal price = packagePriceFunctionByCampaignId.get(campaign.getId())
                .apply(adGroup.getGeo(), mapList(retCondition.getRetCondition().collectGoals(), ModelWithId::getId),
                        adGroup.getProjectParamConditions()).getPrice();
        Map<Long, BigDecimal> pricePackagePriceByAdGroupId = Map.of(adGroup.getId(), price);
        return updateRetargetingValidationService.validate(validationResult, adGroupsById, emptyMap(), clientId, shard,
                false, pricePackagePriceByAdGroupId);
    }
}
