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

import ru.yandex.direct.core.entity.banner.model.BannerVcardWithModerationInfo;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerVcardModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository.ModerationVersion;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate.READY;
import static ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate.SENDING;
import static ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate.SENT;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerVcardSenderTest {


    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private BannerVcardSender sender;

    private int shard;
    private Vcard vcard;
    private OldTextBanner banner;
    private OldTextBanner banner2;

    @Before
    public void before() throws IOException {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        shard = clientInfo.getShard();
        vcard = steps.vcardSteps().createVcard(campaignInfo)
                .getVcard();

        banner = steps.bannerSteps().createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                        .withLanguage(Language.UNKNOWN)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withBody("TestBody")
                        .withTitle("TestTitle")
                        .withLanguage(Language.RU_)
                        .withVcardId(vcard.getId())
                        .withPhoneFlag(READY)
                        .withTitleExtension("TestTitleExt"),
                clientInfo
        ).getBanner();
        banner2 = steps.bannerSteps().createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                        .withLanguage(Language.UNKNOWN)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withBody("TestBody")
                        .withTitle("TestTitle")
                        .withLanguage(Language.RU_)
                        .withVcardId(vcard.getId())
                        .withPhoneFlag(READY)
                        .withTitleExtension("TestTitleExt"),
                clientInfo
        ).getBanner();
    }

    @Test
    public void createNewVersionTest() {
        sender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        ModerationVersion version = testModerationRepository.getVcardVersionObj(shard,
                banner.getId());
        assertThat(version.getVersion()).isEqualTo(BannerVcardSender.INITIAL_VERSION);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createVcardVersion(shard, banner.getId(), 75100L, LocalDateTime.now().minusDays(1));

        sender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        ModerationVersion version =
                testModerationRepository.getVcardVersionObj(shard, banner.getId());
        assertThat(version.getVersion()).isEqualTo(75101L);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createVcardVersion(shard, banner.getId(), 75100L, LocalDateTime.now().minusDays(1));
        testModerationRepository.createVcardVersion(shard, banner2.getId(), 75200L, LocalDateTime.now().minusDays(1));

        sender.send(shard, List.of(banner.getId(), banner2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getVcardVersionObj(shard, banner.getId()).getVersion()).isEqualTo(75101L);
        assertThat(testModerationRepository.getVcardVersionObj(shard, banner2.getId()).getVersion()).isEqualTo(75201L);

        checkStatusModerate(SENT, banner.getId());
        checkStatusModerate(SENT, banner2.getId());
    }

    @Test
    public void versionChangeToInitialTest() {
        testModerationRepository.createVcardVersion(shard, banner.getId(), 12L,
                LocalDateTime.now().minusDays(1));

        sender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getVcardVersionObj(shard, banner.getId());
        assertThat(version.getVersion()).isEqualTo(BannerVcardSender.INITIAL_VERSION);
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<BannerVcardModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerVcardWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(READY, banner.getId());

        sender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(BannerVcardSender.INITIAL_VERSION);

        sender.afterSendTransaction(shard, objects);

        checkStatusModerate(SENT, banner.getId());

        assertThat(testModerationRepository.getVcardVersionObj(shard, banner.getId()).getVersion())
                .isEqualTo(BannerVcardSender.INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<BannerVcardModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<BannerVcardWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(READY, banner.getId());

        sender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        sender.beforeSendTransaction(shard, List.of(banner.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);


        checkStatusModerate(SENDING, banner.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(75001L);

        sender.afterSendTransaction(shard, objects);

        checkStatusModerate(SENT, banner.getId());
        assertThat(testModerationRepository.getVcardVersionObj(shard, banner.getId()).getVersion())
                .isEqualTo(75001L);
    }

    @Test
    public void reModerateFlagWillBeRemoved() {
        testModerationRepository.createReModerationRecord(shard, banner.getId(),
                Set.of(RemoderationType.CONTACTS));

        sender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });


        Set<RemoderationType> flags = testModerationRepository.getReModerationRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    @Test
    public void autoAcceptanceFlagWillBeRemoved() {
        testModerationRepository.createAutoAcceptRecord(shard, banner.getId(),
                Set.of(AutoAcceptanceType.CONTACTS));

        sender.send(shard, List.of(banner.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        Set<AutoAcceptanceType> flags = testModerationRepository.getAutoAcceptanceRecord(shard,
                banner.getId());

        assertThat(flags).isNull();
    }

    private void checkStatusModerate(StatusPhoneFlagModerate statusModerate, Long id) {

        List<OldBanner> banners = bannerRepository.getBanners(shard,
                Collections.singleton(id));

        assertThat(banners).isNotEmpty();

        OldTextBanner banner = (OldTextBanner) banners.get(0);
        assertThat(banner.getPhoneFlag()).isEqualTo(statusModerate);
    }
}
