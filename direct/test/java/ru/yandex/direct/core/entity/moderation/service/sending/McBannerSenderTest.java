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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdImageModerationInfo;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldMcBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.moderation.ModerationOperationMode;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.ad_image.AdImageBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMcBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.AGE6;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.BABY_FOOD0;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class McBannerSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private ModerationOperationModeProvider moderationOperationModeProvider;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private McBannerSender mcBannerSender;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldMcBanner banner;
    private OldMcBanner banner2;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() throws IOException {
        moderationOperationModeProvider.disableForcedMode();
        moderationOperationModeProvider.disableImmutableVersionMode();

        clientInfo = steps.clientSteps().createDefaultClient();
        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        adGroupInfo = steps.adGroupSteps().createAdGroup(activeMcBannerAdGroup(campaignInfo.getCampaignId()),
                clientInfo);

        shard = clientInfo.getShard();

        banner = (OldMcBanner) steps.bannerSteps()
                .createBanner(activeMcBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                                .withStatusModerate(OldBannerStatusModerate.READY)
                                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                        adGroupInfo
                )
                .getBanner();
        banner2 = (OldMcBanner) steps.bannerSteps()
                .createBanner(activeMcBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                                .withStatusModerate(OldBannerStatusModerate.READY)
                                .withFlags(BannerFlags.fromSource("age:6,annoying,plus18,suspicious_goods,baby_food:0")),
                        adGroupInfo
                )
                .getBanner();

        testModerationRepository.addBannerImageFormat(shard, banner.getImage().getImageHash(),
                new ImageSize().withHeight(100).withWidth(100));
        testModerationRepository.addBannerImageFormat(shard, banner2.getImage().getImageHash(),
                new ImageSize().withHeight(200).withWidth(200));
    }

    @Test
    public void versionIncrementTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L);

        mcBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(13L);
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createBannerVersion(shard, banner.getId(), 12L);
        testModerationRepository.createBannerVersion(shard, banner2.getId(), 22L);

        mcBannerSender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(13L);
        assertThat(testModerationRepository.getBannerVersion(shard, banner2.getId())).isEqualTo(23L);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        checkStatusModerate(OldBannerStatusModerate.SENT, banner2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<AdImageBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithAdImageModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        mcBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(75000L);

        mcBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());

        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(75000L);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<AdImageBannerModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerWithAdImageModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(OldBannerStatusModerate.READY, banner.getId());

        mcBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        mcBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(OldBannerStatusModerate.SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(75001L);

        mcBannerSender.afterSendTransaction(shard, objects);

        checkStatusModerate(OldBannerStatusModerate.SENT, banner.getId());
        assertThat(testModerationRepository.getBannerVersion(shard, banner.getId())).isEqualTo(75001L);
    }

    private void checkStatusModerate(OldBannerStatusModerate statusModerate, Long id) {

        List<OldBanner> banners = bannerRepository.getBanners(shard,
                Collections.singleton(id));

        assertThat(banners).isNotEmpty();

        OldMcBanner banner = (OldMcBanner) banners.get(0);

        assertThat(banner.getStatusModerate()).isEqualTo(statusModerate);
    }

    @Test
    public void reModerateFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER, RemoderationType.SITELINKS_SET));

        mcBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(RemoderationType.SITELINKS_SET);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.BANNER));

        mcBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeUnCheckedAndNotRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER, AutoAcceptanceType.SITELINKS_SET));

        mcBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).containsExactly(AutoAcceptanceType.SITELINKS_SET);
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.BANNER));

        mcBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void campaignWillBeSent() {
        testModerationRepository.setCampaignStatusModerate(shard, banner.getCampaignId(),
                CampaignStatusModerate.READY);

        mcBannerSender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        assertThat(testModerationRepository.getCampaignStatusModerate(shard, banner.getCampaignId())).isEqualTo(CampaignStatusModerate.SENT);
    }

    @Test
    public void flagsTest() {
        moderationOperationModeProvider.forceMode(ModerationOperationMode.RESTRICTED);
        moderationOperationModeProvider.setImmutableVersionMode();

        testModerationRepository.createBannerVersion(shard, banner.getId(), 75200L, LocalDateTime.now().minusDays(1));

        AtomicReference<List<AdImageBannerModerationRequest>> requests = new AtomicReference<>();
        mcBannerSender.beforeSendTransaction(shard, List.of(banner.getId()), new AtomicReference<>(), requests,
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
