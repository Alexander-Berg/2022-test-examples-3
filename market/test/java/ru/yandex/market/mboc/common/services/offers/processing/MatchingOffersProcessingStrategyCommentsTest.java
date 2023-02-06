package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.models.ImportAttachment;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.MatchingOffer;
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
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.startrek.client.model.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.DEPARTMENT_FROZEN;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.INCORRECT_INFORMATION;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.LEGAL_CONFLICT;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.NEED_CLASSIFICATION_INFORMATION;
import static ru.yandex.market.mboc.common.offers.model.ContentCommentType.NO_KNOWLEDGE;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT_ITEMS1;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT_TYPE1;
import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.CONTENT_COMMENT_TYPE2;

/**
 * @author yuramalinov
 * @created 31.10.18
 */
public class MatchingOffersProcessingStrategyCommentsTest extends BaseDbTestClass {
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private AntiMappingRepository antiMappingRepository;

    private SupplierService supplierService;

    private TrackerServiceMock trackerService;
    private ManagersServiceMock managersService;
    private MatchingOffersProcessingStrategy strategy;
    private ProcessingTicketInfoService processingTicketInfoService;
    private OffersProcessingStatusService offersProcessingStatusService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;

    @Before
    public void setUp() {
        var categoryCachingService = new CategoryCachingServiceMock().enableAuto();
        var config = new OffersToExcelFileConverterConfig(categoryCachingService);
        var filter = new NeedSizeMeasureFilter(null, offerRepository);
        trackerService = new TrackerServiceMock();
        managersService = new ManagersServiceMock();
        processingTicketInfoService = new ProcessingTicketInfoService(new ProcessingTicketInfoRepositoryMock());
        supplierService = new SupplierService(supplierRepository);
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var helper = new ProcessingTicketHelper(Environments.DEVELOPMENT, trackerService,
            managersService, categoryCachingService, processingTicketInfoService);
        var applySettingsService = Mockito.mock(ApplySettingsService.class);

        var supplierService = new SupplierService(supplierRepository);
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        var modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        modelStorageCachingServiceMock.setAutoModel(new Model());
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingServiceMock,
            offerBatchProcessor, supplierRepository);
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        var offerMappingActionServiceV2 = new OfferMappingActionService(legacyOfferMappingActionService);
        offersProcessingStatusService = new OffersProcessingStatusService(offerBatchProcessor,
            needContentStatusService, supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService,
            offerMappingActionService, categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            storageKeyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3,
            categoryInfoCache);

        strategy = new MatchingOffersProcessingStrategy(trackerService,
            offerRepository, supplierRepository, null,
            managersService, offerMappingActionServiceV2,
            config.matchingConverter(new ModelStorageCachingServiceMock(), offerRepository), filter, helper,
            applySettingsService, offersProcessingStatusService);
    }


    @Test
    public void testCloseIfOffersGoneToContentLab() {

        Supplier supplier =
            new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real");
        supplierRepository.insert(supplier);

        managersService.addUser(1, new MboUser(1, "Test Man 1", "test1@y.ru"));
        managersService.addUser(2, new MboUser(2, "Test Man 2", "test2@y.ru"));

        Offer offer1 = OfferTestUtils.simpleOffer(supplier)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer offer2 = OfferTestUtils.simpleOffer(supplier)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED).setShopSku("shop-sku-2");
        Offer offer3 = OfferTestUtils.simpleOffer(supplier)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED).setShopSku("shop-sku-3")
            .setThroughContentLab(true)
            .setContentLabState(Offer.ContentLabState.CL_CONTENT);

        offerRepository.insertOffers(Arrays.asList(offer1, offer2, offer3));

        Issue issue = strategy.createTicket(OfferTestUtils.TEST_SUPPLIER_ID,
            Arrays.asList(offer1, offer2, offer3), "Test Author");

        MbocAssertions.assertThat(issue).hasSummaryEqualTo("(Лаба) 1P Заведение msku 'Test Supplier'");

        // do it manually in
        Stream.of(offer1, offer2, offer3)
            .map(Offer::getId)
            .map(id -> offerRepository.getOfferById(id))
            .peek(o -> {
                o.setTrackerTicket(issue);
                o.updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS);
            })
            .forEach(
                o -> offerRepository.updateOffer(o)
            );

        assertThat(strategy.findNotProcessedOffers(issue))
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(offer1.getId(), offer2.getId(), offer3.getId());


        offer3 = offerRepository.getOfferById(offer3.getId());
        offer3.setContentLabState(Offer.ContentLabState.CL_READY);
        offerRepository.updateOffer(offer3);

        assertThat(strategy.findNotProcessedOffers(issue))
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(offer1.getId(), offer2.getId());

        offer3 = offerRepository.getOfferById(offer3.getId());
        offer3.setContentLabState(Offer.ContentLabState.CL_PROCESSED);
        offerRepository.updateOffer(offer3);

        assertThat(strategy.findNotProcessedOffers(issue))
            .extracting(Offer::getId)
            .containsExactlyInAnyOrder(offer1.getId(), offer2.getId());
    }

    @Test
    public void testConstraints() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        assertThat(excelFile.getTextConstraintsForHeader(CONTENT_COMMENT_TYPE1.getTitle())).isNotEmpty();
        assertThat(excelFile.getTextConstraintsForHeader(CONTENT_COMMENT_TYPE1.getTitle()))
            .contains("Укажите вендора", "*Расхождение информации в полях");
        assertThat(excelFile.getTextConstraintsForHeader(CONTENT_COMMENT_TYPE2.getTitle())).isNotEmpty();
    }

    @Test
    public void testCommentIsSet() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), INCORRECT_INFORMATION.getDescription())
            .setValue(1, CONTENT_COMMENT_ITEMS1.getTitle(), "test")
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(offer, result.getOffers().get(0));

        assertThat(offer.getContentComments()).containsExactly(
            new ContentComment(INCORRECT_INFORMATION, "test"));
    }

    @Test
    public void testWrongCommentScope() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), NEED_CLASSIFICATION_INFORMATION.getDescription())
            .setValue(1, CONTENT_COMMENT_ITEMS1.getTitle(), "test")
            .build());

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getErrors()).allSatisfy(
            error -> assertThat(error).contains("не может быть использован для тикета MATCHING"));
    }

    @Test
    public void testParseRequiredMark() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), "*" + INCORRECT_INFORMATION.getDescription())
            .setValue(1, CONTENT_COMMENT_ITEMS1.getTitle(), "test")
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(offer, result.getOffers().get(0));

        assertThat(offer.getContentComments()).containsExactly(
            new ContentComment(INCORRECT_INFORMATION, "test"));
    }

    @Test
    public void testParseLegacyComment() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .addHeader(CONTENT_COMMENT.getTitle())
            .setValue(1, CONTENT_COMMENT.getTitle(), "legacy")
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(offer, result.getOffers().get(0));

        assertThat(offer.getContentComments()).isEmpty();
        assertThat(offer.getContentComment()).isEqualTo("legacy");
    }

    @Test
    public void testSpecialActionsDontAct() {
        var categoryId = 1L;
        Offer offer = OfferTestUtils.simpleOkOffer().setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED);
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), INCORRECT_INFORMATION.getDescription())
            .setValue(1, CONTENT_COMMENT_ITEMS1.getTitle(), "test")
            .build());
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        categoryKnowledgeService.addCategory(categoryId);

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(offer, result.getOffers().get(0));
        offersProcessingStatusService.processOffers(List.of(offer));
        assertThat(offer.getProcessingStatus()).isEqualTo(Offer.ProcessingStatus.NEED_INFO);
    }

    @Test
    public void testSpecialActionsLegalConflict() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), LEGAL_CONFLICT.getDescription())
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(offer, result.getOffers().get(0));
    }

    @Test
    public void testSpecialActionsDepartmentFrozen() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), DEPARTMENT_FROZEN.getDescription())
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(offer, result.getOffers().get(0));
    }

    @Test
    public void testSpecialActionsNoKnowledge() {
        Offer offer = OfferTestUtils.simpleOffer();
        ExcelFile excelFile = strategy.createExcelFile(Collections.singletonList(offer));
        OffersParseResult<MatchingOffer> result = strategy.parseExcelFile(excelFile.toBuilder()
            .setValue(1, CONTENT_COMMENT_TYPE1.getTitle(), NO_KNOWLEDGE.getDescription())
            .build());

        result.throwIfFailed();

        assertThat(result.getOffers()).hasSize(1);
        strategy.mergeOffer(offer, result.getOffers().get(0));
    }

    @Test
    public void testTicketAndAttachmentName() {
        Supplier supplier =
            new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real");
        supplierRepository.insert(supplier);
        managersService.addUser(1, new MboUser(1, "Test Man 1", "test1@y.ru"));
        managersService.addUser(2, new MboUser(2, "Test Man 2", "test2@y.ru"));

        Offer offer1 = OfferTestUtils.simpleOffer(supplier).setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer offer2 = OfferTestUtils.simpleOffer(supplier).setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED);
        Offer offer3 = OfferTestUtils.simpleOffer(supplier).setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED);
        Issue issue = strategy.createTicket(OfferTestUtils.TEST_SUPPLIER_ID,
            Arrays.asList(offer1, offer2, offer3), "Test Author");

        MbocAssertions.assertThat(issue).hasSummaryEqualTo("1P Заведение msku 'Test Supplier'");
        Assertions.assertThat(trackerService.getTicketImportAttachments(issue))
            .extracting(ImportAttachment::getFileName)
            .containsExactly("Test Man 1 (1) 1P Test Supplier.xlsx", "Test Man 2 (2) 1P Test Supplier.xlsx");
    }

    @Test
    public void testTickerHaveContentLabPrefix() {
        Supplier supplier =
            new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real");
        supplierRepository.insert(supplier);
        managersService.addUser(1, new MboUser(1, "Test Man 1", "test1@y.ru"));
        managersService.addUser(2, new MboUser(2, "Test Man 2", "test2@y.ru"));

        Offer offer1 = OfferTestUtils.simpleOffer(supplier)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer offer2 = OfferTestUtils.simpleOffer(supplier)
            .setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED).setThroughContentLab(true);
        Issue issue = strategy.createTicket(OfferTestUtils.TEST_SUPPLIER_ID,
            Arrays.asList(offer1, offer2), "Test Author");

        MbocAssertions.assertThat(issue).hasSummaryEqualTo("(Лаба) 1P Заведение msku 'Test Supplier'");
    }

    @Test
    public void testAddAfterReclassificationTag() {
        supplierRepository.insert(
            new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real"));

        Offer offer1 = OfferTestUtils.simpleOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer offer2 = OfferTestUtils.simpleOffer().setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .addAdditionalTicket(Offer.AdditionalTicketType.RECLASSIFICATION, "MCPTEST-100");

        Issue issue = strategy.createTicket(OfferTestUtils.TEST_SUPPLIER_ID,
            Arrays.asList(offer1, offer2), "Test Author");

        assertThat(issue.getTags()).contains("after_reclassification");
    }


    @Test
    public void testAddAfterWaitContentTag() {
        supplierRepository.insert(
            new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real"));

        Offer offer1 = OfferTestUtils.simpleOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer offer2 = OfferTestUtils.simpleOffer().setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .addAdditionalTicket(Offer.AdditionalTicketType.WAIT_CONTENT, "MCPTEST-100");

        Issue issue = strategy.createTicket(OfferTestUtils.TEST_SUPPLIER_ID,
            Arrays.asList(offer1, offer2), "Test Author");

        assertThat(issue.getTags()).contains("after_wait_content");
    }

    @Test
    public void testAddAfterSizeMeasureValue() {
        supplierRepository.insert(
            new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real"));

        Offer offer1 = OfferTestUtils.simpleOffer().setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED);
        Offer offer2 = OfferTestUtils.simpleOffer().setCategoryIdForTests(2L, Offer.BindingKind.SUGGESTED)
            .addAdditionalTicket(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "MCPTEST-100");

        Issue issue = strategy.createTicket(OfferTestUtils.TEST_SUPPLIER_ID,
            Arrays.asList(offer1, offer2), "Test Author");

        assertThat(issue.getTags()).contains("after_size_measure_value");
    }

    @Test
    public void testAddGoodContentTag() {
        supplierRepository.insert(
            new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId("real")
                .setNewContentPipeline(true));
        Offer offer = OfferTestUtils.simpleOffer().setBusinessId(OfferTestUtils.TEST_SUPPLIER_ID);
        ru.yandex.startrek.client.model.Issue issue = strategy.createTicket(OfferTestUtils.TEST_SUPPLIER_ID,
            Collections.singletonList(offer), "Test Author");

        assertThat(issue.getTags()).contains("good_content");
    }
}
