package ru.yandex.market.mboc.app.content;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.xmlrpc.webserver.HttpServletResponseImpl;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.logisticsparams.LogisticParamsWebService;
import ru.yandex.market.mboc.app.offers.OffersWebService;
import ru.yandex.market.mboc.app.offers.models.OffersWebFilter;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.exceptions.NotFoundException;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.offers.ManualVendorService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.IMasterDataRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OfferCriterias;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepositoryMock;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.converter.OffersToExcelFileConverter;
import ru.yandex.market.mboc.common.services.excel.template.ExcelTemplateGenerator;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.SecurityContextAuthenticationHelper;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;
import ru.yandex.market.mboc.common.web.Result;

@SuppressWarnings("checkstyle:magicnumber")
public class ContentControllerTest extends BaseMbocAppTest {
    private ContentController controller;

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private IMasterDataRepository masterDataRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private SupplierRepository supplierRepository;
    private List<OfferForService> templateGeneratorOffers = new Vector<>();

    private CategoryInfoRepositoryMock categoryInfoRepository;
    private RetrieveMappingSkuTypeService retrieveMappingSkuTypeService;
    private OfferMappingActionService offerMappingActionService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;

    private GlobalVendorsCachingService globalVendorsCachingService;
    private ManualVendorService manualVendorService;
    private CategoryCachingServiceMock categoryCachingService;
    private ProcessingTicketInfoServiceForTesting processingTicketInfoService;
    private SupplierService supplierService;
    private OffersProcessingStatusService offersProcessingStatusService;
    private ModelStorageCachingServiceMock modelStorageCachingService;

    @Before
    public void setup() throws IOException {
        supplierRepository.insert(new Supplier().setId(42).setName("supplier42").setNewContentPipeline(false));
        supplierRepository.insert(new Supplier().setId(12).setName("supplier12").setNewContentPipeline(true));
        supplierRepository.insert(OfferTestUtils.realSupplier());
        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(
            new ProcessingTicketInfoRepositoryMock());

        categoryInfoRepository = new CategoryInfoRepositoryMock(Mockito.mock(MboUsersRepository.class));
        categoryInfoRepository.insertBatch(Arrays.asList(
            new CategoryInfo(1).setModerationInYang(true),
            new CategoryInfo(2),
            new CategoryInfo(3),
            new CategoryInfo(4)));
        categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        categoryKnowledgeService
            .addCategory(1)
            .addCategory(2)
            .addCategory(4);
        globalVendorsCachingService = new GlobalVendorsCachingServiceMock();
        categoryCachingService = new CategoryCachingServiceMock();
        categoryCachingService.addCategory(new Category().setCategoryId(1)
            .setHasKnowledge(true)
            .setAcceptGoodContent(false));
        categoryCachingService.addCategory(new Category().setCategoryId(2)
            .setHasKnowledge(true)
            .setAcceptGoodContent(false));
        categoryCachingService.addCategory(new Category().setCategoryId(3)
            .setHasKnowledge(false)
            .setAcceptGoodContent(false));
        categoryCachingService.addCategory(new Category().setCategoryId(4)
            .setHasKnowledge(true)
            .setAcceptGoodContent(true));

        supplierService = new SupplierService(supplierRepository);
        NeedContentStatusService needContentStatusService = new NeedContentStatusService(
            categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet())
        );
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        manualVendorService = new ManualVendorService(offerMappingActionService);
        modelStorageCachingService = new ModelStorageCachingServiceMock();
        retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);

        BusinessSupplierService businessSupplierService = new BusinessSupplierService(
            supplierRepository, offerRepository);

        OffersWebService offersWebService = new OffersWebService(offerRepository, null, categoryCachingService,
            businessSupplierService, transactionHelper);
        offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            needContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(needContentStatusService),
            false, false, 3, categoryInfoCache);

        OffersToExcelFileConverterConfig config = new OffersToExcelFileConverterConfig(categoryCachingService);

        var masterDataServiceMock = new MasterDataServiceMock();
        var supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        var masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        ExcelTemplateGenerator templateGenerator = Mockito.mock(ExcelTemplateGenerator.class);
            Mockito.doAnswer((Answer<Void>) invocation -> {
                Object[] args = invocation.getArguments();
                templateGeneratorOffers = (List<OfferForService>) args[0];
                return null;
            }).when(templateGenerator).generateExcelByTemplate(Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any());

        controller = new ContentController(
            config.displayConverter(),
            offerRepository,
            supplierRepository,
            masterDataHelperService,
            templateGenerator,
            globalVendorsCachingService,
            manualVendorService,
            offersWebService,
            Mockito.mock(LogisticParamsWebService.class),
            processingTicketInfoService,
            masterDataRepository,
            categoryCachingService, false,
            categoryInfoRepository,
            offersProcessingStatusService,
            businessSupplierService, offerBatchProcessor);


        SecurityContextAuthenticationHelper.setAuthenticationToken();
    }

    @After
    public void tearDown() throws Exception {
        SecurityContextAuthenticationHelper.clearAuthenticationToken();
        templateGeneratorOffers = null;
    }

    @Test
    public void testOffersForModeration() {
        // correct offer
        offerRepository.insertOffer(createTestOffer());
        offerRepository.insertOffer(createTestOffer()
            .setId(2L)
            .setCategoryIdForTests(3L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(3L, Offer.MappingConfidence.CONTENT)
            .setSupplierSkuMapping(null));
        offerRepository.insertOffer(createTestOffer().setId(3L).setCategoryIdForTests(3L, Offer.BindingKind.SUGGESTED));

        Result res = controller.createTask(new ContentController.CreateTaskRequest()
            .setSupplierId(42)
            .setOfferIds(Arrays.asList(1L, 2L, 3L)));

        Assertions.assertThat(offerRepository.getOffersByIds(Arrays.asList(1L, 3L)))
            .extracting(Offer::getProcessingStatus)
            .containsExactlyInAnyOrder(
                Offer.ProcessingStatus.IN_MODERATION,
                Offer.ProcessingStatus.IN_MODERATION
            );
        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(res.getMessage()).isEqualTo("Офферов отправлено на модерацию: 2 шт.\n" +
            "Ожидают появления знаний в категории: 1 шт.");
    }

    @Test
    public void testErrorResult() {
        offerRepository.insertOffer(createTestOffer()
            .setId(2L)
            .setCategoryIdForTests(3L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(3L, Offer.MappingConfidence.CONTENT)
            .setSupplierSkuMapping(null));

        Result res = controller.createTask(new ContentController.CreateTaskRequest()
            .setSupplierId(42)
            .setOfferIds(List.of(2L)));

        Assertions.assertThat(offerRepository.getOfferById(2L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NO_KNOWLEDGE);

        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        Assertions.assertThat(res.getMessage()).isEqualTo("Нет товаров для создания тикета или модерации.\n" +
            "Ожидают появления знаний в категории: 1 шт.");
    }

    @Test
    public void testToContentLab() {
        Offer noKnowledgeOffer = createTestOffer().updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE);
        offerRepository.insertOffers(noKnowledgeOffer);

        Result res = controller.createTask(new ContentController.CreateTaskRequest()
            .setSupplierId(noKnowledgeOffer.getBusinessId())
            .setToContentLab(true)
            .setOfferIds(List.of(noKnowledgeOffer.getId())));

        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(offerRepository.getOfferById(noKnowledgeOffer.getId()).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NO_KNOWLEDGE);
        Assertions.assertThat(res.getMessage()).isEqualTo(
            "Товары запрошены в лабораторию.\n" +
                "Ожидают появления знаний в категории: 1 шт.");
    }

    @Test
    public void testForceToClassification() {
        offerRepository.insertOffer(createTestOffer()
            .setId(1L)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_KNOWLEDGE)
            .setBindingKind(Offer.BindingKind.APPROVED));
        offerRepository.insertOffer(createTestOffer()
            .setId(2L)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NO_CATEGORY)
            .setBindingKind(Offer.BindingKind.APPROVED));
        offerRepository.insertOffer(createTestOffer()
            .setId(3L)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.NEED_CONTENT)
            .setBindingKind(Offer.BindingKind.APPROVED));
        offerRepository.insertOffer(createTestOffer()
            .setId(4L)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN));

        Result res = controller.forceToClassification(new ContentController.ForceToClassificationRequest()
            .setSupplierId(42)
            .setOfferIds(Arrays.asList(1L, 2L, 3L, 4L)));

        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(res.getMessage()).isEqualTo(
            "3 офферов отправлено на классификацию.");
    }

    @Test
    public void testForceToClassificationError() {
        Set<Offer.ProcessingStatus> forForceClassificationStatuses = Set.of(
            Offer.ProcessingStatus.REOPEN,
            Offer.ProcessingStatus.NO_KNOWLEDGE,
            Offer.ProcessingStatus.NO_CATEGORY,
            Offer.ProcessingStatus.NEED_CONTENT);

        Iterator<Long> idsIterator = LongStream.iterate(1L, l -> l + 1).iterator();
        List<Offer> offers = Stream.of(Offer.ProcessingStatus.values())
            .filter(Predicate.not(forForceClassificationStatuses::contains))
            .map(status -> createTestOffer()
                .setId(idsIterator.next())
                .updateProcessingStatusIfValid(status))
            .collect(Collectors.toList());

        offerRepository.insertOffers(offers);

        List<Long> offerIds = offers.stream()
            .map(Offer::getId)
            .collect(Collectors.toList());

        Result res = controller.forceToClassification(new ContentController.ForceToClassificationRequest()
            .setSupplierId(42)
            .setOfferIds(offerIds));

        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        Assertions.assertThat(res.getMessage()).isEqualTo(
            "Нет подходящих офферов для создания тикета на классификацию категории.");
    }

    @Test
    public void testConcatResult() {
        offerRepository.insertOffer(createTestOffer());
        offerRepository.insertOffer(createTestOffer().setId(2L)
            .setCategoryIdForTests(3L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(3L, Offer.MappingConfidence.CONTENT)
        );
        offerRepository.insertOffer(createTestOffer().setId(3L).setCategoryIdForTests(3L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(createTestOffer().setId(4L)
            .setCategoryIdForTests(4L, Offer.BindingKind.APPROVED)
            .setMappedCategoryId(4L, Offer.MappingConfidence.CONTENT)
            .setSupplierSkuMapping(null)
        );

        Result res = controller.createTask(new ContentController.CreateTaskRequest()
            .setSupplierId(42)
            .setOfferIds(Arrays.asList(1L, 2L, 3L, 4L)));

        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(res.getMessage()).isEqualTo(
            "Офферов отправлено на разбор офферов: 1 шт.\n" +
                "Офферов отправлено на модерацию: 2 шт.\n" +
                "Ожидают появления знаний в категории: 1 шт.");
    }

    @Test
    public void testToGoodContent() {
        offerRepository.insertOffer(createTestOffer()
            .setBusinessId(12)
            .setId(1L)
            .setCategoryIdForTests(4L, Offer.BindingKind.SUGGESTED));
        offerRepository.insertOffer(createTestOffer()
            .setBusinessId(12)
            .setId(2L)
            .setCategoryIdForTests(4L, Offer.BindingKind.APPROVED).setSupplierSkuMapping(null));

        Result res = controller.createTask(new ContentController.CreateTaskRequest()
            .setSupplierId(42)
            .setOfferIds(Arrays.asList(1L, 2L)));

        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(offerRepository.getOfferById(1L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_MODERATION);
        Assertions.assertThat(offerRepository.getOfferById(2L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        Assertions.assertThat(res.getMessage()).isEqualTo(
            "Офферов отправлено на модерацию: 1 шт.\n" +
                "Офферов отправлено в good-content: 1 шт.");
    }

    @Test
    public void testToGoodContentEnabledChangeOrder() {
        NeedContentStatusService needContentStatusService = new NeedContentStatusService(
            categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet())
        );
        BusinessSupplierService businessSupplierService = new BusinessSupplierService(
            supplierRepository, offerRepository);
        OffersWebService offersWebService = new OffersWebService(offerRepository, null,
            categoryCachingService, businessSupplierService, transactionHelper);
        ContentController localController = new ContentController(
            Mockito.mock(OffersToExcelFileConverter.class),
            offerRepository,
            supplierRepository,
            Mockito.mock(MasterDataHelperService.class),
            Mockito.mock(ExcelTemplateGenerator.class),
            globalVendorsCachingService,
            manualVendorService,
            offersWebService,
            Mockito.mock(LogisticParamsWebService.class),
            processingTicketInfoService,
            masterDataRepository,
            categoryCachingService, true,
            categoryInfoRepository,
            offersProcessingStatusService,
            businessSupplierService, offerBatchProcessor);
        offerRepository.insertOffer(createTestOffer()
            .setBusinessId(12)
            .setId(1L)
            .setCategoryIdForTests(4L, Offer.BindingKind.APPROVED).setSupplierSkuMapping(null)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK));
        offerRepository.insertOffer(createTestOffer()
            .setBusinessId(12)
            .setId(2L)
            .setCategoryIdForTests(4L, Offer.BindingKind.SUGGESTED).setSupplierSkuMapping(null)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK));
        offerRepository.insertOffer(createTestOffer()
            .setBusinessId(12)
            .setId(3L)
            .setCategoryIdForTests(4L, Offer.BindingKind.SUGGESTED).setSupplierSkuMapping(null)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK));

        Result res = localController.createTask(new ContentController.CreateTaskRequest()
            .setSupplierId(42)
            .setOfferIds(Arrays.asList(1L, 2L, 3L)));

        Assertions.assertThat(res.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(offerRepository.getOfferById(1L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.NEED_CONTENT);
        Assertions.assertThat(offerRepository.getOfferById(2L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Assertions.assertThat(offerRepository.getOfferById(3L).getProcessingStatus())
            .isEqualTo(Offer.ProcessingStatus.IN_CLASSIFICATION);
        Assertions.assertThat(res.getMessage()).isEqualTo(
            "Офферов отправлено на классификацию: 2 шт." +
                "\nОфферов отправлено в good-content: 1 шт."
        );
    }

    @Test
    public void testSetDeadline() {
        var offer1 = createTestOffer().setId(1L);
        var offer2 = createTestOffer().setId(2L).setTicketDeadline(LocalDate.of(2019, 1, 1));
        processingTicketInfoService.createNewNoTicket(Offer.ProcessingStatus.IN_MODERATION,
            Collections.singletonList(offer1));
        processingTicketInfoService.createNewNoTicket(Offer.ProcessingStatus.IN_MODERATION,
            Collections.singletonList(offer2));
        offerRepository.insertOffer(offer1);
        offerRepository.insertOffer(offer2);

        LocalDate deadline = LocalDate.of(2019, 2, 2);
        Result result = controller.setTicketDeadline(new ContentController.SetTicketDeadLineRequest()
            .setDeadline(deadline)
            .setOfferIds(""));
        Assertions.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(result.getMessage()).isEqualTo("0 офферу(ам) был проставлен дедлайн 2019-02-02.");

        result = controller.setTicketDeadline(new ContentController.SetTicketDeadLineRequest()
            .setDeadline(deadline)
            .setOfferIds("1\n2\n"));
        Assertions.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(result.getMessage()).isEqualTo("2 офферу(ам) был проставлен дедлайн 2019-02-02.");

        List<Offer> offers = offerRepository.findOffers(new OffersFilter().setOfferIds(1, 2));
        Assertions.assertThat(offers)
            .hasSize(2)
            .extracting(Offer::getTicketDeadline)
            .allMatch(deadline::equals);
        Assertions.assertThat(processingTicketInfoService.getByIds(
                Arrays.asList(offer1.getProcessingTicketId(), offer2.getProcessingTicketId())
            )).extracting(ProcessingTicketInfo::getDeadline)
            .containsExactly(deadline, deadline);
    }

    @Test
    public void testSetNullDeadline() {
        offerRepository.insertOffer(createTestOffer().setId(1L));
        offerRepository.insertOffer(createTestOffer().setId(2L).setTicketDeadline(LocalDate.of(2019, 1, 1)));

        Result result = controller.setTicketDeadline(new ContentController.SetTicketDeadLineRequest()
            .setDeadline(null)
            .setOfferIds("1\n2\n"));
        Assertions.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(result.getMessage()).isEqualTo("У 2 оффера(ов) был удален дедлайн.");

        List<Offer> offers = offerRepository.findOffers(new OffersFilter().setOfferIds(1, 2));
        Assertions.assertThat(offers)
            .hasSize(2)
            .extracting(Offer::getTicketDeadline)
            .allMatch(Objects::isNull);
    }

    @Test
    public void testSetMappedCategoryId() {
        var testOffers = Stream.of(
                createTestOffer().setId(1L).
                    setProcessingStatusInternal(Offer.ProcessingStatus.CONTENT_PROCESSING),
                createTestOffer().setId(2L)
                    .setProcessingStatusInternal(Offer.ProcessingStatus.IN_CLASSIFICATION),
                createTestOffer().setId(5L)
                    .setProcessingStatusInternal(Offer.ProcessingStatus.IN_RECLASSIFICATION),
                createTestOffer().setId(3L)
                    .setProcessingStatusInternal(Offer.ProcessingStatus.CLASSIFIED),
                createTestOffer().setId(4L)
                    .setProcessingStatusInternal(Offer.ProcessingStatus.AUTO_PROCESSED)
            )
            .peek(o -> o
                .setMarketSpecificContentHashSent(123L))
            .collect(Collectors.toList());

        offerRepository.insertOffers(testOffers);

        long mappedCategoryId = 101;

        categoryCachingService.addCategory(new Category().setCategoryId(mappedCategoryId)
            .setLeaf(true));

        Result result = controller.setMappedCategoryId(new ContentController.SetMappedCategoryIdRequest()
            .setMappedCategoryId(mappedCategoryId)
            .setOfferIds("1\n2\n3\n4\n"));

        Assertions.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(
            result.getMessage()).isEqualTo("2 офферам была проставлена mappedCategoryId " + mappedCategoryId + "."
        );

        List<Offer> offersNotUpdated = offerRepository.findOffers(new OffersFilter().setOfferIds(1, 2));
        List<Offer> offersUpdated = offerRepository.findOffers(new OffersFilter().setOfferIds(3, 4));

        Assertions.assertThat(offersNotUpdated)
            .extracting(Offer::getMappedCategoryId)
            .allMatch(Objects::isNull);
        Assertions.assertThat(offersNotUpdated)
            .extracting(Offer::getMappedCategoryConfidence)
            .allMatch(Objects::isNull);
        Assertions.assertThat(offersNotUpdated)
            .extracting(Offer::getMarketSpecificContentHashSent)
            .allMatch(Objects::nonNull);

        Assertions.assertThat(offersUpdated)
            .extracting(Offer::getMappedCategoryId)
            .allMatch(categoryId -> categoryId == mappedCategoryId);
        Assertions.assertThat(offersUpdated)
            .extracting(Offer::getMappedCategoryConfidence)
            .allMatch(confidence -> confidence.equals(Offer.MappingConfidence.CONTENT));
        Assertions.assertThat(offersUpdated)
            .extracting(Offer::getMarketSpecificContentHashSent)
            .allMatch(Objects::isNull);
    }

    @Test
    public void testMappedCategoryIsNotLeaf() {
        long mappedCategoryId = 101;

        categoryCachingService.addCategory(new Category().setCategoryId(mappedCategoryId)
            .setLeaf(false));

        Result result = controller.setMappedCategoryId(new ContentController.SetMappedCategoryIdRequest()
            .setMappedCategoryId(mappedCategoryId)
            .setOfferIds("1\n2\n3\n4\n"));

        Assertions.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        Assertions.assertThat(
            result.getMessage()).isEqualTo("Категория " + mappedCategoryId + " не листовая");
    }

    @Test(expected = NotFoundException.class)
    public void testMappedCategoryIsNotFound() {
        long mappedCategoryId = 101;

        controller.setMappedCategoryId(new ContentController.SetMappedCategoryIdRequest()
            .setMappedCategoryId(mappedCategoryId)
            .setOfferIds("1\n2\n3\n4\n"));
    }

    @Test(expected = NotFoundException.class)
    public void testSetMappedCategoryIdNotFoundCategory() {
        offerRepository.insertOffer(createTestOffer().setId(1L));
        offerRepository.insertOffer(createTestOffer().setId(2L));

        long mappedCategoryId = 123;

        controller.setMappedCategoryId(new ContentController.SetMappedCategoryIdRequest()
            .setMappedCategoryId(mappedCategoryId)
            .setOfferIds("1\n2\n"));
    }

    @Test
    public void hideFromTolokaTest() {
        List<Long> categoryIds = Arrays.asList(1L, 2L);

        ContentController.HideFromTolokaRequest request = new ContentController.HideFromTolokaRequest()
            .setHideFromToloka(false)
            .setCategoryIds(categoryIds);

        controller.setHideFromToloka(request);
        categoryInfoRepository.findByIdsForUpdate(categoryIds)
            .forEach(categoryInfo -> {
                Assert.assertFalse(categoryInfo.isHideFromToloka());
            });

        request.setHideFromToloka(true);
        controller.setHideFromToloka(request);
        categoryInfoRepository.findByIdsForUpdate(categoryIds)
            .forEach(categoryInfo -> {
                Assert.assertTrue(categoryInfo.isHideFromToloka());
            });
    }

    @Test
    public void whenFilteringDatacampOffersThenOk() {
        var datacampOffer1 = createTestOffer().setId(1L).setDataCampOffer(true);
        var datacampOffer2 = createTestOffer().setId(2L).setDataCampOffer(true);
        var notDatacampOffer1 = createTestOffer().setId(3L).setDataCampOffer(false);
        var notDatacampOffer2 = createTestOffer().setId(4L).setDataCampOffer(false);

        offerRepository.insertOffer(datacampOffer1);
        offerRepository.insertOffer(datacampOffer2);
        offerRepository.insertOffer(notDatacampOffer1);
        offerRepository.insertOffer(notDatacampOffer2);

        OffersFilter dataCampFilter = new OffersFilter();
        dataCampFilter.addCriteria(OfferCriterias.isDataCampOffer());

        OffersFilter notDatacampFilter = new OffersFilter();
        notDatacampFilter.addCriteria(OfferCriterias.notDataCampOffer());

        var datacampOffers = offerRepository.findOffersForService(dataCampFilter, false).stream()
                .map(OfferForService::getBaseOffer).collect(Collectors.toList());
        var notDatacampOffers = offerRepository
            .findOffersForService(notDatacampFilter, false).stream()
                .map(OfferForService::getBaseOffer).collect(Collectors.toList());

        Assertions.assertThat(datacampOffers).containsExactlyInAnyOrder(datacampOffer1, datacampOffer2);
        Assertions.assertThat(notDatacampOffers).containsExactlyInAnyOrder(notDatacampOffer1, notDatacampOffer2);
    }

    @Test
    public void whenQueryForNotDatacampOr1pOfferThenOk() {
        var datacamp3pOffer = createTestOffer().setId(1L).setDataCampOffer(true);
        var datacamp1pOffer = create1pOffer().setId(2L).setDataCampOffer(true);
        var notDatacamp1pOffer = create1pOffer().setId(3L).setDataCampOffer(false);
        var notDatacamp3pOffer = createTestOffer().setId(4L).setDataCampOffer(false);

        offerRepository.insertOffer(datacamp3pOffer);
        offerRepository.insertOffer(datacamp1pOffer);
        offerRepository.insertOffer(notDatacamp1pOffer);
        offerRepository.insertOffer(notDatacamp3pOffer);

        OffersFilter notDatacampOr1pOffer = new OffersFilter();
        notDatacampOr1pOffer.addCriteria(OfferCriterias.notDataCampOfferOrIs1p());

        var notDatacampOr1pOffers =
            offerRepository.findOffersForService(notDatacampOr1pOffer, false).stream()
                .map(OfferForService::getBaseOffer)
                .collect(Collectors.toList());

        Assertions.assertThat(notDatacampOr1pOffers)
            .containsExactlyInAnyOrder(notDatacamp1pOffer, notDatacamp3pOffer, datacamp1pOffer);
        Assertions.assertThat(notDatacampOr1pOffers).doesNotContain(datacamp3pOffer);
    }

    @Test
    public void queryForNotDatacampOr1pOfferWorksFineWithOtherCriterias() {
        var testVendor = "testVendorName";
        var datacamp3pOffer = createTestOffer().setId(1L).setDataCampOffer(true);
        var datacamp1pOffer = create1pOffer().setId(2L).setVendor(testVendor).setDataCampOffer(true);
        var notDatacamp1pOffer = create1pOffer().setId(3L).setDataCampOffer(false);
        var notDatacamp3pOffer = createTestOffer().setId(4L).setDataCampOffer(false);

        offerRepository.insertOffer(datacamp3pOffer);
        offerRepository.insertOffer(datacamp1pOffer);
        offerRepository.insertOffer(notDatacamp1pOffer);
        offerRepository.insertOffer(notDatacamp3pOffer);

        OffersFilter notDatacampOr1pOffer = new OffersFilter();
        notDatacampOr1pOffer.addCriteria(OfferCriterias.vendor(List.of(testVendor)));
        notDatacampOr1pOffer.addCriteria(OfferCriterias.notDataCampOfferOrIs1p());
        notDatacampOr1pOffer.addCriteria(OfferCriterias.searchByIds(List.of(2L, 3L)));

        var notDatacampOr1pOffers =
            offerRepository.findOffersForService(notDatacampOr1pOffer, false).stream()
                .map(OfferForService::getBaseOffer)
                .collect(Collectors.toList());

        Assertions.assertThat(notDatacampOr1pOffers).containsExactlyInAnyOrder(datacamp1pOffer);
        Assertions.assertThat(notDatacampOr1pOffers)
            .doesNotContain(datacamp3pOffer, notDatacamp3pOffer, notDatacamp1pOffer);
    }

    @Test
    public void whenDownloadExcelThenAllOffersProcessed() throws IOException {
        var datacampOffer1 = createTestOffer().setId(1L).setDataCampOffer(true);
        var datacampOffer2 = createTestOffer().setId(2L).setDataCampOffer(true);
        var notDatacampOffer1 = createTestOffer().setId(3L).setDataCampOffer(false);
        var notDatacampOffer2 = createTestOffer().setId(4L).setDataCampOffer(false);

        offerRepository.insertOffer(datacampOffer1);
        offerRepository.insertOffer(datacampOffer2);
        offerRepository.insertOffer(notDatacampOffer1);
        offerRepository.insertOffer(notDatacampOffer2);

        OffersWebFilter filter = new OffersWebFilter();
        filter.setSupplierId(42);
        filter.setOfferIds(List.of(datacampOffer1.getId(), datacampOffer2.getId(), notDatacampOffer1.getId(),
            notDatacampOffer2.getId()));

        var response = Mockito.mock(HttpServletResponseImpl.class);
        controller.downloadExcelTemplate(filter, false, true, response);

        var datacampServiceOffers =
            templateGeneratorOffers.stream().map(OfferForService::getBaseOffer).collect(Collectors.toList());
        Assertions.assertThat(datacampServiceOffers)
            .containsExactlyInAnyOrder(datacampOffer1, datacampOffer2);
    }

    private Offer createTestOffer() {
        return OfferTestUtils.nextOffer()
            .setId(1)
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier())
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierSkuMapping(new Offer.Mapping(15L, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET))
            .setTitle("Offer_" + (long) 1);
    }

    private Offer create1pOffer() {
        return OfferTestUtils.next1pOffer()
            .setId(1)
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.firstPartySupplier())
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .updateProcessingStatusIfValid(Offer.ProcessingStatus.OPEN)
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .setSupplierSkuMapping(new Offer.Mapping(15L, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET))
            .setTitle("Offer_" + (long) 1);
    }
}
