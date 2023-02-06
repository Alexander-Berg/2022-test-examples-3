package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.VideoWithModerationInfo;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerVideoAdditionModerationRequest;
import ru.yandex.direct.core.entity.moderation.repository.sending.BannerVideoAdditionSendingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository.ModerationVersion;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate.Ready;
import static ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate.Sending;
import static ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate.Sent;
import static ru.yandex.direct.feature.FeatureName.SOCIAL_ADVERTISING;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerVideoAdditionSenderTest {
    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TestBannerCreativeRepository testBannerCreativeRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BannerVideoAdditionSendingRepository bannerVideoAdditionSendingRepository;

    @Autowired
    private ModerationOperationModeProvider moderationOperationModeProvider;

    @Autowired
    private CampaignRepository campaignRepository;

    private BannerVideoAdditionSender sender;

    @Mock
    FeatureService featureService;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private int shard;
    private TextBannerInfo bannerInfo;
    private TextBannerInfo bannerInfo2;
    private CreativeInfo creativeInfo;

    @Before
    public void before() throws IOException {
        MockitoAnnotations.openMocks(this);

        sender = new BannerVideoAdditionSender(dslContextProvider,
                bannerVideoAdditionSendingRepository,
                moderationOperationModeProvider,
                campaignRepository,
                featureService);

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        shard = clientInfo.getShard();

        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                        .withLanguage(Language.UNKNOWN)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withBody("TestBody")
                        .withLanguage(Language.RU_)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.READY)
                        .withTitleExtension("TestTitleExt"),
                clientInfo
        );
        bannerInfo2 = steps.bannerSteps().createBanner(activeTextBanner(campaignInfo.getCampaignId(), null)
                        .withLanguage(Language.UNKNOWN)
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withBody("TestBody")
                        .withLanguage(Language.RU_)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.READY)
                        .withTitleExtension("TestTitleExt"),
                clientInfo
        );

        creativeInfo = steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo);
        steps.bannerCreativeSteps().createTextBannerCreative(new BannerCreativeInfo()
                .withCreativeInfo(creativeInfo)
                .withBannerInfo(bannerInfo));
        steps.bannerCreativeSteps().createTextBannerCreative(new BannerCreativeInfo()
                .withCreativeInfo(creativeInfo)
                .withBannerInfo(bannerInfo2));
    }

    @Test
    public void createNewVersionTest() {
        sender.send(shard, List.of(bannerInfo.getBannerId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        ModerationVersion version = testModerationRepository.getBannerCreativeVersionObj(shard,
                bannerInfo.getBannerId(), creativeInfo.getCreativeId());
        assertThat(version.getVersion()).isEqualTo(BannerVideoAdditionSender.INITIAL_VERSION);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createBannerCreativeVersion(shard, bannerInfo.getBannerId(),
                creativeInfo.getCreativeId(), 80100L,
                LocalDateTime.now().minusDays(1));

        sender.send(shard, List.of(bannerInfo.getBannerId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getBannerCreativeVersionObj(shard, bannerInfo.getBannerId(),
                        creativeInfo.getCreativeId());
        assertThat(version.getVersion()).isEqualTo(80101L);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createBannerCreativeVersion(shard, bannerInfo.getBannerId(),
                creativeInfo.getCreativeId(), 80100L,
                LocalDateTime.now().minusDays(1));
        testModerationRepository.createBannerCreativeVersion(shard, bannerInfo2.getBannerId(),
                creativeInfo.getCreativeId(), 80200L,
                LocalDateTime.now().minusDays(1));

        sender.send(shard, List.of(bannerInfo.getBannerId(), bannerInfo2.getBannerId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getBannerCreativeVersionObj(shard, bannerInfo.getBannerId(),
                creativeInfo.getCreativeId()).getVersion())
                .isEqualTo(80101L);
        assertThat(testModerationRepository.getBannerCreativeVersionObj(shard, bannerInfo2.getBannerId(),
                creativeInfo.getCreativeId()).getVersion())
                .isEqualTo(80201L);

        checkStatusModerate(Sent, bannerInfo.getBannerId());
        checkStatusModerate(Sent, bannerInfo2.getBannerId());
    }

    @Test
    public void versionChangeToInitialTest() {
        testModerationRepository.createBannerCreativeVersion(shard, bannerInfo.getBannerId(),
                creativeInfo.getCreativeId(),
                12L,
                LocalDateTime.now().minusDays(1));

        sender.send(shard, List.of(bannerInfo.getBannerId()), e -> System.currentTimeMillis(), e -> "", lst -> {
        });

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getBannerCreativeVersionObj(shard, bannerInfo.getBannerId(),
                        creativeInfo.getCreativeId());
        assertThat(version.getVersion()).isEqualTo(BannerVideoAdditionSender.INITIAL_VERSION);
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<BannerVideoAdditionModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<VideoWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(Ready, bannerInfo.getBannerId());

        sender.beforeSendTransaction(shard, List.of(bannerInfo.getBannerId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(Sending, bannerInfo.getBannerId());

        assertThat(requests.get()).hasSize(1);

        sender.afterSendTransaction(shard, objects);

        checkStatusModerate(Sent, bannerInfo.getBannerId());

        assertThat(testModerationRepository.getBannerCreativeVersionObj(shard, bannerInfo.getBannerId(),
                creativeInfo.getCreativeId()).getVersion())
                .isEqualTo(BannerVideoAdditionSender.INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<BannerVideoAdditionModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<VideoWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(Ready, bannerInfo.getBannerId());

        sender.beforeSendTransaction(shard, List.of(bannerInfo.getBannerId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        sender.beforeSendTransaction(shard, List.of(bannerInfo.getBannerId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);


        checkStatusModerate(Sending, bannerInfo.getBannerId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(80001L);

        sender.afterSendTransaction(shard, objects);

        checkStatusModerate(Sent, bannerInfo.getBannerId());
        assertThat(testModerationRepository.getBannerCreativeVersionObj(shard, bannerInfo.getBannerId(),
                creativeInfo.getCreativeId()).getVersion())
                .isEqualTo(80001L);
    }

    @Test
    public void checkRequestDataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        Mockito.when(featureService.isEnabledForClientId(Mockito.any(ClientId.class), Mockito.eq(SOCIAL_ADVERTISING)))
                .thenReturn(true);

        AtomicReference<List<BannerVideoAdditionModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<VideoWithModerationInfo>> objects = new AtomicReference<>();

        sender.beforeSendTransaction(shard, List.of(bannerInfo.getBannerId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        assertThat(requests.get()).hasSize(1);
        var data = requests.get().get(0).getData();
        assertThat(data.getAsSoonAsPossible()).isTrue();
        assertThat(data.getIsSocialAdvertisement()).isTrue();
    }

    private void checkStatusModerate(BannersPerformanceStatusmoderate expectedStatusModerate, Long id) {
        BannersPerformanceStatusmoderate statusModerate =
                testBannerCreativeRepository.getBannerPerformanceStatus(shard, id);
        assertEquals(expectedStatusModerate, statusModerate);
    }
}
