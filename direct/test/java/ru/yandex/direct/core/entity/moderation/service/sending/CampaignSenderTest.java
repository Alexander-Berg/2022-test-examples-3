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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithModerationInfo;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.ClientFlags;
import ru.yandex.direct.core.entity.moderation.model.campaign.CampaignModerationRequest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusPostModerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignSenderTest {
    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private CampaignSender campaignSender;

    @Autowired
    private CampaignRepository campaignRepository;

    private int shard;
    private ClientInfo clientInfo;

    private CampaignInfo campaignInfo;
    private CampaignInfo campaignInfo2;
    private CampaignInfo performanceCampaignInfo;

    @Before
    public void before() throws IOException {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        steps.clientOptionsSteps().setClientFlags(clientInfo, ClientFlags.NO_TEXT_AUTOCORRECTION.getTypedValue());

        campaignInfo = createReadyTextCampaign();
        campaignInfo2 = createReadyTextCampaign();
        performanceCampaignInfo = createReadyPerformanceCampaign();
    }

    private CampaignInfo createReadyTextCampaign() {
        return steps.campaignSteps().createCampaign(
                activeTextCampaign(null, null)
                        .withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.NEW)
                        .withStatusBsSynced(StatusBsSynced.SENDING),
                clientInfo);
    }

    private CampaignInfo createReadyPerformanceCampaign() {
        return steps.campaignSteps().createCampaign(
                activePerformanceCampaign(null, null)
                        .withStatusModerate(StatusModerate.READY)
                        .withStatusPostModerate(StatusPostModerate.NEW)
                        .withStatusBsSynced(StatusBsSynced.SENDING),
                clientInfo);
    }

    @Test
    public void createNewVersion() {
        send(List.of(campaignInfo.getCampaignId()));
        checkModerationVersion(CampaignSender.INITIAL_VERSION, campaignInfo.getCampaignId());
        checkStatusModerate(CampaignStatusModerate.SENT, campaignInfo.getCampaignId());
    }

    @Test
    public void incrementExistingVersion() {
        testModerationRepository.createCampaignVersion(shard, campaignInfo.getCampaignId(),
                101L, LocalDateTime.now().minusDays(1));

        send(List.of(campaignInfo.getCampaignId()));
        checkModerationVersion(102L, campaignInfo.getCampaignId());
        checkStatusModerate(CampaignStatusModerate.SENT, campaignInfo.getCampaignId());
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createCampaignVersion(shard, campaignInfo.getCampaignId(),
                101L, LocalDateTime.now().minusDays(1));
        testModerationRepository.createCampaignVersion(shard, campaignInfo2.getCampaignId(),
                201L, LocalDateTime.now().minusDays(1));

        send(List.of(campaignInfo.getCampaignId(), campaignInfo2.getCampaignId()));

        checkModerationVersion(102L, campaignInfo.getCampaignId());
        checkModerationVersion(202L, campaignInfo2.getCampaignId());

        checkStatusModerate(CampaignStatusModerate.SENT, campaignInfo.getCampaignId());
        checkStatusModerate(CampaignStatusModerate.SENT, campaignInfo2.getCampaignId());
    }

    @Test
    public void sendPerformanceCampaign() {
        testModerationRepository.createCampaignVersion(shard, performanceCampaignInfo.getCampaignId(),
                101L, LocalDateTime.now().minusDays(1));

        send(List.of(performanceCampaignInfo.getCampaignId()));

        checkModerationVersion(102L, performanceCampaignInfo.getCampaignId());

        checkStatusModerate(CampaignStatusModerate.YES, performanceCampaignInfo.getCampaignId());
    }

    @Test
    public void checkDataIsCorrect() {
        AtomicReference<List<CampaignModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<CampaignWithModerationInfo>> objects = new AtomicReference<>();

        campaignSender.beforeSendTransaction(shard, List.of(campaignInfo.getCampaignId()),
                objects, requests, e -> System.currentTimeMillis(), el -> null);

        assertThat(requests.get()).hasSize(1);

        var request = requests.get().get(0);

        var meta = request.getMeta();
        assertThat(meta.getVersionId()).isEqualTo(CampaignSender.INITIAL_VERSION);
        assertThat(meta.getCampaignId()).isEqualTo(campaignInfo.getCampaignId());
        assertThat(meta.getClientId()).isEqualTo(campaignInfo.getClientId().asLong());
        assertThat(meta.getUid()).isEqualTo(campaignInfo.getClientInfo().getUid());

        var data = request.getData();
        assertThat(data.getName()).isEqualTo(campaignInfo.getCampaign().getName());
        assertThat(data.getLogin()).isEqualTo(campaignInfo.getClientInfo().getLogin());
        assertThat(data.getPayForConversion()).isNull();
        assertThat(data.getSpellingAutoCorrect()).isFalse();
    }

    private void send(List<Long> campaignIds) {
        campaignSender.send(shard, campaignIds, e -> System.currentTimeMillis(), e -> "", lst -> {
        });
    }

    private void checkModerationVersion(Long expectedVersion, Long id) {
        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getCampaignVersionObj(shard, id);
        assertThat(moderationVersion.getVersion()).isEqualTo(expectedVersion);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    private void checkStatusModerate(CampaignStatusModerate statusModerate, Long id) {
        var campaign = campaignRepository.getCampaigns(shard, List.of(id)).get(0);

        assertThat(campaign).isNotNull();
        assertThat(campaign.getStatusModerate()).isEqualTo(statusModerate);
    }
}
