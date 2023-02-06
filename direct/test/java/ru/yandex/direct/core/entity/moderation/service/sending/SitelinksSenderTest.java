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

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.model.sitelinks.SitelinksModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSetWithModerationInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SitelinksSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private SitelinksSender sitelinksSender;


    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private OldTextBanner banner2;
    private SitelinkSetInfo sitelinkSetInfo;
    private SitelinkSetInfo sitelinkSetInfo2;

    @Before
    public void before() throws IOException {

        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();

        clientInfo = campaignInfo.getClientInfo();

        shard = clientInfo.getShard();

        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
        sitelinkSetInfo2 = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);

        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                                .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                                .withStatusSitelinksModerate(StatusSitelinksModerate.READY),
                        clientInfo
                )
                .getBanner();
        banner2 = steps.bannerSteps()
                .createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                                .withSitelinksSetId(sitelinkSetInfo2.getSitelinkSetId())
                                .withStatusSitelinksModerate(StatusSitelinksModerate.READY),
                        clientInfo
                )
                .getBanner();
    }

    @Test
    public void versionIncrementTest() {
        testModerationRepository.createSitelinksVersion(shard, banner.getId(), 12L);

        sitelinksSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getSitelinksVersion(shard, banner.getId())).isEqualTo(13L);
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createSitelinksVersion(shard, banner.getId(), 12L);
        testModerationRepository.createSitelinksVersion(shard, banner2.getId(), 22L);

        sitelinksSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getSitelinksVersion(shard, banner.getId())).isEqualTo(13L);
        assertThat(testModerationRepository.getSitelinksVersion(shard, banner2.getId())).isEqualTo(23L);

        checkStatusModerate(StatusSitelinksModerate.SENT, banner.getId());
        checkStatusModerate(StatusSitelinksModerate.SENT, banner2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<SitelinksModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<SitelinkSetWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(StatusSitelinksModerate.READY, banner.getId());

        sitelinksSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(StatusSitelinksModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(40000000L);

        sitelinksSender.afterSendTransaction(shard, objects);

        checkStatusModerate(StatusSitelinksModerate.SENT, banner.getId());

        assertThat(testModerationRepository.getSitelinksVersion(shard, banner.getId())).isEqualTo(40000000L);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<SitelinksModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<SitelinkSetWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(StatusSitelinksModerate.READY, banner.getId());

        sitelinksSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        sitelinksSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(StatusSitelinksModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(40000001L);

        sitelinksSender.afterSendTransaction(shard, objects);

        checkStatusModerate(StatusSitelinksModerate.SENT, banner.getId());
        assertThat(testModerationRepository.getSitelinksVersion(shard, banner.getId())).isEqualTo(40000001L);
    }

    private void checkStatusModerate(StatusSitelinksModerate statusModerate, Long id) {

        List<OldBanner> dbDisplayhrefs = bannerRepository.getBanners(shard,
                Collections.singleton(id));

        assertThat(dbDisplayhrefs).isNotEmpty();

        OldTextBanner banner = (OldTextBanner) dbDisplayhrefs.get(0);

        assertThat(banner.getStatusSitelinksModerate()).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.SITELINKS_SET));

        sitelinksSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.BANNER);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.SITELINKS_SET));

        sitelinksSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET));

        sitelinksSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.BANNER);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.SITELINKS_SET));

        sitelinksSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        sitelinksSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId())).isEqualTo(CampaignStatusModerate.SENT);
    }

}
