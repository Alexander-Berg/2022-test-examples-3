package ru.yandex.direct.core.copyentity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.copyentity.translations.RenameProcessor;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithKeywordsService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase;
import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestKeywords;
import ru.yandex.direct.core.testing.data.TestNewMcBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewMcBannerInfo;
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
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeMcBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.feature.FeatureName.TARGET_TAGS_ALLOWED;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("unchecked")
public class CopyOperationMcBannerAdGroupSameCampaignTest extends AdGroupsAddOperationTestBase {

    private static final String TRACKING_URL =
            "http://" + TrustedRedirectSteps.DOMAIN + "/newnewnew?aaa=" + RandomNumberUtils.nextPositiveLong();

    private static final String GROUP_NAME = "McBanner Group";
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
    @Autowired
    private AdGroupWithKeywordsService adGroupWithKeywordsService;
    @Autowired
    private RenameProcessor renameProcessor;

    private Long uid;
    private Client client;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long mcBannerCampaignId;
    private CampaignInfo mcBannerCampaignInfo;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        client = clientInfo.getClient();

        steps.featureSteps().addClientFeature(clientId, TARGET_TAGS_ALLOWED, true);

        mcBannerCampaignInfo = steps.campaignSteps().createCampaign(
                activeMcBannerCampaign(clientId, clientInfo.getUid()),
                clientInfo);
        mcBannerCampaignId = mcBannerCampaignInfo.getCampaignId();

        steps.trustedRedirectSteps().addValidCounters();

        asserts.init(clientId, clientId, uid);
    }

    @After
    public void after() {
        steps.trustedRedirectSteps().deleteTrusted();
    }

    @Test
    public void copyAdGroup() {
        McBannerAdGroup addedAdGroup = createAdGroupToCopy();

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        McBannerAdGroup expectedAdGroup = getExpectedAdGroup(addedAdGroup, xerox.getCopyContainer());

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        checkState(!copiedAdGroupIds.isEmpty(), "AdGroup not copied");
        var copiedAdGroup = actualAdGroup(copiedAdGroupIds.get(0));

        assertThat(copiedAdGroup)
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expectedAdGroup);
    }

    /**
     * При копировании ГО/McBanner группы время последней модификации (LastChange) не копируется
     */
    @Test
    public void copyAdGroupWithLastChange() {
        var addedAdGroup = createAdGroupToCopy();
        var adGroupId = addedAdGroup.getId();

        var xerox = factory.build(copyConfig(adGroupId));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getLastChange()).isNotEqualTo(LAST_CHANGE);
    }

    /**
     * При копировании ГО/McBanner группы вероятность загрузки группы в движок БК (is_bs_rarely_loaded) не копируется
     */
    @Test
    public void copyAdGroupWithBsRarelyLoaded() {
        var addedAdGroup = createAdGroupToCopy();

        var xerox = factory.build(copyConfig(addedAdGroup.getId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        List<Long> copiedAdGroupIds = List.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var adGroup = actualAdGroup(copiedAdGroupIds.get(0));
        assertThat(adGroup.getBsRarelyLoaded()).isFalse();
    }

    /**
     * Проверка копирования баннера ГО/McBanner группы
     */
    @Test
    public void copyAdGroupWithBanner() {
        AdGroupInfo adGroupInfo = createAdGroupToCopyInfo();

        String imageHash = steps.bannerSteps().createBannerImageFormat(clientInfo).getImageHash();

        var bannerInfo = steps.mcBannerSteps().createMcBanner(new NewMcBannerInfo()
                .withBanner(TestNewMcBanners
                        .fullMcBanner(mcBannerCampaignId, adGroupInfo.getAdGroupId(), imageHash)
                        .withHref(TRACKING_URL)
                        .withFlags(new BannerFlags().with(BannerFlags.AGE, Age.AGE_6))
                        .withStatusModerate(BannerStatusModerate.READY)
                        .withIsMobileImage(true)
                        .withDomain(TrustedRedirectSteps.DOMAIN))
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
     * Проверка копирования ключевых слов ГО/McBanner группы
     */
    @Test
    public void copyAdGroupWithKeyword() {
        AdGroupInfo adGroupInfo = createAdGroupToCopyInfo();

        var keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo, TestKeywords.defaultKeyword());

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        var copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedKeywordIds = adGroupWithKeywordsService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(Keyword.class, copiedKeywordIds, List.of(keywordInfo.getKeyword()), COPIED);
    }

    private AdGroupInfo createAdGroupToCopyInfo() {
        return new AdGroupInfo()
                .withCampaignInfo(mcBannerCampaignInfo)
                .withClientInfo(clientInfo)
                .withAdGroup(createAdGroupToCopy());
    }

    private McBannerAdGroup createAdGroupToCopy() {
        var adGroup = activeMcBannerAdGroup(mcBannerCampaignId)
                .withName(GROUP_NAME)
                .withMinusKeywords(MINUS_KEYWORD)
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withGeo(GEO)
                .withBsRarelyLoaded(true)
                .withLastChange(LAST_CHANGE);

        MassResult<Long> result = createAddOperation(Applicability.FULL, List.of(adGroup), uid, clientId,
                geoTree, shard, true).prepareAndApply();
        Assert.assertThat(result, isFullySuccessful());
        return actualAdGroup(result.get(0).getResult());
    }

    private McBannerAdGroup getExpectedAdGroup(AdGroup originalAdGroup, CopyOperationContainer copyContainer) {
        return new McBannerAdGroup()
                .withCampaignId(mcBannerCampaignId)
                .withType(AdGroupType.MCBANNER)
                .withName(renameProcessor.generateAdGroupCopyName(
                        originalAdGroup.getName(), originalAdGroup.getId(), copyContainer.getLocale()))
                .withMinusKeywords(MINUS_KEYWORD)
                .withPageGroupTags(PAGE_GROUP_TAGS)
                .withGeo(GEO);
    }

    private McBannerAdGroup actualAdGroup(Long adGroupId) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(adGroupId));
        checkState(!isNullOrEmpty(adGroups), "AdGroup not found");
        return (McBannerAdGroup) adGroups.get(0);
    }

    private CopyConfig copyConfig(Long copyId) {
        return CopyEntityTestUtils.adGroupCopyConfig(clientInfo, copyId, mcBannerCampaignId, uid);
    }
}
