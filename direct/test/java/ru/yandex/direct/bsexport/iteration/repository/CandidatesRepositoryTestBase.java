package ru.yandex.direct.bsexport.iteration.repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.bsexport.configuration.BsExportTest;
import ru.yandex.direct.bsexport.iteration.container.ExportCandidatesSelectionCriteria;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bs.export.model.WorkerSpec;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportQueueInfo;
import ru.yandex.direct.core.entity.bs.export.queue.model.BsExportSpecials;
import ru.yandex.direct.core.entity.bs.export.queue.model.QueueType;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportSpecialsRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.core.testing.data.TestBsExportQueueRecords.recordWithFullStat;
import static ru.yandex.direct.dbschema.ppc.Tables.BS_EXPORT_QUEUE;
import static ru.yandex.direct.dbschema.ppc.Tables.BS_EXPORT_SPECIALS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

@BsExportTest
@ExtendWith(SpringExtension.class)
abstract class CandidatesRepositoryTestBase {
    protected static final int TEST_SHARD = 2;
    protected static final WorkerSpec LOCKED_BY = WorkerSpec.STD_1;

    @Autowired
    protected CandidatesRepository queueRepository;

    @Autowired
    protected BsExportQueueRepository bsExportQueueRepository;

    @Autowired
    protected BsExportSpecialsRepository specialsRepository;

    @Autowired
    protected DslContextProvider dslContextProvider;

    @Autowired
    protected Steps steps;

    @Autowired
    protected CampaignRepository campaignRepository;

    protected ClientInfo clientInfo;
    protected ExportCandidatesSelectionCriteria.Builder selectionCriteria;
    protected DSLContext testShardContext;

    private Set<Long> ids = new HashSet<>();


    @BeforeEach
    void prepare() {
        testShardContext = dslContextProvider.ppc(TEST_SHARD);

        clientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(TEST_SHARD));

        selectionCriteria = ExportCandidatesSelectionCriteria.builder()
                .setShard(TEST_SHARD)
                .setWorkerSpec(LOCKED_BY)
                .setLockNewCampaigns(true);
    }

    @AfterEach
    void cleanup() {
        if (testShardContext == null) {
            return;
        }
        testShardContext.delete(BS_EXPORT_QUEUE).where(BS_EXPORT_QUEUE.CID.in(ids));
        testShardContext.delete(BS_EXPORT_SPECIALS).where(BS_EXPORT_SPECIALS.CID.in(ids));
    }

    void setCampaignStatusBsSynced(DSLContext dslContext, Long campaignId, StatusBsSynced statusBsSynced) {
        dslContext.update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_BS_SYNCED, CampaignMappings.statusBsSyncedToDb(statusBsSynced))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    void setQueueType(Long campaignId, QueueType queueType) {
        if (queueType == null) {
            return;
        }
        specialsRepository.add(testShardContext, Collections.singletonList(new BsExportSpecials()
                .withCampaignId(campaignId)
                .withType(queueType)));

    }

    Long prepareCampaignWallet(DSLContext dsl, @Nullable WorkerSpec lockedBy) {
        return prepareCampaign(dsl, CampaignType.WALLET, lockedBy);
    }

    Long prepareCampaignText(DSLContext dsl, @Nullable WorkerSpec lockedBy) {
        return prepareCampaign(dsl, CampaignType.TEXT, lockedBy);
    }

    Campaign prepareCampaignWithWallet(DSLContext dsl, Long campWalletId, @Nullable WorkerSpec lockedBy) {
        Long campId = steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, campWalletId, BigDecimal.ONE).getCampaignId();
        setCampaignStatusBsSynced(dsl, campId, StatusBsSynced.NO);

        BsExportQueueInfo queueRecord = recordWithFullStat(campId)
                .withLockedBy(lockedBy != null ? (int) lockedBy.getWorkerId() : null);
        bsExportQueueRepository.insertRecord(dsl, queueRecord);
        ids.add(campId);

        List<Campaign> campaigns = campaignRepository.getCampaigns(TEST_SHARD, Collections.singletonList(campId));
        return Iterables.getOnlyElement(campaigns);
    }

    Long prepareCampaign(DSLContext dsl, CampaignType campaignType, @Nullable WorkerSpec lockedBy) {
        Long campaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        ids.add(campaignId);
        if (campaignType != CampaignType.TEXT) {
            dsl.update(CAMPAIGNS)
                    .set(CAMPAIGNS.TYPE, CampaignType.toSource(campaignType))
                    .where(CAMPAIGNS.CID.eq(campaignId))
                    .execute();
        }
        setCampaignStatusBsSynced(dsl, campaignId, StatusBsSynced.NO);

        BsExportQueueInfo queueRecord = recordWithFullStat(campaignId)
                .withLockedBy(lockedBy != null ? (int) lockedBy.getWorkerId() : null);
        bsExportQueueRepository.insertRecord(dsl, queueRecord);

        return campaignId;
    }

    @QueryWithoutIndex("Удаление всех данных для тестов")
    void runWithEmptyBsExportQueueTable(Consumer<DSLContext> test) {
        try {
            dslContextProvider.ppcTransaction(TEST_SHARD, configuration -> {
                DSLContext dsl = configuration.dsl();
                dsl.deleteFrom(BS_EXPORT_QUEUE).execute();

                test.accept(dsl);

                throw new RollbackException();
            });
        } catch (RollbackException ignored) {
        }
    }
}
