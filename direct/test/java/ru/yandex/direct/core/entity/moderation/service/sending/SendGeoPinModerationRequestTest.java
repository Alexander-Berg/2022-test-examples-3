package ru.yandex.direct.core.entity.moderation.service.sending;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoImage;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoText;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.cpm.geopin.CpmGeoPinBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.cpm.geopin.CpmGeoPinBannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.moderation.service.ModerationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmGeoPinBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.testing.data.TestBanners.ANOTHER_DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmGeoPinBanner;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmGeoPinAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoPinAdGroup;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendGeoPinModerationRequestTest {

    private static final long DEFAULT_VERSION = 2L;
    private static final long PERMALINK_ID = 123L;

    private static final String LOGO_URL = "http://ya.ru/logo";
    private static final String IMAGE_URL = "http://ya.ru/image";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String DOMAIN = "direct.yandex.ru";
    private static final String BUTTON_LINK = "http://" + DOMAIN + "/button";
    private static final String PHONE = "71234567890";

    @Autowired
    TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    OldBannerRepository bannerRepository;

    @Autowired
    ModerationService moderationService;

    @Autowired
    CpmGeoPinBannerSender cpmGeoPinBannerSender;

    private CpmGeoPinBannerInfo bannerInfo;
    private Creative creative;
    private int shard;
    private ClientId clientId;

    @Before
    public void before() {
        AdGroupInfo adGroupInfo =
                steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                        .withAdGroup(activeCpmGeoPinAdGroup(null).withGeo(Collections.singletonList(Region.MOSCOW_REGION_ID)))
                        .withClientInfo(new ClientInfo().withClient(defaultClient())));
        bannerInfo = steps.bannerSteps().createActiveCpmGeoPinBanner(
                activeCpmGeoPinBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), null, PERMALINK_ID)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.READY)
                        .withStatusBsSynced(StatusBsSynced.NO),
                adGroupInfo);

        creative = defaultCpmGeoPinAddition(adGroupInfo.getClientId(), null);
        creative.getModerationInfo().setImages(List.of(
                new ModerationInfoImage().withType("logo").withUrl(LOGO_URL),
                new ModerationInfoImage().withType("image").withUrl(IMAGE_URL)
        ));
        creative.getModerationInfo().setTexts(List.of(
                new ModerationInfoText().withType("headline").withText(NAME),
                new ModerationInfoText().withType("description").withText(DESCRIPTION),
                new ModerationInfoText().withType("domain").withText(BUTTON_LINK),
                new ModerationInfoText().withType("phone").withText(PHONE)
        ));
        steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());

        testBannerCreativeRepository.linkBannerWithCreative(bannerInfo, creative.getId());

        testModerationRepository.createBannerVersion(adGroupInfo.getShard(), bannerInfo.getBannerId(), DEFAULT_VERSION);

        shard = bannerInfo.getShard();
        clientId = bannerInfo.getClientId();
    }

    @Test
    public void moderateOneGeoPinBanner_CheckRequestData() {
        BannerModerationMeta expectedRequestMeta = getExpectedBannerModerationMeta(bannerInfo);

        CpmGeoPinBannerModerationRequest request = makeCpmGeoPinModerationRequests(bannerInfo);
        BannerModerationMeta requestMeta = request.getMeta();

        assertThat("мета соответствует ожиданию", requestMeta, beanDiffer(expectedRequestMeta));

        CpmGeoPinBannerRequestData expectedRequestData = getExpectedCpmGeoPinBannerRequestData(bannerInfo, creative);
        CpmGeoPinBannerRequestData requestData = request.getData();

        assertThat("data соответствует ожиданию", requestData, beanDiffer(expectedRequestData));
    }

    @Test
    public void moderateOneGeoPinBanner_ClientWithAsapFlag_CheckRequestData() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        BannerModerationMeta expectedRequestMeta = getExpectedBannerModerationMeta(bannerInfo);

        CpmGeoPinBannerModerationRequest request = makeCpmGeoPinModerationRequests(bannerInfo);
        BannerModerationMeta requestMeta = request.getMeta();

        assertThat("мета соответствует ожиданию", requestMeta, beanDiffer(expectedRequestMeta));

        CpmGeoPinBannerRequestData expectedRequestData = getExpectedCpmGeoPinBannerRequestData(bannerInfo, creative);
        expectedRequestData.setAsSoonAsPossible(true);
        CpmGeoPinBannerRequestData requestData = request.getData();

        assertThat("data соответствует ожиданию", requestData, beanDiffer(expectedRequestData));
    }

    @Test
    public void moderateOneGeoPinBanner_ClientWithNoAsapFlag_NoAsapFlagInRequest() {
        CpmGeoPinBannerModerationRequest request = makeCpmGeoPinModerationRequests(bannerInfo);
        CpmGeoPinBannerRequestData requestData = request.getData();

        assertThat("data соответствует ожиданию", toJson(requestData), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void moderateOneGeoPinBanner_CheckDataInDb() {
        makeCpmGeoPinModerationRequests(bannerInfo);

        long bannerVersion =
                testModerationRepository.getBannerVersion(bannerInfo.getShard(), bannerInfo.getBannerId());
        assertEquals(DEFAULT_VERSION + 1, bannerVersion);

        assertEquals(BannersStatusmoderate.Sent,
                testModerationRepository.getStatusModerate(bannerInfo.getShard(), bannerInfo.getBannerId()));
    }

    @Test
    public void makeCpmGeoPinBannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, bannerInfo.getBannerId(),
                List.of(RemoderationType.BANNER));

        CpmGeoPinBannerModerationRequest request = makeCpmGeoPinModerationRequests(bannerInfo);

        assertFalse(steps.bannerSteps().isBannerReModerationFlagPresent(shard, bannerInfo.getBannerId()));

        BannerModerationMeta actual = request.getMeta();

        assertEquals(request.getWorkflow(), MANUAL);

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(bannerInfo.getBannerId());
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(bannerInfo.getUid());
        expected.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(DEFAULT_VERSION + 1);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeCpmGeoPinBannerModerationRequests_AutoAccept() {

        testModerationRepository.createAutoAcceptRecord(shard, bannerInfo.getBannerId(),
                Set.of(AutoAcceptanceType.TURBOLANDING));

        CpmGeoPinBannerModerationRequest request = makeCpmGeoPinModerationRequests(bannerInfo);

        assertFalse(steps.bannerSteps().isBannerReModerationFlagPresent(shard, bannerInfo.getBannerId()));

        BannerModerationMeta actual = request.getMeta();

        assertEquals(request.getWorkflow(), AUTO_ACCEPT);

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(bannerInfo.getBannerId());
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(bannerInfo.getUid());
        expected.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(DEFAULT_VERSION + 1);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private BannerModerationMeta getExpectedBannerModerationMeta(CpmGeoPinBannerInfo bannerInfo) {
        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setClientId(bannerInfo.getClientId().asLong());
        meta.setCampaignId(bannerInfo.getCampaignId());
        meta.setAdGroupId(bannerInfo.getAdGroupId());
        meta.setBannerId(bannerInfo.getBannerId());
        meta.setUid(bannerInfo.getUid());
        meta.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        meta.setVersionId(DEFAULT_VERSION + 1);
        return meta;
    }

    private CpmGeoPinBannerRequestData getExpectedCpmGeoPinBannerRequestData(CpmGeoPinBannerInfo bannerInfo,
                                                                             Creative creative) {
        CpmGeoPinBannerRequestData data = new CpmGeoPinBannerRequestData();
        data.setPermalinkId(bannerInfo.getBanner().getPermalinkId());
        data.setCreativeId(bannerInfo.getBanner().getCreativeId());
        data.setCreativePreviewUrl(creative.getPreviewUrl());
        data.setLogo(LOGO_URL);
        data.setImage(IMAGE_URL);
        data.setName(NAME);
        data.setDescription(DESCRIPTION);
        data.setDomain(DOMAIN);
        data.setButtonLink(BUTTON_LINK);
        data.setPhone(PHONE);
        data.setModerationInfo(creative.getModerationInfo());
        data.setUserFlags(emptyList());
        return data;
    }

    private CpmGeoPinBannerModerationRequest makeCpmGeoPinModerationRequests(CpmGeoPinBannerInfo bannerInfo) {
        return makeCpmGeoPinModerationRequests(Collections.singletonList(bannerInfo)).get(0);
    }

    private List<CpmGeoPinBannerModerationRequest> makeCpmGeoPinModerationRequests(List<CpmGeoPinBannerInfo> bannerInfo) {
        Consumer<List<CpmGeoPinBannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<CpmGeoPinBannerModerationRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);

        cpmGeoPinBannerSender.send(bannerInfo.get(0).getShard(), mapList(bannerInfo,
                AbstractBannerInfo::getBannerId), e -> System.currentTimeMillis(), o -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }
}
