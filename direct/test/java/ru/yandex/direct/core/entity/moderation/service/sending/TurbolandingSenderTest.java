package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.turbolandings.TurbolandingModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingWithModerationInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.repository.TestTurboLandingRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TurbolandingSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TestTurboLandingRepository testTurboLandingRepository;

    @Autowired
    private TurbolandingSender turbolandingSender;

    private int shard;
    private CpmBannerInfo cpmBannerInfo;
    private CpmBannerInfo cpmBannerInfo2;
    private ClientInfo clientInfo;
    private CreativeInfo creativeInfo;
    private TurboLanding turboLanding;
    private TurboLanding turboLanding2;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo);
        CampaignInfo campaignInfo = adGroupInfo.getCampaignInfo();
        steps.campaignSteps().createCampaign(campaignInfo);
        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);
        long adGroupId = adGroupInfo.getAdGroupId();
        long campaignId = campaignInfo.getCampaignId();

        creativeInfo = steps.creativeSteps()
                .addDefaultCpmVideoAdditionCreative(clientInfo, steps.creativeSteps().getNextCreativeId());


        turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());
        turboLanding2 = steps.turboLandingSteps().createDefaultTurboLanding(clientInfo.getClientId());
        OldCpmBanner cpmBanner = activeCpmVideoBanner(campaignId, adGroupId, creativeInfo.getCreativeId());
        OldCpmBanner cpmBanner2 = activeCpmVideoBanner(campaignId, adGroupId, creativeInfo.getCreativeId());

        cpmBanner.withTurboLandingId(turboLanding.getId());
        cpmBanner.withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY);

        cpmBanner2.withTurboLandingId(turboLanding2.getId());
        cpmBanner2.withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.READY);

        cpmBannerInfo = steps.bannerSteps().createActiveCpmVideoBanner(
                cpmBanner,
                clientInfo);
        cpmBannerInfo2 = steps.bannerSteps().createActiveCpmVideoBanner(
                cpmBanner2,
                clientInfo);

        steps.turboLandingSteps().addBannerToBannerTurbolandingsTableOrUpdate(campaignId, List.of(cpmBanner, cpmBanner2));

        shard = cpmBannerInfo.getShard();
    }

    @Test
    public void versionIncrementTest() {
        testModerationRepository.createTurbolandingVersion(shard, cpmBannerInfo.getBannerId(), 12L);

        turbolandingSender.send(shard, List.of(cpmBannerInfo.getBannerId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getTurbolandingVersion(shard, cpmBannerInfo.getBannerId())).isEqualTo(13L);
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createTurbolandingVersion(shard, cpmBannerInfo.getBannerId(), 12L);
        testModerationRepository.createTurbolandingVersion(shard, cpmBannerInfo2.getBannerId(), 22L);

        turbolandingSender.send(shard, List.of(cpmBannerInfo.getBannerId(), cpmBannerInfo2.getBannerId()),
                e -> System.currentTimeMillis(), e -> "", lst -> {});

        assertThat(testModerationRepository.getTurbolandingVersion(shard, cpmBannerInfo.getBannerId())).isEqualTo(13L);
        assertThat(testModerationRepository.getTurbolandingVersion(shard, cpmBannerInfo2.getBannerId())).isEqualTo(23L);

        checkTurboStatus(OldBannerTurboLandingStatusModerate.SENT, cpmBannerInfo.getBannerId());
        checkTurboStatus(OldBannerTurboLandingStatusModerate.SENT, cpmBannerInfo2.getBannerId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<TurbolandingModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<TurboLandingWithModerationInfo>> objects = new AtomicReference<>();

        checkTurboStatus(OldBannerTurboLandingStatusModerate.READY, cpmBannerInfo.getBannerId());

        turbolandingSender.beforeSendTransaction(shard, List.of(cpmBannerInfo.getBannerId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkTurboStatus(OldBannerTurboLandingStatusModerate.SENDING, cpmBannerInfo.getBannerId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(1600);

        turbolandingSender.afterSendTransaction(shard, objects);

        checkTurboStatus(OldBannerTurboLandingStatusModerate.SENT, cpmBannerInfo.getBannerId());

        assertThat(testModerationRepository.getTurbolandingVersion(shard, cpmBannerInfo.getBannerId())).isEqualTo(1600);
    }

    @Test
    public void versionDoesntChangedAfterRetry() {
        AtomicReference<List<TurbolandingModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<TurboLandingWithModerationInfo>> objects = new AtomicReference<>();

        checkTurboStatus(OldBannerTurboLandingStatusModerate.READY, cpmBannerInfo.getBannerId());

        turbolandingSender.beforeSendTransaction(shard, List.of(cpmBannerInfo.getBannerId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        turbolandingSender.beforeSendTransaction(shard, List.of(cpmBannerInfo.getBannerId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkTurboStatus(OldBannerTurboLandingStatusModerate.SENDING, cpmBannerInfo.getBannerId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(1601);

        turbolandingSender.afterSendTransaction(shard, objects);

        checkTurboStatus(OldBannerTurboLandingStatusModerate.SENT, cpmBannerInfo.getBannerId());
        assertThat(testModerationRepository.getTurbolandingVersion(shard, cpmBannerInfo.getBannerId())).isEqualTo(1601);
    }

    private void checkTurboStatus(OldBannerTurboLandingStatusModerate statusModerate, Long id) {
        OldBannerTurboLandingStatusModerate bannerTurboLandingStatusModerate =
                testTurboLandingRepository.getBannerTurboLandingStatusModerate(shard, id);

        assertThat(bannerTurboLandingStatusModerate).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, cpmBannerInfo.getBannerId(),
                Set.of(RemoderationType.BANNER, RemoderationType.TURBOLANDING));

        turbolandingSender.send(shard, List.of(cpmBannerInfo.getBannerId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                cpmBannerInfo.getBannerId());

        assertThat(flags).containsExactly(RemoderationType.BANNER);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, cpmBannerInfo.getBannerId(),
                Set.of(RemoderationType.TURBOLANDING));

        turbolandingSender.send(shard, List.of(cpmBannerInfo.getBannerId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                cpmBannerInfo.getBannerId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, cpmBannerInfo.getBannerId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.TURBOLANDING));

        turbolandingSender.send(shard, List.of(cpmBannerInfo.getBannerId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                cpmBannerInfo.getBannerId());

        assertThat(flags).containsExactly(AutoAcceptanceType.BANNER);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, cpmBannerInfo.getBannerId(),
                Set.of(AutoAcceptanceType.TURBOLANDING));

        turbolandingSender.send(shard, List.of(cpmBannerInfo.getBannerId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                cpmBannerInfo.getBannerId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, cpmBannerInfo.getCampaignId(),
                CampaignStatusModerate.READY);

        turbolandingSender.send(shard, List.of(cpmBannerInfo.getBannerId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, cpmBannerInfo.getCampaignId())).isEqualTo(CampaignStatusModerate.SENT);
    }

}
