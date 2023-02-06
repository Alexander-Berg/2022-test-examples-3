package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdImageModerationInfo;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.ModerationOperationMode;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.ad_image.AdImageBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.entity.moderation.service.sending.AbstractAdImageBannerSender.INITIAL_VERSION;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.AGE6;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.BABY_FOOD0;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdImageSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private ModerationOperationModeProvider moderationOperationModeProvider;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private AdImageBannerSender adImageBannerSender;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldImageHashBanner banner;
    private OldImageHashBanner banner2;

    @Before
    public void before() throws IOException {
        moderationOperationModeProvider.disableForcedMode();
        moderationOperationModeProvider.disableImmutableVersionMode();

        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        shard = clientInfo.getShard();

        banner = (OldImageHashBanner) steps.bannerSteps()
                .createBanner(activeImageHashBanner(campaignInfo.getCampaignId(), null)
                                .withStatusModerate(OldBannerStatusModerate.READY)
                                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                        clientInfo
                )
                .getBanner();
        banner2 = (OldImageHashBanner) steps.bannerSteps()
                .createBanner(activeImageHashBanner(campaignInfo.getCampaignId(), null)
                                .withStatusModerate(OldBannerStatusModerate.READY)
                                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                        clientInfo
                )
                .getBanner();

        testModerationRepository.addBannerImageFormat(shard, banner.getImage().getImageHash(),
                new ImageSize().withHeight(100).withWidth(100));
        testModerationRepository.addBannerImageFormat(shard, banner2.getImage().getImageHash(),
                new ImageSize().withHeight(200).withWidth(200));

    }

    @Test
    public void createNewVersionTest() {
        adImageBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getBannerVersionObj(shard, banner.getId());
        assertThat(moderationVersion.getVersion()).isEqualTo(INITIAL_VERSION);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L, LocalDateTime.now().minusDays(1));

        adImageBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getBannerVersionObj(shard, banner.getId());
        assertThat(moderationVersion.getVersion()).isEqualTo(13L);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L, LocalDateTime.now().minusDays(1));
        testModerationRepository.createBannerVersion(shard, banner2.getId(), 22L, LocalDateTime.now().minusDays(1));

        adImageBannerSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerVersionObj(shard, banner.getId()).getVersion()).isEqualTo(13L);
        assertThat(testModerationRepository.getBannerVersionObj(shard, banner2.getId()).getVersion()).isEqualTo(23L);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        checkStatusModerate(OldBannerStatusModerate.SENT, banner2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<AdImageBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithAdImageModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        adImageBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION);

        adImageBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<AdImageBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithAdImageModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        adImageBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        adImageBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(75001L);

        adImageBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(75001L);
    }

    private void checkStatusModerate(OldBannerStatusModerate statusModerate, Long id) {

        List<OldBanner> banners = bannerRepository.getBanners(shard,
                Collections.singleton(id));

        assertThat(banners).isNotEmpty();

        OldImageHashBanner banner = (OldImageHashBanner) banners.get(0);

        assertThat(banner.getStatusModerate()).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.SITELINKS_SET));

        adImageBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.SITELINKS_SET);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        adImageBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET));

        adImageBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.SITELINKS_SET);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER));

        adImageBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        adImageBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId()))
                .isEqualTo(CampaignStatusModerate.SENT);
    }

    @Test
    public void flagsTest() {
        moderationOperationModeProvider.forceMode(ModerationOperationMode.RESTRICTED);
        moderationOperationModeProvider.setImmutableVersionMode();

        testModerationRepository.createBannerVersion(shard, banner.getId(), 75200L, LocalDateTime.now().minusDays(1));

        AtomicReference<List<AdImageBannerModerationRequest>> requests = new AtomicReference<>();
        adImageBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), new AtomicReference<>(), requests,
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
