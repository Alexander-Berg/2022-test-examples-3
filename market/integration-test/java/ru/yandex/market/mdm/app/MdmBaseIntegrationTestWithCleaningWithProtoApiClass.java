package ru.yandex.market.mdm.app;

import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SupplierDqScoreRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToErpQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SilverSskuYtStorageQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuPartnerVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MskuSyncResultRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.utils.SecurityUtil;

@Transactional
public abstract class MdmBaseIntegrationTestWithCleaningWithProtoApiClass
    extends MdmBaseIntegrationTestWithProtoApiClass {
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    protected ServiceSskuConverter serviceSskuConverter;
    @Autowired
    protected MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    protected MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private SupplierDqScoreRepository supplierDqScoreRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private CustomsCommCodeRepository codeRepository;
    @Autowired
    private PriceInfoRepository priceInfoRepository;
    @Autowired
    private StorageKeyValueRepository storageKeyValueRepository;
    @Autowired
    private SendReferenceItemQRepository sendReferenceItemQRepository;
    @Autowired
    private SskuPartnerVerdictRepository sskuPartnerVerdictRepository;
    @Autowired
    private SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
    @Autowired
    private SendToDatacampQRepository sendToDatacampQRepository;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private MskuSyncResultRepository mskuSyncResultRepository;
    @Autowired
    private MskuToMboQueueRepository mskuToMboQueueRepository;
    @Autowired
    private SilverSskuYtStorageQueue silverSskuYtStorageQueue;
    @Autowired
    private SendToErpQueueRepository sendToErpQueueRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private CargoTypeRepository cargoTypeRepository;

    @After
    public void cleanDb() {
        mappingsCacheRepository.deleteAll();
        sskuToRefreshRepository.deleteAll();

        priceInfoRepository.deleteAll();
        codeRepository.deleteAll();
        mdmSupplierRepository.deleteAll();
        supplierDqScoreRepository.deleteAll();
        goldSskuRepository.deleteAllSskus();
        silverSskuRepository.deleteAll();
        referenceItemRepository.deleteAll();
        categoryParamValueRepository.deleteAll();
        sskuToRefreshRepository.deleteAll();
        fromIrisItemRepository.deleteAll();
        masterDataRepository.deleteAll();
        mskuToRefreshRepository.deleteAll();
        sskuToRefreshRepository.deleteAll();
        storageKeyValueRepository.deleteAll();
        sendReferenceItemQRepository.deleteAll();
        sskuPartnerVerdictRepository.deleteAll();
        sendToDatacampQRepository.deleteAll();
        qualityDocumentRepository.deleteAll();
        sskuGoldenVerdictRepository.deleteAll();
        mskuToMboQueueRepository.deleteAll();
        mskuSyncResultRepository.deleteAll();
        silverSskuYtStorageQueue.deleteAll();
        sendToErpQueueRepository.deleteAll();
        sskuExistenceRepository.clearRepository();
        cargoTypeRepository.deleteAll();

        keyValueService.invalidateCache();
        mdmParamCache.refresh();
        TestTransaction.flagForCommit();
        TestTransaction.end();

        SecurityUtil.deauthenticate();
    }
}
