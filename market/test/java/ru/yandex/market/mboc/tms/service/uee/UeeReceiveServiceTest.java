package ru.yandex.market.mboc.tms.service.uee;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.micrometer.core.instrument.Metrics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.ir.uee.model.UserRun;
import ru.yandex.market.ir.uee.model.UserRunData;
import ru.yandex.market.ir.uee.model.UserRunReq;
import ru.yandex.market.ir.uee.model.UserRunState;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferToSmStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.SmMatchTarget;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.OfferToSm;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.SmBatch;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.queue.LongKeyQueueItem;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.queue.OfferToSmQueueRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.modelstorage.models.SimpleModel;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.uee.repository.OfferToSmRepository;
import ru.yandex.market.mboc.common.uee.repository.SmBatchRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createOffer;

public class UeeReceiveServiceTest extends BaseDbTestClass {
    private static final String BASE_PATH = "//tmp/uee";
    private static final String DEFAULT_YT_CLUSTER = "hahn";
    private static final long MSKU_ID = 100501L;
    private static final long MSKU_ID2 = 100502L;

    private UeeReceiveService ueeReceiveService;
    private TestYt testYt;
    private UeeServiceMock ueeService;

    @Resource
    SmBatchRepository smBatchRepository;

    @Resource
    OfferRepository offerRepository;

    @Resource
    OfferToSmRepository offerToSmRepository;

    @Resource
    SupplierRepository supplierRepository;

    @Resource
    OfferToSmQueueRepository offerToSmQueueRepository;

    private ModelStorageCachingServiceMock modelStorageCachingService;
    private Supplier supplier;
    private OffersProcessingStatusService offersProcessingStatusService;

    @Before
    public void setUp() {
        testYt = new TestYt();
        StorageKeyValueServiceMock storageKeyValueService = new StorageKeyValueServiceMock();
        UeeYtService ueeYtService = new UeeYtServiceImpl(UnstableInit.simple(testYt), BASE_PATH, storageKeyValueService);
        ueeService = new UeeServiceMock();
        modelStorageCachingService = new ModelStorageCachingServiceMock();
        modelStorageCachingService.addModel(new Model()
            .setId(MSKU_ID)
            .setCategoryId(13L)
            .setVendorId(1000)
            .setModelType(SimpleModel.ModelType.SKU)
        );

        supplier = new Supplier(1, "Test supplier", null, null);
        supplierRepository.insert(supplier);

        var legacyActionService = new LegacyOfferMappingActionService(null, null, offerDestinationCalculator,
            this.storageKeyValueService);
        var mappingActionService = new OfferMappingActionService(legacyActionService);

        offersProcessingStatusService = Mockito.mock(OffersProcessingStatusService.class);

        StorageKeyValueService keyValueServiceMock = new StorageKeyValueServiceMock();
        keyValueServiceMock.putValue(UeeReceiveService.SM_CONFIDENCE_THRESHOLD_PREFIX + "default", -2.0);
        ueeReceiveService = new UeeReceiveService(Metrics.globalRegistry,
            smBatchRepository,
            ueeService,
            ueeYtService,
            offerRepository,
            offerToSmRepository,
            modelStorageCachingService,
            offersProcessingStatusService,
            mappingActionService,
            transactionTemplate,
            keyValueServiceMock,
            1
        );
    }

    @Test
    public void whenFailedResultUEEThenWriteFails() {
        Offer offer = createOffer(supplier);
        offerRepository.insertOffer(offer);

        UserRunReq userRunReq = new UserRunReq();
        userRunReq.putFieldMappingsItem("category_id", "category_id");
        UserRun userRun = ueeService.createUserRun(userRunReq);
        userRun.setState(UserRunState.ERROR);
        SmBatch smBatch = createSmBatch(userRun.getId());
        smBatch = smBatchRepository.save(smBatch);
        OfferToSm offerToSm = new OfferToSm();
        offerToSm.setStatus(OfferToSmStatus.SENT);
        offerToSm.setSmBatchId(smBatch.getId());
        offerToSm.setOfferId(offer.getId());
        offerToSm = offerToSmRepository.save(offerToSm);

        ueeReceiveService.processSmBatch(smBatch);

        smBatch = smBatchRepository.findByIdOrThrow(smBatch.getId());
        assertThat(smBatch.getEndTs()).isNotNull();

        offerToSm = offerToSmRepository.findByIdOrThrow(offerToSm.getId());
        assertThat(offerToSm.getStatus()).isEqualTo(OfferToSmStatus.FAILED);
        assertThat(offerToSm.getMarketSkuId()).isNull();

        assertThat(offerToSmQueueRepository.findAll())
            .extracting(LongKeyQueueItem::getId)
            .containsExactly(offerToSm.getId());
    }

    @Test
    public void whenEmptyResultUEEThenWriteEmptyResult() {
        Offer offer = createOffer(supplier);
        offerRepository.insertOffer(offer);

        UserRunReq userRunReq = new UserRunReq();
        UserRunData userRunData = new UserRunData();
        userRunData.setCluster(DEFAULT_YT_CLUSTER);
        String ytOutPath = BASE_PATH + "/output";
        userRunData.setYtPath(ytOutPath);
        UserRun userRun = ueeService.createUserRun(userRunReq);
        userRun.setState(UserRunState.COMPLETED);
        userRun.setResult(userRunData);
        ueeService.putUserRun(userRun);

        testYt.tables().write(YPath.simple(ytOutPath), YTableEntryTypes.YSON, List.of(
            YTree.mapBuilder()
                .key(UeeYtService.INPUT_OFFER_ID).value(offer.getId())
                .key(UeeYtService.OUTPUT_SM_RESULT).value(List.of())
                .buildMap()
            )
        );


        SmBatch smBatch = createSmBatch(userRun.getId());
        smBatch = smBatchRepository.save(smBatch);
        OfferToSm offerToSm = new OfferToSm();
        offerToSm.setStatus(OfferToSmStatus.SENT);
        offerToSm.setSmBatchId(smBatch.getId());
        offerToSm.setOfferId(offer.getId());
        offerToSm = offerToSmRepository.save(offerToSm);

        ueeReceiveService.processSmBatch(smBatch);

        smBatch = smBatchRepository.findByIdOrThrow(smBatch.getId());
        assertThat(smBatch.getEndTs()).isNotNull();
        assertThat(smBatch.getYtResponsePath()).isEqualTo(ytOutPath);

        offerToSm = offerToSmRepository.findByIdOrThrow(offerToSm.getId());
        assertThat(offerToSm.getStatus()).isEqualTo(OfferToSmStatus.SMART_MATCHER_EMPTY_RESULT);
        assertThat(offerToSm.getMarketSkuId()).isNull();

        assertThat(offerToSmQueueRepository.findAll())
            .extracting(LongKeyQueueItem::getId)
            .containsExactly(offerToSm.getId());
    }

    @Test
    public void whenOkResultUEEThenWriteApprovedOfferToSm() {
        final float confidence = 100.0f;
        Offer offer = createOffer(supplier);
        offerRepository.insertOffer(offer);

        UserRunReq userRunReq = new UserRunReq();
        String ytOutPath = BASE_PATH + "/output";
        UserRun userRun = ueeService.createUserRun(userRunReq);

        UserRunData userRunData = new UserRunData();
        userRunData.setCluster(DEFAULT_YT_CLUSTER);
        userRunData.setYtPath(ytOutPath);
        userRun.setResult(userRunData);
        userRun.setState(UserRunState.COMPLETED);

        ueeService.putUserRun(userRun);

        writeOfferDataToYt(offer, ytOutPath, confidence);

        SmBatch smBatch = createSmBatch(userRun.getId());
        smBatch = smBatchRepository.save(smBatch);
        OfferToSm offerToSm = new OfferToSm();
        offerToSm.setStatus(OfferToSmStatus.SENT);
        offerToSm.setSmBatchId(smBatch.getId());
        offerToSm.setOfferId(offer.getId());
        offerToSm = offerToSmRepository.save(offerToSm);

        ueeReceiveService.processSmBatch(smBatch);

        smBatch = smBatchRepository.findByIdOrThrow(smBatch.getId());
        assertThat(smBatch.getEndTs()).isNotNull();
        assertThat(smBatch.getYtResponsePath()).isEqualTo(ytOutPath);

        offerToSm = offerToSmRepository.findByIdOrThrow(offerToSm.getId());
        assertThat(offerToSm)
            .extracting(OfferToSm::getStatus, OfferToSm::getMarketSkuId, OfferToSm::getTarget, OfferToSm::getConfidence)
            .containsExactly(OfferToSmStatus.APPROVED, MSKU_ID, SmMatchTarget.SKU_LIKE, confidence);

        assertThat(offerToSmQueueRepository.findAll())
            .extracting(LongKeyQueueItem::getId)
            .containsExactly(offerToSm.getId());
    }

    @Test
    public void whenThresholdIsSmallUEEThenWriteThresholdReject() {
        final float confidence = -100.0f;
        Offer offer = createOffer(supplier);
        offerRepository.insertOffer(offer);

        UserRunReq userRunReq = new UserRunReq();
        UserRunData userRunData = new UserRunData();
        userRunData.setCluster(DEFAULT_YT_CLUSTER);
        String ytOutPath = BASE_PATH + "/output";
        userRunData.setYtPath(ytOutPath);
        UserRun userRun = ueeService.createUserRun(userRunReq);
        userRun.setState(UserRunState.COMPLETED);
        userRun.setResult(userRunData);
        ueeService.putUserRun(userRun);

        writeOfferDataToYt(offer, ytOutPath, confidence);

        SmBatch smBatch = createSmBatch(userRun.getId());
        smBatch = smBatchRepository.save(smBatch);
        OfferToSm offerToSm = new OfferToSm();
        offerToSm.setStatus(OfferToSmStatus.SENT);
        offerToSm.setSmBatchId(smBatch.getId());
        offerToSm.setOfferId(offer.getId());
        offerToSm = offerToSmRepository.save(offerToSm);

        ueeReceiveService.processSmBatch(smBatch);

        smBatch = smBatchRepository.findByIdOrThrow(smBatch.getId());
        assertThat(smBatch.getEndTs()).isNotNull();
        assertThat(smBatch.getYtResponsePath()).isEqualTo(ytOutPath);

        offerToSm = offerToSmRepository.findByIdOrThrow(offerToSm.getId());
        assertThat(offerToSm)
            .extracting(OfferToSm::getStatus, OfferToSm::getMarketSkuId, OfferToSm::getTarget, OfferToSm::getConfidence)
            .containsExactly(OfferToSmStatus.THRESHOLD_REJECTED, MSKU_ID, SmMatchTarget.SKU_LIKE, confidence);

        assertThat(offerToSmQueueRepository.findAll())
            .extracting(LongKeyQueueItem::getId)
            .containsExactly(offerToSm.getId());
    }

    @Test
    public void whenChangedStatusFromSentToAnotherThenWriteInQueue() {
        Offer offer = createOffer(supplier);
        offerRepository.insertOffer(offer);

        SmBatch smBatch = createSmBatch(0);
        smBatch = smBatchRepository.save(smBatch);
        OfferToSm offerToSm = new OfferToSm();
        offerToSm.setStatus(OfferToSmStatus.SENT);
        offerToSm.setSmBatchId(smBatch.getId());
        offerToSm.setOfferId(offer.getId());
        offerToSm = offerToSmRepository.save(offerToSm);

        List<LongKeyQueueItem> keyQueueItems = offerToSmQueueRepository.findAll();

        assertThat(keyQueueItems).isEmpty();

        offerToSmQueueRepository.deleteBatch(keyQueueItems);

        offerToSm.setStatus(OfferToSmStatus.APPROVED);
        offerToSm = offerToSmRepository.save(offerToSm);

        keyQueueItems = offerToSmQueueRepository.findAll();

        assertThat(keyQueueItems)
            .extracting(LongKeyQueueItem::getId)
            .containsExactly(offerToSm.getId());

        offerToSmQueueRepository.deleteBatch(keyQueueItems);

        offerToSm.setStatus(OfferToSmStatus.UC_CONFLICT);
        offerToSm = offerToSmRepository.save(offerToSm);

        keyQueueItems = offerToSmQueueRepository.findAll();

        assertThat(keyQueueItems).isEmpty();

        offerToSmQueueRepository.deleteBatch(keyQueueItems);

        offerToSm.setStatus(OfferToSmStatus.THRESHOLD_REJECTED);
        offerToSmRepository.save(offerToSm);

        keyQueueItems = offerToSmQueueRepository.findAll();

        assertThat(keyQueueItems).isEmpty();
    }


    @Test
    public void whenReceiveOfferToSmsThenProcessIt() {
        Set<Long> processedOfferIds = new HashSet<>();
        mockProcessing(processedOfferIds);

        Offer offerApproved = createOffer(supplier);
        offerApproved.setShopSku(offerApproved.getShopSku() + "APPROVED");
        offerApproved.setProcessingStatusInternal(Offer.ProcessingStatus.IN_SMARTMATCHER);
        offerApproved.setLastPrimaryProcessingStatus(Offer.ProcessingStatus.OPEN);

        Offer offerRejected = createOffer(supplier);
        offerRejected.setShopSku(offerRejected.getShopSku() + "REJECTED");
        offerRejected.setProcessingStatusInternal(Offer.ProcessingStatus.IN_SMARTMATCHER);
        offerRejected.setLastPrimaryProcessingStatus(Offer.ProcessingStatus.OPEN);

        Offer offerUcConflict = createOffer(supplier);
        offerUcConflict.setShopSku(offerRejected.getShopSku() + "CONFLICT");
        offerUcConflict.setProcessingStatusInternal(Offer.ProcessingStatus.IN_SMARTMATCHER);
        offerUcConflict.setSuggestSkuMapping(
            Offer.Mapping.updateIfChanged(
                offerUcConflict.getSuggestSkuMapping(), MSKU_ID, Offer.SkuType.MARKET
            )
        );
        offerUcConflict.setSuggestMappingSource(Offer.SuggestMappingSource.ULTRA_CONTROLLER);
        offerUcConflict.setLastPrimaryProcessingStatus(Offer.ProcessingStatus.OPEN);

        Offer offerCategoryChanged = createOffer(supplier);
        offerCategoryChanged.setShopSku(offerRejected.getShopSku() + "CHANGED");
        offerCategoryChanged.setProcessingStatusInternal(Offer.ProcessingStatus.IN_SMARTMATCHER);
        offerCategoryChanged.setLastPrimaryProcessingStatus(Offer.ProcessingStatus.OPEN);
        offerCategoryChanged.setCategoryIdForTests(offerCategoryChanged.getCategoryId() + 1L,
            Offer.BindingKind.APPROVED);

        offerRepository.insertOffers(offerApproved, offerRejected, offerUcConflict, offerCategoryChanged);

        SmBatch smBatch = createSmBatch(0);
        smBatch = smBatchRepository.save(smBatch);
        OfferToSm offerToSmApproved = createOfferToSm(
            smBatch, OfferToSmStatus.APPROVED, 100f, MSKU_ID, offerApproved.getId()
        );

        OfferToSm offerToSmRejected = createOfferToSm(
            smBatch, OfferToSmStatus.THRESHOLD_REJECTED, -100f, MSKU_ID2, offerRejected.getId()
        );

        OfferToSm offerToSmUcConflict = createOfferToSm(
            smBatch, OfferToSmStatus.APPROVED, 100f, MSKU_ID2, offerUcConflict.getId()
        );

        OfferToSm offerToSmCategoryChanged = createOfferToSm(
            smBatch, OfferToSmStatus.APPROVED, 100f, MSKU_ID2, offerCategoryChanged.getId()
        );


        ueeReceiveService.processOfferToSm(List.of(
            offerToSmApproved.getId(),
            offerToSmRejected.getId(),
            offerToSmUcConflict.getId(),
            offerToSmCategoryChanged.getId()
        ));

        offerToSmApproved = offerToSmRepository.findByIdOrThrow(offerToSmApproved.getId());
        assertThat(offerToSmApproved.getStatus()).isEqualTo(OfferToSmStatus.APPROVED);

        offerToSmRejected = offerToSmRepository.findByIdOrThrow(offerToSmRejected.getId());
        assertThat(offerToSmRejected.getStatus()).isEqualTo(OfferToSmStatus.THRESHOLD_REJECTED);

        assertThat(processedOfferIds)
            .containsExactlyInAnyOrder(
                offerApproved.getId(), offerRejected.getId(), offerUcConflict.getId(), offerCategoryChanged.getId()
            );

        offerApproved = offerRepository.getOfferById(offerApproved.getId());
        assertThat(offerApproved)
            .extracting(Offer::getProcessingStatus, Offer::getSmSkuId, Offer::getSuggestMappingSource)
            .containsExactly(Offer.ProcessingStatus.OPEN, MSKU_ID, Offer.SuggestMappingSource.SMART_MATCHER);

        assertThat(offerApproved.getSuggestSkuMapping())
            .extracting(Offer.Mapping::getMappingId, Offer.Mapping::getSkuType)
            .containsExactly(MSKU_ID, Offer.SkuType.MARKET);

        offerRejected = offerRepository.getOfferById(offerRejected.getId());
        assertThat(offerRejected)
            .extracting(Offer::getProcessingStatus, Offer::getSmSkuId, Offer::getSuggestMappingSource)
            .containsExactly(Offer.ProcessingStatus.OPEN, null, null);

        assertThat(offerRejected.getSuggestSkuMapping()).isNull();

        offerUcConflict = offerRepository.getOfferById(offerUcConflict.getId());
        assertThat(offerUcConflict)
            .extracting(Offer::getProcessingStatus, Offer::getSmSkuId, Offer::getSuggestMappingSource)
            .containsExactly(Offer.ProcessingStatus.OPEN, null, Offer.SuggestMappingSource.ULTRA_CONTROLLER);

        assertThat(offerUcConflict.getSuggestSkuMapping())
            .extracting(Offer.Mapping::getMappingId, Offer.Mapping::getSkuType)
            .containsExactly(MSKU_ID, Offer.SkuType.MARKET);

        offerCategoryChanged = offerRepository.getOfferById(offerCategoryChanged.getId());
        assertThat(offerCategoryChanged)
            .extracting(Offer::getProcessingStatus, Offer::getSmSkuId, Offer::getSuggestMappingSource)
            .containsExactly(Offer.ProcessingStatus.OPEN, null, null);

        assertThat(offerCategoryChanged.getSuggestSkuMapping()).isNull();
    }

    private OfferToSm createOfferToSm(SmBatch smBatch, OfferToSmStatus approved, float v,
                                      long mskuId, long offerId) {
        OfferToSm offerToSmApproved = new OfferToSm();
        offerToSmApproved.setStatus(approved);
        offerToSmApproved.setTarget(SmMatchTarget.SKU_LIKE);
        offerToSmApproved.setConfidence(v);
        offerToSmApproved.setMarketSkuId(mskuId);
        offerToSmApproved.setSmBatchId(smBatch.getId());
        offerToSmApproved.setOfferId(offerId);
        offerToSmApproved = offerToSmRepository.save(offerToSmApproved);
        return offerToSmApproved;
    }

    private void writeOfferDataToYt(Offer offer, String ytOutPath, double v) {
        testYt.tables().write(YPath.simple(ytOutPath), YTableEntryTypes.YSON, List.of(
            YTree.mapBuilder()
                .key(UeeYtService.INPUT_OFFER_ID).value(offer.getId())
                .key(UeeYtService.OUTPUT_SM_RESULT).value(
                List.of(YTree
                    .mapBuilder()
                    .key(SMResult.SM_DOCUMENT_ID_NODE).value(MSKU_ID)
                    .key(SMResult.SM_CONFIDENCE_NODE).value(v)
                    .key(SMResult.SM_TYPE_NODE).value("SKU_LIKE")
                    .buildMap()
                )
            )
                .buildMap()
            )
        );
    }

    private SmBatch createSmBatch(Integer userRunId) {
        SmBatch smBatch = new SmBatch();
        smBatch.setStartTs(Instant.now());
        smBatch.setUserRunId(userRunId);
        smBatch.setYtCluster(DEFAULT_YT_CLUSTER);
        smBatch.setYtRequestPath(BASE_PATH + "/input");
        smBatch.setSmVersion("default");
        return smBatch;
    }

    private void mockProcessing(Set<Long> processedOfferIds) {
        Mockito.when(offersProcessingStatusService.processOffers(Mockito.anyCollection()))
            .then(invocation -> {
                    Collection<Offer> offerIds = invocation.getArgument(0);
                    processedOfferIds.addAll(offerIds.stream().map(Offer::getId).collect(Collectors.toSet()));
                    return null;
                }
            );
    }
}
