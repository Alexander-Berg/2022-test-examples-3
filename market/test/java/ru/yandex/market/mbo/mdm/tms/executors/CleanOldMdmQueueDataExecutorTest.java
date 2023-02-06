package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.infrastructure.model.LbDump;
import ru.yandex.market.mbo.mdm.common.infrastructure.repository.LbDumpRepository;
import ru.yandex.market.mbo.mdm.common.infrastructure.repository.LbMessageQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.BusinessLockStatus;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.BusinessLockStatusRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.LbFailedOfferQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToErpQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SilverSskuYtStorageQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.failed.SendToDatacampQueueFailedRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.processed.SendToDatacampQueueProcessedRepository;
import ru.yandex.market.mbo.mdm.common.service.ServiceOfferMigrationRepositoryCleaningServiceImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static ru.yandex.market.mboc.common.utils.MdmProperties.OLD_MDM_QUEUE_DATA_HISTORY_IN_HOURS;

/**
 * @author dmserebr
 * @date 05/03/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CleanOldMdmQueueDataExecutorTest extends MdmBaseDbTestClass {

    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;

    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;

    @Autowired
    private SendToDatacampQRepository sendToDatacampQRepository;

    @Autowired
    private SendReferenceItemQRepository sendReferenceItemQRepository;

    @Autowired
    private LbDumpRepository lbDumpRepository;

    @Autowired
    private StorageKeyValueService keyValueService;

    @Autowired
    private ServiceOfferMigrationRepository serviceOfferMigrationRepository;

    @Autowired
    private ServiceOfferMigrationRepositoryCleaningServiceImpl serviceOfferCleaningService;

    @Autowired
    private BusinessLockStatusRepository lockStatusRepository;

    @Autowired
    private LbMessageQueueRepository lbMessageQueueRepository;

    @Autowired
    private LbFailedOfferQueueRepository lbFailedOfferQueueRepository;

    @Autowired
    private MskuToMboQueueRepository mskuToMboQueueRepository;

    @Autowired
    private SilverSskuYtStorageQueue silverSskuYtStorageQueue;

    @Autowired
    private SendToErpQueueRepository sendToErpQueueRepository;

    @Autowired
    private SendToDatacampQueueProcessedRepository sendToDatacampProcessedRepository;

    @Autowired
    private SendToDatacampQueueFailedRepository sendToDatacampQueueFailedRepository;

    private CleanOldMdmQueueDataExecutor executor;

    @Before
    public void before() {
        executor = new CleanOldMdmQueueDataExecutor(
            sskuToRefreshRepository,
            mskuToRefreshRepository,
            sendToDatacampQRepository,
            sendReferenceItemQRepository,
            lbDumpRepository,
            serviceOfferCleaningService,
            lbMessageQueueRepository,
            lbFailedOfferQueueRepository,
            mskuToMboQueueRepository,
            silverSskuYtStorageQueue,
            sendToErpQueueRepository,
            sendToDatacampProcessedRepository,
            sendToDatacampQueueFailedRepository
        );
        keyValueService.putValue(OLD_MDM_QUEUE_DATA_HISTORY_IN_HOURS, -1);
    }

    @Test
    public void whenRemoveOldRecordsShouldRemoveOnlyOldAndProcessedSsku() {

        for (int i = 0; i < 20; ++i) {
            sskuToRefreshRepository.enqueue(new ShopSkuKey(i, "ssku"), MdmEnqueueReason.CHANGED_BY_MDM_ADMIN);
        }

        List<SskuToRefreshInfo> all = sskuToRefreshRepository.findAll();
        sskuToRefreshRepository.markProcessed(all.stream().map(MdmQueueInfoBase::getId).collect(Collectors.toList()));

        Instant sskuTs = all.stream()
            .filter(info -> info.getEntityKey().getSupplierId() == 10)
            .map(MdmQueueInfoBase::getAddedTimestamp)
            .findFirst()
            .orElseThrow();

        executor.execute();

        var remainingOldSskus = sskuToRefreshRepository.findAll().stream()
            .filter(dao -> dao.getAddedTimestamp().isBefore(sskuTs))
            .collect(Collectors.toList());

        Assertions.assertThat(remainingOldSskus).isEmpty();
    }

    @Test
    public void whenRemoveOldRecordsShouldRemoveOnlyOldAndProcessedMsku() {
        for (long i = 0; i < 20; ++i) {
            mskuToRefreshRepository.enqueue(i, MdmEnqueueReason.CHANGED_BY_MDM_ADMIN);
        }

        List<MdmMskuQueueInfo> all = mskuToRefreshRepository.findAll();
        mskuToRefreshRepository.markProcessed(all.stream().map(MdmQueueInfoBase::getId).collect(Collectors.toList()));

        Instant mskuTs = all.stream()
            .filter(info -> info.getEntityKey() == 10L)
            .map(MdmQueueInfoBase::getAddedTimestamp)
            .findFirst()
            .orElseThrow();

        executor.execute();

        var remainingOldMskus = mskuToRefreshRepository.findAll().stream()
            .filter(dao -> dao.getAddedTimestamp().isBefore(mskuTs))
            .collect(Collectors.toList());

        Assertions.assertThat(remainingOldMskus).isEmpty();
    }

    @Test
    public void whenRemoveOldRecordsShouldRemoveOnlyOldAndProcessedDatacamps() {
        for (int i = 0; i < 20; ++i) {
            sendToDatacampQRepository.enqueue(new ShopSkuKey(i, "ssku"));
        }

        List<SskuToRefreshInfo> all = sendToDatacampQRepository.findAll();
        sendToDatacampQRepository.markProcessed(all.stream().map(MdmQueueInfoBase::getId).collect(Collectors.toList()));

        Instant sskuTs = all.stream()
            .filter(info -> info.getEntityKey().getSupplierId() == 10)
            .map(MdmQueueInfoBase::getAddedTimestamp)
            .findFirst()
            .orElseThrow();

        executor.execute();

        var remainingOldSskus = sendToDatacampQRepository.findAll().stream()
            .filter(dao -> dao.getAddedTimestamp().isBefore(sskuTs))
            .collect(Collectors.toList());

        Assertions.assertThat(remainingOldSskus).isEmpty();
    }

    @Test
    public void whenRemoveOldRecordsShouldRemoveOnlyOldAndProcessedReferenceItems() {
        for (int i = 0; i < 20; ++i) {
            sendReferenceItemQRepository.enqueue(new ShopSkuKey(i, "ssku"));
        }

        List<SskuToRefreshInfo> all = sendReferenceItemQRepository.findAll();
        sendReferenceItemQRepository
            .markProcessed(all.stream().map(MdmQueueInfoBase::getId).collect(Collectors.toList()));

        Instant sskuTs = all.stream()
            .filter(info -> info.getEntityKey().getSupplierId() == 10)
            .map(MdmQueueInfoBase::getAddedTimestamp)
            .findFirst()
            .orElseThrow();

        executor.execute();

        var remainingOldSskus = sendReferenceItemQRepository.findAll().stream()
            .filter(dao -> dao.getAddedTimestamp().isBefore(sskuTs))
            .collect(Collectors.toList());

        Assertions.assertThat(remainingOldSskus).isEmpty();
    }

    @Test
    public void whenRemoveOldRecordsShouldRemoveOnlyOldLbDumps() {
        for (int i = 0; i < 20; ++i) {
            lbDumpRepository.insert(
                new LbDump()
                    .setTopic("topic " + i)
                    .setGroupId(i)
                    .setMsgSeqNo(i)
            );
        }

        List<LbDump> all = lbDumpRepository.findAll();

        Instant ts = all.stream()
            .filter(info -> info.getKey().getMsgSeqNo() == 10)
            .map(LbDump::getReadAt)
            .findFirst()
            .orElseThrow();

        executor.execute();

        var remainingOldDumps = lbDumpRepository.findAll().stream()
            .filter(dao -> dao.getReadAt().isBefore(ts))
            .collect(Collectors.toList());

        Assertions.assertThat(remainingOldDumps).isEmpty();
    }

    @Test
    public void whenRemovingOldRecordShouldRemoveOnlyProcessedMigrationInfos() {
        for (int i = 0; i < 20; ++i) {
            serviceOfferMigrationRepository.insert(
                new ServiceOfferMigrationInfo().setSupplierId(i).setShopSku("test").setDstBusinessId(i + 100)
                    .setAddedTimestamp(Instant.now()));
            lockStatusRepository.insert(
                new BusinessLockStatus().setBusinessId(20L).setShopId((long) i)
                    .setStatus(BusinessLockStatus.Status.UNLOCKED));
        }

        List<ServiceOfferMigrationInfo> all = serviceOfferMigrationRepository.findAll();
        serviceOfferMigrationRepository.markProcessed(
            all.stream().map(ServiceOfferMigrationInfo::getShopSkuKey).collect(Collectors.toList()));


        List<ServiceOfferMigrationInfo> processed = serviceOfferMigrationRepository.findAll();

        Instant ts = processed.stream()
            .filter(info -> info.getSupplierId() == 10)
            .map(ServiceOfferMigrationInfo::getProcessedTimestamp)
            .findFirst()
            .orElseThrow();

        serviceOfferMigrationRepository.updateBatch(processed.stream()
            .filter(info -> info.getSupplierId() > 10)
            .peek(info -> info.setProcessedTimestamp(ts.plus(2, ChronoUnit.HOURS)))
            .collect(Collectors.toList()));

        executor.execute();

        var remainingOldSskus = serviceOfferMigrationRepository.findAll();

        Assertions.assertThat(remainingOldSskus).hasSize(9);
    }
}
