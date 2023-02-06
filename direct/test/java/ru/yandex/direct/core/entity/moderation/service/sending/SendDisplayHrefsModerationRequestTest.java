package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
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

import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.COMMON;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendDisplayHrefsModerationRequestTest {

    @Autowired
    private Steps steps;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private DisplayHrefsSender displayHrefsSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private OldTextBanner banner;

    @Before
    public void before() throws IOException {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withDisplayHref("New displayhref").withDisplayHrefStatusModerate(DisplayHrefStatusModerate.READY),
                        campaignInfo).getBanner();
    }

    @Test
    public void makeDisplayHrefsModerationRequests_RequestDataIsCorrect() {
        List<DisplayHrefsModerationRequest> requests =
                makeDisplayHrefsModerationRequests(shard, singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        DisplayHrefsRequestData actual = requests.get(0).getData();

        DisplayHrefsRequestData expected = new DisplayHrefsRequestData();
        expected.setText("New displayhref");
        expected.setLang("en");

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeDisplayHrefsModerationRequests_ClientWithAsapFlag_RequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<DisplayHrefsModerationRequest> requests = makeDisplayHrefsModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        DisplayHrefsRequestData actual = requests.get(0).getData();
        DisplayHrefsRequestData expected = new DisplayHrefsRequestData();

        expected.setText("New displayhref");
        expected.setLang("en");
        expected.setAsSoonAsPossible(true);

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeDisplayHrefsModerationRequests_ClientWithNoAsapFlag_NoAsapFlagInRequest() {
        List<DisplayHrefsModerationRequest> requests = makeDisplayHrefsModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        DisplayHrefsRequestData actual = requests.get(0).getData();
        assertThat("Данные не содержат флага asap", toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    @Test
    public void makeDisplayHrefsModerationRequests_MetaIsCorrect() {
        List<DisplayHrefsModerationRequest> requests =
                makeDisplayHrefsModerationRequests(shard,
                        singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        DisplayHrefsModerationMeta actual = requests.get(0).getMeta();

        DisplayHrefsModerationMeta expected = new DisplayHrefsModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setVersionId(30000);
        expected.setDisplayHrefId(banner.getId());

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }


    private List<DisplayHrefsModerationRequest> makeDisplayHrefsModerationRequests(int shard, List<Long> bids) {
        Consumer<List<DisplayHrefsModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<DisplayHrefsModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);


        displayHrefsSender.send(shard, bids, el -> System.currentTimeMillis(), el -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }

    @Test
    public void makeDisplayhrefsModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.DISPLAY_HREFS));

        List<DisplayHrefsModerationRequest> requests = makeDisplayHrefsModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeDisplayhrefsModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.DISPLAY_HREFS));

        List<DisplayHrefsModerationRequest> requests = makeDisplayHrefsModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeDisplayhrefsModerationRequests_WithNoFlags() {
        List<DisplayHrefsModerationRequest> requests = makeDisplayHrefsModerationRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись правильные данные", requests.get(0).getWorkflow(), Matchers.is(COMMON));
    }

}
