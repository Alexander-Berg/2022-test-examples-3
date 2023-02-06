package ru.yandex.direct.core.entity.moderation.service.sending;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerWithModerationInfo;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.cpm.video.CpmVideoBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.entity.moderation.service.sending.BaseCpmVideoBannerSender.INITIAL_VERSION;

public abstract class AbstractCpmVideoBannerSenderTest {

    @Autowired
    protected Steps steps;

    @Autowired
    protected TestModerationRepository testModerationRepository;

    @Autowired
    protected ModerationOperationModeProvider moderationOperationModeProvider;

    @Autowired
    protected OldBannerRepository bannerRepository;

    protected BaseCpmVideoBannerSender bannerSender;
    protected ClientInfo clientInfo;
    protected int shard;
    protected OldBanner banner;
    protected OldBanner banner2;

    protected abstract void init();

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        init();
    }

    @After
    public void after() {
        moderationOperationModeProvider.disableImmutableVersionMode();
        moderationOperationModeProvider.disableForcedMode();
    }

    @Test
    public void createNewVersionTest() {
        bannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getBannerVersionObj(shard, banner.getId());
        assertThat(moderationVersion.getVersion()).isEqualTo(INITIAL_VERSION);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 75100L, LocalDateTime.now().minusDays(1));

        bannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getBannerVersionObj(shard, banner.getId());
        assertThat(moderationVersion.getVersion()).isEqualTo(75101L);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 75100L);
        testModerationRepository.createBannerVersion(shard, banner2.getId(), 75200L);

        bannerSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(75101L);
        assertThat(testModerationRepository.getBannerVersion(shard, banner2.getId())).isEqualTo(75201L);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        checkStatusModerate(OldBannerStatusModerate.SENT, banner2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<CpmVideoBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        bannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION);

        bannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<CpmVideoBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        bannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        bannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);


        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(1L + INITIAL_VERSION);

        bannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(1L + INITIAL_VERSION);
    }

    private void checkStatusModerate(OldBannerStatusModerate statusModerate, Long id) {
        List<OldBanner> banners = bannerRepository.getBanners(shard,
                Collections.singleton(id));

        assertThat(banners).isNotEmpty();
        assertThat(banners.get(0).getStatusModerate()).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.SITELINKS_SET));

        bannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.SITELINKS_SET);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        bannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET));

        bannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard, banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.SITELINKS_SET);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER));

        bannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard, banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        bannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId()))
                .isEqualTo(CampaignStatusModerate.SENT);
    }
}
