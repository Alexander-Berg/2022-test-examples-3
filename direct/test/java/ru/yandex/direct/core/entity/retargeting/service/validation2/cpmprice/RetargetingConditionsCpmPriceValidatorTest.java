package ru.yandex.direct.core.entity.retargeting.service.validation2.cpmprice;

import java.util.List;
import java.util.Map;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupPriceSales;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PriceRetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidByDefaultAdGroup;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidForPricePackage;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_18_24;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_25_34;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_35_44;
import static ru.yandex.direct.core.testing.data.TestPricePackages.C1_INCOME_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.FEMALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MID_INCOME_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.SCIENCE_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.anotherPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.ltvGoal;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.ltvRule;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.ruleOrSocialDemo;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionsCpmPriceValidatorTest {

    @Autowired
    private Steps steps;

    private ClientInfo client;
    private Goal behaviorGoalFromPackage;
    private PricePackage pricePackageYndxFrontpage;
    private PricePackage pricePackageVideo;

    @Before
    public void before() {
        client = steps.clientSteps().createDefaultClient();

        behaviorGoalFromPackage = ltvGoal();

        pricePackageYndxFrontpage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom()
                        .withRetargetingCondition(new PriceRetargetingCondition()
                                .withCryptaSegments(
                                List.of(AGE_18_24, AGE_25_34, AGE_35_44, MALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID))
                                .withAllowMetrikaSegments(true)
                        )
                )
                .withClients(List.of(allowedPricePackageClient(client)));
        pricePackageYndxFrontpage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withCryptaSegments(List.of(behaviorGoalFromPackage.getId()))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackageYndxFrontpage);

        pricePackageVideo = anotherPricePackage();
        pricePackageVideo.getTargetingsFixed().setCryptaSegments(List.of(AGE_18_24, SCIENCE_GOAL_ID));
        pricePackageVideo.getTargetingsCustom().getRetargetingCondition()
                .withCryptaSegments(List.of(AGE_18_24, AGE_25_34, MALE_CRYPTA_GOAL_ID))
                .withAllowMetrikaSegments(true);
        steps.pricePackageSteps().createPricePackage(pricePackageVideo);
    }

    @Test
    public void defaultAdGroup_SuccessValidation() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageYndxFrontpage);
        var defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, client);

        RetargetingCondition retargetingCondition = defaultRetCondition(client.getClientId());
        retargetingCondition
                .withRules(List.of(new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of(behaviorGoalFromPackage))))
                .withType(interests);

        ValidationResult<?, Defect> result = validate(retargetingCondition, campaign, defaultAdGroup, pricePackageYndxFrontpage);

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void defaultAdGroup_GoalsNotAllowedByPackage() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageYndxFrontpage);
        var defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, client);

        RetargetingCondition retargetingCondition = defaultRetCondition(client.getClientId());
        retargetingCondition
                .withRules(List.of(new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of(defaultGoalByType(GoalType.BEHAVIORS)))))
                .withType(interests);

        ValidationResult<?, Defect> result = validate(retargetingCondition, campaign, defaultAdGroup, pricePackageYndxFrontpage);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                retargetingConditionIsInvalidForPricePackage()))));
    }

    @Test
    public void specificAdGroup_GoalTypeNotAllowed() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageYndxFrontpage);
        var specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, client);

        RetargetingCondition retargetingCondition = defaultRetCondition(client.getClientId());
        retargetingCondition
                .withRules(List.of(new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of(defaultGoalByType(GoalType.FAMILY)))))
                .withType(interests);
        var retargetingConditionDefault = defaultCpmRetCondition();
        retargetingConditionDefault//дефолтная (18-24 или 25-34 или 35-44) И (средний доход)
                .withRules(List.of(ruleOrSocialDemo(List.of(AGE_18_24, AGE_25_34, AGE_35_44)),
                        ruleOrSocialDemo(MID_INCOME_GOAL_ID),
                        ltvRule()));

        ValidationResult<?, Defect> result = validate(retargetingCondition, campaign, specificAdGroup,
                pricePackageYndxFrontpage, retargetingConditionDefault);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                retargetingConditionIsInvalidByDefaultAdGroup()))));
    }

    private static ValidationResult<?, Defect> validate(RetargetingCondition retargetingCondition,
                                                        CpmPriceCampaign campaign,
                                                        AdGroupPriceSales adGroup, PricePackage pricePackage) {
        return validate(retargetingCondition, campaign, adGroup, pricePackage, null);
    }

    private static ValidationResult<?, Defect> validate(RetargetingCondition retargetingCondition,
                                                        CpmPriceCampaign campaign,
                                                        AdGroupPriceSales adGroup, PricePackage pricePackage,
                                                        RetargetingCondition defaultAdGroupRetargetingCondition) {
        retargetingCondition.withId(1L);
        var data = new RetargetingConditionsCpmPriceValidationData(
                Map.of(campaign.getId(), campaign),
                Map.of(campaign.getId(), pricePackage),
                defaultAdGroupRetargetingCondition == null ? emptyMap() :
                        Map.of(campaign.getId(), defaultAdGroupRetargetingCondition),
                Map.of(adGroup.getId(), campaign.getId()),
                Map.of(adGroup.getId(), adGroup.getPriority()),
                Map.of(retargetingCondition.getId(), List.of(adGroup.getId())),
                Map.of(adGroup.getId(), adGroup.getType())
        );
        return new RetargetingConditionCpmPriceValidator(data).apply(retargetingCondition);
    }

    @Test
    @Description("Видео группа. Сохраняем разрешённые пакетом цели")
    public void videoAdGroup_SuccessValidation() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageVideo);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, client);
        var retargetingCondition = defaultCpmRetCondition();
        retargetingCondition
                .withRules(List.of(ruleOrSocialDemo(AGE_18_24), ruleOrSocialDemo(MALE_CRYPTA_GOAL_ID)));

        ValidationResult<?, Defect> result = validate(retargetingCondition, campaign, adGroup, pricePackageVideo);

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Description("Цели запрещены пакетом (женщины)")
    public void videoAdGroup_Error() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageVideo);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, client);
        var retargetingCondition = defaultCpmRetCondition();
        retargetingCondition
                .withRules(List.of(ruleOrSocialDemo(AGE_18_24), ruleOrSocialDemo(FEMALE_CRYPTA_GOAL_ID)));

        ValidationResult<?, Defect> result = validate(retargetingCondition, campaign, adGroup, pricePackageVideo);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                retargetingConditionIsInvalidForPricePackage()))));
    }

    @Test
    @Description("Отсутствует обязательная цель 18-24")
    public void videoAdGroup_fixedTarget_Error() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageVideo);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, client);
        var retargetingCondition = defaultCpmRetCondition();
        retargetingCondition
                .withRules(List.of(ruleOrSocialDemo(AGE_25_34), ruleOrSocialDemo(MALE_CRYPTA_GOAL_ID)));

        ValidationResult<?, Defect> result = validate(retargetingCondition, campaign, adGroup, pricePackageVideo);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                retargetingConditionIsInvalidForPricePackage()))));
    }

    @Test
    @Description("тесты на валидацию специфичной группы об дефолтную. Специфичная сужает")
    public void specificAdGroup_Goals_Ok() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageYndxFrontpage);
        var specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, client);

        var retargetingConditionDefault = defaultCpmRetCondition();
        retargetingConditionDefault//дефолтная (18-24 или 25-34 или 35-44) И (средний доход)
                .withRules(List.of(ruleOrSocialDemo(List.of(AGE_18_24, AGE_25_34, AGE_35_44)),
                        ruleOrSocialDemo(MID_INCOME_GOAL_ID),
                        ltvRule()));

        var retargetingConditionSpecific = defaultCpmRetCondition();
        retargetingConditionSpecific//в специфической 25-34 и средний доход и интересы животные
                .withRules(List.of(ruleOrSocialDemo(AGE_25_34),
                        ruleOrSocialDemo(MID_INCOME_GOAL_ID),
                        new Rule().withType(RuleType.OR).withGoals(List.of(
                                defaultGoalByTypeAndId(2499001255L, GoalType.INTERESTS))),
                        ltvRule()
                        ));

        ValidationResult<?, Defect> result = validate(retargetingConditionSpecific, campaign, specificAdGroup,
                pricePackageYndxFrontpage, retargetingConditionDefault);

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Description("тесты на валидацию специфичной группы об дефолтную. Специфичная шире")
    public void specificAdGroup_emptyBehaviorsInSnapshot_AnyGoals_Error() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageYndxFrontpage);
        var specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, client);

        var retargetingConditionDefault = defaultCpmRetCondition();
        retargetingConditionDefault//дефолтная (18-24 или 25-34 или 35-44) И (средний доход)
                .withRules(List.of(ruleOrSocialDemo(List.of(AGE_18_24, AGE_25_34, AGE_35_44)),
                        ruleOrSocialDemo(MID_INCOME_GOAL_ID),
                        ltvRule()));
        var retargetingConditionSpecific = defaultCpmRetCondition();
        retargetingConditionSpecific//25-34 и средний и высокий доход
                .withRules(List.of(ruleOrSocialDemo(AGE_25_34),
                        ruleOrSocialDemo(List.of(MID_INCOME_GOAL_ID, C1_INCOME_GOAL_ID)),
                        ltvRule()
                ));

        ValidationResult<?, Defect> result = validate(retargetingConditionSpecific, campaign, specificAdGroup,
                pricePackageYndxFrontpage, retargetingConditionDefault);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                retargetingConditionIsInvalidByDefaultAdGroup()))));
    }

    @Test
    @Description("тесты на валидацию специфичной группы об дефолтную. Учитывается период интересов")
    public void specificAdGroup_CryptaInterestType_Error() {
        /*на дефолтной группе выбраны интересы период "краткосрочные"
        на специфичной такие же интересны, "любой период"..
        Должна быть ошибка валидации на сохранении специфичной*/
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageYndxFrontpage);
        var specificAdGroup = steps.adGroupSteps().createSpecificAdGroupForPriceSales(campaign, client);

        var retargetingConditionDefault = defaultCpmRetCondition();
        retargetingConditionDefault
                .withRules(List.of(new Rule().withType(RuleType.OR).withInterestType(CryptaInterestType.short_term)
                                .withGoals(List.of(defaultGoalByTypeAndId(2499001255L, GoalType.INTERESTS))),
                        ltvRule()));
        var retargetingConditionSpecific = defaultCpmRetCondition();
        retargetingConditionSpecific
                .withRules(List.of(new Rule().withType(RuleType.OR).withInterestType(CryptaInterestType.all)
                                .withGoals(List.of(defaultGoalByTypeAndId(2499001255L, GoalType.INTERESTS))),
                        ltvRule()
                ));

        ValidationResult<?, Defect> result = validate(retargetingConditionSpecific, campaign, specificAdGroup,
                pricePackageYndxFrontpage, retargetingConditionDefault);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                retargetingConditionIsInvalidByDefaultAdGroup()))));
    }

    @Test
    @Description("Видео группа цели метрики разрешены")
    public void videoAdGroup_Metrika_SuccessValidation() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageVideo);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, client);

        ValidationResult<?, Defect> result =
                validate(defaultMetrikaGoalsRetargetingCondition(ruleOrSocialDemo(AGE_18_24)),
                campaign, adGroup, pricePackageVideo);

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Description("для дефолтной прайсовой группы нельзя задавать цели метрики")
    public void defaultAdGroup_Metrika_NotAllowed() {
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(client, pricePackageYndxFrontpage);
        var defaultAdGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, client);

        ValidationResult<?, Defect> result = validate(defaultMetrikaGoalsRetargetingCondition(ltvRule()),
                campaign, defaultAdGroup, pricePackageYndxFrontpage);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(path(),
                retargetingConditionIsInvalidForPricePackage()))));
    }

    private static RetargetingCondition defaultMetrikaGoalsRetargetingCondition(Rule additionalRule) {
        var retargetingCondition = defaultCpmRetCondition();
        retargetingCondition
                .withRules(List.of(additionalRule,
                        new Rule()
                        .withType(RuleType.OR)
                        .withGoals(defaultMetrikaGoals())));
        return retargetingCondition;
    }
}
