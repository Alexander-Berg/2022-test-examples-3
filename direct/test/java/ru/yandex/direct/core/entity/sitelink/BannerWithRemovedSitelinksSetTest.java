package ru.yandex.direct.core.entity.sitelink;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationRequest;
import ru.yandex.direct.core.entity.moderation.service.ModerationService;
import ru.yandex.direct.core.entity.moderation.service.sending.SitelinksSender;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithRemovedSitelinksSetTest {

    private final static long REMOVED_SITELINKS_SET_ID = 907565L;

    @Autowired
    private Steps steps;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private SitelinksSender sitelinksSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner bannerWithoutSitelinks;
    private SitelinkSetInfo sitelinkSetInfo;
    private OldTextBanner correctBanner;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();

        clientInfo = campaignInfo.getClientInfo();

        shard = clientInfo.getShard();

        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);

        correctBanner = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                                .withStatusSitelinksModerate(StatusSitelinksModerate.READY),
                        clientInfo
                )
                .getBanner();

        bannerWithoutSitelinks = steps.bannerSteps()

                .createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                                .withSitelinksSetId(REMOVED_SITELINKS_SET_ID)
                                .withStatusSitelinksModerate(StatusSitelinksModerate.READY),
                        clientInfo
                )
                .getBanner();
    }

    @Test
    public void makeSitelinksRequest_onlyOneBrokenObject() {
        List<SitelinksModerationRequest> requests =
                makeSitelinksModerationRequests(shard, singletonList(bannerWithoutSitelinks.getId()), 0);

        assumeThat(requests, hasSize(0));

        OldTextBanner b = (OldTextBanner) bannerRepository.getBanners(shard, List.of(bannerWithoutSitelinks.getId())).get(0);

        assertThat(b.getStatusSitelinksModerate()).isEqualTo(StatusSitelinksModerate.NEW);
        assertThat(b.getSitelinksSetId()).isNull();

    }

    @Test
    public void makeSitelinksModerationRequests_OneCorrectBannerAndOneBroken() {
        List<SitelinksModerationRequest> requests =
                makeSitelinksModerationRequests(shard, List.of(correctBanner.getId(), bannerWithoutSitelinks.getId()),
                        1);

        assumeThat(requests, hasSize(1));

        OldTextBanner b = (OldTextBanner) bannerRepository.getBanners(shard, List.of(bannerWithoutSitelinks.getId())).get(0);
        OldTextBanner b1 = (OldTextBanner) bannerRepository.getBanners(shard, List.of(correctBanner.getId())).get(0);

        assertThat(b.getStatusSitelinksModerate()).isEqualTo(StatusSitelinksModerate.NEW);
        assertThat(b.getSitelinksSetId()).isNull();

        assertThat(b1.getStatusSitelinksModerate()).isEqualTo(StatusSitelinksModerate.SENT);
        assertThat(b1.getSitelinksSetId()).isEqualTo(sitelinkSetInfo.getSitelinkSetId());
    }

    private List<SitelinksModerationRequest> makeSitelinksModerationRequests(int shard, List<Long> bids,
                                                                             int times) {
        Consumer<List<SitelinksModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<SitelinksModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);


        sitelinksSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.times(times)).accept(requestsCaptor.capture());

        if (times > 0) {
            return requestsCaptor.getValue();
        }

        return List.of();
    }


}
