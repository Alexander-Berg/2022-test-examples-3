package ru.yandex.direct.bsexport.iteration.repository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bs.export.model.WorkerSpec;
import ru.yandex.direct.core.entity.bs.export.queue.repository.BsExportQueueRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.dbschema.ppc.Tables.BS_EXPORT_QUEUE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

class CandidatesRepositoryGetLockedWalletsTest extends CandidatesRepositoryTestBase{

    /**
     * Метод возвращает кампании, залоченные воркерами вызывающими блокировки группы заказов в БК
     */
    @ParameterizedTest
    @EnumSource(value = WorkerSpec.class, names = {"STD_1", "STD_2", "HEAVY_2", "BUGGY_1", "CAMP_1", "FAST_1",
            "DEV1_1", "DEV2_1", "PREPROD_1"})
    void seeWalletsLockedByRelatedWorkers(WorkerSpec workerSpec) {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaign(dsl, CampaignType.WALLET, workerSpec);
            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).contains(campWalletId);
        });
    }

    /**
     * Метод не возвращает кампании, залолченные воркерами не вызывающими блокировки группы заказов
     * (указаны в {@link BsExportQueueRepository#WORKER_IDS_NOT_RESTRICTED_BY_LOCKED_WALLETS})
     */
    @ParameterizedTest
    @EnumSource(value = WorkerSpec.class, names = {"FULL_LB_EXPORT_1", "FULL_LB_EXPORT_4", "STDPRICE_1", "STDPRICE_2",
            "DEVPRICE1_1", "DEVPRICE2_1", "MASTER"})
    void doesNotSeeWalletsLockedByUnrelatedWorkers(WorkerSpec workerSpec) {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaign(dsl, CampaignType.WALLET, workerSpec);
            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).doesNotContain(campWalletId);
        });
    }

    /**
     * Если подходящих значений в таблице BS_EXPORT_QUEUE для вывода метода
     * {@link BsExportQueueRepository#getLockedWallets} больше лимита -> размер ответа равен лимиту
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void resultSizeIsNotExceedingLimit(int expectSize) {
        runWithEmptyBsExportQueueTable(dsl -> {
            for (int i = 0; i < expectSize + 1; i++) {
                prepareCampaignWallet(dsl, LOCKED_BY);
            }

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, expectSize);
            assertThat(lockedWallets).hasSize(expectSize);
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть две залоченные кампании под одним общим счетом -> метод
     * {@link BsExportQueueRepository#getLockedWallets} вернет один элемент и он будет равен wallet_cid этих кампаний
     */
    @Test
    void twoLockedCampaignsUnderWallet_returnsOneWalletId() {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaignWallet(dsl, null);
            Campaign campaign1 = prepareCampaignWithWallet(dsl, campWalletId, LOCKED_BY);
            Campaign campaign2 = prepareCampaignWithWallet(dsl, campWalletId, LOCKED_BY);
            assumeThat(campaign1.getWalletId(), equalTo(campaign2.getWalletId()));

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 20);
            assertThat(lockedWallets)
                    .hasSize(1)
                    .containsExactly(campaign1.getWalletId());
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть залоченная кампания под общим счетом -> метод
     * {@link BsExportQueueRepository#getLockedWallets} вернет wallet_cid этой кампании
     */
    @Test
    void oneLockedCampaignsUnderWallet_returnsWalletId() {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaignWallet(dsl, null);
            Campaign campaign = prepareCampaignWithWallet(dsl, campWalletId, LOCKED_BY);

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).containsExactly(campaign.getWalletId());
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть залоченный кошелек -> метод
     * {@link BsExportQueueRepository#getLockedWallets} вернет его cid
     */
    @Test
    void oneLockedWallet_returnsWalletId() {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaignWallet(dsl, LOCKED_BY);

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).containsExactly(campWalletId);
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть незалоченный кошелек -> метод
     * {@link BsExportQueueRepository#getLockedWallets} не вернет его cid
     */
    @Test
    void oneNotLockedWallet_doesNotReturnWalletId() {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaignWallet(dsl, null);

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).doesNotContain(campWalletId);
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть залоченный и синхронизированный с БК кошелек -> метод
     * {@link BsExportQueueRepository#getLockedWallets} не вернет его cid
     */
    @Test
    void oneLockedButSynchronizedWallet_doesNotReturnWalletId() {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaignWallet(dsl, LOCKED_BY);
            setCampaignStatusBsSynced(dsl, campWalletId, StatusBsSynced.YES);

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).doesNotContain(campWalletId);
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть залоченный кошелек с camps_num = 0 -> метод
     * {@link BsExportQueueRepository#getLockedWallets} не вернет его cid
     */
    @Test
    void oneLockedWalletWithoutCampaignStat_doesNotReturnWalletId() {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaign(dsl, CampaignType.WALLET, LOCKED_BY);
            dsl.update(BS_EXPORT_QUEUE)
                    .set(BS_EXPORT_QUEUE.CAMPS_NUM, 0L)
                    .where(BS_EXPORT_QUEUE.CID.eq(campWalletId))
                    .execute();

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).doesNotContain(campWalletId);
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть залоченный кошелек и при этом вызвать метод
     * {@link BsExportQueueRepository#getLockedWallets} с игнорируемым workerId, которым залочена данная кампания
     * -> метод не вернет его cid
     */
    @Test
    void oneLockedByUsWallet_doesNotReturnWalletId() {
        runWithEmptyBsExportQueueTable(dsl -> {
            Long campWalletId = prepareCampaignWallet(dsl, LOCKED_BY);

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, LOCKED_BY.getWorkerId(), 200);
            assertThat(lockedWallets).doesNotContain(campWalletId);
        });
    }

    /**
     * Если в таблице BS_EXPORT_QUEUE есть залоченная кампания без общего счета -> метод
     * {@link BsExportQueueRepository#getLockedWallets} не вернет его cid
     */
    @Test
    void oneLockedCampaignWithoutWallet_returnsNothing() {
        runWithEmptyBsExportQueueTable(dsl -> {
            prepareCampaignText(dsl, LOCKED_BY);

            List<Long> lockedWallets = queueRepository.getLockedWallets(dsl, -1, 200);
            assertThat(lockedWallets).isEmpty();
        });
    }
}
