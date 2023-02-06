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

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldMobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.asset.ImageModerationData;
import ru.yandex.direct.core.entity.moderation.model.mobile_content.MobileAppModerationData;
import ru.yandex.direct.core.entity.moderation.model.text.TextBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.text.TextBannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.AUTO_ACCEPT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow.MANUAL;
import static ru.yandex.direct.core.entity.moderation.service.sending.TextBannerSender.INITIAL_VERSION;
import static ru.yandex.direct.core.testing.data.TestBanners.ANOTHER_DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileContentBannerModerationRequestTest {
    @Autowired
    private Steps steps;

    @Autowired
    private MobileContentBannerSender mobileContentBannerSender;

    @Autowired
    private TestModerationRepository testModerationRepository;
    private int shard;
    private ClientInfo clientInfo;

    private OldMobileAppBanner banner;

    private AbstractBannerInfo bannerWithImage;
    private BannerImageInfo bannerImage;
    private MobileContentAdGroup mobileContentAdGroup;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup(clientInfo);
        mobileContentAdGroup = (MobileContentAdGroup) adGroupInfo.getAdGroup();
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        shard = clientInfo.getShard();

        banner = (OldMobileAppBanner) steps.bannerSteps().createBanner(activeMobileAppBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withBody("TestBody")
                        .withTitle("TestTitle")
                        .withLanguage(Language.UNKNOWN),
                adGroupInfo
        ).getBanner();

        bannerWithImage = steps.bannerSteps().createBanner(activeMobileAppBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withBody("TestBody")
                        .withTitle("TestTitle")
                        .withLanguage(Language.UNKNOWN),
                adGroupInfo
        );

        bannerImage = steps.bannerSteps().createBannerImage(bannerWithImage,
                steps.bannerSteps().createBannerImageFormat(clientInfo),
                defaultBannerImage(banner.getId(), randomAlphanumeric(16)).withBsBannerId(3L)
                        .withStatusModerate(OldStatusBannerImageModerate.READY)
        );
    }

    @Test
    public void makeTextBannerModerationRequests_RequestDataIsCorrect() {
        List<TextBannerModerationRequest> requests =
                captureSentRequests(shard, singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        TextBannerRequestData actual = requests.get(0).getData();

        TextBannerRequestData expected = new TextBannerRequestData();

        expected.setBody("TestBody");
        expected.setTitle("TestTitle");
        expected.setLanguage("UNKNOWN");
        expected.setHref("https://trusted1.com");
        expected.setParametrizedHref("https://trusted1.com");
        expected.setGeo("225");
        expected.setUserFlags(emptyList());

        expected.setMobileContentModerationData(getExpectedMobileAppModerationData());

        assertThat("Вернулись неправильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeTextBannerWithImageModerationRequests_RequestDataIsCorrect() {
        List<TextBannerModerationRequest> requests =
                captureSentRequests(shard, singletonList(bannerWithImage.getBannerId()));

        assumeThat(requests, hasSize(1));

        TextBannerRequestData actual = requests.get(0).getData();

        TextBannerRequestData expected = new TextBannerRequestData();

        expected.setBody("TestBody");
        expected.setTitle("TestTitle");
        expected.setLanguage("UNKNOWN");
        expected.setHref("https://trusted1.com");
        expected.setParametrizedHref("https://trusted1.com");
        expected.setGeo("225");
        expected.setUserFlags(emptyList());

        ImageModerationData imageData = new ImageModerationData();
        imageData.setImageHash(bannerImage.getBannerImage().getImageHash());
        imageData.setMdsGroupId(bannerImage.getBannerImageFormat().getMdsGroupId());
        imageData.setHost(bannerImage.getBannerImageFormat().getAvatarHost().getHost());
        imageData.setNamespace(bannerImage.getBannerImageFormat().getAvatarNamespace().getValue());
        expected.addAsset(imageData);

        expected.setMobileContentModerationData(getExpectedMobileAppModerationData());

        assertThat("Вернулись неправильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeTextBannerModerationRequests_WithPreModeration() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        List<TextBannerModerationRequest> requests = captureSentRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись неправильные данные", requests.get(0).getWorkflow(), Matchers.is(MANUAL));
    }

    @Test
    public void makeTextBannerModerationRequests_WithAutoAccept() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER));

        List<TextBannerModerationRequest> requests = captureSentRequests(shard,
                singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat("Вернулись неправильные данные", requests.get(0).getWorkflow(), Matchers.is(AUTO_ACCEPT));
    }

    @Test
    public void makeTextBannerModerationRequests_MetaIsCorrect() {
        List<TextBannerModerationRequest> requests =
                captureSentRequests(shard,
                        singletonList(banner.getId()));

        assumeThat(requests, hasSize(1));

        assertThat(requests.get(0).getType().getValue(), equalTo("text_sm"));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(banner.getCampaignId());
        expected.setAdGroupId(banner.getAdGroupId());
        expected.setBannerId(banner.getId());
        expected.setClientId(clientInfo.getClientId().asLong());
        expected.setUid(clientInfo.getUid());
        expected.setBsBannerId(ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(INITIAL_VERSION);
        expected.setBannerType(BannersBannerType.mobile_content);
        expected.setCampaignType(CampaignsType.mobile_content);
        expected.setAdgroupType(PhrasesAdgroupType.mobile_content);

        assertThat("Вернулась неправильная мета", actual, beanDiffer(expected));
    }

    private List<TextBannerModerationRequest> captureSentRequests(int shard, List<Long> bids) {
        Consumer<List<TextBannerModerationRequest>> sender =
                Mockito.mock(Consumer.class);

        ArgumentCaptor<List<TextBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        mobileContentBannerSender.send(shard, bids, e -> System.currentTimeMillis(), el -> null, sender);
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
