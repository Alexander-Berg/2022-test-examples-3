package ru.yandex.market.mboc.tms.executors;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepository;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.managers.ManagersService;
import ru.yandex.market.mboc.common.services.managers.ManagersServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.BaseOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.ClassificationOffersProcessingService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.ProcessingTicketHelper;
import ru.yandex.market.mboc.common.services.offers.processing.ReclassificationProcessingStrategy;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;


public class SuppliersWhiteListTest extends BaseDbTestClass {

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MboUsersRepository mboUsersRepository;
    @Autowired
    private ProcessingTicketInfoRepository processingTicketInfoRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;

    private TrackerServiceMock trackerService;
    private NeedContentStatusService needContentStatusService;
    private OfferMappingActionService offerMappingActionService;
    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private ProcessingTicketInfoService processingTicketInfoService;
    private ReclassificationProcessingStrategy reclassificationProcessingStrategy;

    private SupplierService supplierService;

    @Before
    public void setUp() {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        categoryCachingServiceMock = new CategoryCachingServiceMock();
        var config = new OffersToExcelFileConverterConfig(categoryCachingServiceMock);

        trackerService = new TrackerServiceMock();
        supplierService = new SupplierService(supplierRepository);
        needContentStatusService = new NeedContentStatusService(categoryCachingServiceMock, supplierService,
            new BooksService(categoryCachingServiceMock, Collections.emptySet()));
        ManagersService managersService = new ManagersServiceMock();
        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(processingTicketInfoRepository);

        HonestMarkClassificationService honestMarkClassificationService =
            Mockito.mock(HonestMarkClassificationService.class);
        Mockito.when(honestMarkClassificationService.getClassificationResult(
            Mockito.any(Offer.class),
            Mockito.anyLong(),
            Mockito.any(),
            Mockito.any(),
            Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(ClassificationResult.CONFIDENT, null, true));

        var helper = new ProcessingTicketHelper(Environments.TESTING, trackerService,
            managersService, categoryCachingServiceMock, processingTicketInfoService);
        var clsConverter = config.classifierConverter(categoryCachingServiceMock);
        var categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        Mockito.when(retrieveMappingSkuTypeService.retrieveMappingSkuType(anyCollection(), anySet(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor, needContentStatusService, supplierService, categoryKnowledgeService,
            retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);
        var classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingServiceMock,
            offerMappingActionService,
            offerDestinationCalculator
        );
        reclassificationProcessingStrategy = new ReclassificationProcessingStrategy(
            trackerService, offerRepository, supplierRepository, masterDataHelperService,
            clsConverter, categoryKnowledgeService, classificationOffersProcessingService, helper,
            Mockito.mock(HonestMarkClassificationCounterService.class),
            honestMarkClassificationService, needContentStatusService,
            Mockito.mock(ApplySettingsService.class), offersProcessingStatusService,
            false);

        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void createTicketWithSupplierFromWhiteList() {
        BaseOffersProcessingStrategy.setSuppliersWhiteList(Set.of(OfferTestUtils.TEST_SUPPLIER_ID));
        Offer offer1 = createOffer(1L, OfferTestUtils.TEST_SUPPLIER_ID);
        offerRepository.insertOffers(Arrays.asList(offer1));

        OffersProcessingStrategy.OptionalTicket ticket =
            reclassificationProcessingStrategy.createTrackerTicket(new OffersFilter());

        assertThat(ticket.getTicket()).isNotNull();
    }

    @Test
    public void dontCreateTicketWithSupplierNotFromWhiteList() {
        supplierRepository.insertBatch(
            OfferTestUtils.simpleSupplier().setId(1),
            OfferTestUtils.simpleSupplier().setId(2)
        );
        BaseOffersProcessingStrategy.setSuppliersWhiteList(Set.of(OfferTestUtils.TEST_SUPPLIER_ID));
        Offer offer1 = createOffer(1L, 1);
        Offer offer2 = createOffer(2L, 2);
        offerRepository.insertOffers(Arrays.asList(offer1, offer2));

        OffersProcessingStrategy.OptionalTicket ticket =
            reclassificationProcessingStrategy.createTrackerTicket(new OffersFilter());

        assertThat(ticket.getTicket()).isNull();
    }

    @Test
    public void createTicketWhenWhiteListIsNullTest() {
        BaseOffersProcessingStrategy.setSuppliersWhiteList(null);

        Offer offer1 = createOffer(1L, OfferTestUtils.TEST_SUPPLIER_ID);
        offerRepository.insertOffers(Arrays.asList(offer1));

        OffersProcessingStrategy.OptionalTicket ticket =
            reclassificationProcessingStrategy.createTrackerTicket(new OffersFilter());

        assertThat(ticket.getTicket()).isNotNull();
    }

    private Offer createOffer(long id, int supplierId) {
        return OfferTestUtils.simpleOffer(id)
            .setTitle("test")
            .setBusinessId(supplierId)
            .setShopSku("Sku-" + id)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECLASSIFICATION)
            .setAcceptanceStatusInternal(Offer.AcceptanceStatus.OK)
            .setOfferDestination(Offer.MappingDestination.BLUE)
            .setBindingKind(Offer.BindingKind.SUGGESTED);
    }
}
