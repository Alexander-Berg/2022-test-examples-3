package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerRequestData;
import ru.yandex.direct.core.entity.moderation.model.mobile_content.MobileAppModerationData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewImageBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.adgroup.repository.AdGroupMappings.geoToDb;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.entity.moderation.service.sending.Html5BannerSender.INITIAL_VERSION;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.fullImageBannerWithCreative;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@CoreTest
@RunWith(SpringRunner.class)
public class SendMobileHtml5ModerationRequestTest {
    public static final String HREF_BASE = "https://www.yandex.ru/?ad_id=";
    public static final String HREF_WITH_PARAMS = HREF_BASE + "{ad_id}";

    @Autowired
    private Steps steps;

    @Autowired
    private MobileHtml5BannerSender html5BannerSender;

    private int shard;
    private NewImageBannerInfo bannerInfo;
    private ImageBanner banner;
    private ClientInfo clientInfo;
    private ClientId clientId;
    private TurboLanding turbolanding;
    private MobileContentAdGroup mobileContentAdGroup;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        turbolanding = steps.turboLandingSteps().createDefaultTurboLanding(clientId);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        mobileContentAdGroup = (MobileContentAdGroup) adGroupInfo.getAdGroup();

        bannerInfo = createBanner(turbolanding, clientInfo, adGroupInfo);
        banner = bannerInfo.getBanner();
    }

    private NewImageBannerInfo createBanner(TurboLanding turbolanding, ClientInfo clientInfo, AdGroupInfo adGroupInfo) {
        var creative = defaultHtml5(clientId, null);
        var banner = fullImageBannerWithCreative(null).withHref(HREF_WITH_PARAMS)
                .withTurboLandingId(turbolanding.getId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.READY)
                .withStatusModerate(BannerStatusModerate.READY);

        return steps.imageBannerSteps().createImageBanner(
                new NewImageBannerInfo().withClientInfo(clientInfo).withAdGroupInfo(adGroupInfo).withBanner(banner).withCreative(creative));
    }

    @Test
    public void makeHtml5ModerationRequests_RequestDataIsCorrect() {
        List<Html5BannerModerationRequest> requests = makeMobileHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        Html5BannerRequestData actual = requests.get(0).getData();

        Creative creative = bannerInfo.getCreative();
        Html5BannerRequestData expected = new Html5BannerRequestData();
        expected.setDomain(banner.getDomain());
        expected.setLinks(List.of(
                new BannerLink()
                        .setHref(banner.getHref())
                        .setParametrizedHref(HREF_BASE + banner.getId())
                        .setMobileHref(turbolanding.getUrl())
        ));
        var expectedGeo = bannerInfo.getAdGroupInfo().getAdGroup().getGeo();
        expected.setGeo(geoToDb(expectedGeo));
        expected.setCreativeId(creative.getId());
        expected.setPreviewUrl(creative.getPreviewUrl());
        expected.setArchiveUrl(creative.getArchiveUrl());
        expected.setLivePreviewUrl(creative.getLivePreviewUrl());
        expected.setSimplePicture(creative.getIsGenerated());
        expected.setAspectRatio(new AspectRatio(creative.getWidth(), creative.getHeight()));
        expected.setUserFlags(emptyList());
        expected.setMobileContentModerationData(getExpectedMobileAppModerationData());

        assertThat("?????????????????? ???????????????????? ????????????", actual, beanDiffer(expected).useCompareStrategy(
                // ???????????? ???? ??????????????????, ?????? ???????????????????????? ???? ???????????????????????? ????????????????, ?????????????????? ???? ?????? ???? ??????????????
                allFieldsExcept(newPath("currency"))
        ));
    }

    @Test
    public void makeHtml5ModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<Html5BannerModerationRequest> requests = makeMobileHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        Html5BannerRequestData actual = requests.get(0).getData();

        assertThat("?? ?????????????? ???????????????? asap ????????", actual.getAsSoonAsPossible(), is(true));
    }

    @Test
    public void makeHtml5ModerationRequests_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<Html5BannerModerationRequest> requests = makeMobileHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        Html5BannerRequestData actual = requests.get(0).getData();

        assertThat("?? ?????????????? ?????? ?????????????????? asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeHtml5BannerModerationRequests_MetaIsCorrect() {
        List<Html5BannerModerationRequest> requests = makeMobileHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setBsBannerId(12345L);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.image_ad);
        expected.setCampaignType(CampaignsType.mobile_content);
        expected.setAdgroupType(PhrasesAdgroupType.mobile_content);

        assertThat("?????????????????? ???????????????????? ????????", actual, beanDiffer(expected));
    }

    @Test
    public void makeHtml5BannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, banner.getId(), List.of(RemoderationType.BANNER));

        List<Html5BannerModerationRequest> requests = makeMobileHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assertFalse(steps.bannerSteps().isBannerReModerationFlagPresent(shard, banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        assertEquals(requests.get(0).getWorkflow(), MANUAL);

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setBsBannerId(12345L);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.image_ad);
        expected.setCampaignType(CampaignsType.mobile_content);
        expected.setAdgroupType(PhrasesAdgroupType.mobile_content);

        assertThat("?????????????????? ???????????????????? ????????", actual, beanDiffer(expected));
    }

    @Test
    public void makeHtml5BannerModerationRequests_AutoAccept() {

        steps.bannerSteps().addBannerAutoModerationFlag(shard, banner.getId());

        List<Html5BannerModerationRequest> requests = makeMobileHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assertFalse(steps.bannerSteps().isBannerAutoModerationFlagPresent(shard, banner.getId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        assertEquals(requests.get(0).getWorkflow(), AUTO_ACCEPT);

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setClientId(bannerInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setCampaignId(bannerInfo.getCampaignId());
        expected.setAdGroupId(bannerInfo.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setBsBannerId(12345L);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.image_ad);
        expected.setCampaignType(CampaignsType.mobile_content);
        expected.setAdgroupType(PhrasesAdgroupType.mobile_content);

        assertThat("?????????????????? ???????????????????? ????????", actual, beanDiffer(expected));
    }

    private List<Html5BannerModerationRequest> makeMobileHtml5ModerationRequests(int shard, List<Long> bids) {
        Consumer<List<Html5BannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<Html5BannerModerationRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);

        html5BannerSender.send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());

        return requestsCaptor.getValue();
    }

    private MobileAppModerationData getExpectedMobileAppModerationData() {
        MobileAppModerationData data = new MobileAppModerationData();

        data.setStoreContentId(mobileContentAdGroup.getMobileContent().getStoreContentId());
        data.setAppHref(mobileContentAdGroup.getStoreUrl());
        data.setMobileContentId(mobileContentAdGroup.getMobileContentId());
        data.setBundleId(mobileContentAdGroup.getMobileContent().getBundleId());

        return data;
    }
}
