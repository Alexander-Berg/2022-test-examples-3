package ru.yandex.direct.core.entity.sitelink;

import java.io.IOException;
import java.util.ArrayList;
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
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksRequestData;
import ru.yandex.direct.core.entity.moderation.service.sending.SitelinksSender;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelinkSet;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendSitelinksHrefParameterizationTest {

    @Autowired
    private Steps steps;

    @Autowired
    private SitelinksSender sitelinksSender;

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

        SitelinkSet sitelinkSet = defaultSitelinkSet();

        sitelinkSet.getSitelinks().get(0).withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&camptype={campaign_type}");
        sitelinkSet.getSitelinks().get(1).withHref("http://some.domain/ya.ru?pid={adgroup_id}");

        sitelinkSetInfo = steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, clientInfo);

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

        sitelinkRequests.get(0).setParametrizedHref("http://some.domain/ya.ru?bid=" + banner.getId() + "&cid=" + banner.getCampaignId()+"&camptype=type1");
        sitelinkRequests.get(1).setParametrizedHref("http://some.domain/ya.ru?pid=" + banner.getAdGroupId());

        expected.setSitelinkRequestList(sitelinkRequests);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
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
