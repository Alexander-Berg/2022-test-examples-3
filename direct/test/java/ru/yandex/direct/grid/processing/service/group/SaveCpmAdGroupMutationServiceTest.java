package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.CpmAudioAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTraffic;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafficAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.repository.PlacementRepository;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.userssegments.repository.UsersSegmentRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierExpress;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierExpressAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierExpressLiteral;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobileAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupPageBlock;
import ru.yandex.direct.grid.processing.model.group.GdCriterionType;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateCpmAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateCpmAdGroupItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdUpdateCpmRetargetingConditionItem;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.qatools.allure.annotations.Description;

import static com.google.common.base.Preconditions.checkState;
import static java.math.RoundingMode.CEILING;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.model.CriterionType.KEYWORD;
import static ru.yandex.direct.core.entity.adgroup.model.CriterionType.USER_PROFILE;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds.Gen.AD_GROUP_TYPE_NOT_SUPPORTED;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds.Gen.EITHER_KEYWORDS_OR_RETARGETINGS_ALLOWED;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds.Gen.KEYWORDS_NOT_ALLOWED;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefectIds.Gen.NOT_ACCEPTABLE_AD_GROUP_TYPE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AUDIO_GENRES;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.CONTENT_CATEGORY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithDefaultBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithDefaultBlock;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.EXPRESS_TRAFFIC_MULTIPLIER;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.MOBILE_MULTIPLIER;
import static ru.yandex.direct.grid.processing.model.group.GdVideoGoalType.COMPLETE;
import static ru.yandex.direct.grid.processing.model.group.GdVideoGoalType.START;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_AUDIO;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_BANNER;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_GEOPRODUCT;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_GEO_PIN;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_INDOOR;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_OUTDOOR;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdCpmGroupType.CPM_VIDEO;
import static ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType.OR;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.long_term;
import static ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb.short_term;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SaveCpmAdGroupMutationServiceTest {
    private static final BigDecimal PRICE1 = BigDecimal.valueOf(111).setScale(2, CEILING);
    private static final BigDecimal PRICE2 = BigDecimal.valueOf(222).setScale(2, CEILING);
    private static final BigDecimal INVALID_LOW_PRICE_FOR_GEOPRODUCT =
            BigDecimal.valueOf(3).setScale(2, CEILING);
    private static final String KEYWORD1 = "keyword1";
    private static final String KEYWORD2 = "keyword2";
    private static final String KEYWORD3 = "keyword3";
    private static final String AD_GROUP_NAME1 = "Test name1";
    private static final String AD_GROUP_NAME2 = "Test name2";
    private static final Long OUTDOOR1 = nextLong(1, Integer.MAX_VALUE);
    private static final Long OUTDOOR2 = nextLong(1, Integer.MAX_VALUE);
    private static final Long INDOOR1 = nextLong(1, Integer.MAX_VALUE);
    private static final Long INDOOR2 = nextLong(1, Integer.MAX_VALUE);
    private static final long BLOCK_ID = 1L;
    public static final int SHARD = 1;

    @Autowired
    Steps steps;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    KeywordRepository keywordRepository;
    @Autowired
    RetargetingConditionRepository retargetingConditionRepository;
    @Autowired
    RetargetingRepository retargetingRepository;
    @Autowired
    TestCryptaSegmentRepository testCryptaSegmentRepository;
    @Autowired
    AdGroupMutationService adGroupMutationService;
    @Autowired
    private UsersSegmentRepository usersSegmentRepository;
    @Autowired
    private PlacementRepository placementRepository;
    @Autowired
    BidModifierRepository bidModifierRepository;
    @Autowired
    MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    private FeatureSteps featureSteps;

    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private CampaignInfo autoBudgetCampaignInfo;
    private Long campaignId;
    private Goal goal1;
    private Goal goal2;
    private Goal goal3;
    private Goal goal4;
    private Goal goal5;

    @Before
    public void init() {
        campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign();
        clientInfo = campaignInfo.getClientInfo();
        campaignId = campaignInfo.getCampaignId();
        autoBudgetCampaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        //Для рекламы в помещениях можно указывать только пол или возраст
        goal1 = defaultGoalWithId(2499000001L, SOCIAL_DEMO); //Мужчины
        goal2 = defaultGoalWithId(2499000002L, SOCIAL_DEMO); //Женщины
        steps.cryptaGoalsSteps().addAllSocialDemoGoals();
        goal3 = defaultGoalWithId(2499990001L, AUDIO_GENRES); //Музыкальные жанры: Поп
        goal4 = defaultGoalWithId(4_294_968_296L, CONTENT_CATEGORY); //Категории контента: Авто
        goal5 = defaultGoalWithId(4_294_968_298L, CONTENT_CATEGORY); //Категории контента: География и путешествия
        testCryptaSegmentRepository.addAll(asList(goal3, goal4, goal5));
        OutdoorPlacement placement1 = outdoorPlacementWithDefaultBlock(OUTDOOR1, BLOCK_ID);
        OutdoorPlacement placement2 = outdoorPlacementWithDefaultBlock(OUTDOOR2, BLOCK_ID);
        IndoorPlacement placement3 = indoorPlacementWithDefaultBlock(INDOOR1, BLOCK_ID);
        IndoorPlacement placement4 = indoorPlacementWithDefaultBlock(INDOOR2, BLOCK_ID);
        placementRepository.createOrUpdatePlacements(asList(placement1, placement2, placement3, placement4));
    }

    @Test
    @Description("Создание группы с ключевыми словами")
    public void createCpmAdGroups_keywordsType() {
        Long adGroupId = createCpmKeywordAdGroup();

        CpmBannerAdGroup expectedAdGroup = new CpmBannerAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignId)
                .withCriterionType(KEYWORD)
                .withGeo(singletonList(SAINT_PETERSBURG_REGION_ID));

        List<Keyword> expectedKeywords = asList(
                new Keyword().withPhrase(KEYWORD1),
                new Keyword().withPhrase(KEYWORD2));

        checkKeywordAdGroup(adGroupId, expectedAdGroup, expectedKeywords);
    }

    @Test
    @Description("Обновление группы с ключевыми словами")
    public void updateCpmAdGroups_keywordsType() {
        Long adGroupId = createCpmKeywordAdGroup();
        List<Keyword> currentKeywords = keywordRepository.getKeywordsByAdGroupId(SHARD, adGroupId);

        //Оставляем одно существующее слово из двух и добавляем третье
        List<GdUpdateAdGroupKeywordItem> keywords = asList(
                new GdUpdateAdGroupKeywordItem().withId(currentKeywords.get(0).getId()),
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD3));

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withType(CPM_BANNER)
                .withAdGroupId(adGroupId)
                .withAdGroupName(AD_GROUP_NAME2)
                .withRegionIds(singletonList((int) MOSCOW_REGION_ID))
                .withGeneralPrice(PRICE2)
                .withKeywords(keywords);

        updateAndCheckNoErrors(item, false);

        //Проверяем
        CpmBannerAdGroup expectedAdGroup = new CpmBannerAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignId)
                .withCriterionType(KEYWORD)
                .withGeo(singletonList(MOSCOW_REGION_ID));

        List<Keyword> expectedKeywords = asList(
                new Keyword().withPhrase(KEYWORD1),
                new Keyword().withPhrase(KEYWORD3));

        checkKeywordAdGroup(adGroupId, expectedAdGroup, expectedKeywords);
    }

    @Test
    @Description("Проверка неизменности минус-фраз при обновлении группы с ключевыми словами")
    public void updateCpmAdGroups_minusKeywords() {
        MinusKeywordsPackInfo minusKeywordsPack = steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(libraryMinusKeywordsPack().withIsLibrary(false), clientInfo);

        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createAdGroup(new AdGroupInfo()
                        .withAdGroup(activeCpmBannerAdGroup(campaignInfo.getCampaignId())
                                .withCriterionType(CriterionType.KEYWORD)
                                .withMinusKeywordsId(minusKeywordsPack.getMinusKeywordPackId()))
                        .withCampaignInfo(campaignInfo));

        Long adGroupId = adGroupInfo.getAdGroupId();//createCpmKeywordAdGroup();

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withType(CPM_BANNER)
                .withAdGroupId(adGroupId)
                .withAdGroupName(AD_GROUP_NAME2)
                .withRegionIds(singletonList((int) MOSCOW_REGION_ID))
                .withGeneralPrice(PRICE1)
                .withKeywords(singletonList(new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1)));

        updateAndCheckNoErrors(item, false);

        //Проверяем
        CpmBannerAdGroup expectedAdGroup = new CpmBannerAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignId)
                .withCriterionType(KEYWORD)
                .withGeo(singletonList(MOSCOW_REGION_ID))
                .withMinusKeywordsId(minusKeywordsPack.getMinusKeywordPackId())
                .withMinusKeywords(minusKeywordsPack.getMinusKeywordsPack().getMinusKeywords());

        List<Keyword> expectedKeywords = singletonList(new Keyword().withPhrase(KEYWORD1));

        checkKeywordAdGroup(adGroupId, expectedAdGroup, expectedKeywords);
    }

    @Test
    @Description("Создание группы с профилем пользователя")
    public void createCpmAdGroups_userProfileType() {
        Long adGroupId = createCpmUserProfileAdGroup();

        CpmBannerAdGroup expectedAdGroup = new CpmBannerAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withCriterionType(USER_PROFILE)
                .withGeo(singletonList(SAINT_PETERSBURG_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE1);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    @Description("Обновление группы с профилем пользователя")
    public void updateCpmAdGroups_userProfileType() {
        Long adGroupId = createCpmUserProfileAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withType(CPM_BANNER);

        updateAndCheckNoErrors(item, false);

        CpmBannerAdGroup expectedAdGroup = new CpmBannerAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignInfo.getCampaignId())
                .withCriterionType(USER_PROFILE)
                .withGeo(singletonList(MOSCOW_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.long_term, goal2);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE2);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    @Description("Обновление группы с категориями контента")
    public void updateCpmAdGroups_contentCategories() {
        Long adGroupId = createCpmContentCategoriesAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedContentCategoriesItem(adGroupId)
                .withType(CPM_BANNER);

        updateAndCheckNoErrors(item, false);

        var goal = (Goal) new Goal()
                .withId(4_294_968_298L)
                .withType(CONTENT_CATEGORY);

        var rules = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(List.of(goal)));

        CpmBannerAdGroup expectedCpmAdGroup = new CpmBannerAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withContentCategoriesRetargetingConditionRules(rules);

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(SHARD, singletonList(adGroupId)).get(0);

        SoftAssertions.assertSoftly(soft -> soft.assertThat(actualAdGroup)
                .is(matchedBy(beanDiffer(expectedCpmAdGroup)
                        .useCompareStrategy(onlyExpectedFields()))));
    }

    @Test
    @Description("Обновление группы с пустым списком категорий контента")
    public void updateCpmAdGroups_emptyContentCategories() {
        Long adGroupId = createCpmContentCategoriesAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedContentCategoriesItem(adGroupId)
                .withContentCategoriesRetargetingConditionRules(emptyList())
                .withType(CPM_BANNER);

        var gdUpdateAdGroupPayload = update(item, false);

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(AdGroupDefectIds.Gen.EITHER_KEYWORDS_OR_RETARGETINGS_ALLOWED.getCode())
                        .withPath("updateCpmAdGroupItems[0]")));
    }

    @Test
    @Description("Обновление группы с пустым списком категорий контента с заполненным сriterionType")
    public void updateCpmAdGroups_emptyContentCategoriesWithCategoriesTargeting() {
        Long adGroupId = createCpmContentCategoriesAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedContentCategoriesItem(adGroupId)
                .withContentCategoriesRetargetingConditionRules(emptyList())
                .withType(CPM_BANNER)
                .withCriterionType(GdCriterionType.CONTENT_CATEGORY);

        var gdUpdateAdGroupPayload = update(item, false);

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(AdGroupDefectIds.Gen.EMPTY_CONTENT_CATEGORIES_NOT_ALLOWED.getCode())
                        .withPath("updateCpmAdGroupItems[0].contentCategoriesRetargetingConditionRules")));
    }

    @Test
    @Description("Обновление группы с пустым списком категорий контента и ключевыми словами")
    public void updateCpmAdGroups_contentCategoriesWithKeywords() {
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, false);

        Long adGroupId = createCpmKeywordAdGroup();

        var keywords = asList(
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1),
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD2));
        GdUpdateCpmAdGroupItem item = createChangedContentCategoriesItem(adGroupId)
                .withContentCategoriesRetargetingConditionRules(emptyList())
                .withType(CPM_BANNER)
                .withKeywords(keywords);

        var gdUpdateAdGroupPayload = update(item, false);

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult(), nullValue());
    }

    @Test
    @Description("Обновление группы с непустым списком категорий контента и ключевыми словами")
    public void updateCpmAdGroups_contentCategoriesWithKeywordsAndCategories() {
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, false);

        Long adGroupId = createCpmKeywordAdGroup();

        var keywords = asList(
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1),
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD2));
        GdUpdateCpmAdGroupItem item = createChangedContentCategoriesItem(adGroupId)
                .withType(CPM_BANNER)
                .withKeywords(keywords);

        var gdUpdateAdGroupPayload = update(item, false);

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(AdGroupDefectIds.Gen.EITHER_KEYWORDS_OR_RETARGETINGS_ALLOWED.getCode())
                        .withPath("updateCpmAdGroupItems[0]")));
    }

    @Test
    @Description("Обновление группы с непустым категорий контента и ключевыми словами")
    public void updateCpmAdGroups_contentCategoriesWithKeywordsAndCategoriesExtended() {
        try {
            steps.featureSteps().addClientFeature(
                    clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, true);

            Long adGroupId = createCpmKeywordAdGroup();

            var keywords = asList(
                    new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1),
                    new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD2));
            GdUpdateCpmAdGroupItem item = createChangedContentCategoriesItem(adGroupId)
                    .withType(CPM_BANNER)
                    .withKeywords(keywords);

            var gdUpdateAdGroupPayload = update(item, false);

            Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult(), nullValue());
        } finally {
            steps.featureSteps().addClientFeature(
                    clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, false);
        }
    }

    @Test
    @Description("Обновление группы с пустым списком категорий контента и ключевыми словами")
    public void updateCpmAdGroups_contentCategoriesWithProfile() {
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, false);

        Long adGroupId = createCpmUserProfileAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withContentCategoriesRetargetingConditionRules(emptyList())
                .withType(CPM_BANNER);

        var gdUpdateAdGroupPayload = update(item, false);

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult(), nullValue());
    }

    @Test
    @Description("Обновление группы с непустым списком категорий контента и ключевыми словами")
    public void updateCpmAdGroups_contentCategoriesWithProfileAndCategories() {
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, false);

        Long adGroupId = createCpmUserProfileAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withContentCategoriesRetargetingConditionRules(singletonList(createContentCategoriesRuleItem(goal5)))
                .withType(CPM_BANNER);

        var gdUpdateAdGroupPayload = update(item, false);

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(AdGroupDefectIds.Gen.EITHER_KEYWORDS_OR_RETARGETINGS_ALLOWED.getCode())
                        .withPath("updateCpmAdGroupItems[0]")));
    }

    @Test
    @Description("Обновление группы с непустым категорий контента и ключевыми словами")
    public void updateCpmAdGroups_contentCategoriesWithProfileAndCategoriesExtended() {
        try {
            steps.featureSteps().addClientFeature(
                    clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, true);

            Long adGroupId = createCpmUserProfileAdGroup();

            GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                    .withContentCategoriesRetargetingConditionRules(singletonList(createContentCategoriesRuleItem(goal5)))
                    .withType(CPM_BANNER);

            var gdUpdateAdGroupPayload = update(item, false);

            Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult(), nullValue());
        } finally {
            steps.featureSteps().addClientFeature(
                    clientInfo.getClientId(), FeatureName.CONTENT_CATEGORY_TARGETING_CPM_EXTENDED, false);
        }
    }

    @Test
    @Description("Создание аудио группы")
    public void createCpmAdGroups_audio() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_AUDIO_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_AUDIO_GROUPS_EDIT_FOR_DNA, true);
        Long adGroupId = createCpmAudioAdGroup();

        CpmAudioAdGroup expectedAdGroup = new CpmAudioAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(SAINT_PETERSBURG_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE1);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
        checkVideoGoals(adGroupId, AdShowType.START);
    }

    @Test
    @Description("Обновление аудио группы")
    public void updateCpmAdGroups_audio() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_AUDIO_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_AUDIO_GROUPS_EDIT_FOR_DNA, true);
        Long adGroupId = createCpmAudioAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withType(CPM_AUDIO)
                .withVideoGoals(singletonList(COMPLETE));

        updateAndCheckNoErrors(item, false);

        CpmAudioAdGroup expectedAdGroup = new CpmAudioAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(MOSCOW_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.long_term, goal2);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE2);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
        checkVideoGoals(adGroupId, AdShowType.COMPLETE);
    }

    @Test
    @Description("Создание видео группы")
    public void createCpmAdGroups_video() {
        Long adGroupId = createCpmVideoAdGroup();

        CpmVideoAdGroup expectedAdGroup = new CpmVideoAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(SAINT_PETERSBURG_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE1);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
        checkVideoGoals(adGroupId, AdShowType.START);
    }

    @Test
    @Description("Обновление видео группы")
    public void updateCpmAdGroups_video() {
        Long adGroupId = createCpmVideoAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withType(CPM_VIDEO)
                .withVideoGoals(singletonList(COMPLETE));

        updateAndCheckNoErrors(item, false);

        CpmVideoAdGroup expectedAdGroup = new CpmVideoAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(MOSCOW_REGION_ID));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.long_term, goal2);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE2);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
        checkVideoGoals(adGroupId, AdShowType.COMPLETE);
    }

    @Test
    @Description("Создание группы наружной рекламы")
    public void createCpmAdGroups_outdoor() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_OUTDOOR_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_OUTDOOR_GROUPS_EDIT_FOR_DNA, true);
        Long adGroupId = createCpmOutdoorAdGroup();

        CpmOutdoorAdGroup expectedAdGroup = new CpmOutdoorAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(RUSSIA_REGION_ID))
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR1)
                        .withImpId(BLOCK_ID)));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE1);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    @Description("Обновление группы наружной рекламы")
    public void updateCpmAdGroups_outdoor() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_OUTDOOR_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_OUTDOOR_GROUPS_EDIT_FOR_DNA, true);
        Long adGroupId = createCpmOutdoorAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withType(CPM_OUTDOOR)
                .withRegionIds(null)
                .withPageBlocks(singletonList(new GdAdGroupPageBlock()
                        .withPageId(OUTDOOR2)
                        .withImpId(BLOCK_ID)));

        updateAndCheckNoErrors(item, false);

        CpmOutdoorAdGroup expectedAdGroup = new CpmOutdoorAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(RUSSIA_REGION_ID))
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR2)
                        .withImpId(BLOCK_ID)));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.long_term, goal2);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE2);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    @Description("Создание группы рекламы в помещениях")
    public void createCpmAdGroups_indoor() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_INDOOR_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_INDOOR_GROUPS_EDIT_FOR_DNA, true);
        Long adGroupId = createCpmIndoorAdGroup();

        CpmIndoorAdGroup expectedAdGroup = new CpmIndoorAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(RUSSIA_REGION_ID))
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(INDOOR1)
                        .withImpId(BLOCK_ID)));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE1);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    @Description("Обновление группы рекламы в помещениях")
    public void updateCpmAdGroups_indoor() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_INDOOR_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_INDOOR_GROUPS_EDIT_FOR_DNA, true);
        Long adGroupId = createCpmIndoorAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withType(CPM_INDOOR)
                .withRegionIds(null)
                .withPageBlocks(singletonList(new GdAdGroupPageBlock()
                        .withPageId(INDOOR2)
                        .withImpId(BLOCK_ID)));

        updateAndCheckNoErrors(item, false);

        CpmIndoorAdGroup expectedAdGroup = new CpmIndoorAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(RUSSIA_REGION_ID))
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(INDOOR2)
                        .withImpId(BLOCK_ID)));

        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.long_term, goal2);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE2);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    @Description("Добавление корректировки в группу с ключевыми словами")
    public void updateCpmAdGroups_bidmodifiers() {
        Long adGroupId = createCpmKeywordAdGroup();

        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers()
                .withBidModifierMobile(new GdUpdateBidModifierMobile()
                        .withAdGroupId(adGroupId)
                        .withCampaignId(campaignId)
                        .withAdjustment(new GdUpdateBidModifierMobileAdjustmentItem()
                                .withPercent(10))
                        .withEnabled(true)
                        .withType(MOBILE_MULTIPLIER));

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withType(CPM_BANNER)
                .withAdGroupId(adGroupId)
                .withAdGroupName(AD_GROUP_NAME1)
                .withRegionIds(singletonList((int) MOSCOW_REGION_ID))
                .withKeywords(singletonList(new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1)))
                .withGeneralPrice(PRICE2)
                .withBidModifiers(bidModifiers);


        updateAndCheckNoErrors(item, false);

        BidModifier expected = new BidModifierMobile()
                .withEnabled(true)
                .withMobileAdjustment(new BidModifierMobileAdjustment().withPercent(10));

        List<BidModifier> actualModifiers = bidModifierRepository
                .getByAdGroupIds(SHARD,
                        singletonMap(adGroupId, campaignId),
                        singleton(BidModifierType.MOBILE_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP));

        assertThat(actualModifiers)
                .is(matchedBy(beanDiffer(singletonList(expected))
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    @Description("Создание группы с ключевыми словами с корректировкой")
    public void createCpmAdGroups_bidmodifiers() {
        List<GdUpdateAdGroupKeywordItem> keywords = asList(
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1),
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD2));

        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers()
                .withBidModifierMobile(new GdUpdateBidModifierMobile()
                        .withCampaignId(campaignId)
                        .withAdjustment(new GdUpdateBidModifierMobileAdjustmentItem()
                                .withPercent(10))
                        .withEnabled(true)
                        .withType(MOBILE_MULTIPLIER));

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withType(CPM_BANNER)
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupName(AD_GROUP_NAME1)
                .withRegionIds(singletonList((int) SAINT_PETERSBURG_REGION_ID))
                .withGeneralPrice(PRICE1)
                .withKeywords(keywords)
                .withBidModifiers(bidModifiers);

        updateAndCheckNoErrors(item, true);
    }

    @Test
    @Description("Создание группы с ключевыми словами с универсальной корректировкой")
    public void createCpmAdGroups_expressionBidmodifiers() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_OUTDOOR_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_OUTDOOR_GROUPS_EDIT_FOR_DNA, true);
        GdUpdateBidModifiers bidModifiers = new GdUpdateBidModifiers()
                .withBidModifierExpress(List.of(new GdUpdateBidModifierExpress()
                        .withType(EXPRESS_TRAFFIC_MULTIPLIER)
                        .withEnabled(true)
                        .withCampaignId(campaignId)
                        .withAdjustments(List.of(new GdUpdateBidModifierExpressAdjustmentItem()
                                .withPercent(223)
                                .withCondition(List.of(List.of(new GdUpdateBidModifierExpressLiteral()
                                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                                .withOperation(BidModifierExpressionOperator.EQ)
                                                .withValue("3")
                                        ))
                                )))
                ));
        BidModifierTraffic expectedTrafficModifier = new BidModifierTraffic()
                .withType(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)
                .withEnabled(true)
                .withCampaignId(campaignId)
                .withExpressionAdjustments(List.of(new BidModifierTrafficAdjustment()
                        .withPercent(223)
                        .withCondition(List.of(List.of(new BidModifierExpressionLiteral()
                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                .withOperation(BidModifierExpressionOperator.EQ)
                                .withValueString("3")
                        )))
                ));

        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_OUTDOOR)
                .withRegionIds(null)
                .withPageBlocks(singletonList(new GdAdGroupPageBlock()
                        .withPageId(OUTDOOR1)
                        .withImpId(BLOCK_ID)))
                .withBidModifiers(bidModifiers);

        Long adGroupId = updateAndCheckNoErrors(item, true).get(0).getAdGroupId();

        List<BidModifier> actualModifiers = bidModifierRepository
                .getByAdGroupIds(SHARD,
                        singletonMap(adGroupId, campaignId),
                        singleton(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP));
        assertThat(actualModifiers)
                .is(matchedBy(beanDiffer(singletonList(expectedTrafficModifier))
                        .useCompareStrategy(onlyExpectedFields())
                ));

    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoproductAdGroupWithUserProfile_GroupDataIsCorrect() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        Long adGroupId = createCpmGeoproductUserProfileAdGroup();

        CpmGeoproductAdGroup expectedAdGroup = new CpmGeoproductAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(SAINT_PETERSBURG_REGION_ID));
        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE1);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoproductAdGroupWithUserProfile_AutoBudget() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        Long adGroupId = createCpmGeoproductUserProfileAdGroupAutobudget();

        CpmGeoproductAdGroup expectedAdGroup = new CpmGeoproductAdGroup()
                .withName(AD_GROUP_NAME1)
                .withCampaignId(autoBudgetCampaignInfo.getCampaignId())
                .withGeo(singletonList(SAINT_PETERSBURG_REGION_ID));
        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.short_term, goal1);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(
                CurrencyRub.getInstance().getMinCpmPrice().setScale(2, CEILING));

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoproductAdGroupWithLowPrice_Error() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT)
                .withGeneralPrice(INVALID_LOW_PRICE_FOR_GEOPRODUCT);

        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, true);
        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(MUST_BE_IN_THE_INTERVAL_INCLUSIVE.getCode())
                        .withPath("updateCpmAdGroupItems[0].generalPrice")
                        .withParams(new NumberDefectParams()
                                .withMin(CurrencyRub.getInstance().getMinCpmPrice().setScale(0))
                                .withMax(CurrencyRub.getInstance().getMaxCpmPrice()))));
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoproductAdGroupWithMinPrice_NoError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT)
                .withGeneralPrice(CurrencyRub.getInstance().getMinCpmPrice());

        updateAndCheckNoErrors(item, true);
    }

    @Test
    public void updateCpmAdGroups_UpdateCpmGeoproductAdGroupWithUserProfile_GroupDataIsCorrect() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        Long adGroupId = createCpmGeoproductUserProfileAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withType(CPM_GEOPRODUCT);
        updateAndCheckNoErrors(item, false);

        CpmGeoproductAdGroup expectedAdGroup = new CpmGeoproductAdGroup()
                .withName(AD_GROUP_NAME2)
                .withCampaignId(campaignInfo.getCampaignId())
                .withGeo(singletonList(MOSCOW_REGION_ID));
        RetargetingCondition expectedRetCondition = getExpectedRetCondition(CryptaInterestType.long_term, goal2);
        Retargeting expectedRetargeting = new Retargeting().withPriceContext(PRICE2);

        checkUserProfileAdGroup(adGroupId, expectedAdGroup, expectedRetCondition, expectedRetargeting);
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoproductAdGroupWithKeywords_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = createCpmGeoproductKeywordAdGroup();

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withCode(NOT_ACCEPTABLE_AD_GROUP_TYPE.getCode())
                        .withPath("updateCpmAdGroupItems[0].keywords[0].adGroupId"),
                new GdDefect().withCode(KEYWORDS_NOT_ALLOWED.getCode()).withPath("updateCpmAdGroupItems[0]")));
    }

    @Test
    public void updateCpmAdGroups_UpdateCpmGeoproductAdGroup_AddKeywords_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        Long adGroupId = createCpmGeoproductUserProfileAdGroup();

        GdUpdateCpmAdGroupItem item = createChangedUserProfileItem(adGroupId)
                .withRetargetingCondition(null)
                .withKeywords(singletonList(new GdUpdateAdGroupKeywordItem().withPhrase("keyword")))
                .withType(CPM_GEOPRODUCT);
        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, false);

        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect().withCode(NOT_ACCEPTABLE_AD_GROUP_TYPE.getCode())
                        .withPath("updateCpmAdGroupItems[0].keywords[0].adGroupId"),
                new GdDefect().withCode(KEYWORDS_NOT_ALLOWED.getCode()).withPath("updateCpmAdGroupItems[0]")));
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoproductAdGroup_FeatureIsOff_ValidationError() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT);

        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, true);

        Assert.assertFalse(gdUpdateAdGroupPayload.getValidationResult().getErrors().isEmpty());
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoPinAdGroup_FeatureIsOff_ValidationError() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEO_PIN);

        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, true);

        Assert.assertFalse(gdUpdateAdGroupPayload.getValidationResult().getErrors().isEmpty());
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoPinAdGroup_WithNotGeoAdGroup_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED, true);
        createCpmUserProfileAdGroup();
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEO_PIN);

        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, true);
        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(AD_GROUP_TYPE_NOT_SUPPORTED.getCode())
                        .withPath("updateCpmAdGroupItems[0].type")));
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoPinAdGroup_WithCpmGeoproductAdGroup_NoError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED, true);
        featureSteps.addClientFeature(clientInfo.getClientId(),
                FeatureName.TARGETING_IS_NOT_REQUIRED_FOR_CPM_GEOPRODUCT_GROUP, true);
        createCpmGeoPinUserProfileAdGroup();
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT);

        updateAndCheckNoErrors(item, true);
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoproductAdGroup_WithCpmGeoPinAdGroup_NoError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED, true);
        createCpmGeoproductUserProfileAdGroup();
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEO_PIN);

        updateAndCheckNoErrors(item, true);
    }

    @Test
    public void updateCpmAdGroups_CpmGeoPinAdGroupWithoutRetargeting_NoError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED, true);
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEO_PIN)
                .withRetargetingCondition(null);

        updateAndCheckNoErrors(item, true);
    }

    @Test
    public void updateCpmAdGroups_CpmGeoproductAdGroupWithoutRetargeting_WithoutFeature_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT)
                .withRetargetingCondition(null);

        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, true);

        Assert.assertFalse(gdUpdateAdGroupPayload.getValidationResult().getErrors().isEmpty());
        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(EITHER_KEYWORDS_OR_RETARGETINGS_ALLOWED.getCode())
                        .withPath("updateCpmAdGroupItems[0]")));
    }

    @Test
    public void updateCpmAdGroups_CpmGeoproductAdGroupWithoutRetargeting_WithFeature_NoError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID, true);
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        featureSteps.addClientFeature(clientInfo.getClientId(),
                FeatureName.TARGETING_IS_NOT_REQUIRED_FOR_CPM_GEOPRODUCT_GROUP, true);
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT)
                .withRetargetingCondition(null);

        updateAndCheckNoErrors(item, true);
    }

    @Test
    public void updateCpmAdGroups_CpmGeoproductAdGroupWithKeywordsAndRetargeting_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEOPRODUCT_ENABLED, true);
        featureSteps.addClientFeature(clientInfo.getClientId(),
                FeatureName.TARGETING_IS_NOT_REQUIRED_FOR_CPM_GEOPRODUCT_GROUP, true);
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT)
                .withKeywords(singletonList(new GdUpdateAdGroupKeywordItem().withPhrase("keyword")));

        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, true);

        Assert.assertFalse(gdUpdateAdGroupPayload.getValidationResult().getErrors().isEmpty());
        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(EITHER_KEYWORDS_OR_RETARGETINGS_ALLOWED.getCode())
                        .withPath("updateCpmAdGroupItems[0]")));
    }

    @Test
    public void updateCpmAdGroups_AddCpmGeoPinAdGroup_WithGeoPinAdGroup_NoError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED, true);
        createCpmGeoPinUserProfileAdGroup();
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEO_PIN);

        updateAndCheckNoErrors(item, true);
    }

    @Test
    public void updateCpmAdGroups_AddNotGeoPAdGroup_WithCpmGeoPinAdGroup_ValidationError() {
        featureSteps.addClientFeature(clientInfo.getClientId(), FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED, true);
        createCpmGeoPinUserProfileAdGroup();
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_BANNER);

        GdUpdateAdGroupPayload gdUpdateAdGroupPayload = update(item, true);
        Assert.assertThat(gdUpdateAdGroupPayload.getValidationResult().getErrors(), containsInAnyOrder(
                new GdDefect()
                        .withCode(AD_GROUP_TYPE_NOT_SUPPORTED.getCode())
                        .withPath("updateCpmAdGroupItems[0].type")));
    }

    private Long createCpmKeywordAdGroup() {
        List<GdUpdateAdGroupKeywordItem> keywords = asList(
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1),
                new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD2));

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withType(CPM_BANNER)
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupName(AD_GROUP_NAME1)
                .withRegionIds(singletonList((int) SAINT_PETERSBURG_REGION_ID))
                .withGeneralPrice(PRICE1)
                .withKeywords(keywords);

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private GdUpdateAdGroupPayload createCpmGeoproductKeywordAdGroup() {
        List<GdUpdateAdGroupKeywordItem> keywords =
                singletonList(new GdUpdateAdGroupKeywordItem().withPhrase(KEYWORD1));

        GdUpdateCpmAdGroupItem item = new GdUpdateCpmAdGroupItem()
                .withType(CPM_GEOPRODUCT)
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupName(AD_GROUP_NAME1)
                .withRegionIds(singletonList((int) SAINT_PETERSBURG_REGION_ID))
                .withGeneralPrice(PRICE1)
                .withKeywords(keywords);

        return update(item, true);
    }

    private void checkKeywordAdGroup(Long adGroupId, CpmBannerAdGroup expectedAdGroup, List<Keyword> expectedKeywords) {
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);
        List<Keyword> actualKeywords = keywordRepository.getKeywordsByAdGroupId(SHARD, adGroupId);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualAdGroup)
                    .is(matchedBy(beanDiffer(expectedAdGroup)
                            .useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(actualKeywords)
                    .is(matchedBy(beanDiffer(expectedKeywords)
                            .useCompareStrategy(onlyExpectedFields())));
        });
    }

    private Long createCpmUserProfileAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_BANNER);

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private Long createCpmGeoproductUserProfileAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEOPRODUCT);

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private Long createCpmContentCategoriesAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_BANNER)
                .withRetargetingCondition(null)
                .withContentCategoriesRetargetingConditionRules(singletonList(createContentCategoriesRuleItem(goal4)));

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private Long createCpmGeoproductUserProfileAdGroupAutobudget() {
        GdUpdateCpmAdGroupItem item = createUserProfileItemAutoBudget()
                .withType(CPM_GEOPRODUCT);

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private Long createCpmGeoPinUserProfileAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_GEO_PIN);

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private Long createCpmAudioAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_AUDIO)
                .withVideoGoals(singletonList(START));

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private Long createCpmVideoAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_VIDEO)
                .withVideoGoals(singletonList(START));

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private void checkVideoGoals(Long adGroupId, AdShowType videoGoalsType) {
        List<UsersSegment> actualVideoGoals = usersSegmentRepository.getSegments(SHARD, singletonList(adGroupId));
        List<UsersSegment> expectedVideoGoals = singletonList(new UsersSegment().withType(videoGoalsType));
        assertThat(actualVideoGoals)
                .is(matchedBy(beanDiffer(expectedVideoGoals)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    private Long createCpmOutdoorAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_OUTDOOR)
                .withRegionIds(null)
                .withPageBlocks(singletonList(new GdAdGroupPageBlock()
                        .withPageId(OUTDOOR1)
                        .withImpId(BLOCK_ID)));

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private Long createCpmIndoorAdGroup() {
        GdUpdateCpmAdGroupItem item = createUserProfileItem()
                .withType(CPM_INDOOR)
                .withRegionIds(null)
                .withPageBlocks(singletonList(new GdAdGroupPageBlock()
                        .withPageId(INDOOR1)
                        .withImpId(BLOCK_ID)));

        return updateAndCheckNoErrors(item, true).get(0).getAdGroupId();
    }

    private GdUpdateCpmAdGroupItem createUserProfileItem() {
        GdRetargetingConditionRuleItemReq rule = new GdRetargetingConditionRuleItemReq()
                .withType(OR)
                .withInterestType(short_term)
                .withGoals(singletonList(new GdGoalMinimal().withId(goal1.getId())));
        GdUpdateCpmRetargetingConditionItem retargetingConditionItem = new GdUpdateCpmRetargetingConditionItem()
                .withConditionRules(singletonList(rule));

        return new GdUpdateCpmAdGroupItem()
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupName(AD_GROUP_NAME1)
                .withRegionIds(singletonList((int) SAINT_PETERSBURG_REGION_ID))
                .withRetargetingCondition(retargetingConditionItem)
                .withGeneralPrice(PRICE1);
    }

    private GdRetargetingConditionRuleItemReq createContentCategoriesRuleItem(Goal contentCategory) {
        return new GdRetargetingConditionRuleItemReq()
                .withType(OR)
                .withGoals(singletonList(new GdGoalMinimal().withId(contentCategory.getId())));
    }

    private GdUpdateCpmAdGroupItem createUserProfileItemAutoBudget() {
        GdRetargetingConditionRuleItemReq rule = new GdRetargetingConditionRuleItemReq()
                .withType(OR)
                .withInterestType(short_term)
                .withGoals(singletonList(new GdGoalMinimal().withId(goal1.getId())));
        GdUpdateCpmRetargetingConditionItem retargetingConditionItem = new GdUpdateCpmRetargetingConditionItem()
                .withConditionRules(singletonList(rule));

        return new GdUpdateCpmAdGroupItem()
                .withCampaignId(autoBudgetCampaignInfo.getCampaignId())
                .withAdGroupName(AD_GROUP_NAME1)
                .withRegionIds(singletonList((int) SAINT_PETERSBURG_REGION_ID))
                .withRetargetingCondition(retargetingConditionItem);
    }

    private GdUpdateCpmAdGroupItem createChangedUserProfileItem(Long adGroupId) {
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

    private GdUpdateCpmAdGroupItem createChangedContentCategoriesItem(Long adGroupId) {
        return createUserProfileItem()
                .withType(CPM_BANNER)
                .withAdGroupId(adGroupId)
                .withRetargetingCondition(null)
                .withContentCategoriesRetargetingConditionRules(singletonList(createContentCategoriesRuleItem(goal5)));
    }

    private void checkUserProfileAdGroup(Long adGroupId, AdGroup expectedCpmAdGroup,
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
