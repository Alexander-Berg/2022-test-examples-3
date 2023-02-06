package ru.yandex.direct.core.entity.moderation.service.sending;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithModerationInfo;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.ModerationOperationMode;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.cpm.canvas.CanvasBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerTurbolandingsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerTurbolandingsRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.RESTRICTED_CANVAS_TRANSPORT_NEW_MODERATION;
import static ru.yandex.direct.core.entity.moderation.service.sending.CanvasBannerSender.INITIAL_VERSION;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.AGE6;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.BABY_FOOD0;

@ParametersAreNonnullByDefault
public abstract class AbstractCanvasCreativeBannerSenderTest {

    protected Steps steps;
    protected TestModerationRepository testModerationRepository;
    protected BannerTypedRepository bannerRepository;
    protected CanvasBannerSender canvasBannerSender;
    private PpcPropertiesSupport ppcPropertiesSupport;
    private ModerationOperationModeProvider moderationOperationModeProvider;

    protected int shard;
    protected ClientInfo clientInfo;
    protected CampaignInfo campaignInfo;
    protected OldBanner banner;
    protected OldBanner banner2;
    protected long creativeId;

    public void init(Steps steps,
                     TestModerationRepository testModerationRepository,
                     BannerTypedRepository bannerRepository,
                     CanvasBannerSender canvasBannerSender,
                     PpcPropertiesSupport ppcPropertiesSupport,
                     ModerationOperationModeProvider moderationOperationModeProvider) {
        this.steps = steps;
        this.testModerationRepository = testModerationRepository;
        this.bannerRepository = bannerRepository;
        this.canvasBannerSender = canvasBannerSender;
        this.ppcPropertiesSupport = ppcPropertiesSupport;
        this.moderationOperationModeProvider = moderationOperationModeProvider;

        moderationOperationModeProvider.disableForcedMode();
        moderationOperationModeProvider.disableImmutableVersionMode();
    }

    @Before
    public abstract void before() throws IOException;

    protected void disableRestrictedMode() {
        ppcPropertiesSupport.set(RESTRICTED_CANVAS_TRANSPORT_NEW_MODERATION, String.valueOf(false));
    }

    protected void enableRestrictedMode() {
        ppcPropertiesSupport.set(RESTRICTED_CANVAS_TRANSPORT_NEW_MODERATION, String.valueOf(true));
    }

    @Test
    public void versionIncrementTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 5100L);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(5101L);
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 5100L);
        testModerationRepository.createBannerVersion(shard, banner2.getId(), 5200L);

        canvasBannerSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(5101L);
        assertThat(testModerationRepository.getBannerVersion(shard, banner2.getId())).isEqualTo(5201L);

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());
        checkStatusModerate(BannerStatusModerate.SENT, banner2.getId());
    }

    @Test
    public void versionChangeToInitialTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void handleOnlyObjectsInReadyOrSendingState() {
        testModerationRepository.setBannerStatusModerate(shard, banner.getId(), BannerStatusModerate.SENT);
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());

        // баннер не должен быть обработан, и версия в модерации не должна поменяться
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(12L);
    }

    @Test
    public void handleObjectsInAnyState_whenRestrictedModeEnabled() {
        enableRestrictedMode();
        testModerationRepository.setBannerStatusModerate(shard, banner.getId(), BannerStatusModerate.SENT);
        testModerationRepository.createBannerVersion(shard, banner.getId(), 5100L);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());

        // баннер должен быть обработан, и версия в модерации не должна увеличиться
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(5101L);
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<CanvasBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerStatusModerate.READY, banner.getId());

        canvasBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION);

        canvasBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void upperTransaction_whenRestrictedModeEnabled() {
        AtomicReference<List<CanvasBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithModerationInfo>> objects = new AtomicReference<>();

        enableRestrictedMode();
        checkStatusModerate(BannerStatusModerate.READY, banner.getId());

        canvasBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerStatusModerate.READY, banner.getId()); // статус модерации не меняем
        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION); // версию меняем

        canvasBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerStatusModerate.READY, banner.getId()); // статус модерации снова не меняем
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<CanvasBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerStatusModerate.READY, banner.getId());

        canvasBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        canvasBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION + 1);

        canvasBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerStatusModerate.SENT, banner.getId());
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION + 1);
    }

    @Test
    public void versionChangeAfterRetry_whenRestrictedModeEnabled() {
        AtomicReference<List<CanvasBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithModerationInfo>> objects = new AtomicReference<>();

        enableRestrictedMode();
        checkStatusModerate(BannerStatusModerate.READY, banner.getId());

        canvasBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        canvasBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerStatusModerate.READY, banner.getId());
        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION + 1);

        canvasBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerStatusModerate.READY, banner.getId());
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(INITIAL_VERSION + 1);
    }

    private void checkStatusModerate(BannerStatusModerate statusModerate, Long id) {
        var banners = bannerRepository.getSafely(shard, Collections.singleton(id),
                BannerWithSystemFields.class);

        assertThat(banners).isNotEmpty();

        var banner = banners.get(0);

        assertThat(banner.getStatusModerate()).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.SITELINKS_SET));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.SITELINKS_SET);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void reModerateFlagWillNotBeChanged_whenRestrictedModeEnabled() {
        enableRestrictedMode();
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.SITELINKS_SET));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard, banner.getId());
        assertThat(flags).containsExactlyInAnyOrder(RemoderationType.BANNER, RemoderationType.SITELINKS_SET);
    }

    @Test
    public void reModerateFlagWillNotBeRemoved_whenRestrictedModeEnabled() {
        enableRestrictedMode();
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard, banner.getId());
        assertThat(flags).containsExactly(RemoderationType.BANNER);
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard, banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.SITELINKS_SET);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard, banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillNotBeChanged_whenRestrictedModeEnabled() {
        enableRestrictedMode();
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard, banner.getId());
        assertThat(flags).containsExactlyInAnyOrder(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET);
    }

    @Test
    public void autoAcceptanceFlagWillNotBeRemoved_whenRestrictedModeEnabled() {
        enableRestrictedMode();
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(), Set.of(AutoAcceptanceType.BANNER));

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard, banner.getId());
        assertThat(flags).containsExactly(AutoAcceptanceType.BANNER);
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId()))
                .isEqualTo(CampaignStatusModerate.SENT);
    }

    @Test
    public void campaignWillNotBeSent_whenRestrictedModeEnabled() {
        enableRestrictedMode();
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(), CampaignStatusModerate.READY);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId()))
                .isEqualTo(CampaignStatusModerate.READY);
    }

    @Test
    public void turbolandingWillBeReSent() {
        testModerationRepository.setBannerTurbolandingStatusModerate(shard, banner.getId(),
                BannerTurboLandingStatusModerate.SENT);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        var turbolandings = testModerationRepository.getBannerTurbolandings(shard, List.of(banner.getId()));
        assertThat(turbolandings).extracting(BannerTurbolandingsRecord::getStatusmoderate)
                .hasSize(1)
                .element(0).isEqualTo(BannerTurbolandingsStatusmoderate.Ready);
    }

    @Test
    public void turbolandingWillNotBeReSent_whenRestrictedModeEnabled() {
        enableRestrictedMode();
        testModerationRepository.setBannerTurbolandingStatusModerate(shard, banner.getId(),
                BannerTurboLandingStatusModerate.SENT);

        canvasBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        var turbolandings = testModerationRepository.getBannerTurbolandings(shard, List.of(banner.getId()));
        assertThat(turbolandings).extracting(BannerTurbolandingsRecord::getStatusmoderate)
                .hasSize(1)
                .element(0).isEqualTo(BannerTurbolandingsStatusmoderate.Sent);
    }

    @Test
    public void flagsTest() {
        moderationOperationModeProvider.forceMode(ModerationOperationMode.RESTRICTED);
        moderationOperationModeProvider.setImmutableVersionMode();

        testModerationRepository.createBannerVersion(shard, banner.getId(), 75200L, LocalDateTime.now().minusDays(1));

        AtomicReference<List<CanvasBannerModerationRequest>> requests = new AtomicReference<>();
        canvasBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), new AtomicReference<>(), requests,
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
