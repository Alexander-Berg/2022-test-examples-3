package ru.yandex.direct.core.entity.moderation.service.sending;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.displayhrefs.model.DisplayHrefWithModerationInfo;
import ru.yandex.direct.core.entity.moderation.model.displayhrefs.DisplayHrefsModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerDisplayHrefsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerDisplayHrefsRecord;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.entity.moderation.service.sending.DisplayHrefsSender.VERSION_OFFSET;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DisplayhrefsSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private DisplayHrefsSender displayHrefsSender;


    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private OldTextBanner banner2;

    @Before
    public void before() throws IOException {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        steps.campaignSteps().createCampaign(campaignInfo);
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        banner = steps.bannerSteps()
                .createBanner(
                        activeTextBanner(null, null)
                                .withDisplayHref("New displayhref")
                                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.READY),
                        campaignInfo)
                .getBanner();
        banner2 = steps.bannerSteps()
                .createBanner(
                        activeTextBanner(null, null)
                                .withDisplayHref("New displayhref2")
                                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.READY),
                        campaignInfo)
                .getBanner();
    }

    @Test
    public void createNewVersionTest() {
        displayHrefsSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getDisplayHrefVersionObj(shard, banner.getId());
        assertThat(moderationVersion.getVersion()).isEqualTo(VERSION_OFFSET);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createDisplayHrefsVersion(shard, banner.getId(),
                12L, LocalDateTime.now().minusDays(1));

        displayHrefsSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getDisplayHrefVersionObj(shard, banner.getId());
        assertThat(moderationVersion.getVersion()).isEqualTo(13L);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createDisplayHrefsVersion(shard, banner.getId(), 12L,
                LocalDateTime.now().minusDays(1));
        testModerationRepository.createDisplayHrefsVersion(shard, banner2.getId(), 22L,
                LocalDateTime.now().minusDays(1));

        displayHrefsSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getDisplayHrefVersionObj(shard, banner.getId()).getVersion()).isEqualTo(13L);
        assertThat(testModerationRepository.getDisplayHrefVersionObj(shard, banner2.getId()).getVersion()).isEqualTo(23L);

        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Sent, banner.getId());
        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Sent, banner2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<DisplayHrefsModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<DisplayHrefWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Ready, banner.getId());

        displayHrefsSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Sending, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(30000L);

        displayHrefsSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Sent, banner.getId());

        assertThat(testModerationRepository.getDisplayHrefsVersion(shard, banner.getId())).isEqualTo(30000L);
    }

    @Test
    public void versionChangedAfterRetry() {
        AtomicReference<List<DisplayHrefsModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<DisplayHrefWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Ready, banner.getId());

        displayHrefsSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        displayHrefsSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Sending, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(30001L);

        displayHrefsSender.afterSendTransaction(shard, objects);

        checkStatusModerate(BannerDisplayHrefsStatusmoderate.Sent, banner.getId());
        assertThat(testModerationRepository.getDisplayHrefsVersion(shard, banner.getId())).isEqualTo(30001L);
    }

    private void checkStatusModerate(BannerDisplayHrefsStatusmoderate statusModerate, Long id) {
        List<BannerDisplayHrefsRecord> dbDisplayhrefs = testModerationRepository.getBannerDisplayHrefs(shard,
                Collections.singleton(id));

        assertThat(dbDisplayhrefs).isNotEmpty();
        assertThat(dbDisplayhrefs.get(0).getStatusmoderate()).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.DISPLAY_HREFS));

        displayHrefsSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.BANNER);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.DISPLAY_HREFS));

        displayHrefsSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.DISPLAY_HREFS));

        displayHrefsSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.BANNER);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.DISPLAY_HREFS));

        displayHrefsSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        displayHrefsSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId())).isEqualTo(CampaignStatusModerate.SENT);
    }

}
