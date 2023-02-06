package ru.yandex.direct.core.entity.bs.export.queue.repository;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;

class BsExportQueueRepositoryGetCampaignsCountInFullExportQueueTest extends BsExportQueueRepositoryBase {

    private Long campaignId;

    @BeforeEach
    void createTestData() {
        campaignId = createCampaign();
    }

    @Test
    void doesNotCountCampaignWithoutFlag() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        Long campaignId1 = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        BsExportQueueInfo queueRecord1 = recordWithFullStat(campaignId1).withNeedFullExport(false);

        dslContextProvider.ppcTransaction(TEST_SHARD, configuration -> {
            DSLContext ctx = configuration.dsl();

            int initialCount = queueRepository.getCampaignsCountInFullExportQueue(ctx);

            queueRepository.insertRecord(ctx, queueRecord1);

            assertEquals(initialCount, queueRepository.getCampaignsCountInFullExportQueue(ctx));
        });
    }

    @Test
    void countCampaignWithFlag() {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        Long campaignId1 = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        BsExportQueueInfo queueRecord1 = recordWithFullStat(campaignId1);

        dslContextProvider.ppcTransaction(TEST_SHARD, configuration -> {
            DSLContext ctx = configuration.dsl();

            int initialCount = queueRepository.getCampaignsCountInFullExportQueue(ctx);

            queueRepository.insertRecord(ctx, queueRecord1);

            assertEquals(initialCount + 1, queueRepository.getCampaignsCountInFullExportQueue(ctx));
        });
    }

    @ParameterizedTest
    @EnumSource(value = QueueType.class, names = {"DEV1", "DEV2", "BUGGY", "NOSEND"})
    void doesNotCountCampaignWithSpecialQueueType(QueueType queueType) {
        withParTypeTestBase(queueType, 0);
    }

    @ParameterizedTest
    @EnumSource(value = QueueType.class, names = {"HEAVY", "PREPROD", "FAST"})
    void countCampaignWithUsualQueueType(QueueType queueType) {
        withParTypeTestBase(queueType, 1);
    }

    private void withParTypeTestBase(QueueType queueType, int expectedDiff) {
        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId);
        queueRepository.insertRecord(testShardContext, queueRecord);

        Long campaignId1 = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        BsExportQueueInfo queueRecord1 = recordWithFullStat(campaignId1);
        BsExportSpecials specialsRecord1 = new BsExportSpecials().withCampaignId(campaignId1).withType(queueType);

        dslContextProvider.ppcTransaction(TEST_SHARD, configuration -> {
            DSLContext ctx = configuration.dsl();

            int initialCount = queueRepository.getCampaignsCountInFullExportQueue(ctx);

            queueRepository.insertRecord(ctx, queueRecord1);
            specialsRepository.add(ctx, singletonList(specialsRecord1));

            assertEquals(initialCount + expectedDiff, queueRepository.getCampaignsCountInFullExportQueue(ctx));
        });
    }
}

