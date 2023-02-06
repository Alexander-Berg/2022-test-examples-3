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

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderation.model.AspectRatio;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo;
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
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@CoreTest
@RunWith(SpringRunner.class)
public class SendCpmHtml5ModerationRequestTest {
    public static final String HREF_BASE = "https://www.yandex.ru/?ad_id=";
    public static final String HREF_WITH_PARAMS = HREF_BASE + "{ad_id}";

    @Autowired
    private Steps steps;

    @Autowired
    private CpmHtml5BannerSender html5BannerSender;

    private int shard;
    private NewCpmBannerInfo bannerInfo;
    private CpmBanner banner;
    private ClientInfo clientInfo;
    private ClientId clientId;
    private TurboLanding turbolanding;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        turbolanding = steps.turboLandingSteps().createDefaultTurboLanding(clientId);
        bannerInfo = createBanner(turbolanding);
        banner = bannerInfo.getBanner();
    }

    private NewCpmBannerInfo createBanner(TurboLanding turbolanding) {
        var banner = fullCpmBanner(null).withHref(HREF_WITH_PARAMS)
                .withTurboLandingId(turbolanding.getId())
                .withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.READY)
                .withStatusModerate(BannerStatusModerate.READY);

        return steps.cpmBannerSteps().createCpmBanner(new NewCpmBannerInfo()
                .withClientInfo(clientInfo)
                .withBanner(banner)
                .withCreative(defaultHtml5(clientInfo.getClientId(), null))
        );
    }

    @Test
    public void makeHtml5ModerationRequests_RequestDataIsCorrect() {
        List<Html5BannerModerationRequest> requests = makeCpmHtml5ModerationRequests(shard,
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

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected).useCompareStrategy(
                // валюту не проверяем, она отправляется по историческим причинам, модерация на неё не смотрит
                allFieldsExcept(newPath("currency"))
        ));
    }

    @Test
    public void makeHtml5ModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<Html5BannerModerationRequest> requests = makeCpmHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        Html5BannerRequestData actual = requests.get(0).getData();

        assertThat("В запросе параметр asap есть", actual.getAsSoonAsPossible(), is(true));
    }

    @Test
    public void makeHtml5ModerationRequests_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<Html5BannerModerationRequest> requests = makeCpmHtml5ModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        Html5BannerRequestData actual = requests.get(0).getData();

        assertThat("В запросе нет параметра asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeHtml5BannerModerationRequests_MetaIsCorrect() {
        List<Html5BannerModerationRequest> requests = makeCpmHtml5ModerationRequests(shard,
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
        expected.setCampaignType(CampaignsType.text);
        expected.setBannerType(BannersBannerType.cpm_banner);
        expected.setAdgroupType(PhrasesAdgroupType.cpm_banner);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeHtml5BannerModerationRequests_ManualOnly() {

        steps.bannerSteps().addBannerReModerationFlag(shard, banner.getId(), List.of(RemoderationType.BANNER));

        List<Html5BannerModerationRequest> requests = makeCpmHtml5ModerationRequests(shard,
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
        expected.setVersionId(INITIAL_VERSION);
        expected.setBsBannerId(12345L);
        expected.setCampaignType(CampaignsType.text);
        expected.setBannerType(BannersBannerType.cpm_banner);
        expected.setAdgroupType(PhrasesAdgroupType.cpm_banner);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeHtml5BannerModerationRequests_AutoAccept() {

        steps.bannerSteps().addBannerAutoModerationFlag(shard, banner.getId());

        List<Html5BannerModerationRequest> requests = makeCpmHtml5ModerationRequests(shard,
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
        expected.setVersionId(INITIAL_VERSION);
        expected.setBsBannerId(12345L);
        expected.setCampaignType(CampaignsType.text);
        expected.setBannerType(BannersBannerType.cpm_banner);
        expected.setAdgroupType(PhrasesAdgroupType.cpm_banner);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    private List<Html5BannerModerationRequest> makeCpmHtml5ModerationRequests(int shard, List<Long> bids) {
        Consumer<List<Html5BannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<Html5BannerModerationRequest>> requestsCaptor = ArgumentCaptor.forClass(List.class);

        html5BannerSender.send(shard, bids, e -> System.currentTimeMillis(), e -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());

        return requestsCaptor.getValue();
    }
}
