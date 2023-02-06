package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.models.ImportAttachment;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.AutoClassificationResult;
import ru.yandex.market.mboc.common.honestmark.ClassificationResult;
import ru.yandex.market.mboc.common.honestmark.GcClassificationResult;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationCounterService;
import ru.yandex.market.mboc.common.honestmark.HonestMarkClassificationService;
import ru.yandex.market.mboc.common.offers.ClassifierOffer;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepositoryMock;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.mboc.common.services.managers.ManagersServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.INCORRECT_INFORMATION;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.NEED_CLASSIFICATION_INFORMATION;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.NO_CATEGORY;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.SAMPLE_LINE;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT_ITEMS1;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT_TYPE1;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT_TYPE2;

/**
 * @author yuramalinov
 * @created 31.10.18
 */
public class ClassificationOffersProcessingStrategyCommentsTest extends BaseDbTestClass {
    private static final int REAL_SUPPLIER_ID = 24;

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private TrackerServiceMock trackerService;
    private ClassificationOffersProcessingStrategy strategy;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;
    private ProcessingTicketInfoService processingTicketInfoService;
    private HonestMarkClassificationService honestMarkClassificationService;
    private HonestMarkClassificationCounterService classificationCounterService;
    private OfferMappingActionService offerMappingActionService;
    private SupplierService supplierService;

    private Supplier supplier;

    @Before
    public void setUp() {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);
        var legacyOfferMappingActionService = Mockito.mock(LegacyOfferMappingActionService.class);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var categoryCachingService = new CategoryCachingServiceMock().enableAuto();
        supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var config = new OffersToExcelFileConverterConfig(categoryCachingService);
        var categoryKnowledgeService = new CategoryKnowledgeServiceMock().enableAllCategories();

        var retrieveMappingSkuTypeService = Mockito.mock(RetrieveMappingSkuTypeService.class);
        Mockito.when(retrieveMappingSkuTypeService.retrieveMappingSkuType(anyCollection(), anySet(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor, needContentStatusService, supplierService, categoryKnowledgeService,
            retrieveMappingSkuTypeService, offerMappingActionService,
            categoryInfoRepository, antiMappingRepository, offerDestinationCalculator, storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);
        var classificationOffersProcessingService = new ClassificationOffersProcessingService(
            categoryCachingService,
            offerMappingActionService,
            offerDestinationCalculator
        );
        honestMarkClassificationService = Mockito.mock(HonestMarkClassificationService.class);
        classificationCounterService = Mockito.mock(HonestMarkClassificationCounterService.class);

        Mockito.when(honestMarkClassificationService.getClassificationResult(
            Mockito.any(Offer.class),
            Mockito.any(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(ClassificationResult.CONFIDENT, null, true));

        trackerService = new TrackerServiceMock();
        processingTicketInfoService = new ProcessingTicketInfoService(new ProcessingTicketInfoRepositoryMock());
        var helper = new ProcessingTicketHelper("unit-test", trackerService,
            new ManagersServiceMock(), categoryCachingService, processingTicketInfoService);
        var applySettingsService = Mockito.mock(ApplySettingsService.class);
        strategy = new ClassificationOffersProcessingStrategy(
            trackerService, offerRepository, supplierRepository, masterDataHelperService,
            config.classifierConverter(categoryCachingService), categoryKnowledgeService,
            classificationOffersProcessingService, helper,
            classificationCounterService,
            honestMarkClassificationService, needContentStatusService,
            applySettingsService, offersProcessingStatusService,
            false);
        supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);
    }

    @Test
    public void testConstraints() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        assertThat(excelFile.getTextConstraintsForHeader(CONTENT_COMMENT_TYPE1.getTitle())).isNotEmpty();
        assertThat(excelFile.getTextConstraintsForHeader(CONTENT_COMMENT_TYPE2.getTitle())).isNotEmpty();
    }

    @Test
    public void testCommentIsSet() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<ClassifierOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), NEED_CLASSIFICATION_INFORMATION.getDescription())
            .setValue(1, CONTENT_COMMENT_ITEMS1.getTitle(), "test")
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(supplier, offer, result.getOffers().get(0));

        assertThat(offer.getContentComments()).containsExactly(
            new ContentComment(NEED_CLASSIFICATION_INFORMATION, "test"));
    }

    @Test
    public void testCommentItemsNotAlwaysRequired() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<ClassifierOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), NEED_CLASSIFICATION_INFORMATION.getDescription())
            // Note: no items set
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(supplier, offer, result.getOffers().get(0));

        assertThat(offer.getContentComments()).containsExactly(
            new ContentComment(NEED_CLASSIFICATION_INFORMATION));
    }

    @Test
    public void testWrongCommentScope() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<ClassifierOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), INCORRECT_INFORMATION.getDescription())
            .setValue(1, CONTENT_COMMENT_ITEMS1.getTitle(), "test")
            .build());

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getErrors()).allSatisfy(
            error -> assertThat(error).contains("не может быть использован для тикета CLASSIFICATION"));
    }

    @Test
    public void testParseLegacyComment() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<ClassifierOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .addHeader(CONTENT_COMMENT.getTitle())
            .setValue(1, CONTENT_COMMENT.getTitle(), "legacy")
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(supplier, offer, result.getOffers().get(0));

        assertThat(offer.getContentComments()).isEmpty();
        assertThat(offer.getContentComment()).isEqualTo("legacy");
    }

    @Test
    public void testSpecialActionsDontAct() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<ClassifierOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), NEED_CLASSIFICATION_INFORMATION.getDescription())
            .setValue(1, CONTENT_COMMENT_ITEMS1.getTitle(), "test")
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(supplier, offer, result.getOffers().get(0));

        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
    }

    @Test
    public void testSpecialActionsSample() {
        Offer offer = OfferTestUtils.simpleOffer()
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<ClassifierOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), SAMPLE_LINE.getDescription())
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(supplier, offer, result.getOffers().get(0));

        assertThat(offer.getProcessingStatus()).isNotEqualTo(Offer.ProcessingStatus.CLASSIFIED);
        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
        //in real case would be sent to TRASH by OffersProcessingStatusService
    }

    @Test
    public void testSpecialActionsNoCategory() {
        Offer offer = OfferTestUtils.simpleOffer().updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<ClassifierOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), NO_CATEGORY.getDescription())
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(supplier, offer, result.getOffers().get(0));

        assertThat(offer.getAcceptanceStatus()).isEqualTo(Offer.AcceptanceStatus.OK);
    }

    @Test
    public void testTicketAndAttachmentName() {
        Supplier supplier = new Supplier(REAL_SUPPLIER_ID, "Test Supplier")
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setRealSupplierId("real");
        supplierRepository.insert(supplier);
        Offer offer = OfferTestUtils.simpleOffer(supplier);
        ru.yandex.startrek.client.model.Issue issue = strategy.createTicket(REAL_SUPPLIER_ID,
            Collections.singletonList(offer), "Test Author");

        MbocAssertions.assertThat(issue).hasSummaryEqualTo("1P Классификация офферов для msku 'Test Supplier'");
        Assertions.assertThat(trackerService.getTicketImportAttachments(issue))
            .extracting(ImportAttachment::getFileName)
            .containsExactly("1P Test Supplier.xlsx");
    }

    @Test
    public void testTicketNameContentLab() {
        Supplier supplier =
            new Supplier(REAL_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real");
        supplierRepository.insert(supplier);
        Offer offer = OfferTestUtils.simpleOffer(supplier).setThroughContentLab(true);
        ru.yandex.startrek.client.model.Issue issue = strategy.createTicket(REAL_SUPPLIER_ID,
            Collections.singletonList(offer), "Test Author");

        MbocAssertions.assertThat(issue).hasSummaryEqualTo("(Лаба) 1P Классификация офферов для msku 'Test Supplier'");
    }

    @Test
    public void testAddGoodContentTag() {
        Supplier supplier =
            new Supplier(REAL_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real")
                .setNewContentPipeline(true);
        supplierRepository.insert(supplier);
        Offer offer = OfferTestUtils.simpleOffer(supplier);
        ru.yandex.startrek.client.model.Issue issue = strategy.createTicket(REAL_SUPPLIER_ID,
            Collections.singletonList(offer), "Test Author");

        assertThat(issue.getTags()).contains("good_content");
    }

    @Test
    public void testCountTotalManualClassification() {
        Offer offer = OfferTestUtils.simpleOffer();

        strategy.countClassificationOffers(Collections.singletonList(offer), supplier);

        Mockito.verify(classificationCounterService, Mockito.times(0))
            .incrementTotalAutoClassification(Mockito.anyBoolean());
        Mockito.verify(classificationCounterService, Mockito.times(1))
            .incrementTotalManualClassification(Mockito.anyString());
    }

    @Test
    public void testNoIncrementClassification() {
        Offer offer = OfferTestUtils.simpleOffer();

        Mockito.when(honestMarkClassificationService.getClassificationResult(
            Mockito.any(Offer.class),
            Mockito.any(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(ClassificationResult.CONFIDENT, null, true));

        strategy.countClassificationOffers(Collections.singletonList(offer), supplier);

        Mockito.verify(classificationCounterService, Mockito.times(0))
            .incrementClassification(Mockito.any());
    }

    @Test
    public void testIncrementOnlyOnceClassification() {
        Offer offer = OfferTestUtils.simpleOffer();

        Mockito.when(honestMarkClassificationService.getClassificationResult(
            Mockito.any(Offer.class),
            Mockito.any(),
            Mockito.any(),
            Mockito.anySet(),
            Mockito.anySet())
        )
            .thenReturn(new AutoClassificationResult(
                ClassificationResult.UNCONFIDENT_ALLOW_GC,
                GcClassificationResult.BOTH_UNCONFIDENT,
                true)
            );

        strategy.countClassificationOffers(Collections.singletonList(offer), supplier);

        Mockito.verify(classificationCounterService, Mockito.times(1))
            .incrementClassification(GcClassificationResult.BOTH_UNCONFIDENT);
        Mockito.verify(classificationCounterService, Mockito.times(0))
            .incrementClassification(GcClassificationResult.CONFIDENT_FOR_CLASSIFICATION);
        Mockito.verify(classificationCounterService, Mockito.times(0))
            .incrementClassification(GcClassificationResult.CONFIDENT_FOR_DEPARTMENT_OTHER_WITHOUT_CONFLICT);
        Mockito.verify(classificationCounterService, Mockito.times(0))
            .incrementClassification(GcClassificationResult.CONFIDENT_FOR_DEPARTMENT_WITH_CONFLICT);
        Mockito.verify(classificationCounterService, Mockito.times(0))
            .incrementClassification(GcClassificationResult.CONFIDENT_FOR_DEPARTMENT_WITHOUT_CONFLICT);
    }
}
