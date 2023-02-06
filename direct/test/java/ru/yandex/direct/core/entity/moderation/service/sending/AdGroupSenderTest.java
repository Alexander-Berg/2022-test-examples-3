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

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupWithModerationInfo;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.moderation.model.adgroup.AdGroupModerationRequest;
import ru.yandex.direct.core.entity.moderation.service.sending.adgroup.AdGroupSender;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.entity.moderation.service.sending.adgroup.AdGroupSender.INITIAL_VERSION;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupSenderTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private AdGroupSender adGroupSender;

    @Autowired
    private AdGroupRepository adGroupRepository;

    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private AdGroupInfo adGroupInfo2;

    @Before
    public void before() throws IOException {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        steps.campaignSteps().createCampaign(campaignInfo);
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        adGroupInfo = steps.adGroupSteps()
                .createAdGroup(activeTextAdGroup(campaignInfo.getCampaignId()).withStatusModerate(StatusModerate.READY),
                        campaignInfo);
        adGroupInfo2 = steps.adGroupSteps()
                .createAdGroup(activeTextAdGroup(campaignInfo.getCampaignId()).withStatusModerate(StatusModerate.READY),
                        campaignInfo);
    }

    @Test
    public void createNewVersion() {
        send(List.of(adGroupInfo.getAdGroupId()));
        checkModerationVersion(INITIAL_VERSION, adGroupInfo.getAdGroupId());
    }

    @Test
    public void incrementExistingVersion() {
        testModerationRepository.createAdGroupVersion(shard, adGroupInfo.getAdGroupId(),
                30018L, LocalDateTime.now().minusDays(1));

        send(List.of(adGroupInfo.getAdGroupId()));
        checkModerationVersion(30019L, adGroupInfo.getAdGroupId());
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createAdGroupVersion(shard, adGroupInfo.getAdGroupId(),
                30018L, LocalDateTime.now().minusDays(1));
        testModerationRepository.createAdGroupVersion(shard, adGroupInfo2.getAdGroupId(),
                30028L, LocalDateTime.now().minusDays(1));

        send(List.of(adGroupInfo.getAdGroupId(), adGroupInfo2.getAdGroupId()));

        checkModerationVersion(30019L, adGroupInfo.getAdGroupId());
        checkModerationVersion(30029L, adGroupInfo2.getAdGroupId());

        checkStatuses(StatusModerate.YES, StatusPostModerate.YES, adGroupInfo.getAdGroupId());
        checkStatuses(StatusModerate.YES, StatusPostModerate.YES, adGroupInfo2.getAdGroupId());
    }

    @Test
    public void versionChangeToInitialTest() {
        testModerationRepository.createAdGroupVersion(shard, adGroupInfo.getAdGroupId(),
                12L, LocalDateTime.now().minusDays(1));

        send(List.of(adGroupInfo.getAdGroupId()));
        checkModerationVersion(INITIAL_VERSION, adGroupInfo.getAdGroupId());
    }

    @Test
    public void preparationBeforeSending() {
        AtomicReference<List<AdGroupWithModerationInfo>> objects = new AtomicReference<>();
        AtomicReference<List<AdGroupModerationRequest>> requests = new AtomicReference<>();

        beforeSendTransaction(List.of(adGroupInfo.getAdGroupId()), objects, requests);

        checkStatuses(StatusModerate.SENDING, StatusPostModerate.YES, adGroupInfo.getAdGroupId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(INITIAL_VERSION);
    }

    @Test
    public void finishAfterSending() {
        AtomicReference<List<AdGroupWithModerationInfo>> objects = new AtomicReference<>();
        AtomicReference<List<AdGroupModerationRequest>> requests = new AtomicReference<>();

        beforeSendTransaction(List.of(adGroupInfo.getAdGroupId()), objects, requests);
        afterSendTransaction(objects);
        checkStatuses(StatusModerate.YES, StatusPostModerate.YES, adGroupInfo.getAdGroupId());
    }

    private void send(List<Long> adGroupIds) {
        adGroupSender.send(shard, adGroupIds, e -> System.currentTimeMillis(), e -> "", lst -> {
        });
    }

    private void beforeSendTransaction(List<Long> adGroupIds,
                                       AtomicReference<List<AdGroupWithModerationInfo>> objects,
                                       AtomicReference<List<AdGroupModerationRequest>> requests) {
        ModerationSendingService<Long, AdGroupModerationRequest, AdGroupWithModerationInfo> sender =
                ((ModerationSendingService<Long, AdGroupModerationRequest, AdGroupWithModerationInfo>) adGroupSender);
        sender.beforeSendTransaction(shard, adGroupIds, objects, requests,
                e -> System.currentTimeMillis(), el -> null);
    }

    private void afterSendTransaction(AtomicReference<List<AdGroupWithModerationInfo>> objects) {
        ModerationSendingService<Long, AdGroupModerationRequest, AdGroupWithModerationInfo> sender =
                ((ModerationSendingService<Long, AdGroupModerationRequest, AdGroupWithModerationInfo>) adGroupSender);
        sender.afterSendTransaction(shard, objects);
    }

    private void checkModerationVersion(Long expectedVersion, Long id) {
        TestModerationRepository.ModerationVersion moderationVersion =
                testModerationRepository.getAdGroupVersionObj(shard, id);
        assertThat(moderationVersion.getVersion()).isEqualTo(expectedVersion);
        assertThat(moderationVersion.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    private void checkStatuses(StatusModerate statusModerate, StatusPostModerate statusPostmoderate, Long id) {
        AdGroup adGroup = adGroupRepository.getAdGroups(shard, List.of(id)).get(0);

        assertThat(adGroup).isNotNull();
        assertThat(adGroup.getStatusModerate()).isEqualTo(statusModerate);
        assertThat(adGroup.getStatusPostModerate()).isEqualTo(statusPostmoderate);
    }
}
