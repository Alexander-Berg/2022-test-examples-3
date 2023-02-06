package ru.yandex.direct.core.entity.moderation.service.sending;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithInternalAdModerationInfo;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.internalad.InternalBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class InternalBannerSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private BannerTypedRepository bannerRepository;

    @Autowired
    private InternalBannerSender internalBannerSender;

    private int shard;
    private InternalBanner banner;
    private InternalBanner banner2;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        CampaignInfo campaignInfo =
                steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        NewInternalBannerInfo bannerInfo =
                steps.internalBannerSteps().createModeratedInternalBanner(adGroupInfo, BannerStatusModerate.READY);
        NewInternalBannerInfo bannerInfo2 =
                steps.internalBannerSteps().createModeratedInternalBanner(adGroupInfo, BannerStatusModerate.READY);
        banner = bannerInfo.getBanner();
        banner2 = bannerInfo2.getBanner();

        shard = clientInfo.getShard();
    }

    @Test
    public void versionIncrementTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L);

        internalBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(13L);
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L);
        testModerationRepository.createBannerVersion(shard, banner2.getId(), 22L);

        internalBannerSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(13L);
        assertThat(testModerationRepository.getBannerVersion(shard, banner2.getId())).isEqualTo(23L);

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());
        checkStatusModerate(BannerStatusModerate.SENT, banner2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<InternalBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithInternalAdModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerStatusModerate.READY, banner.getId());

        internalBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(1L);

        internalBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(1L);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<InternalBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithInternalAdModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerStatusModerate.READY, banner.getId());

        internalBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        internalBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(2L);

        internalBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(2L);
    }

    private void checkStatusModerate(BannerStatusModerate statusModerate, Long id) {
        List<Banner> banners = bannerRepository.getBanners(shard, List.of(id), null);
        assertThat(banners).isNotEmpty();

        var banner = (InternalBanner) banners.get(0);
        assertThat(banner.getStatusModerate())
                .isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(), Set.of(RemoderationType.BANNER));

        internalBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard, banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(), Set.of(AutoAcceptanceType.BANNER));

        internalBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard, banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeReady() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(), CampaignStatusModerate.READY);

        internalBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId()))
                .isEqualTo(CampaignStatusModerate.READY);
    }

}
