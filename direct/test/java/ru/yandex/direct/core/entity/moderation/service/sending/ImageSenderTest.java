package ru.yandex.direct.core.entity.moderation.service.sending;


import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.image.model.ImageWithModerationInfo;
import ru.yandex.direct.core.entity.moderation.model.image.ImageModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerImagesRecord;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ImageSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private ImageSender imageSender;

    @Autowired
    private BannerSteps bannerSteps;

    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private OldTextBanner banner2;
    private BannerImageInfo<TextBannerInfo> bannerImage;
    private BannerImageInfo<TextBannerInfo> bannerImage2;

    @Before
    public void before() throws IOException {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        steps.campaignSteps().createCampaign(campaignInfo);
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        TextBannerInfo textBannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null), campaignInfo);
        TextBannerInfo textBannerInfo2 = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null), campaignInfo);

        banner = textBannerInfo.getBanner();
        banner2 = textBannerInfo2.getBanner();

        bannerImage = steps.bannerSteps().createBannerImage(textBannerInfo,
                bannerSteps.createBannerImageFormat(clientInfo),
                defaultBannerImage(banner.getId(), randomAlphanumeric(16)).withBsBannerId(3L)
                        .withStatusModerate(OldStatusBannerImageModerate.READY)
        );
        bannerImage2 = steps.bannerSteps().createBannerImage(textBannerInfo2,
                bannerSteps.createBannerImageFormat(clientInfo),
                defaultBannerImage(banner2.getId(), randomAlphanumeric(16)).withBsBannerId(3L)
                        .withStatusModerate(OldStatusBannerImageModerate.READY)
        );
    }

    @Test
    public void versionIncrementTest() {
        testModerationRepository.createBannerImageVersion(shard, banner.getId(), 12L);

        imageSender.send(shard, List.of(bannerImage.getBannerImageId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerImageVersion(shard, banner.getId())).isEqualTo(13L);
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createBannerImageVersion(shard, banner.getId(), 12L);
        testModerationRepository.createBannerImageVersion(shard, banner2.getId(), 22L);

        imageSender.send(shard, List.of(bannerImage.getBannerImageId(), bannerImage2.getBannerImageId()),
                e -> System.currentTimeMillis(), e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerImageVersion(shard, banner.getId())).isEqualTo(13L);
        assertThat(testModerationRepository.getBannerImageVersion(shard, banner2.getId())).isEqualTo(23L);

        checkStatusModerate(BannerImagesStatusmoderate.Sent, banner.getId());
        checkStatusModerate(BannerImagesStatusmoderate.Sent, banner2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<ImageModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<ImageWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerImagesStatusmoderate.Ready, banner.getId());

        imageSender.beforeSendTransaction(shard, List.of(bannerImage.getBannerImageId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerImagesStatusmoderate.Sending, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(10000000L);

        imageSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerImagesStatusmoderate.Sent, banner.getId());

        assertThat(testModerationRepository.getBannerImageVersion(shard, banner.getId())).isEqualTo(10000000L);
    }

    @Test
    public void versionChangedAfterRetry() {
        AtomicReference<List<ImageModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<ImageWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerImagesStatusmoderate.Ready, banner.getId());

        imageSender.beforeSendTransaction(shard, List.of(bannerImage.getBannerImageId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        imageSender.beforeSendTransaction(shard, List.of(bannerImage.getBannerImageId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerImagesStatusmoderate.Sending, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(10000001L);

        imageSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerImagesStatusmoderate.Sent, banner.getId());
        assertThat(testModerationRepository.getBannerImageVersion(shard, banner.getId())).isEqualTo(10000001L);
    }

    private void checkStatusModerate(BannerImagesStatusmoderate statusModerate, Long id) {
        List<BannerImagesRecord> bannerImages = testModerationRepository.getBannerImages(shard,
                Collections.singleton(id));

        assertThat(bannerImages).isNotEmpty();
        assertThat(bannerImages.get(0).getStatusmoderate()).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.BANNER_IMAGE));

        imageSender.send(shard, List.of(bannerImage.getBannerImageId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.BANNER);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER_IMAGE));

        imageSender.send(shard, List.of(bannerImage.getBannerImageId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.BANNER_IMAGE));

        imageSender.send(shard, List.of(bannerImage.getBannerImageId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.BANNER);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER_IMAGE));

        imageSender.send(shard, List.of(bannerImage.getBannerImageId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        imageSender.send(shard, List.of(bannerImage.getBannerImageId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId())).isEqualTo(CampaignStatusModerate.SENT);
    }

}
