package ru.yandex.direct.core.copyentity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithKeywordsService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithRelevanceMatchesService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase;
import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalInterest;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RuleInterest;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.entity.retargeting.service.AdGroupWithRetargetingsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestKeywords;
import ru.yandex.direct.core.testing.data.TestRetargetings;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.amazonaws.util.CollectionUtils.isNullOrEmpty;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.TABLET;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting.CELLULAR;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting.WI_FI;
import static ru.yandex.direct.core.entity.retargeting.Constants.INTEREST_LINK_TIME_VALUE;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileContentCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentFromStoreUrl;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.fullMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.feature.FeatureName.TARGET_TAGS_ALLOWED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(JUnitParamsRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationMobileContentAdGroupSameClientDifferentCampaignTest extends AdGroupsAddOperationTestBase {

    private static final String TRACKING_URL =
            "http://" + TrustedRedirectSteps.DOMAIN + "/newnewnew?aaa=" + RandomNumberUtils.nextPositiveLong();
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.ya.test";
    private static final String MINIMAL_OPERATING_SYSTEM_VERSION = "8.0";
    private static final String GROUP_NAME = "Mobile Content Group";
    private static final List<String> MINUS_KEYWORD = List.of("minus1", "minus2");
    private static final List<String> PAGE_GROUP_TAGS = List.of("tag1", "tag2");
    private static final List<Long> GEO = List.of(5L, 15L);
    private static final Long TARGETING_CATEGORY_ID = 154L;
    private static final BigInteger TARGETING_IMPORT_ID = BigInteger.valueOf(555L);
    private static final TargetingCategory TARGETING_CATEGORY =
            new TargetingCategory(TARGETING_CATEGORY_ID, null, "", "", TARGETING_IMPORT_ID, true);
    private static final LocalDateTime LAST_CHANGE = LocalDateTime.of(2000, 1, 1, 0, 0);

    private static final RecursiveComparisonConfiguration COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
            .withIgnoreAllExpectedNullFields(true)
            .build();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CopyOperationFactory factory;
    @Autowired
    private CopyOperationAssert asserts;
    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;
    @Autowired
    private AdGroupWithKeywordsService adGroupWithKeywordsService;
    @Autowired
    private AdGroupWithRetargetingsService adGroupWithRetargetingsService;
    @Autowired
    private AdGroupWithRelevanceMatchesService adGroupWithRelevanceMatchesService;
    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;
    @Autowired
    private RetargetingConditionRepository retargetingConditionRepository;

    private Long uid;
    private Client client;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private CampaignInfo mobileContentCampaignInfoFrom;
    private Long mobileContentCampaignIdFrom;
    private Long mobileContentCampaignIdTo;
    private MobileContent mobileContent;
    private Callout firstCallout;
    private Callout secondCallout;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        client = clientInfo.getClient();

        steps.trustedRedirectSteps().addValidMobileCounter();

        steps.featureSteps().addClientFeature(clientId, TARGET_TAGS_ALLOWED, true);

        firstCallout = steps.calloutSteps().createDefaultCallout(clientInfo);
        secondCallout = steps.calloutSteps().createDefaultCallout(clientInfo);

        mobileContentCampaignInfoFrom = steps.campaignSteps().createCampaign(
                activeMobileContentCampaign(clientId, clientInfo.getUid())
                        .withEmail("test_from@yandex-team.ru"),
                clientInfo);
        mobileContentCampaignIdFrom = mobileContentCampaignInfoFrom.getCampaignId();

        CampaignInfo mobileContentCampaignInfoTo = steps.campaignSteps().createCampaign(
                activeMobileContentCampaign(clientId, clientInfo.getUid())
                        .withEmail("test_to@yandex-team.ru"),
                clientInfo);
        mobileContentCampaignIdTo = mobileContentCampaignInfoTo.getCampaignId();

        MobileContentInfo mobileContentInfo = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContent(mobileContentFromStoreUrl(STORE_URL)));
        mobileContent = mobileContentInfo.getMobileContent();

        testTargetingCategoriesRepository.addTargetingCategory(TARGETING_CATEGORY);

        asserts.init(clientId, clientId, uid);
    }

    @After
    public void after() {
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void copyAdGroup() {
        var addedAdGroup = createAdGroupToCopy(Set.of(WI_FI), Set.of(PHONE));

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        MobileContentAdGroup expectedAdGroup = getExpectedAdGroup();

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        var copiedAdGroup = actualAdGroup(copiedAdGroupIds.get(0));

        assertThat(copiedAdGroup)
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expectedAdGroup);
    }

    public static Object[] networkTargetingParameters() {
        return new Object[][]{
                {"Передан список из WI_FI -> нет ошибок", Set.of(WI_FI)},
                {"Передан список из CELLULAR -> нет ошибок", Set.of(CELLULAR)},
                {"Передан список из WI_FI,CELLULAR -> нет ошибок", Set.of(WI_FI, CELLULAR)}};
    }

    /**
     * Проверка копирования РМП группы при разных значениях таргетинга на тип подключения к сети (networkTargeting)
     */
    @Test
    @Parameters(method = "networkTargetingParameters")
    @TestCaseName("{0}")
    public void copyAdGroup_CheckNetworkTargeting(@SuppressWarnings("unused") String description,
                                                  Set<MobileContentAdGroupNetworkTargeting> networkTargeting) {
        var addedAdGroup = createAdGroupToCopy(networkTargeting, Set.of(PHONE));

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        var copiedAdGroup = actualAdGroup(copiedAdGroupIds.get(0));

        assertThat(copiedAdGroup.getNetworkTargeting())
                .as("таргетинг на тип подключения к сети")
                .isEqualTo(networkTargeting);
    }

    public static Object[] deviceTypeTargetingParameters() {
        return new Object[][]{
                {"Передан список из PHONE -> нет ошибок", Set.of(PHONE)},
                {"Передан список из TABLET -> нет ошибок", Set.of(TABLET)},
                {"Передан список из PHONE,TABLET -> нет ошибок", Set.of(PHONE, TABLET)}
        };
    }

    /**
     * Проверка копирования РМП группы при разных значениях таргетинга на мобильное устройство (DeviceTypeTargeting)
     */
    @Test
    @Parameters(method = "deviceTypeTargetingParameters")
    @TestCaseName("{0}")
    public void copyAdGroup_CheckDeviceTargeting(@SuppressWarnings("unused") String description,
                                                 Set<MobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings) {
        var addedAdGroup = createAdGroupToCopy(Set.of(WI_FI), deviceTypeTargetings);

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        var copiedAdGroup = actualAdGroup(copiedAdGroupIds.get(0));

        assertThat(copiedAdGroup.getDeviceTypeTargeting())
                .as("таргетинг на мобильное устройство")
                .isEqualTo(deviceTypeTargetings);
    }

    /**
     * Проверка копирования ретаргетинга РМП группы
     */
    @Test
    public void copyAdGroupWithRetargeting() {
        var adGroupInfo = createAdGroupToCopy();
        var retCondInfo = steps.retConditionSteps().createDefaultRetCondition(clientInfo);
        TargetInterest retargeting = TestRetargetings
                .defaultTargetInterest(
                        adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), retCondInfo.getRetConditionId())
                .withPriceContext(BigDecimal.valueOf(42));

        steps.retargetingSteps().addRetargeting(shard, retargeting);

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");

        var copiedRetargetingIds =
                adGroupWithRetargetingsService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Retargeting.class, copiedRetargetingIds,
                List.of(retargeting), COPIED);
    }

    /**
     * Проверка копирования интереса РМП группы
     */
    @Test
    public void copyAdGroupWithInterest() {
        var adGroupInfo = createAdGroupToCopy();

        GoalInterest goalInterest = new GoalInterest(TARGETING_IMPORT_ID.longValue(), INTEREST_LINK_TIME_VALUE);
        RuleInterest ruleInterest = new RuleInterest(RuleType.ALL, List.of(goalInterest));
        RetargetingCondition retargetingCondition = (RetargetingCondition) defaultRetCondition(clientId)
                .withRules(singletonList(ruleInterest))
                .withInterest(true);
        var retCondInfo = steps.retConditionSteps().createRetCondition(retargetingCondition, clientInfo);

        steps.retargetingSteps().createRetargeting(
                defaultRetargeting(mobileContentCampaignIdFrom, adGroupInfo.getAdGroupId(),
                        retCondInfo.getRetConditionId()),
                adGroupInfo, retCondInfo);

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");

        Map<Long, List<RetargetingCondition>> retConditionsByAdGroupIds =
                retargetingConditionRepository.getRetConditionsByAdGroupIds(shard, copiedAdGroupIds);

        RetargetingCondition expectInterest = (RetargetingCondition) new RetargetingCondition()
                .withRules(singletonList(ruleInterest))
                .withInterest(true)
                .withType(ConditionType.metrika_goals)
                .withDeleted(false);

        assertSoftly(soft -> {
            soft.assertThat(retConditionsByAdGroupIds)
                    .as("условия ретаргетинга")
                    .containsOnlyKeys(copiedAdGroupIds.get(0));
            soft.assertThat(retConditionsByAdGroupIds.get(copiedAdGroupIds.get(0)))
                    .as("условие для нацеливания по интересам")
                    .is(matchedBy(beanDiffer(singletonList(expectInterest)).useCompareStrategy(onlyExpectedFields())));
        });
    }

    /**
     * При копировании РМП группы время последней модификации (LastChange) не копируется
     */
    @Test
    public void copyAdGroupWithLastChange() {
        var addedAdGroup = createAdGroupToCopy(Set.of(WI_FI), Set.of(PHONE));
        var adGroupId = addedAdGroup.getId();

        var xerox = factory.build(copyConfig(adGroupId));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getLastChange()).isNotEqualTo(LAST_CHANGE);
    }

    /**
     * При копировании РМП группы вероятность загрузки группы в движок БК (is_bs_rarely_loaded) не копируется
     */
    @Test
    public void copyAdGroupWithBsRarelyLoaded() {
        var addedAdGroup = createAdGroupToCopy(Set.of(WI_FI), Set.of(PHONE));

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getBsRarelyLoaded()).isFalse();
    }

    /**
     * Проверка копирования баннера РМП группы
     */
    @Test
    public void copyAdGroupWithBanner() {
        AdGroupInfo adGroupInfo = createAdGroupToCopy();

        var bannerInfo = steps.mobileAppBannerSteps().createMobileAppBanner(
                new NewMobileAppBannerInfo()
                        .withBanner(fullMobileAppBanner(mobileContentCampaignIdFrom, adGroupInfo.getAdGroupId())
                                .withHref(TRACKING_URL)
                                .withFlags(new BannerFlags().with(BannerFlags.AGE, Age.AGE_6))
                                .withStatusModerate(BannerStatusModerate.READY)
                                .withDomain(TrustedRedirectSteps.DOMAIN)
                                .withCalloutIds(List.of(firstCallout.getId(), secondCallout.getId()))
                                .withReflectedAttributes(Map.of(
                                        NewReflectedAttribute.PRICE, true,
                                        NewReflectedAttribute.ICON, true,
                                        NewReflectedAttribute.RATING, false,
                                        NewReflectedAttribute.RATING_VOTES, false)))
                        .withAdGroupInfo(adGroupInfo));

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        Set<Long> copiedBannerIds =
                adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);

        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    /**
     * Проверка копирования ключевых слов РМП группы
     */
    @Test
    public void copyAdGroupWithKeyword() {
        AdGroupInfo adGroupInfo = createAdGroupToCopy();

        var keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo,
                TestKeywords.defaultKeyword()
                        .withPrice(BigDecimal.ZERO)
                        .withPriceContext(BigDecimal.ZERO));

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedKeywordIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Keyword.class, copiedKeywordIds, List.of(keywordInfo.getKeyword()), COPIED);
    }

    /**
     * Проверка копирования настроек автотаргетинга
     */
    @Test
    public void copyAdGroupWithRelevanceMatch() {
        AdGroupInfo adGroupInfo = createAdGroupToCopy();

        var relMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupInfo);
        steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(List.of(relMatch), adGroupInfo);

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedRelMatchIds =
                adGroupWithRelevanceMatchesService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(RelevanceMatch.class, copiedRelMatchIds, List.of(relMatch), COPIED);
    }

    private AdGroupInfo createAdGroupToCopy() {
        return new AdGroupInfo()
                .withCampaignInfo(mobileContentCampaignInfoFrom)
                .withClientInfo(clientInfo)
                .withAdGroup(createAdGroupToCopy(Set.of(WI_FI), Set.of(PHONE)));
    }

    private MobileContentAdGroup createAdGroupToCopy(Set<MobileContentAdGroupNetworkTargeting> networkTargeting,
                                                     Set<MobileContentAdGroupDeviceTypeTargeting> deviceTypeTargetings) {
        var adGroup = activeMobileAppAdGroup(mobileContentCampaignIdFrom)
                .withStoreUrl(STORE_URL)
                .withMinimalOperatingSystemVersion(MINIMAL_OPERATING_SYSTEM_VERSION)
                .withName(GROUP_NAME)
                .withNetworkTargeting(networkTargeting)
                .withDeviceTypeTargeting(deviceTypeTargetings)
                .withMinusKeywords(MINUS_KEYWORD)
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withGeo(GEO)
                .withMobileContentId(mobileContent.getId())
                .withBsRarelyLoaded(true)
                .withLastChange(LAST_CHANGE);

        MassResult<Long> result = createAddOperation(Applicability.FULL, List.of(adGroup), uid, clientId,
                geoTree, shard, true)
                .prepareAndApply();
        Assert.assertThat(result, isFullySuccessful());
        return actualAdGroup(result.get(0).getResult());
    }

    private MobileContentAdGroup getExpectedAdGroup() {
        return new MobileContentAdGroup()
                .withCampaignId(mobileContentCampaignIdTo)
                .withType(AdGroupType.MOBILE_CONTENT)
                .withStoreUrl(STORE_URL)
                .withMinimalOperatingSystemVersion(MINIMAL_OPERATING_SYSTEM_VERSION)
                .withNetworkTargeting(Set.of(WI_FI))
                .withDeviceTypeTargeting(Set.of(PHONE))
                .withMinusKeywords(MINUS_KEYWORD)
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withGeo(GEO)
                .withMobileContentId(mobileContent.getId());
    }

    private MobileContentAdGroup actualAdGroup(Long adGroupId) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(adGroupId));
        checkState(!isNullOrEmpty(adGroups), "AdGroup not found");
        return (MobileContentAdGroup) adGroups.get(0);
    }

    private CopyConfig copyConfig(Long copyId) {
        return CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, copyId, mobileContentCampaignIdFrom, mobileContentCampaignIdTo, uid);
    }
}
