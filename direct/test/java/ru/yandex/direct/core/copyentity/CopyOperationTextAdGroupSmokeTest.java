package ru.yandex.direct.core.copyentity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBidModifiersService;
import ru.yandex.direct.core.entity.banner.model.BannerWithCreativeModeration;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.retargeting.service.AdGroupWithRetargetingsService;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;

/**
 * Проверяем копирование групп ТГО
 * Проверка остальных объектов группы ТГО в {@link CopyOperationCommonAdGroupSmokeTest}
 */
@SuppressWarnings("rawtypes")
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationTextAdGroupSmokeTest {
    @Autowired
    private AdGroupService adgroupService;

    @Autowired
    private RetargetingService retargetingService;

    @Autowired
    private AdGroupWithRetargetingsService adGroupWithRetargetingsService;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private AdGroupWithBidModifiersService adGroupWithBidModifiersService;

    @Autowired
    private BannerCreativeRepository newBannerCreativeRepository;

    @Autowired
    private Steps steps;

    private Long uid;

    private ClientInfo clientInfoFrom;
    private ClientId clientIdFrom;
    private ClientInfo clientInfoTo;

    private Long campaignIdFrom;
    private Long campaignIdTo;
    private Long campaignIdToDifferentClient;
    private Long adGroupIdFrom;

    @Autowired
    private CopyOperationFactory factory;

    @Before
    public void setUp() {
        var superInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superInfo.getUid();
        steps.featureSteps().setCurrentClient(superInfo.getClientId());
        clientInfoFrom = steps.clientSteps().createDefaultClient();

        clientIdFrom = clientInfoFrom.getClientId();

        var campaignInfoFrom = steps.campaignSteps().createActiveTextCampaign(clientInfoFrom);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfoFrom);
        adGroupIdFrom = adGroupInfo.getAdGroupId();

        var bannerInfo1 = steps.bannerSteps().createBanner(activeTextBanner().withTitle("copy-banner-1"), adGroupInfo);
        var bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo1);
        var retCondInfo = steps.retConditionSteps().createDefaultRetCondition(clientInfoFrom);
        var retInfo = steps.retargetingSteps().createRetargeting(
                defaultRetargeting().withPriceContext(BigDecimal.valueOf(42)), adGroupInfo, retCondInfo);
        var bidModifier = steps.bidModifierSteps().createDefaultAdGroupBidModifierMobile(adGroupInfo);

        Long creativeId = steps.creativeSteps().getNextCreativeId();
        var creative = steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfoFrom, creativeId);
        @SuppressWarnings("unchecked")
        var bannerCreative = steps.bannerCreativeSteps().createTextBannerCreative(
                new BannerCreativeInfo<>()
                        .withBannerInfo((AbstractBannerInfo) bannerInfo1)
                        .withCreativeInfo(creative));

        var bannerInfoFromOtherCampaign = steps.bannerSteps()
                .createBanner(activeTextBanner().withTitle("banner-image-banner"));
        steps.bannerSteps().createBannerImage(
                bannerInfoFromOtherCampaign, bannerImageInfo.getBannerImageFormat(), new OldBannerImage());

        clientInfoTo = steps.clientSteps().createDefaultClient();

        var campaignInfoTo = steps.campaignSteps().createActiveTextCampaign(clientInfoFrom);
        campaignIdTo = campaignInfoTo.getCampaignId();

        var campaignInfoDifferentClient = steps.campaignSteps().createActiveTextCampaign(clientInfoTo);
        campaignIdToDifferentClient = campaignInfoDifferentClient.getCampaignId();
    }


    @Test
    public void copyAdGroupSameCampaign() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfoFrom, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();

        checkErrors(copyResult.getMassResult());

        @SuppressWarnings("unchecked")
        Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                .select(Long.class)
                .toSet();
        var copiedAdGroups = adgroupService.get(clientIdFrom, uid, copiedAdGroupIds);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(copiedAdGroups)
                .as("группы")
                .hasSize(1);

        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientIdFrom, uid,
                copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientIdFrom, uid, copiedBannerIds);
        soft.assertThat(copiedBanners)
                .as("баннеры")
                .hasSize(1);
        soft.assertThat(StreamEx.of(copiedBanners)
                .select(TextBanner.class)
                .map(TextBanner::getImageHash)
                .nonNull()
                .toList())
                .as("ТГО баннер с картинкой")
                .hasSize(1);

        var copiedRetIds = adGroupWithRetargetingsService.getChildEntityIdsByParentIds(clientIdFrom, uid,
                copiedAdGroupIds);
        var copiedRetargetings = retargetingService.get(clientIdFrom, uid, copiedRetIds);
        soft.assertThat(copiedRetargetings)
                .as("ретаргетинг")
                .hasSize(1);

        var copiedBidModifierIds = adGroupWithBidModifiersService.getChildEntityIdsByParentIds(clientIdFrom, uid,
                copiedAdGroupIds);
        var copiedBidModifiers = bidModifierService.get(clientIdFrom, uid, copiedBidModifierIds);
        soft.assertThat(copiedBidModifiers)
                .as("корректировки ставок")
                .hasSize(1);

        Map<Long, BannerWithCreativeModeration> copiedBannerCreatives =
                newBannerCreativeRepository.getBannersWithNotNullCreativeByBannerId(
                        clientInfoFrom.getShard(), copiedBannerIds);
        soft.assertThat(copiedBannerCreatives)
                .as("баннер с креативом")
                .hasSize(1);

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentCampaignSameClient() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(clientInfoFrom, adGroupIdFrom, campaignIdTo, uid);

        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();

        checkErrors(copyResult.getMassResult());

        @SuppressWarnings("unchecked")
        Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                .select(Long.class)
                .toSet();
        var copiedAdGroups = adgroupService.get(clientIdFrom, uid, copiedAdGroupIds);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(copiedAdGroups)
                .as("баннеры")
                .hasSize(1);

        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientIdFrom, uid,
                copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientIdFrom, uid, copiedBannerIds);
        soft.assertThat(copiedBanners).hasSize(1);
        soft.assertThat(StreamEx.of(copiedBanners)
                .select(TextBanner.class)
                .map(TextBanner::getImageHash)
                .nonNull()
                .toList())
                .as("ТГО баннер с картинкой")
                .hasSize(1);

        var copiedRetIds = adGroupWithRetargetingsService.getChildEntityIdsByParentIds(clientIdFrom, uid,
                copiedAdGroupIds);
        var copiedRetargetings = retargetingService.get(clientIdFrom, uid, copiedRetIds);
        soft.assertThat(copiedRetargetings)
                .as("ретаргетинг")
                .hasSize(1);

        var copiedBidModifierIds = adGroupWithBidModifiersService.getChildEntityIdsByParentIds(clientIdFrom, uid,
                copiedAdGroupIds);
        var copiedBidModifiers = bidModifierService.get(clientIdFrom, uid, copiedBidModifierIds);
        soft.assertThat(copiedBidModifiers)
                .as("корректировки ставок")
                .hasSize(1);

        Map<Long, BannerWithCreativeModeration> copiedBannerCreatives =
                newBannerCreativeRepository.getBannersWithNotNullCreativeByBannerId(
                        clientInfoFrom.getShard(), copiedBannerIds);
        soft.assertThat(copiedBannerCreatives)
                .as("баннер с креативом")
                .hasSize(1);

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentClient() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupBetweenClientsCopyConfig(
                clientInfoFrom, clientInfoTo, adGroupIdFrom, campaignIdFrom, campaignIdToDifferentClient, uid);
        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        assertThat(copyResult.getMassResult().getValidationResult().flattenErrors()).isNotEmpty();
    }

    private void checkErrors(MassResult massResult) {
        assertThat(massResult.getValidationResult().flattenErrors()).isEmpty();
    }
}
