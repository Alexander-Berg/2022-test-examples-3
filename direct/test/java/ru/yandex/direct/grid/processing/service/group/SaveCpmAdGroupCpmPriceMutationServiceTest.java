package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;

import jdk.jfr.Description;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefectIds;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateCpmAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateCpmAdGroupItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdUpdateCpmRetargetingConditionItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static com.google.common.base.Preconditions.checkState;
import static java.math.RoundingMode.CEILING;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.INTERESTS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MID_INCOME_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_PRICE;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType.OR;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.all;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SaveCpmAdGroupCpmPriceMutationServiceTest {
    private static final BigDecimal PRICE1 = BigDecimal.valueOf(97).setScale(2, CEILING);
    private static final BigDecimal PRICE2 = BigDecimal.valueOf(101).setScale(2, CEILING);
    private static final String AD_GROUP_NAME1 = "Test name1";
    private static final String AD_GROUP_NAME2 = "Test name2";
    private static final String RET_COND_NAME1 = "Ret Cond Name1";
    private static final String RET_COND_NAME2 = "Ret Cond Name2";

    private static final String MUTATION_NAME = "saveCpmAdGroups";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAdGroupItems {"
            + "      adGroupId"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateCpmAdGroup, GdUpdateAdGroupPayload>
            UPDATE_GROUP_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
            GdUpdateCpmAdGroup.class, GdUpdateAdGroupPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    Steps steps;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    RetargetingConditionRepository retargetingConditionRepository;
    @Autowired
    RetargetingRepository retargetingRepository;
    @Autowired
    TestCryptaSegmentRepository testCryptaSegmentRepository;
    @Autowired
    AdGroupMutationService adGroupMutationService;
    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    private Goal goal1;
    private Goal goal2;

    private final List<Long> geo1 = List.of(SAINT_PETERSBURG_REGION_ID);
    private final List<Long> geo2 = List.of(MOSCOW_REGION_ID);

    private User operator;
    private ClientInfo clientInfo;

    private PricePackage pricePackage;
    private CpmPriceCampaign priceCampaign;
    private int shard;

    @Before
    public void init() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);

        priceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        goal1 = defaultGoalWithId(2499000001L, SOCIAL_DEMO); //Мужчины
        goal2 = defaultGoalWithId(2499000002L, SOCIAL_DEMO); //Женщины
        steps.cryptaGoalsSteps().addAllSocialDemoGoals();

        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                clientInfo.getShard(), priceCampaign.getId(),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));
    }

    @Test
    @Description("при добавлении группы generalPrice можно не передавать. он заполняется из пакета автоматически")
    public void create_withoutPrice() {
        saveAndValidate(null, null, geo1, goal1, RET_COND_NAME1, AD_GROUP_NAME1);
    }

    @Test
    @Description("если при добавлении группы generalPrice передан - он будет проигнорирован, " +
            "ставка проставится из пакета")
    public void create_withPrice() {
        assumeThat(pricePackage.getPrice().compareTo(PRICE1), is(not(0)));
        saveAndValidate(null, PRICE1, geo1, goal1, RET_COND_NAME1, AD_GROUP_NAME1);
    }

    @Test
    public void create_withDisabledFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        GdUpdateAdGroupPayload result = saveCpmAdGroups(null, PRICE1, geo1, goal1, RET_COND_NAME1, AD_GROUP_NAME1, PRIORITY_SPECIFIC);
        GdDefect defect = new GdDefect()
                .withPath("updateCpmAdGroupItems[0].campaignId")
                .withCode("CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS");
        assumeThat(result.getValidationResult().getErrors().get(0), is(defect));
    }

    @Test
    @Description("При обновлении группы generalPrice можно не передавать. " +
            "priceContext заполняется из пакета автоматически")
    public void update() {
        var adGroupId = saveAndValidate(null, null, geo1, goal1, RET_COND_NAME1, AD_GROUP_NAME1);
        saveAndValidate(adGroupId, null, geo2, goal2, RET_COND_NAME2, AD_GROUP_NAME2);
    }

    @Test
    public void update_WithDisabledFeature() {
        var adGroupId = saveAndValidate(null, null, geo1, goal1, RET_COND_NAME1, AD_GROUP_NAME1);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        GdUpdateAdGroupPayload result = saveCpmAdGroups(adGroupId, null, geo2, goal2, RET_COND_NAME2, AD_GROUP_NAME2, null);
        GdDefect defect = new GdDefect()
                .withPath("updateCpmAdGroupItems[0]")
                .withCode("DefectIds.NO_RIGHTS");
        assumeThat(result.getValidationResult().getErrors().get(0), is(defect));
    }

    @Test
    @Description("Если при изменении группы generalPrice передан он будет проигнорирован. " +
            "price_context будет заполнен значением из пакета. " +
            "в RetargetingUpdateOperation.onChangesApplied мы проставляем price_context для всех кампаний " +
            "кроме прайсовой. Поэтому для прайсовой загрузится price_context из базы, потом провалидируется " +
            "что он совпадает со значением в базе, а general_price ни на что не повлияет.")
    public void update_withPrice() {
        // чтобы проверить, что значение от фронта игнорируется, нужно чтобы это значение отличалось от значения в базе
        assumeThat(pricePackage.getPrice().compareTo(PRICE2), is(not(0)));
        // при создании передаём generalPrice = null
        // проверяем, что priceContext = pricePackagePrice
        Long adGroupId = saveAndValidate(null, null, geo1, goal1, RET_COND_NAME1, AD_GROUP_NAME1);
        // при обновлении передаём generalPrice = PRICE2
        // проверяем, что generalPrice проигнорирован и по прежнему priceContext = pricePackagePrice
        saveAndValidate(adGroupId, PRICE2, geo2, goal2, RET_COND_NAME2, AD_GROUP_NAME2);
    }

    @Test
    @Description("при добавлении группы допускается не указывать retargetingCondition")
    public void create_withoutRetargetingCondition() {
        saveAndValidate(null, null, geo1, null, RET_COND_NAME1, AD_GROUP_NAME1);
    }

    @Test
    @Description("при обновлении группы допускается не указывать retargetingCondition")
    public void update_withoutRetargetingCondition() {
        var adGroupId = saveAndValidate(null, null, geo1, goal1, RET_COND_NAME1, AD_GROUP_NAME1);
        saveAndValidate(adGroupId, null, geo2, null, RET_COND_NAME2, AD_GROUP_NAME2);
    }

    @Test
    @Description("разрешается таргетинг на интересы")
    public void create_and_update_withInterestGoalType() {
        var interestGoal1 = defaultGoalWithId(2499001100L, INTERESTS);
        var interestGoal2 = defaultGoalWithId(2499001101L, INTERESTS);
        testCryptaSegmentRepository.addAll(List.of(interestGoal1, interestGoal2));

        var adGroupId = saveAndValidate(null, null, geo1, interestGoal1, RET_COND_NAME1, AD_GROUP_NAME1);
        saveAndValidate(adGroupId, null, geo2, interestGoal2, RET_COND_NAME2, AD_GROUP_NAME2);
    }

    @Test
    @Description("при добавлении группы ошибки валидации retargetingCondition на верхнем уровне")
    public void create_path_retargetingCondition() {
        var goal = defaultGoalByTypeAndId(MID_INCOME_GOAL_ID, GoalType.SOCIAL_DEMO);
        GdUpdateAdGroupPayload payload = saveCpmAdGroups(null, null, List.of(RUSSIA), goal,
                RET_COND_NAME1, AD_GROUP_NAME1, PRIORITY_DEFAULT);
        Assert.assertThat(payload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withCode(RetargetingDefectIds.Gen.INVALID_RETARGETING_CONDITION_BY_PRICE_PACKAGE.getCode())
                        .withPath("updateCpmAdGroupItems[0]")));
    }

    /**
     * передаём null general_price и проверяем что ставка проставится из пакета
     */
    private Long saveAndValidate(@Nullable Long adGroupId, @Nullable BigDecimal generalPrice, @Nullable List<Long> geo,
                                 @Nullable Goal goal, String retCondName, String adGroupName) {
        GdUpdateAdGroupPayload payload = saveCpmAdGroups(adGroupId, generalPrice, geo, goal, retCondName, adGroupName,
                adGroupId == null ? PRIORITY_SPECIFIC : null);
        return validatePayloadAndExtractId(payload, adGroupId, pricePackage.getPrice(), geo, goal, adGroupName);
    }

    private GdUpdateAdGroupPayload saveCpmAdGroups(@Nullable Long adGroupId, @Nullable BigDecimal generalPrice,
                                                   @Nullable List<Long> geo, @Nullable Goal goal, String retCondName,
                                                   String adGroupName, Long priority) {
        boolean isNewAdGroup = adGroupId == null;

        GdUpdateCpmRetargetingConditionItem retargetingConditionItem;
        if (goal == null) {
            retargetingConditionItem = null;
        } else {
            GdRetargetingConditionRuleItemReq rule = new GdRetargetingConditionRuleItemReq()
                    .withType(OR)
                    .withInterestType(all)
                    .withGoals(singletonList(new GdGoalMinimal()
                            .withId(goal.getId())
                            .withTime(goal.getTime())));

            retargetingConditionItem = new GdUpdateCpmRetargetingConditionItem()
                    .withConditionRules(singletonList(rule))
                    .withName(retCondName);
        }

        List<Integer> intGeo = mapList(geo, Long::intValue);

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withAdGroupId(adGroupId)
                .withCampaignId(priceCampaign.getId())
                .withAdGroupName(adGroupName)
                .withRetargetingCondition(retargetingConditionItem)
                .withType(CPM_PRICE)
                .withGeneralPrice(generalPrice)
                .withRegionIds(intGeo)
                .withPriority(priority);

        GdUpdateCpmAdGroup input = new GdUpdateCpmAdGroup()
                .withIsNewGroups(isNewAdGroup)
                .withUpdateCpmAdGroupItems(singletonList(item));

        return processor.doMutationAndGetPayload(UPDATE_GROUP_MUTATION, input, operator);
    }

    private Long validatePayloadAndExtractId(GdUpdateAdGroupPayload gdUpdateAdGroupPayload, @Nullable Long adGroupId,
                                             @Nullable BigDecimal priceContext, @Nullable List<Long> geo,
                                             @Nullable Goal goal, String adGroupName) {
        List<GdUpdateAdGroupPayloadItem> updatedAdGroupItems = gdUpdateAdGroupPayload.getUpdatedAdGroupItems();
        checkState(updatedAdGroupItems.size() == 1);

        Long adGroupIdFromResponse = updatedAdGroupItems.get(0).getAdGroupId();
        // adGroupId == null если создаём новую группы и != null если обновляем
        if (adGroupId != null) {
            assertThat(adGroupIdFromResponse, equalTo(adGroupId));
        }

        checkUserProfileAdGroup(adGroupIdFromResponse, priceContext, geo, goal, adGroupName);
        return adGroupIdFromResponse;
    }

    private void checkUserProfileAdGroup(Long adGroupId, @Nullable BigDecimal priceContext, @Nullable List<Long> geo,
                                         @Nullable Goal goal, String adGroupName) {
        CpmYndxFrontpageAdGroup expectedAdGroup = new CpmYndxFrontpageAdGroup()
                .withName(adGroupName)
                .withCampaignId(priceCampaign.getId())
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withGeo(geo);

        RetargetingCondition expectedRetCondition = new RetargetingCondition();
        if (goal == null) {
            expectedRetCondition.setRules(List.of());
        } else {
            expectedRetCondition
                    .withRules(singletonList(new Rule()
                            .withType(RuleType.OR)
                            .withInterestType(CryptaInterestType.all)
                            .withGoals(singletonList((Goal) new Goal().withId(goal.getId())))));
        }

        Retargeting expectedRetargeting = new Retargeting().withPriceContext(priceContext);
        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    private void checkUserProfileAdGroup(Long adGroupId, AdGroup expectedCpmAdGroup,
                                         RetargetingCondition expectedRetCondition, Retargeting expectedRetargeting) {
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        RetargetingCondition actualRetCondition = retargetingConditionRepository
                .getRetConditionsByAdGroupIds(shard, singletonList(adGroupId))
                .get(adGroupId).get(0);

        Retargeting actualRetargeting = retargetingRepository
                .getRetargetingsByAdGroups(shard, singletonList(adGroupId)).get(0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualAdGroup)
                    .is(matchedBy(beanDiffer(expectedCpmAdGroup)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualRetCondition)
                    .is(matchedBy(beanDiffer(expectedRetCondition)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualRetargeting)
                    .is(matchedBy(beanDiffer(expectedRetargeting)
                            .useCompareStrategy(onlyExpectedFields())));
        });
    }

}
