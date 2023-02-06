package ru.yandex.direct.core.entity.moderation.service.sending;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerWithTextAndImageModerationInfo;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.ModerationOperationMode;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.text.TextBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.direct.core.entity.moderation.service.sending.TextBannerSender.INITIAL_VERSION;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.AGE6;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.BABY_FOOD0;

@ParametersAreNonnullByDefault
public abstract class AbstractTextBannerSenderTest {

    @Autowired
    protected Steps steps;

    @Autowired
    protected TestModerationRepository testModerationRepository;

    @Autowired
    protected ModerationOperationModeProvider moderationOperationModeProvider;

    @Autowired
    private OldBannerRepository bannerRepository;

    private TextBannerSender textBannerSender;

    protected int shard;
    protected OldBanner banner;
    protected OldBanner banner2;

    protected AbstractBannerInfo bannerWithImage;
    protected BannerImageInfo bannerImage;

    protected abstract TextBannerSender getBannerSender();

    @Before
    public void setSender() {
        textBannerSender = getBannerSender();
    }

    @Test
    public void createNewVersionTest() {
        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getBannerVersionObj(shard, banner.getId());
        assertThat(moderationVersion.getVersion()).isEqualTo(INITIAL_VERSION);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void createBannerWithImageNewVersionTest() {
        assumeTrue(bannerWithImage != null);

        textBannerSender.send(shard, List.of(bannerWithImage.getBannerId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getBannerVersionObj(shard, bannerWithImage.getBannerId());
        assertThat(moderationVersion.getVersion()).isEqualTo(INITIAL_VERSION);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 75100L, LocalDateTime.now().minusDays(1));

        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
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

        textBannerSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(75101L);
        assertThat(testModerationRepository.getBannerVersion(shard, banner2.getId())).isEqualTo(75201L);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        checkStatusModerate(OldBannerStatusModerate.SENT, banner2.getId());
    }

    @Test
    public void versionChangeToInitialTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L);

        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<TextBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithTextAndImageModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        textBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION);

        textBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<TextBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithTextAndImageModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        textBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        textBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);


        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(75001L);

        textBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(75001L);
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

        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.SITELINKS_SET);
    }

    @Test
    public void reModerateFlagWillBeSet() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });


        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET));

        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.SITELINKS_SET);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER));

        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        textBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });


        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId())).isEqualTo(CampaignStatusModerate.SENT);
    }

    @Test
    public void languageTest() {
        AtomicReference<List<TextBannerModerationRequest>> requests = new AtomicReference<>();
        textBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), new AtomicReference<>(), requests,
                e -> System.currentTimeMillis(), el -> null);

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getData().getLanguage()).isEqualTo("RU");
    }

    @Test
    public void flagsTest() {
        moderationOperationModeProvider.forceMode(ModerationOperationMode.RESTRICTED);
        moderationOperationModeProvider.setImmutableVersionMode();

        testModerationRepository.createBannerVersion(shard, banner.getId(), 75200L, LocalDateTime.now().minusDays(1));

        AtomicReference<List<TextBannerModerationRequest>> requests = new AtomicReference<>();
        textBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), new AtomicReference<>(), requests,
                e -> System.currentTimeMillis(), el -> null, el -> ModerationWorkflow.USER_FLAGS);

        assertThat(requests.get()).hasSize(1);

        var request = requests.get().get(0);

        assertThat(request.getMeta().getVersionId()).isEqualTo(75200L);
        assertThat(request.getWorkflow()).isEqualTo(ModerationWorkflow.USER_FLAGS);

        var data = request.getData();

        Set<Integer> userFlags = new HashSet<>(data.getUserFlags());
        assertThat(userFlags).isEqualTo(Set.of(AGE6.getNumber(), BABY_FOOD0.getNumber()));

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(75200L);
    }
}
