package ru.yandex.direct.core.entity.sitelink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.moderation.service.sending.SitelinksSender;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.COMMON;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendSitelinksModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private SitelinksSender sitelinksSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private SitelinkSetInfo sitelinkSetInfo;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();

        clientInfo = campaignInfo.getClientInfo();

        shard = clientInfo.getShard();

        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);

        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                                .withStatusSitelinksModerate(StatusSitelinksModerate.READY),
                        clientInfo
                )
                .getBanner();
    }

    @Test
    public void makeSitelinksModerationRequests_RequestDataIsCorrect() {
        List<SitelinksModerationRequest> requests =
                makeSitelinksModerationRequests(shard, singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        SitelinksRequestData actual = requests.get(0).getData();

        SitelinksRequestData expected = new SitelinksRequestData();
        List<SitelinksRequestData.SitelinkRequest> sitelinkRequests = new ArrayList<>();

        for (var sitelink : sitelinkSetInfo.getSitelinkSet().getSitelinks()) {
            SitelinksRequestData.SitelinkRequest request = new SitelinksRequestData.SitelinkRequest();
            request.setTurbolinkId(sitelink.getTurboLandingId());
            request.setSitelinkId(sitelink.getId());
            request.setHref(sitelink.getHref());
            request.setParametrizedHref(sitelink.getHref());
            request.setDescription(sitelink.getDescription());
            request.setTitle(sitelink.getTitle());

            sitelinkRequests.add(request);
        }

        expected.setSitelinkRequestList(sitelinkRequests);

        assertThat("?????????????????? ???????????????????? ????????????", actual, beanDiffer(expected));
    }

    @Test
    public void makeSitelinksModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientInfo.getClientId());
        steps.clientOptionsSteps().setClientFlags(shard, clientInfo.getClientId(), "as_soon_as_possible");

        List<SitelinksModerationRequest> requests = makeSitelinksModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        SitelinksRequestData actual = requests.get(0).getData();

        assertThat("?????????????????? ???????????????????? ????????????", actual.getAsSoonAsPossible(), Matchers.is(true));
    }

    @Test
    public void makeSitelinksModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.SITELINKS_SET));

        List<SitelinksModerationRequest> requests = makeSitelinksModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("?????????????????? ???????????????????? ????????????", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeSitelinksModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.SITELINKS_SET));

        List<SitelinksModerationRequest> requests = makeSitelinksModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("?????????????????? ???????????????????? ????????????", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeSitelinksModerationRequests_WithNoFlags() {

        List<SitelinksModerationRequest> requests = makeSitelinksModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("?????????????????? ???????????????????? ????????????", requests.get(0).getWorkflow(), Matchers.is(COMMON));
    }

    @Test
    public void makeSitelinksModerationRequests_MetaIsCorrect() {
        List<SitelinksModerationRequest> requests =
                makeSitelinksModerationRequests(shard,
                        singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        SitelinksModerationMeta actual = requests.get(0).getMeta();

        SitelinksModerationMeta expected = new SitelinksModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setVersionId(40_000_000L);
        expected.setSitelinksSetId(banner.getSitelinksSetId());

        assertThat("?????????????????? ???????????????????? ????????", actual, beanDiffer(expected));
    }

    private List<SitelinksModerationRequest> makeSitelinksModerationRequests(int shard, List<Long> bids) {
        Consumer<List<SitelinksModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<SitelinksModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);


        sitelinksSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }


}
