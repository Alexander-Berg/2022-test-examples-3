package ru.yandex.market.mboc.common.services.sku;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.RemovedMappingLogRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.offers.model.AntiMapping.SourceType.MODERATION_REJECT;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.RESET;

/**
 * @author apluhin
 * @created 11/16/21
 */
public class OfferClearMappingServiceTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;
    private OfferRepository spyOfferRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    private AntiMappingService antiMappingService;
    private OfferMappingActionService offerMappingActionService;
    @Autowired
    private RemovedMappingLogRepository removedMappingLogRepository;
    private RemovedMappingLogRepository spyRemovedMappingLogRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private OffersProcessingStatusService spyOffersProcessingStatusService;

    private CategoryCachingServiceMock categoryCachingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;

    private OfferClearMappingService offerClearMappingService;

    @Before
    public void setUp() {
        spyOfferRepository = Mockito.spy(offerRepository);
        spyRemovedMappingLogRepository = Mockito.spy(removedMappingLogRepository);
        categoryCachingService = new CategoryCachingServiceMock();
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        var supplierService = new SupplierService(supplierRepository);

        antiMappingService = new AntiMappingService(antiMappingRepository, transactionHelper);

        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService, null,
            offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService,
            offerBatchProcessor, supplierRepository);
        var offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            storageKeyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3,
            categoryInfoCache);
        spyOffersProcessingStatusService = Mockito.spy(offersProcessingStatusService);


        offerClearMappingService = new OfferClearMappingService(
            spyOfferRepository,
            offerMappingActionService,
            antiMappingService,
            spyOffersProcessingStatusService,
            spyRemovedMappingLogRepository,
            transactionHelper
        );


        List<Supplier> suppliers = IntStream.of(
            42, 43, 44, 45, 50, 99, 100
        )
            .mapToObj(id -> OfferTestUtils.simpleSupplier().setId(id))
            .collect(Collectors.toList());
        supplierRepository.insertOrUpdateAll(suppliers);

        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/sample-offers.json");

        offerRepository.insertOffers(offers);
    }

    @Test
    public void testDoNothingByEmptyOffers() {
        List<Long> testSku = List.of(1000L, 1001L);

        OffersMappingClearInfo offersMappingClearInfo = offerClearMappingService.removeMappingBySkuId(testSku);

        Assertions.assertThat(offersMappingClearInfo.getNotFoundSkuIds())
            .containsExactlyInAnyOrderElementsOf(testSku);
        Mockito.verify(spyRemovedMappingLogRepository, Mockito.times(0))
            .insertBatch(Mockito.anyCollection());
        Mockito.verify(spyOffersProcessingStatusService, Mockito.times(0))
            .processOffers(Mockito.anyCollection());
        Mockito.verify(spyOfferRepository, Mockito.times(0))
            .updateOffers(Mockito.anyCollection());
    }

    @Test
    public void testSuccessClear() {
        List<Long> testSku = List.of(15L);
        Offer testOffer = offerRepository.findOffers(new OffersFilter().setApprovedSkuIds(List.of(15L))).get(0);
        testOffer.setMarketSpecificContentHashSent(1L);
        testOffer.setMarketSpecificContentHash(1L);
        testOffer.updateApprovedSkuMapping(testOffer.getApprovedSkuMapping().copyWithSkuType(Offer.SkuType.PARTNER10));
        testOffer.setSuggestSkuMapping(testOffer.getApprovedSkuMapping().copyWithSkuType(Offer.SkuType.PARTNER10));
        offerRepository.updateOffers(testOffer);

        OffersMappingClearInfo offersMappingClearInfo = offerClearMappingService.removeMappingBySkuId(testSku);

        Assertions.assertThat(offersMappingClearInfo.getSuccessSkuIds()).containsExactly(15L);

        ArgumentCaptor<Collection> offerCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(spyOfferRepository, Mockito.times(1))
            .updateOffers(offerCaptor.capture());
        Offer clearedOffer = ((List<Offer>) offerCaptor.getValue()).get(0);
        Assertions.assertThat(clearedOffer.hasApprovedSkuMapping()).isFalse();
        Assertions.assertThat(clearedOffer.hasSuggestSkuMapping()).isFalse();
        Assertions.assertThat(clearedOffer.getCategoryId()).isEqualTo(1L);
        Assertions.assertThat(clearedOffer.getModelId()).isEqualTo(11L);
        Assertions.assertThat(clearedOffer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.OPEN);
        Assertions.assertThat(clearedOffer.getApprovedSkuMappingConfidence()).isEqualTo(RESET);
        Assertions.assertThat(clearedOffer.getMarketSpecificContentHashSent()).isNull();
        Assertions.assertThat(clearedOffer.getSmLastExecutionTs()).isNotNull();
        Assertions.assertThat(clearedOffer.getContentChangedTs()).isAfter(LocalDateTime.now().minusMinutes(1));


        List<OfferSkuRemoveInfo> rowLogs = removedMappingLogRepository.findAll();
        Assertions.assertThat(rowLogs.size()).isEqualTo(1);
        OfferSkuRemoveInfo log = rowLogs.get(0);
        assertRowLog(testOffer, log);

        AntiMapping antiMapping = antiMappingRepository.findAll().get(0);
        Assertions.assertThat(antiMapping.getOfferId()).isEqualTo(testOffer.getId());
        Assertions.assertThat(antiMapping.getNotSkuId()).isEqualTo(testSku.get(0));
        Assertions.assertThat(antiMapping.getUpdatedUser()).isEqualTo("import-anti-mappings-tool-MBOASSORT-1017");
        Assertions.assertThat(antiMapping.getSourceType()).isEqualTo(MODERATION_REJECT);
        Assertions.assertThat(antiMapping.getUploadRequestTs()).isNotNull();
    }


    @Test
    public void testSaveLogOnlyForProcessOffers() {
        List<Long> testSku = List.of(124L, 15L);
        Offer testOffer = offerRepository.findOffers(new OffersFilter().setApprovedSkuIds(List.of(15L))).get(0);

        OffersMappingClearInfo offersMappingClearInfo = offerClearMappingService.removeMappingBySkuId(testSku);

        Assertions.assertThat(offersMappingClearInfo.getSuccessSkuIds()).containsExactly(15L);
        Assertions.assertThat(offersMappingClearInfo.getNotFoundSkuIds()).containsExactly(124L);

        ArgumentCaptor<Collection> offerCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(spyOfferRepository, Mockito.times(1))
            .updateOffers(offerCaptor.capture());
        Assertions.assertThat(offerCaptor.getValue().size()).isEqualTo(1L);

        List<OfferSkuRemoveInfo> rowLogs = removedMappingLogRepository.findAll();
        Assertions.assertThat(rowLogs.size()).isEqualTo(1);
        OfferSkuRemoveInfo log = rowLogs.get(0);
        assertRowLog(testOffer, log);
    }

    private void assertRowLog(Offer oldOffer, OfferSkuRemoveInfo log) {
        Assertions.assertThat(log.getApprovedSkuId()).isEqualTo(15L);
        Assertions.assertThat(log.getBusinessId()).isEqualTo(oldOffer.getBusinessId());
        Assertions.assertThat(log.getShopSku()).isEqualTo(oldOffer.getShopSku());
        Assertions.assertThat(log.getMappingConfidence()).isEqualTo(oldOffer.getApprovedSkuMappingConfidence());
        Assertions.assertThat(log.getMappingSkuType()).isEqualTo(oldOffer.getApprovedSkuMapping().getSkuType());
    }

}
