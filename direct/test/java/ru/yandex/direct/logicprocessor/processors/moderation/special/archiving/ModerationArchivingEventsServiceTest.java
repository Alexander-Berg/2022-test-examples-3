package ru.yandex.direct.logicprocessor.processors.moderation.special.archiving;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestBannerModerationVersionsRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.ess.common.circuits.moderation.ModerationArchivingObjectType;
import ru.yandex.direct.ess.logicobjects.moderation.special.ModerationArchivingEvent;
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration;

@ContextConfiguration(classes = EssLogicProcessorTestConfiguration.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ModerationArchivingEventsServiceTest {
    @Autowired
    private ModerationArchivingEventsService moderationArchivingEventsService;
    @Autowired
    private TestBannerModerationVersionsRepository moderationVersionsRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private List<List<ModerationArchivingRequest>> requestsResult;

    private ClientInfo clientInfo;
    private int shard;

    private static final long INITIAL_VERSION = 75_000;

    private void consumeRequest(List<ModerationArchivingRequest> requests) {
        requestsResult.add(requests);
    }

    private void setProcessBannerEventsProperty(Boolean value) {
        ppcPropertiesSupport.set(PpcPropertyNames.ENABLE_BANNER_ARCHIVING_TRANSPORT, value.toString());
    }

    private void setProcessCampaignEventsProperty(Boolean value) {
        ppcPropertiesSupport.set(PpcPropertyNames.ENABLE_BANNER_ARCHIVING_BY_CAMPAIGN_TRANSPORT, value.toString());
    }

    @BeforeAll
    void beforeAll() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @BeforeEach
    public void beforeEach() {
        requestsResult = new ArrayList<>();
        setProcessBannerEventsProperty(false);
        setProcessCampaignEventsProperty(false);
    }

    @Test
    public void testNoEvents() {
        setProcessBannerEventsProperty(true);
        setProcessCampaignEventsProperty(true);

        moderationArchivingEventsService.processEvents(shard, Collections.emptyList(), this::consumeRequest);
        Assertions.assertThat(requestsResult).isEmpty();
    }

    @Test
    public void testNotExistingCampaignEvent() {
        setProcessCampaignEventsProperty(true);
        var event = ModerationArchivingEvent.campaignArchivingEvent("tag", 0L, 0L);
        moderationArchivingEventsService.processEvents(shard, List.of(event), this::consumeRequest);
        Assertions.assertThat(requestsResult).isEmpty();
    }

    @Test
    public void testBannerEventSkipByProperty() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        var event = ModerationArchivingEvent.bannerArchivingEvent("tag", 0L, bannerInfo.getCampaignId(),
                bannerInfo.getAdGroupId(), bannerInfo.getBannerId(), BannersBannerType.text);

        moderationArchivingEventsService.processEvents(shard, List.of(event), this::consumeRequest);
        Assertions.assertThat(requestsResult).isEmpty();
    }

    @Test
    public void testCampaignEventSkipByProperty() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var event = ModerationArchivingEvent.campaignArchivingEvent("tag", 0L, campaignInfo.getCampaignId());

        moderationArchivingEventsService.processEvents(shard, List.of(event), this::consumeRequest);
        Assertions.assertThat(requestsResult).isEmpty();
    }

    @Test
    public void testBannerEvents() {
        setProcessBannerEventsProperty(true);

        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        var bannerInfo2 = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        long eventTime = 100500;
        long version = 1;
        moderationVersionsRepository.addVersion(shard, bannerInfo1.getBannerId(), version);

        var events = List.of(
                ModerationArchivingEvent.bannerArchivingEvent("tag", eventTime, bannerInfo1.getCampaignId(),
                        bannerInfo1.getAdGroupId(), bannerInfo1.getBannerId(), BannersBannerType.text),
                ModerationArchivingEvent.bannerArchivingEvent("tag", eventTime + 1, bannerInfo2.getCampaignId(),
                        bannerInfo2.getAdGroupId(), bannerInfo2.getBannerId(), BannersBannerType.text));

        moderationArchivingEventsService.processEvents(shard, events, this::consumeRequest);

        Assertions.assertThat(requestsResult).containsExactlyInAnyOrder(List.of(
                new ModerationArchivingRequest(ModerationArchivingObjectType.BANNER, bannerInfo1.getBannerId(),
                        version, eventTime),
                new ModerationArchivingRequest(ModerationArchivingObjectType.BANNER, bannerInfo2.getBannerId(), INITIAL_VERSION - 1,
                        eventTime + 1)));
    }

    @Test
    public void testCampaignEventGeneratesBannerEvents() {
        setProcessCampaignEventsProperty(true);

        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        var bannerInfo2 = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        long eventTime = 100500;
        long version = 75000L;
        moderationVersionsRepository.addVersion(shard, bannerInfo1.getBannerId(), version);

        var event = ModerationArchivingEvent.campaignArchivingEvent("tag", eventTime, campaignInfo.getCampaignId());

        moderationArchivingEventsService.processEvents(shard, List.of(event), this::consumeRequest);

        Assertions.assertThat(requestsResult).containsExactlyInAnyOrder(List.of(
                new ModerationArchivingRequest(ModerationArchivingObjectType.BANNER, bannerInfo1.getBannerId(),
                        version, eventTime),
                new ModerationArchivingRequest(ModerationArchivingObjectType.BANNER, bannerInfo2.getBannerId(), INITIAL_VERSION - 1,
                        eventTime)));
    }

    @Test
    public void testBatchEventsProcessing() {
        setProcessCampaignEventsProperty(true);

        int batchSize = 10;
        moderationArchivingEventsService.setBatchSize(batchSize);

        var campaignInfo1 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var campaignInfo2 = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        var events = new ArrayList<ModerationArchivingEvent>();
        var expectedRequests = new ArrayList<ModerationArchivingRequest>();

        for (var campaignInfo : List.of(campaignInfo1, campaignInfo2)) {
            long eventTime = campaignInfo.getCampaignId() * 10;

            events.add(ModerationArchivingEvent.campaignArchivingEvent("tag", eventTime,
                    campaignInfo.getCampaignId()));

            for (int i = 0; i < batchSize + batchSize / 2; ++i) {
                var bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);
                long version = INITIAL_VERSION - 1;
                if (i < 3) {
                    version = i + 1;
                    moderationVersionsRepository.addVersion(shard, bannerInfo.getBannerId(), version);
                }
                expectedRequests.add(new ModerationArchivingRequest(ModerationArchivingObjectType.BANNER,
                        bannerInfo.getBannerId(), version, eventTime));
            }
        }
        moderationArchivingEventsService.processEvents(shard, events, this::consumeRequest);
        Assertions.assertThat(requestsResult).containsExactlyElementsOf(Lists.partition(expectedRequests, batchSize));
    }

    @Test
    public void testDuplicateBannerEvents() {
        setProcessBannerEventsProperty(true);

        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        long eventTime = 100500;

        var event = ModerationArchivingEvent.bannerArchivingEvent("tag", eventTime, bannerInfo1.getCampaignId(),
                bannerInfo1.getAdGroupId(), bannerInfo1.getBannerId(), BannersBannerType.text);

        moderationArchivingEventsService.processEvents(shard, List.of(event, event), this::consumeRequest);

        Assertions.assertThat(requestsResult).containsExactly(List.of(
                new ModerationArchivingRequest(ModerationArchivingObjectType.BANNER, bannerInfo1.getBannerId(),
                        INITIAL_VERSION - 1, eventTime)));
    }

    @Test
    public void testDuplicateCampaignEvents() {
        setProcessCampaignEventsProperty(true);

        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        long eventTime = 100500;

        var event = ModerationArchivingEvent.campaignArchivingEvent("tag", eventTime, campaignInfo.getCampaignId());

        moderationArchivingEventsService.processEvents(shard, List.of(event, event), this::consumeRequest);

        Assertions.assertThat(requestsResult).containsExactly(List.of(
                new ModerationArchivingRequest(ModerationArchivingObjectType.BANNER, bannerInfo1.getBannerId(),
                        INITIAL_VERSION - 1, eventTime)));
    }

    @Test
    public void testNoInitialVersionFound() {
        setProcessBannerEventsProperty(true);

        var event = ModerationArchivingEvent.bannerArchivingEvent("tag", 1L, 2L, 3L, 4L, BannersBannerType.performance);

        moderationArchivingEventsService.processEvents(shard, List.of(event), this::consumeRequest);

        Assertions.assertThat(requestsResult).isEmpty();
    }
}
