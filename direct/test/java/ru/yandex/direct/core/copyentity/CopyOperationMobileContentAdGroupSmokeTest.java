package ru.yandex.direct.core.copyentity;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners;
import ru.yandex.direct.core.testing.data.TestNewImageBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewCpcVideoBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TrustedRedirectSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;

/**
 * Проверяем копирование групп РМП
 * Проверка остальных объектов группы РМП в {@link CopyOperationCommonAdGroupSmokeTest}
 */
@SuppressWarnings("rawtypes")
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationMobileContentAdGroupSmokeTest {

    private static final String TRACKING_URL =
            "http://" + TrustedRedirectSteps.DOMAIN + "/test?key=" + RandomNumberUtils.nextPositiveLong();

    @Autowired
    private AdGroupService adgroupService;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;
    @Autowired
    private BannerTypedRepository newBannerTypedRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private TestNewCpcVideoBanners testNewCpcVideoBanners;

    private Long uid;
    private ClientInfo clientInfoFrom;
    private ClientId clientIdFrom;
    private Client clientFrom;
    private ClientInfo clientInfoTo;
    private Client clientTo;
    private int shardFrom;
    private Long campaignIdFrom;
    private Long campaignIdDifferentClient;
    private Long adGroupIdFrom;

    @Autowired
    private CopyOperationFactory factory;

    @Before
    public void setUp() {
        var superInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superInfo.getUid();

        steps.trustedRedirectSteps().addValidMobileCounter();

        prepareClientData();
        prepareDifferentClientData();
    }

    @After
    public void after() {
        steps.trustedRedirectSteps().deleteTrusted();
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

        var copiedBannerIds = adGroupWithBannersService
                .getChildEntityIdsByParentIds(clientIdFrom, uid, copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientIdFrom, uid, copiedBannerIds);
        soft.assertThat(copiedBanners)
                .as("баннеры")
                .hasSize(2);

        var copiedCpcVideoBanner = newBannerTypedRepository.getSafely(shardFrom, copiedBannerIds,
                CpcVideoBanner.class);
        soft.assertThat(copiedCpcVideoBanner)
                .as("видео баннер")
                .hasSize(1);

        var copiedImageBanner = newBannerTypedRepository.getSafely(shardFrom, copiedBannerIds, ImageBanner.class);
        soft.assertThat(copiedImageBanner)
                .as("графический баннер")
                .hasSize(1);

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentCampaignSameClient() {
        var campaignInfoTo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfoFrom);
        Long campaignIdTo = campaignInfoTo.getCampaignId();

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfoFrom, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);

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

        var copiedBannerIds = adGroupWithBannersService
                .getChildEntityIdsByParentIds(clientIdFrom, uid, copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientIdFrom, uid, copiedBannerIds);
        soft.assertThat(copiedBanners)
                .as("баннеры")
                .hasSize(2);

        var copiedCpcVideoBanner = newBannerTypedRepository.getSafely(shardFrom, copiedBannerIds,
                CpcVideoBanner.class);
        soft.assertThat(copiedCpcVideoBanner)
                .as("видео баннер")
                .hasSize(1);

        var copiedImageBanner = newBannerTypedRepository.getSafely(shardFrom, copiedBannerIds, ImageBanner.class);
        soft.assertThat(copiedImageBanner)
                .as("графический баннер")
                .hasSize(1);

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentClient() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupBetweenClientsCopyConfig(
                clientInfoFrom, clientInfoTo, adGroupIdFrom, campaignIdFrom, campaignIdDifferentClient, uid);

        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();

        // Копирование CPC_VIDEO между клиентами пока не реализовано
        assertThat(copyResult.getMassResult().getValidationResult().flattenErrors()).isNotEmpty();
    }

    private void checkErrors(MassResult massResult) {
        assertThat(massResult.getValidationResult().flattenErrors()).isEmpty();
    }

    private void prepareClientData() {
        clientInfoFrom = steps.clientSteps().createDefaultClient();

        clientFrom = clientInfoFrom.getClient();
        clientIdFrom = clientInfoFrom.getClientId();
        shardFrom = clientInfoFrom.getShard();

        var campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfoFrom);
        campaignIdFrom = campaignInfo.getCampaignId();

        var adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(campaignInfo);
        adGroupIdFrom = adGroupInfo.getAdGroupId();

        Creative creative = defaultCpcVideoForCpcVideoBanner(clientIdFrom, null);
        var creativeInfo = steps.creativeSteps().createCreative(creative, clientInfoFrom);
        steps.cpcVideoBannerSteps().createBanner(
                new NewCpcVideoBannerInfo()
                        .withBanner(testNewCpcVideoBanners.fullCpcVideoBanner(creativeInfo.getCreativeId())
                                .withHref(TRACKING_URL))
                        .withAdGroupInfo(adGroupInfo));

        String imageHash = steps.bannerSteps().createImageAdImageFormat(clientInfoFrom).getImageHash();

        ImageBanner imageBanner = TestNewImageBanners
                .fullImageBannerWithImage(campaignIdFrom, adGroupIdFrom, imageHash)
                .withHref(TRACKING_URL);
        steps.imageBannerSteps().createImageBanner(imageBanner, adGroupInfo);
    }

    private void prepareDifferentClientData() {
        clientInfoTo = steps.clientSteps().createDefaultClient();
        clientTo = clientInfoTo.getClient();

        var campaignInfoDifferentClient = steps.campaignSteps().createActiveMobileAppCampaign(clientInfoTo);
        campaignIdDifferentClient = campaignInfoDifferentClient.getCampaignId();
    }
}
