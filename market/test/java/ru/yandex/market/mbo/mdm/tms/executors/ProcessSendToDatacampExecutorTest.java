package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampUnitedOffer;
import com.google.protobuf.ByteString;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampService;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueuePriorities;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.failed.SendToDatacampQueueFailedRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.processed.SendToDatacampQueueProcessedRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.to_process.SendToDatacampQueueToProcessRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManagerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmToDatacampConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.CommonSskuResolutionFetcher;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessSendToDatacampQueueService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.mockito.Mockito.doNothing;

public class ProcessSendToDatacampExecutorTest extends MdmBaseDbTestClass {

    @Autowired
    private SendToDatacampQRepository sendToDatacampQRepository;
    @Autowired
    private MdmSskuGroupManager groupManager;
    @Autowired
    private MdmToDatacampConverter toDatacampConverter;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private ServiceSskuConverter converter;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private ServiceOfferMigrationRepository serviceOfferMigrationRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private CommonSskuResolutionFetcher commonSskuResolutionFetcher;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private SendToDatacampQueueToProcessRepository toProcessRepository;
    @Autowired
    private SendToDatacampQueueProcessedRepository processedRepository;
    @Autowired
    private SendToDatacampQueueFailedRepository failedRepository;
    @Autowired
    private MasterDataBusinessMergeService mergeService;

    private MdmDatacampService mdmDatacampService;
    private ProcessSendToDatacampQueueService processSendToDatacampQueueService;
    private ProcessSendToDatacampQueueExecutor sendToDatacampQueueExecutor;
    private EnhancedRandom random;
    private ArgumentCaptor<List<DataCampUnitedOffer.UnitedOffer>> sentToDc;
    private List<String> generatedDocumentsList;

    private static final int SUPPLIER_3P_XU = 1234;
    private static final int SUPPLIER_3P_YU = 1235;
    private static final int SUPPLIER_BIZ_U = 777;

    @Before
    public void setup() {
        storageKeyValueService.putValue(MdmProperties.SEND_SSKU_TO_DATACAMP_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.invalidateCache();

        random = TestDataUtils.defaultRandom(1234560);
        mdmDatacampService = Mockito.mock(MdmDatacampService.class);
        sentToDc = ArgumentCaptor.forClass(List.class);
        doNothing().when(mdmDatacampService).sendOffersToDatacamp(sentToDc.capture());
        groupManager = new MdmSskuGroupManagerImpl(masterDataRepository, referenceItemRepository,
            new MdmSupplierCachingServiceImpl(mdmSupplierRepository, storageKeyValueService), converter,
            mappingsCacheRepository,
            serviceOfferMigrationRepository, storageKeyValueService,
            sskuExistenceRepository, new BeruIdMock());
        processSendToDatacampQueueService = new ProcessSendToDatacampQueueService(
            sendToDatacampQRepository,
            mdmDatacampService,
            storageKeyValueService,
            groupManager,
            mergeService,
            toDatacampConverter,
            commonSskuResolutionFetcher,
            transactionTemplate,
            namedParameterJdbcTemplate,
            toProcessRepository,
            processedRepository,
            failedRepository);
        sendToDatacampQueueExecutor = new ProcessSendToDatacampQueueExecutor(processSendToDatacampQueueService);

        mdmSupplierRepository.insertBatch(
            buildSupplier(BeruIdMock.DEFAULT_PROD_BIZ_ID, MdmSupplierType.BUSINESS, null),
            buildSupplier(BeruIdMock.DEFAULT_PROD_FP_ID, MdmSupplierType.FIRST_PARTY, BeruIdMock.DEFAULT_PROD_BIZ_ID),
            buildSupplier(SUPPLIER_BIZ_U, MdmSupplierType.BUSINESS, null),
            buildSupplier(SUPPLIER_3P_XU, MdmSupplierType.THIRD_PARTY, SUPPLIER_BIZ_U),
            buildSupplier(SUPPLIER_3P_YU, MdmSupplierType.THIRD_PARTY, SUPPLIER_BIZ_U));

        generatedDocumentsList = generateData();
    }

    @Test
    public void canTakeFromQueueAndSendCorrectOffersToDatacamp() {
        assertUnprocessedQueueSize(3);

        sendToDatacampQueueExecutor.execute();
        DataCampUnitedOffer.UnitedOffer offer = sentToDc.getValue().get(0);

        String[] documentNumbersArr = new String[generatedDocumentsList.size()];
        Assertions.assertThat(offer.getBasic().getContent().getMasterData().getCertificates().getValueCount())
            .isEqualTo(3);
        Assertions
            .assertThat(offer.getBasic().getContent().getMasterData().getCertificates().getValueList()
                .asByteStringList().stream()
                .map(ByteString::toStringUtf8)
                .collect(Collectors.toList()))
            .containsExactlyInAnyOrder(generatedDocumentsList.toArray(documentNumbersArr));

        offer.getServiceMap().forEach((id, serviceOffer) -> Assertions.assertThat(
            serviceOffer.getContent().getMasterData().getCertificates().getValueCount())
            .isEqualTo(0));

        assertUnprocessedQueueSize(0);
    }

    @Test
    public void ifNoKnownSilverThenIgnore() {
        unmarkEox(SUPPLIER_3P_XU, "u");
        unmarkEox(SUPPLIER_3P_YU, "u");
        unmarkEox(SUPPLIER_BIZ_U, "u");
        assertUnprocessedQueueSize(3);

        sendToDatacampQueueExecutor.execute();
        Assertions.assertThat(sentToDc.getValue()).isEmpty();

        assertUnprocessedQueueSize(0);
    }

    @Test
    public void if1pEnabledShouldNotDiscardThem() {
        sendToDatacampQRepository.deleteAll();
        sendToDatacampQRepository.enqueueAll(List.of(
            new ShopSkuKey(BeruIdMock.DEFAULT_PROD_BIZ_ID, "u"),
            new ShopSkuKey(BeruIdMock.DEFAULT_PROD_FP_ID, "u")
        ));
        assertUnprocessedQueueSize(2);

        sendToDatacampQueueExecutor.execute();
        Assertions.assertThat(sentToDc.getValue()).isNotEmpty();

        assertUnprocessedQueueSize(0);
    }

    @Test
    public void testProcessQueueUsingShapingLogic() {
        // given
        storageKeyValueService.putValue(MdmProperties.USE_SHAPING_IN_SEND_TO_DATACAMP_QUEUE, true);
        storageKeyValueService.putValue(MdmProperties.RETRY_COUNT_LIMIT_FOR_SEND_TO_DATACAMP_QUEUE, 1);
        storageKeyValueService.invalidateCache();

        ShopSkuKey bizKey = new ShopSkuKey(SUPPLIER_BIZ_U, "u");
        List<ShopSkuKey> shopSkuKeys = List.of(
            bizKey,
            new ShopSkuKey(SUPPLIER_3P_XU, "u"),
            new ShopSkuKey(SUPPLIER_3P_YU, "u"));

        // queue manager добавляет только по бизенес-ключу
        queuesManager
            .enqueueSskusToDatacamp(shopSkuKeys, MdmEnqueueReason.DEFAULT, MdmQueuePriorities.DATACAMP_OFFERS_PRIORITY);
        List<SskuToRefreshInfo> enqueued = toProcessRepository.findAll();
        Assertions.assertThat(enqueued.size()).isEqualTo(1);

        // when
        processSendToDatacampQueueService.processQueueItems();

        // then
        List<SskuToRefreshInfo> processedItems = processedRepository.findAll();
        Assertions.assertThat(processedItems.size()).isEqualTo(1);
        List<ShopSkuKey> processedKeys = processedItems.stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
        Assertions.assertThat(processedKeys).contains(bizKey);

        Assertions.assertThat(toProcessRepository.findAll()).isEmpty();
        Assertions.assertThat(failedRepository.findAll()).isEmpty();
    }

    private MdmSupplier buildSupplier(int id, MdmSupplierType type, Integer business) {
        MdmSupplier s = new MdmSupplier();
        s.setId(id);
        s.setType(type);
        if (business != null) {
            s.setBusinessId(business);
        }
        s.setBusinessEnabled(true);
        return s;
    }

    private List<String> generateData() {
        QualityDocument documentXU = generateDocument();
        QualityDocument documentYU = generateDocument();
        QualityDocument documentU = generateDocument();

        qualityDocumentRepository.insertOrUpdateAll(List.of(documentXU, documentYU, documentU));

        MasterData serviceXU = generateMasterData(SUPPLIER_3P_XU, "u", documentXU);
        MasterData serviceYU = generateMasterData(SUPPLIER_3P_YU, "u", documentYU);
        MasterData businessU = generateMasterData(SUPPLIER_BIZ_U, "u", documentU);
        generateMasterData(BeruIdMock.DEFAULT_PROD_FP_ID, "u");
        generateMasterData(BeruIdMock.DEFAULT_PROD_BIZ_ID, "u");
        sendToDatacampQRepository.enqueueAll(List.of(
            serviceXU.getShopSkuKey(),
            serviceYU.getShopSkuKey(),
            businessU.getShopSkuKey()
        ));

        markEox(SUPPLIER_3P_XU, "u");
        markEox(SUPPLIER_3P_YU, "u");
        markEox(SUPPLIER_BIZ_U, "u");
        markEox(BeruIdMock.DEFAULT_PROD_BIZ_ID, "u");
        markEox(BeruIdMock.DEFAULT_PROD_FP_ID, "u");

        return List.of(documentU.getRegistrationNumber(), documentXU.getRegistrationNumber(),
            documentYU.getRegistrationNumber());
    }

    private void markEox(int supplierId, String shopSku) {
        sskuExistenceRepository.markExistence(new ShopSkuKey(supplierId, shopSku), true);
    }

    private void unmarkEox(int supplierId, String shopSku) {
        sskuExistenceRepository.markExistence(new ShopSkuKey(supplierId, shopSku), false);
    }

    private MasterData generateMasterData(int supplierId, String shopSku, QualityDocument... documents) {
        MasterData md = TestDataUtils.generateMasterData(new ShopSkuKey(supplierId, shopSku), random, documents);

        List<DocumentOfferRelation> relations = DocumentOfferRelation.fromMasterData(md);
        qualityDocumentRepository.insertOrUpdateRelations(relations);
        masterDataRepository.insert(md);

        return masterDataRepository.findById(md.getShopSkuKey());
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(random);
    }

    private void assertUnprocessedQueueSize(int expectedSize) {
        List<SskuToRefreshInfo> result = sendToDatacampQRepository.findAll();
        Assertions.assertThat(result.stream()
            .filter(ssku -> !ssku.isProcessed())
            .collect(Collectors.toList()).size())
            .isEqualTo(expectedSize);
    }
}
