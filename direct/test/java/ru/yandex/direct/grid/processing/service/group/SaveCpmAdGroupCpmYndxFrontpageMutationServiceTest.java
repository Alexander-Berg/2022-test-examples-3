package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateCpmAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateCpmAdGroupItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdUpdateCpmRetargetingConditionItem;
import ru.yandex.qatools.allure.annotations.Description;

import static com.google.common.base.Preconditions.checkState;
import static java.math.RoundingMode.CEILING;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_YNDX_FRONTPAGE;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType.OR;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.long_term;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.short_term;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SaveCpmAdGroupCpmYndxFrontpageMutationServiceTest {
    private static final BigDecimal PRICE1 = BigDecimal.valueOf(20).setScale(2, CEILING);
    private static final BigDecimal PRICE2 = BigDecimal.valueOf(30).setScale(2, CEILING);
    private static final String AD_GROUP_NAME1 = "Test name1";
    private static final String AD_GROUP_NAME2 = "Test name2";
    public static final int SHARD = 1;

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
    BidModifierRepository bidModifierRepository;
    @Autowired
    MinusKeywordsPackRepository minusKeywordsPackRepository;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private Goal goal1;
    private Goal goal2;

    @Before
    public void init() {
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.CHF));
        campaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        //Для рекламы в помещениях можно указывать только пол или возраст
        goal1 = defaultGoalWithId(2499000001L, SOCIAL_DEMO); //Мужчины
        goal2 = defaultGoalWithId(2499000002L, SOCIAL_DEMO); //Женщины
        steps.cryptaGoalsSteps().addAllSocialDemoGoals();
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                campaignInfo.getShard(),
                campaignInfo.getCampaignId(),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CPM_YNDX_FRONTPAGE_PROFILE, true);
    }

    @Test
    @Description("Создание медийной группы на главной")
    public void addCpmYndxFrontpageAdGroups_PositiveTest() {
        Long adGroupId = createCpmYndxFrontpageAdGroup();

        CpmYndxFrontpageAdGroup expectedAdGroup = new CpmYndxFrontpageAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(SAINT_PETERSBURG_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE1);

        checkAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    @Description("Обновление медийной группы на главной")
    public void updateCpmYndxFrontpageAdGroups_PositiveTest() {
        Long adGroupId = createCpmYndxFrontpageAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedItem(adGroupId)
                .withType(CPM_YNDX_FRONTPAGE);

        updateAndCheckNoErrors(item, false);

        CpmYndxFrontpageAdGroup expectedAdGroup = new CpmYndxFrontpageAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(MOSCOW_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.long_term, goal2);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE2);

        checkAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    private Long createCpmYndxFrontpageAdGroup() {
        GdRetargetingConditionRuleItemReq rule = new GdRetargetingConditionRuleItemReq()
                .withType(OR)
                .withInterestType(short_term)
                .withGoals(singletonList(new GdGoalMinimal().withId(goal1.getId())));
        GdUpdateCpmRetargetingConditionItem retargetingConditionItem = new GdUpdateCpmRetargetingConditionItem()
                .withConditionRules(singletonList(rule));

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupName(AD_GROUP_NAME1)
                .withRegionIds(singletonList((int) SAINT_PETERSBURG_REGION_ID))
                .withRetargetingCondition(retargetingConditionItem)
                .withGeneralPrice(PRICE1)
                .withType(CPM_YNDX_FRONTPAGE);

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private GdUpdateCpmAdGroupItem createChangedItem(Long adGroupId) {
        GdRetargetingConditionRuleItemReq rule = new GdRetargetingConditionRuleItemReq()
                .withType(OR)
                .withInterestType(long_term)
                .withGoals(singletonList(new GdGoalMinimal().withId(goal2.getId())));
        GdUpdateCpmRetargetingConditionItem retargetingConditionItem = new GdUpdateCpmRetargetingConditionItem()
                .withConditionRules(singletonList(rule));

        return new GdUpdateCpmAdGroupItem()
                .withAdGroupId(adGroupId)
                .withAdGroupName(AD_GROUP_NAME2)
                .withRegionIds(singletonList((int) MOSCOW_REGION_ID))
                .withRetargetingCondition(retargetingConditionItem)
                .withGeneralPrice(PRICE2);
    }

    private void checkAdGroup(Long adGroupId, AdGroup expectedCpmAdGroup,
                              RetargetingCondition expectedRetCondition, Retargeting expectedRetargeting) {

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(SHARD, singletonList(adGroupId)).get(0);

        RetargetingCondition actualRetCondition = retargetingConditionRepository
                .getRetConditionsByAdGroupIds(SHARD, singletonList(adGroupId))
                .get(adGroupId).get(0);

        Retargeting actualRetargeting = retargetingRepository
                .getRetargetingsByAdGroups(SHARD, singletonList(adGroupId)).get(0);

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

    private List<GdUpdateAdGroupPayloadItem> updateAndCheckNoErrors(GdUpdateCpmAdGroupItem item, boolean isNewGroups) {
        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, isNewGroups);

        List<GdUpdateAdGroupPayloadItem> updatedAdGroupItems = gdUpdateAdGroupPayload.getUpdatedAdGroupItems();
        checkState(updatedAdGroupItems.size() == 1);

        return updatedAdGroupItems;
    }

    private GdUpdateAdGroupPayload update(GdUpdateCpmAdGroupItem item, boolean isNewGroups) {
        GdUpdateCpmAdGroup input = new GdUpdateCpmAdGroup()
                .withIsNewGroups(isNewGroups)
                .withUpdateCpmAdGroupItems(singletonList(item));

        return adGroupMutationService.saveCpmAdGroups(clientInfo.getClientId(), clientInfo.getUid(), input);
    }

    private RetargetingCondition getExpectedRetCondition(CryptaInterestType interestType, Goal goal) {
        RetargetingCondition expectedRetCondition = new RetargetingCondition();
        expectedRetCondition
                .withRules(singletonList(new Rule()
                        .withType(RuleType.OR)
                        .withInterestType(interestType)
                        .withGoals(singletonList((Goal) new Goal().withId(goal.getId())))));
        return expectedRetCondition;
    }
}
