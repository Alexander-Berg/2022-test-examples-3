package ru.yandex.direct.core.copyentity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase;
import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewDynamicBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.amazonaws.util.CollectionUtils.isNullOrEmpty;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupStatusArchived;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.feature.FeatureName.TARGET_TAGS_ALLOWED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationDynamicAdGroupSameClientDifferentCampaignTest extends AdGroupsAddOperationTestBase {

    private static final String TRACKING_URL =
            "http://" + TrustedRedirectSteps.DOMAIN + "/newnewnew?aaa=" + RandomNumberUtils.nextPositiveLong();

    private static final String GROUP_NAME = "Dynamic Group";
    private static final List<String> MINUS_KEYWORD = List.of("minus1", "minus2");
    private static final List<String> PAGE_GROUP_TAGS = List.of("tag1", "tag2");
    private static final List<Long> GEO = List.of(5L, 15L);
    private static final LocalDateTime LAST_CHANGE = LocalDateTime.of(2000, 1, 1, 0, 0);

    private static final RecursiveComparisonConfiguration COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
            .withIgnoreAllExpectedNullFields(true)
            .build();

    @Autowired
    private CopyOperationFactory factory;
    @Autowired
    private CopyOperationAssert asserts;
    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    private Long uid;
    private Client client;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long feedId;
    private CampaignInfo dynamicCampaignInfoFrom;
    private Long dynamicCampaignIdFrom;
    private Long dynamicCampaignIdTo;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        client = clientInfo.getClient();

        steps.featureSteps().addClientFeature(clientId, TARGET_TAGS_ALLOWED, true);

        dynamicCampaignInfoFrom = steps.campaignSteps().createCampaign(
                activeDynamicCampaign(clientId, clientInfo.getUid()),
                clientInfo);
        dynamicCampaignIdFrom = dynamicCampaignInfoFrom.getCampaignId();

        CampaignInfo dynamicCampaignInfoTo = steps.campaignSteps().createCampaign(
                activeDynamicCampaign(clientId, clientInfo.getUid()),
                clientInfo);
        dynamicCampaignIdTo = dynamicCampaignInfoTo.getCampaignId();

        feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();

        asserts.init(clientId, clientId, uid);
    }

    @Test
    public void copyAdGroup() {
        var addedAdGroup = createDynamicTextAdGroupToCopy();

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        DynamicAdGroup expectedAdGroup = getExpectedTextAdGroup();

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        var copiedAdGroup = actualAdGroup(copiedAdGroupIds.get(0));

        assertThat(copiedAdGroup)
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expectedAdGroup);
    }

    @Test
    public void adGroupWithArchivedBanner() {
        AdGroupInfo adGroupInfo = createAdGroupToCopyInfo();

        steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo);

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));
        CopyResult<Long> copyResult = xerox.copy();
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(copyResult.getMassResult().getValidationResult())
                .as("ошибка валидации")
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0)), adGroupStatusArchived()))));

        var copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        // архивные группы отфильтровываем
        soft.assertThat(copiedAdGroupIds)
                .as("id скопированных групп")
                .isEmpty();

        soft.assertAll();
    }

    /**
     * При копировании ДО группы время последней модификации (LastChange) не копируется
     */
    @Test
    public void copyAdGroupWithLastChange() {
        var addedAdGroup = createDynamicTextAdGroupToCopy();
        var adGroupId = addedAdGroup.getId();

        var xerox = factory.build(copyConfig(adGroupId));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getLastChange()).isNotEqualTo(LAST_CHANGE);
    }

    /**
     * При копировании ДО группы вероятность загрузки группы в движок БК (is_bs_rarely_loaded) не копируется
     */
    @Test
    public void copyAdGroupWithBsRarelyLoaded() {
        var addedAdGroup = createDynamicTextAdGroupToCopy();

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getBsRarelyLoaded()).isFalse();
    }

    /**
     * Проверка копирования баннера ДО группы
     */
    @Test
    public void copyAdGroupWithBanner() {
        AdGroupInfo adGroupInfo = createAdGroupToCopyInfo();

        String imageHash = steps.bannerSteps().createBannerImageFormat(clientInfo).getImageHash();

        var bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(new NewDynamicBannerInfo()
                .withBanner(TestNewDynamicBanners
                        .fullDynamicBanner(dynamicCampaignIdFrom, adGroupInfo.getAdGroupId())
                        .withTitle("{Dynamic title}")
                        .withHref(TRACKING_URL)
                        .withFlags(new BannerFlags().with(BannerFlags.AGE, Age.AGE_6))
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withDomain(TrustedRedirectSteps.DOMAIN)
                        .withImageHash(imageHash)
                        .withImageStatusShow(true)
                        .withImageBsBannerId(0L)
                        .withImageDateAdded(LocalDateTime.now())
                        .withImageStatusModerate(StatusBannerImageModerate.YES)
                        .withCalloutIds(emptyList())
                )
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
     * Проверка копирования ДО группы с фидом
     */
    @Test
    public void copyAdGroupWithFeed() {
        var addedAdGroup = createDynamicFeedAdGroupToCopy();
        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        DynamicAdGroup expectedAdGroup = getExpectedFeedAdGroup();

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        var copiedAdGroup = actualAdGroup(copiedAdGroupIds.get(0));

        assertThat(copiedAdGroup)
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expectedAdGroup);
    }

    private AdGroupInfo createAdGroupToCopyInfo() {
        return new AdGroupInfo()
                .withCampaignInfo(dynamicCampaignInfoFrom)
                .withClientInfo(clientInfo)
                .withAdGroup(createDynamicTextAdGroupToCopy());
    }

    private DynamicAdGroup createDynamicTextAdGroupToCopy() {
        return createDynamicAdGroupToCopy(activeDynamicTextAdGroup(dynamicCampaignIdFrom));
    }

    private DynamicAdGroup createDynamicFeedAdGroupToCopy() {
        return createDynamicAdGroupToCopy(activeDynamicFeedAdGroup(dynamicCampaignIdFrom, feedId));
    }

    private DynamicAdGroup createDynamicAdGroupToCopy(DynamicAdGroup dynamicAdGroup) {
        dynamicAdGroup.withName(GROUP_NAME)
                .withMinusKeywords(MINUS_KEYWORD)
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withGeo(GEO)
                .withBsRarelyLoaded(true)
                .withLastChange(LAST_CHANGE);

        MassResult<Long> result = createAddOperation(Applicability.FULL, List.of(dynamicAdGroup), uid, clientId,
                geoTree, shard, true).prepareAndApply();
        Assert.assertThat(result, isFullySuccessful());
        return actualAdGroup(result.get(0).getResult());
    }

    private DynamicAdGroup getExpectedTextAdGroup() {
        return fillExpecteAdGroup(new DynamicTextAdGroup());
    }

    private DynamicAdGroup getExpectedFeedAdGroup() {
        return fillExpecteAdGroup(new DynamicFeedAdGroup().withFeedId(feedId));
    }

    private DynamicAdGroup fillExpecteAdGroup(DynamicAdGroup dynamicAdGroup) {
        return dynamicAdGroup.withCampaignId(dynamicCampaignIdTo)
                .withType(AdGroupType.DYNAMIC)
                .withMinusKeywords(MINUS_KEYWORD)
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withGeo(GEO);
    }

    private DynamicAdGroup actualAdGroup(Long adGroupId) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(adGroupId));
        checkState(!isNullOrEmpty(adGroups), "AdGroup not found");
        return (DynamicAdGroup) adGroups.get(0);
    }

    private CopyConfig copyConfig(Long copyId) {
        return CopyEntityTestUtils.adGroupCopyConfig(
                clientInfo, copyId, dynamicCampaignIdFrom, dynamicCampaignIdTo, uid);
    }
}
