package ru.yandex.market.mboc.app.offers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.app.importexcel.BackgroundImportService;
import ru.yandex.market.mboc.app.mapping.RecheckMappingService;
import ru.yandex.market.mboc.app.offers.OffersController.RequestKind;
import ru.yandex.market.mboc.app.offers.enrichment.BackgroundEnrichFileService;
import ru.yandex.market.mboc.app.offers.models.OffersWebFilter;
import ru.yandex.market.mboc.app.offers.web.DisplayOffer;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.OfferProcessingStatus;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.dict.ChildSskuResult;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.dict.WarehouseService;
import ru.yandex.market.mboc.common.dict.WarehouseServiceAuditRecorder;
import ru.yandex.market.mboc.common.dict.WarehouseServiceRepository;
import ru.yandex.market.mboc.common.dict.WarehouseServiceService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.msku.BuyPromoPriceRepository;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.HasDeadlineOrOldProcessingStatusCriteria;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoRepository;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoService;
import ru.yandex.market.mboc.common.processingticket.ProcessingTicketInfoServiceForTesting;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.SecurityContextAuthenticationHelper;
import ru.yandex.market.mboc.common.utils.YangPrioritiesUtil;
import ru.yandex.market.mboc.common.web.DataPage;
import ru.yandex.market.mboc.common.web.Result;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MbocCommon.MappingInfoLite;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class OffersControllerTest extends BaseMbocAppTest {
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private ProcessingTicketInfoRepository processingTicketInfoRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferBatchProcessor offerBatchProcessor;
    @Autowired
    private WarehouseServiceRepository warehouseServiceRepository;

    private OffersController controller;
    private MockHttpServletResponse response;
    private OfferRepository repository;
    private CategoryCachingServiceMock categoryCachingService;
    private CategoryKnowledgeServiceMock categoryKnowledgeService;
    private ProcessingTicketInfoService processingTicketInfoService;
    private SupplierService supplierService;
    private OffersWebService offersWebService;
    private WarehouseServiceService warehouseServiceService;

    private static final String JUST_SUPPLIER_NAME = "Test2";
    private static final int JUST_SUPPLIER_ID = 43;

    private static final int LINKED_SUPPLIER_ID = 2001;
    private static final int OTHER_LINKED_SUPPLIER_ID = 2002;

    @Before
    public void setup() {
        repository = Mockito.spy(offerRepository);
        supplierRepository.insertBatch(ImmutableSet.of(
            new Supplier(42, "Test1"),
            new Supplier(JUST_SUPPLIER_ID, JUST_SUPPLIER_NAME),
            new Supplier(44, "Test3"),
            new Supplier(50, "Test4"),
            new Supplier(51, "Test5"),
            new Supplier(99, "Test6"),
            new Supplier(100, "Test7"),
            new Supplier(45, "Test8")
        ));
        supplierService = new SupplierService(supplierRepository);
        categoryCachingService = new CategoryCachingServiceMock();
        CategoryInfoRepository categoryInfoRepository = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));

        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);

        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        var offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        categoryKnowledgeService = new CategoryKnowledgeServiceMock();

        var offersProcessingStatusService = new OffersProcessingStatusService(
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
            false,
            false,
            3, categoryInfoCache);

        processingTicketInfoService = new ProcessingTicketInfoServiceForTesting(processingTicketInfoRepository);

        BusinessSupplierService businessSupplierService = new BusinessSupplierService(supplierRepository,
            offerRepository);

        offersWebService = new OffersWebService(repository, null, categoryCachingService,
            businessSupplierService, transactionHelper);
        warehouseServiceService = Mockito.spy(new WarehouseServiceService(
            warehouseServiceRepository,
            offerRepository,
            supplierRepository,
            mskuRepository,
            Mockito.mock(WarehouseServiceAuditRecorder.class)
        ));

        controller = new OffersController(repository,
            Mockito.mock(BackgroundImportService.class),
            Mockito.mock(BackgroundEnrichFileService.class),
            supplierRepository,
            mskuRepository,
            new OfferProtoConverter(categoryCachingService,
                Mockito.mock(OfferCategoryRestrictionCalculator.class), mskuRepository, -1),
            offerMappingActionService,
            Mockito.mock(ObjectMapper.class),
            new CategoryCachingServiceMock(),
            Mockito.mock(BuyPromoPriceRepository.class),
            offersProcessingStatusService,
            offersWebService,
            needContentStatusService,
            Mockito.mock(ImportFileService.class),
            processingTicketInfoService,
            businessSupplierService,
            offerBatchProcessor,
            offerDestinationCalculator, false,
            warehouseServiceService,
            new RecheckMappingService(offerRepository,
                offerMappingActionService,
                offersProcessingStatusService,
                transactionHelper,
                new AntiMappingService(antiMappingRepository, transactionHelper))
        );
        response = new MockHttpServletResponse();
        List<Offer> offers = YamlTestUtil.readOffersFromResources("offers/sample-offers.json");
        repository.insertOffers(offers);

        categoryCachingService.addCategory(new Category().setCategoryId(1L).setHasKnowledge(true));
        categoryCachingService.addCategory(new Category().setCategoryId(2L).setHasKnowledge(true));
        categoryCachingService.addCategory(new Category().setCategoryId(11L).setHasKnowledge(true));

        categoryKnowledgeService.addCategory(1L);
        categoryKnowledgeService.addCategory(2L);
        categoryKnowledgeService.addCategory(11L);

        processingTicketInfoRepository.save(
            new ProcessingTicketInfo()
                .setId(1000)
                .setCreated(Instant.now())
                .setTotalOffers(1)
                .setStuckOffers(0)
                .setOfferBaseStatus(OfferProcessingStatus.IN_PROCESS)
                .setComputedDeadline(LocalDate.parse("2018-05-12"))
                .setActiveOffers(ProcessingTicketInfoService.EMPTY_MAP)
                .setOffersByCategory(ProcessingTicketInfoService.EMPTY_MAP));

        SecurityContextAuthenticationHelper.setAuthenticationToken();
    }

    @After
    public void tearDown() {
        SecurityContextAuthenticationHelper.clearAuthenticationToken();
    }

    @Test
    public void testJson() throws Exception {
        controller.bulkLoad(RequestKind.JSON, null, null, null, false, null, null, null, null, null, response);

        assertEquals(200, response.getStatus());
        assertEquals("text/json", response.getContentType());

        String[] lines = response.getContentAsString().split("\n");
        assertEquals(20, lines.length);
    }

    @Test
    public void testBulkLoadLite() throws IOException {
        controller.bulkLoadLite(null, null, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/octet-stream", response.getContentType());

        ByteArrayInputStream in = new ByteArrayInputStream(response.getContentAsByteArray());

        List<MboMappings.ProviderProductInfoLite> infos = new ArrayList<>();
        MboMappings.ProviderProductInfoLite info;
        do {
            info = MboMappings.ProviderProductInfoLite.parseDelimitedFrom(in);
            if (info != null) {
                infos.add(info);
            }
        } while (info != null);

        //1 FAST_SKU offer is filtered out
        assertEquals(20, infos.size());
    }

    @Test
    public void testAfter() throws Exception {
        Offer offerWithMaxVersion = offerRepository.findOffers(new OffersFilter())
            .stream()
            .max((o1, o2) -> (int) (o1.getLastVersion() - o2.getLastVersion()))
            .get();
        controller.bulkLoad(RequestKind.PROTO, offerWithMaxVersion.getLastVersion() - 1,
            null, null, false, null, null, null, null, null, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/octet-stream", response.getContentType());

        ByteArrayInputStream in = new ByteArrayInputStream(response.getContentAsByteArray());
        SupplierOffer.Offer offer = SupplierOffer.Offer.parseDelimitedFrom(in);
        assertEquals(offerWithMaxVersion.getLastVersion(), offer.getLastVersion());
    }

    @Test
    public void testProto() throws Exception {
        controller.bulkLoad(RequestKind.PROTO, null, null, 1L, false, null, null, null, null, null, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/octet-stream", response.getContentType());

        ByteArrayInputStream in = new ByteArrayInputStream(response.getContentAsByteArray());

        SupplierOffer.Offer offer = SupplierOffer.Offer.parseDelimitedFrom(in);

        assertEquals(42, offer.getSupplierId());
        assertFalse(offer.hasMarketCategoryId());
        assertThat(offer.getTitle(), CoreMatchers.containsString("Бутылочка из полипропилена"));
        assertThat(offer.getShopCategoryName(), CoreMatchers.containsString("Бутылочки и ниблеры"));
        assertEquals("Philips Avent", offer.getShopVendor());
        assertEquals("SCF690/17", offer.getVendorCode());
        assertEquals("ssku1", offer.getShopSkuId());
        assertEquals("", offer.getBarcode());
        assertEquals(1, offer.getShopParamsCount());
        assertEquals("shop_super_column", offer.getShopParams(0).getName());
        assertEquals("something else", offer.getShopParams(0).getValue());
        assertEquals("https://www.ozon.ru/?context=search&text=SCF690%2f17", offer.getUrl());
        assertEquals(2, offer.getPictureUrlCount());
        assertEquals("https://1.ru/1.jpg", offer.getPictureUrl(0));
        assertEquals("https://1.ru/2.jpg", offer.getPictureUrl(1));
        assertEquals(123L, offer.getMarketVendorId());

        long expectedTimestamp = LocalDateTime.of(2018, 5, 5, 23, 0).atZone(ZoneId.systemDefault()).toEpochSecond();
        assertEquals(expectedTimestamp, offer.getProcessingStatusTs());

        while (in.available() > 0) {
            offer = SupplierOffer.Offer.parseDelimitedFrom(in);
        }
        assertEquals("SCF690/17", offer.getVendorCode());
    }

    @Test
    public void testProtoCategoryMappings() throws IOException {
        controller.categoryMappings(1, response);

        assertEquals(200, response.getStatus());
        assertEquals("application/octet-stream", response.getContentType());

        ByteArrayInputStream in = new ByteArrayInputStream(response.getContentAsByteArray());

        List<MappingInfoLite> mappings = new ArrayList<>();
        MappingInfoLite mapping;
        do {
            mapping = MappingInfoLite.parseDelimitedFrom(in);
            if (mapping != null) {
                mappings.add(mapping);
            }
        } while (mapping != null);

        List<MappingInfoLite> expected = List.of(
            MappingInfoLite.newBuilder()
                .setCategoryId(1)
                .setSupplierId(99)
                .setShopSku("99")
                .setModelId(120)
                .build(),
            MappingInfoLite.newBuilder()
                .setCategoryId(1)
                .setSupplierId(100)
                .setShopSku("100")
                .setModelId(120)
                .build(),
            MappingInfoLite.newBuilder()
                .setCategoryId(1)
                .setSupplierId(100)
                .setShopSku("101")
                .setModelId(120)
                .build(),
            MappingInfoLite.newBuilder()
                .setCategoryId(1)
                .setSupplierId(100)
                .setShopSku("102")
                .setModelId(15)
                .build()
        );
        Assertions.assertThat(mappings).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testUpdateBySearch() {
        OffersWebFilter filter = new OffersWebFilter().setSearch("SCF690/17");
        OffersController.ChangeAcceptanceStatusRequest request = new OffersController.ChangeAcceptanceStatusRequest();
        request.setFilter(filter);
        request.setNewStatus(Offer.AcceptanceStatus.OK);
        request.setSupplierId(42);
        controller.updateAcceptanceByFilter(request);

        Offer offer1 = repository.getOfferById(1);
        Offer.ServiceOffer serviceOffer1 = offer1.getServiceOffer(42).orElseThrow();
        assertEquals(Offer.AcceptanceStatus.OK, offer1.getAcceptanceStatus());
        assertEquals(Offer.AcceptanceStatus.OK, serviceOffer1.getServiceAcceptance());
        Offer offer2 = repository.getOfferById(2);
        Offer.ServiceOffer serviceOffer2 = offer2.getServiceOffer(42).orElseThrow();
        assertNotEquals(Offer.AcceptanceStatus.OK, offer2.getAcceptanceStatus());
        assertNotEquals(Offer.AcceptanceStatus.OK, serviceOffer2.getServiceAcceptance());
    }

    @Test
    public void testUpdateByFilterAndAcceptance() {
        // у оффера разные acceptance статусы: сервисный NEW, встроенный - TRASH
        // сперва убедимся, что по если мы фильтруем по TRASH (встроенному) - изменения не вносятся
        OffersWebFilter filter = new OffersWebFilter()
            .setSearch("SCF693/17")
            .setSupplierId(42)
            .setAcceptanceStatus(Offer.AcceptanceStatus.TRASH);
        OffersController.ChangeAcceptanceStatusRequest request = new OffersController.ChangeAcceptanceStatusRequest();
        request.setFilter(filter);
        request.setNewStatus(Offer.AcceptanceStatus.OK);
        request.setSupplierId(42);
        controller.updateAcceptanceByFilter(request);
        Offer offer = repository.getOfferById(2);
        Offer.ServiceOffer serviceOffer = offer.getServiceOffer(42).orElseThrow();
        assertEquals(Offer.AcceptanceStatus.TRASH, offer.getAcceptanceStatus());
        assertEquals(Offer.AcceptanceStatus.NEW, serviceOffer.getServiceAcceptance());
    }

    @Test
    public void testUpdateByFilterAndServiceAcceptance() {
        // а если по NEW - все ок
        OffersWebFilter filter = new OffersWebFilter()
            .setSearch("SCF693/17")
            .setSupplierId(42)
            .setAcceptanceStatus(Offer.AcceptanceStatus.NEW);
        OffersController.ChangeAcceptanceStatusRequest request = new OffersController.ChangeAcceptanceStatusRequest();
        request.setFilter(filter);
        request.setNewStatus(Offer.AcceptanceStatus.OK);
        request.setSupplierId(42);
        controller.updateAcceptanceByFilter(request);
        Offer offer = repository.getOfferById(2);
        Offer.ServiceOffer serviceOffer = offer.getServiceOffer(42).orElseThrow();
        assertEquals(Offer.AcceptanceStatus.OK, offer.getAcceptanceStatus());
        assertEquals(Offer.AcceptanceStatus.OK, serviceOffer.getServiceAcceptance());
    }

    @Test
    public void testUpdateByFilterWithServiceOffers() {
        prepareForBusiness();
        OffersWebFilter filter = new OffersWebFilter().setSearch("1").setSupplierId(LINKED_SUPPLIER_ID);
        OffersController.ChangeAcceptanceStatusRequest request = new OffersController.ChangeAcceptanceStatusRequest();
        request.setFilter(filter);
        request.setNewStatus(Offer.AcceptanceStatus.TRASH);
        request.setSupplierId(LINKED_SUPPLIER_ID);
        controller.updateAcceptanceByFilter(request);
        Offer offer = repository.getOfferById(10001);
        assertEquals(Offer.AcceptanceStatus.NEW, offer.getAcceptanceStatus()); // baseOffer acceptance not changed
        Offer.ServiceOffer serviceOffer1 = offer.getServiceOffer(LINKED_SUPPLIER_ID).orElseThrow();
        assertEquals(Offer.AcceptanceStatus.TRASH, serviceOffer1.getServiceAcceptance());
        Offer.ServiceOffer serviceOffer2 = offer.getServiceOffer(OTHER_LINKED_SUPPLIER_ID).orElseThrow();
        assertEquals(Offer.AcceptanceStatus.NEW, serviceOffer2.getServiceAcceptance());
    }

    @Test
    public void testUpdateByFilterWithServiceOffersBaseChanged() {
        prepareForBusiness();
        OffersWebFilter filter = new OffersWebFilter().setSearch("1").setSupplierId(OTHER_LINKED_SUPPLIER_ID);
        OffersController.ChangeAcceptanceStatusRequest request = new OffersController.ChangeAcceptanceStatusRequest();
        request.setFilter(filter);
        request.setNewStatus(Offer.AcceptanceStatus.OK);
        request.setSupplierId(OTHER_LINKED_SUPPLIER_ID);
        controller.updateAcceptanceByFilter(request);

        Offer offer = repository.getOfferById(10001);
        assertEquals(Offer.AcceptanceStatus.OK, offer.getAcceptanceStatus());
        Offer.ServiceOffer serviceOffer1 = offer.getServiceOffer(LINKED_SUPPLIER_ID).orElseThrow();
        assertEquals(Offer.AcceptanceStatus.NEW, serviceOffer1.getServiceAcceptance());
        Offer.ServiceOffer serviceOffer2 = offer.getServiceOffer(OTHER_LINKED_SUPPLIER_ID).orElseThrow();
        assertEquals(Offer.AcceptanceStatus.OK, serviceOffer2.getServiceAcceptance());
    }

    @Test
    public void testUpdateByIds() {
        Result result = controller.updateAcceptance(Arrays.asList(
            new OffersController.AcceptanceUpdate(1, Offer.AcceptanceStatus.OK, 42),
            new OffersController.AcceptanceUpdate(2, Offer.AcceptanceStatus.TRASH, 42),
            new OffersController.AcceptanceUpdate(3, Offer.AcceptanceStatus.NEW, 43)
        ));
        assertEquals(Result.ResultStatus.SUCCESS, result.getStatus());
        assertEquals(Offer.AcceptanceStatus.OK, repository.getOfferById(1).getAcceptanceStatus());
        assertEquals(Offer.AcceptanceStatus.TRASH, repository.getOfferById(2).getAcceptanceStatus());
        assertEquals(Offer.AcceptanceStatus.NEW, repository.getOfferById(3).getAcceptanceStatus());

        Result result2 = controller.updateAcceptance(Arrays.asList(
            new OffersController.AcceptanceUpdate(1, Offer.AcceptanceStatus.OK, 42),
            new OffersController.AcceptanceUpdate(2, Offer.AcceptanceStatus.TRASH, 42),
            new OffersController.AcceptanceUpdate(3, Offer.AcceptanceStatus.NEW, 43),
            new OffersController.AcceptanceUpdate(4, Offer.AcceptanceStatus.NEW, 44),
            new OffersController.AcceptanceUpdate(5, Offer.AcceptanceStatus.NEW, 44)
        ));
        assertEquals(Result.ResultStatus.SUCCESS, result2.getStatus());
        assertEquals(Offer.AcceptanceStatus.NEW, repository.getOfferById(4).getAcceptanceStatus());
        assertEquals(Offer.AcceptanceStatus.NEW, repository.getOfferById(5).getAcceptanceStatus());

        Result result3 = controller.updateAcceptance(Arrays.asList(
            new OffersController.AcceptanceUpdate(4, Offer.AcceptanceStatus.TRASH, 44),
            new OffersController.AcceptanceUpdate(5, Offer.AcceptanceStatus.TRASH, 44)
        ));
        assertEquals(Result.ResultStatus.SUCCESS, result3.getStatus());
        assertEquals(Offer.AcceptanceStatus.TRASH, repository.getOfferById(4).getAcceptanceStatus());
        assertEquals(Offer.AcceptanceStatus.TRASH, repository.getOfferById(5).getAcceptanceStatus());
    }

    @Test
    public void testFindBySearch() {
        DataPage<DisplayOffer> result = controller.list(new OffersWebFilter().setSearch("пустышка"),
            OffsetFilter.firstPage());
        assertEquals(1, result.getItems().size());
        assertEquals(3, result.getItems().get(0).getId());
    }

    @Test
    public void testAcceptMappings() {
        repository.insertOffers(YamlTestUtil.readOffersFromResources("app-offers/supplier-mappings.yml"));

        Result result = controller.changeSupplierMappingStatus(new OffersController.ChangeSupplierMappingStatusRequest()
            .setNewStatus(Offer.MappingStatus.ACCEPTED)
            .setFilter(new OffersWebFilter().setSupplierId(44))
            .setSupplierId(44));

        Assertions.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);

        result = controller.changeSupplierMappingStatus(new OffersController.ChangeSupplierMappingStatusRequest()
            .setNewStatus(Offer.MappingStatus.ACCEPTED)
            .setFilter(new OffersWebFilter().setSupplierId(51))
            .setSupplierId(51));

        Assertions.assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        Assertions.assertThat(result.getDetailsTitle()).isNull();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testFindByAcceptanceStatus() {
        DataPage<DisplayOffer> result = controller.list(
            new OffersWebFilter().setAcceptanceStatus(Offer.AcceptanceStatus.TRASH), OffsetFilter.firstPage());
        Assertions.assertThat(result.getItems())
            .extracting(displayOffer -> (int) displayOffer.getId()).containsExactly(1);
    }

    @Test
    public void testFindByCreatedDateFromAndCreatedDateTo() {
        supplierRepository.insert(new Supplier(22, "Test9"));
        repository.deleteAllInTest();
        List<Offer> list = List.of(
            Offer.builder()
                .id(29L)
                .businessId(22)
                .shopSku("sometext")
                .title("1")
                .shopCategoryName("some text")
                .created(LocalDateTime.of(2018, 10, 25, 14, 21))
                .isOfferContentPresent(true)
                .offerContent(OfferContent.builder().build())
                .serviceOffers(List.of(
                    new Offer.ServiceOffer(
                        22,
                        MbocSupplierType.MARKET_SHOP,
                        Offer.AcceptanceStatus.OK)
                ))
                .build(),
            Offer.builder()
                .id(129L)
                .businessId(22)
                .shopSku("sometext1")
                .title("some text1")
                .shopCategoryName("Text")
                .created(LocalDateTime.of(2018, 10, 26, 12, 59))
                .isOfferContentPresent(true)
                .offerContent(OfferContent.builder().build())
                .serviceOffers(List.of(
                    new Offer.ServiceOffer(
                        22,
                        MbocSupplierType.MARKET_SHOP,
                        Offer.AcceptanceStatus.OK)
                ))
                .build(),
            Offer.builder()
                .id(202L)
                .businessId(22)
                .shopSku("sometext2")
                .title("some text2")
                .shopCategoryName("Text")
                .created(LocalDateTime.of(2019, 1, 28, 19, 0))
                .isOfferContentPresent(true)
                .offerContent(OfferContent.builder().build())
                .serviceOffers(List.of(
                    new Offer.ServiceOffer(
                        22,
                        MbocSupplierType.MARKET_SHOP,
                        Offer.AcceptanceStatus.OK)
                ))
                .build());
        repository.insertOffers(list);

        DataPage<DisplayOffer> result = controller.list(
            new OffersWebFilter().setCreatedDateFrom(LocalDate.parse("2018-10-26")), OffsetFilter.all()
        );
        assertEquals(2, result.getItems().size());

        Assertions.assertThat(result.getItems().stream().map(r -> r.getId()).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(202L, 129L);

        result = controller.list(
            new OffersWebFilter().setCreatedDateTo(LocalDate.parse("2018-10-26")),
            OffsetFilter.all()
        );

        assertEquals(1, result.getItems().size());
        assertEquals(29L, result.getItems().get(0).getId());

        result = controller.list(
            new OffersWebFilter().setCreatedDateFrom(LocalDate.parse("2018-10-27"))
                .setCreatedDateTo(LocalDate.parse("2018-10-28")),
            OffsetFilter.all()
        );

        assertEquals(0, result.getItems().size());
    }


    @Test
    public void testFindByShopId() {
        DataPage<DisplayOffer> result = controller.list(
            new OffersWebFilter().setSupplierId(42), OffsetFilter.firstPage());
        assertEquals(2, result.getItems().size());
        assertEquals(1, result.getItems().get(0).getId());
        assertEquals(2, result.getItems().get(1).getId());
    }

    @Test
    public void testLimitOffset() {
        DataPage<DisplayOffer> result = controller.list(
            new OffersWebFilter(), OffsetFilter.offset(0, 2));
        assertEquals(2, result.getItems().size());
        assertEquals(3, result.getItems().get(0).getId()); // Сортировка в начале по title-у
        assertEquals(1, result.getItems().get(1).getId());

        result = controller.list(
            new OffersWebFilter(), OffsetFilter.offset(2, 2));
        assertEquals(2, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getId());
    }

    @Test
    public void testPredefinedFilters() {
        DataPage<DisplayOffer> page = controller.list(
            new OffersWebFilter()
                .setPredefinedFilter(OffersWebFilter.PredefinedFilter.NO_ACCEPTANCE_STATUS),
            OffsetFilter.firstPage());

        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getId)
            .containsExactly(3L, 2L, 6L, 7L, 8L, 16L, 17L, 18L, 19L, 99L);
    }

    @Test
    public void testSkuTypeFilters() {
        DataPage<DisplayOffer> page = controller.list(
            new OffersWebFilter()
                .setSkuTypeFilter(OffersWebFilter.SkuTypeFilter.MARKET),
            OffsetFilter.firstPage());

        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getId)
            .containsExactly(17L, 18L, 4L, 9L);

        page = controller.list(
            new OffersWebFilter()
                .setSkuTypeFilter(OffersWebFilter.SkuTypeFilter.PARTNER),
            OffsetFilter.firstPage());

        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getId)
            .containsExactlyInAnyOrder(19L, 99L);
    }

    @Test
    public void testApprovedMappingConfidenceFilter() {
        DataPage<DisplayOffer> page = controller.list(
            new OffersWebFilter()
                .setApprovedMappingConfidenceFilter(OffersWebFilter.ApprovedMappingConfidenceFilter.CONTENT),
            OffsetFilter.firstPage());

        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getId)
            .containsExactly(17L, 18L, 4L, 9L);

        page = controller.list(
            new OffersWebFilter()
                .setApprovedMappingConfidenceFilter(OffersWebFilter.ApprovedMappingConfidenceFilter.PARTNER_SELF),
            OffsetFilter.firstPage());

        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getId)
            .containsExactlyInAnyOrder(19L, 99L);
    }

    @Test
    public void testChangeStatus() {
        OffersWebFilter offerFilter = new OffersWebFilter().setSupplierId(43);
        OffersController.ChangeSupplierMappingStatusRequest request =
            new OffersController.ChangeSupplierMappingStatusRequest()
                .setFilter(offerFilter)
                .setSupplierId(43);

        Result result = controller.changeSupplierMappingStatus(request);
        assertEquals(Result.ResultStatus.SUCCESS, result.getStatus());
        assertEquals("1 офферов обновлено.", result.getMessage());
        assertEquals("", result.getDetails());
        assertEquals(null, result.getDetailsTitle());

        Offer.Mapping mapping = new Offer.Mapping(120, LocalDateTime.parse("2018-04-17T23:14:11"));
        Offer offer = repository.getOfferById(3);
        MbocAssertions.assertThat(offer)
            .isEqualTo(new Offer()
                .setId(3)
                .setUploadToYtStamp(0L)
                .setBusinessId(43)
                .setTitle("SCF170/18 Соска-пустышка силиконовая (0-6 мес, 2 шт.) Philips Avent. Серия Classic")
                .setShopCategoryName(
                    "Все товары/Детские товары/Товары для мам и малышей/Кормление/Соски для бутылочек")
                .setIsOfferContentPresent(true)
                .storeOfferContent(
                    OfferContent.builder().addExtraShopFields("shop_super_column", "something else").build())
                .setVendor("Philips Avent")
                .setVendorCode("SCF170/18")
                .setVendorId(123)
                .setShopSku("ssku3")
                .setBarCode("")
                .setCategoryIdForTests(2L, Offer.BindingKind.APPROVED)
                .setSupplierSkuMapping(mapping)
                .setContentSkuMapping(mapping)
                .setMappingModifiedBy("test-user")
                .setSupplierSkuMappingStatus(Offer.MappingStatus.ACCEPTED, "test-user",
                    DateTimeUtils.dateTimeNow())
                .approve(Offer.MappingType.CONTENT, Offer.MappingConfidence.CONTENT)
                .updateProcessingStatusIfValid(Offer.ProcessingStatus.PROCESSED)
                .setMappingDestination(Offer.MappingDestination.BLUE)
                .addNewServiceOfferIfNotExistsForTests(supplierRepository.findById(43))
                .updateAcceptanceStatusForTests(43, Offer.AcceptanceStatus.OK)
                .setProcessingTicketId(null)
                .setTicketCritical(false)
                .storeOfferContent(OfferContent.builder().id(3).build()));
    }

    @Test
    public void updateAcceptanceStatusInOldPipeline() {
        supplierRepository.update(new Supplier(42, "Old pipeline")
            .setNewContentPipeline(false));
        supplierRepository.update(new Supplier(43, "Old pipeline")
            .setNewContentPipeline(false));
        Result result = controller.updateAcceptance(Arrays.asList(
            new OffersController.AcceptanceUpdate(1, Offer.AcceptanceStatus.OK, 42),
            new OffersController.AcceptanceUpdate(2, Offer.AcceptanceStatus.TRASH, 42),
            new OffersController.AcceptanceUpdate(3, Offer.AcceptanceStatus.NEW, 43)
        ));
        assertEquals(Result.ResultStatus.SUCCESS, result.getStatus());

        assertEquals(Offer.AcceptanceStatus.OK, repository.getOfferById(1).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.OPEN, repository.getOfferById(1).getProcessingStatus());

        assertEquals(Offer.AcceptanceStatus.TRASH, repository.getOfferById(2).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.OPEN, repository.getOfferById(2).getProcessingStatus());

        assertEquals(Offer.AcceptanceStatus.NEW, repository.getOfferById(3).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.OPEN, repository.getOfferById(3).getProcessingStatus());
    }

    @Test
    public void updateAcceptanceStatusInNewPipeline() {
        supplierRepository.updateBatch(
            new Supplier(42, "New pipeline").setNewContentPipeline(true),
            new Supplier(43, "New pipeline").setNewContentPipeline(true),
            new Supplier(50, "New pipeline").setNewContentPipeline(true),
            new Supplier(99, "New pipeline").setNewContentPipeline(true)
        );

        categoryCachingService.addCategories(
            new Category().setCategoryId(1).setAcceptGoodContent(false).setHasKnowledge(true),
            new Category().setCategoryId(2).setAcceptGoodContent(false),
            new Category().setCategoryId(11).setAcceptGoodContent(true).setHasKnowledge(true)
        );

        categoryKnowledgeService.addCategory(1);
        categoryKnowledgeService.removeCategory(2);
        categoryKnowledgeService.addCategory(11);

        Result result = controller.updateAcceptance(Arrays.asList(
            // good content is not allowed in category
            new OffersController.AcceptanceUpdate(6, Offer.AcceptanceStatus.OK, 50),
            // no knowledge
            new OffersController.AcceptanceUpdate(7, Offer.AcceptanceStatus.OK, 50),
            // in_moderation
            new OffersController.AcceptanceUpdate(8, Offer.AcceptanceStatus.OK, 50),
            // no changes
            new OffersController.AcceptanceUpdate(2, Offer.AcceptanceStatus.TRASH, 42),
            new OffersController.AcceptanceUpdate(3, Offer.AcceptanceStatus.NEW, 43),
            // good content is allowed in category
            new OffersController.AcceptanceUpdate(16, Offer.AcceptanceStatus.OK, 99)
        ));

        assertEquals(Result.ResultStatus.SUCCESS, result.getStatus());

        assertEquals(Offer.AcceptanceStatus.OK, repository.getOfferById(6).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, repository.getOfferById(6).getProcessingStatus());

        assertEquals(Offer.AcceptanceStatus.OK, repository.getOfferById(7).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.NO_KNOWLEDGE, repository.getOfferById(7).getProcessingStatus());

        assertEquals(Offer.AcceptanceStatus.OK, repository.getOfferById(8).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, repository.getOfferById(8).getProcessingStatus());

        assertEquals(Offer.AcceptanceStatus.TRASH, repository.getOfferById(2).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.OPEN, repository.getOfferById(2).getProcessingStatus());

        assertEquals(Offer.AcceptanceStatus.NEW, repository.getOfferById(3).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.OPEN, repository.getOfferById(3).getProcessingStatus());

        assertEquals(Offer.AcceptanceStatus.OK, repository.getOfferById(16).getAcceptanceStatus());
        assertEquals(Offer.ProcessingStatus.IN_CLASSIFICATION, repository.getOfferById(16).getProcessingStatus());
    }

    @Test
    public void testBulkLoadWithIsProcessOnly() throws Exception {
        controller.bulkLoad(RequestKind.JSON, null, null, null, true, null, null, null, null, null, response);

        assertEquals(200, response.getStatus());
        ArgumentCaptor<OffersFilter> requestCaptor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repository, times(1))
            .findOffers(requestCaptor.capture(), any());
        Assertions.assertThat(requestCaptor.getValue().getProcessingStatuses())
            .containsExactlyInAnyOrder(Offer.ProcessingStatus.IN_PROCESS);
    }

    @Test
    public void testBulkLoadWithWhites() throws Exception {
        var controllerWithWhitesEnabled = new OffersController(repository,
            Mockito.mock(BackgroundImportService.class),
            Mockito.mock(BackgroundEnrichFileService.class),
            supplierRepository,
            mskuRepository,
            new OfferProtoConverter(categoryCachingService,
                Mockito.mock(OfferCategoryRestrictionCalculator.class), null, -1),
            Mockito.mock(OfferMappingActionService.class),
            Mockito.mock(ObjectMapper.class),
            new CategoryCachingServiceMock(),
            Mockito.mock(BuyPromoPriceRepository.class),
            Mockito.mock(OffersProcessingStatusService.class),
            Mockito.mock(OffersWebService.class),
            Mockito.mock(NeedContentStatusService.class),
            Mockito.mock(ImportFileService.class),
            processingTicketInfoService,
            Mockito.mock(BusinessSupplierService.class),
            offerBatchProcessor, offerDestinationCalculator, true,
            Mockito.mock(WarehouseServiceService.class),
            Mockito.mock(RecheckMappingService.class));
        controllerWithWhitesEnabled.bulkLoad(RequestKind.PROTO, null, null, null, false,
            Collections.singletonList(Offer.ProcessingStatus.IN_CLASSIFICATION), null,
            null, null, null, response);

        assertEquals(200, response.getStatus());
        ArgumentCaptor<OffersFilter> requestCaptor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repository, times(1))
            .findOffers(requestCaptor.capture(), any(Consumer.class));
        Assertions.assertThat(requestCaptor.getValue().getOfferDestination()).isNull();
        Assertions.assertThat(requestCaptor.getValue().getCriterias())
            .withFailMessage("Could not find a criteria that matches white or blue offer destination")
            .anyMatch(criteria -> criteria.matches(new Offer().setOfferDestination(Offer.MappingDestination.WHITE))
                && criteria.matches(new Offer().setOfferDestination(Offer.MappingDestination.BLUE)));
    }

    @Test
    public void testBulkLoadWithProcessingStatuses() throws Exception {
        List<Offer.ProcessingStatus> statusList = Arrays.asList(Offer.ProcessingStatus.IN_RE_SORT);
        controller.bulkLoad(RequestKind.JSON, null, null, null, null, statusList, null, null, false, null, response);

        assertEquals(200, response.getStatus());
        ArgumentCaptor<OffersFilter> requestCaptor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repository, times(1))
            .findOffers(requestCaptor.capture(), any(Consumer.class));
        Assertions.assertThat(requestCaptor.getValue().getProcessingStatuses())
            .containsExactlyInAnyOrderElementsOf(statusList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBulkLoadWithInProcessOnlyAndProcessingStatuses() throws Exception {
        List<Offer.ProcessingStatus> statusList = Arrays.asList(Offer.ProcessingStatus.IN_RE_SORT);
        controller.bulkLoad(RequestKind.JSON, null, null, null, true, statusList, null, null, null, null, response);
    }

    @Test
    public void testBulkLoadWithDeadlineFlag() throws Exception {
        controller.bulkLoad(RequestKind.JSON, null, null, null, true, null, null, true, false, null, response);

        assertEquals(200, response.getStatus());
        ArgumentCaptor<OffersFilter> requestCaptor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repository, times(1))
            .findOffers(requestCaptor.capture(), any(Consumer.class));
        Assertions.assertThat(requestCaptor.getValue().getCriterias())
            .hasAtLeastOneElementOfType(HasDeadlineOrOldProcessingStatusCriteria.class);
    }

    @Test
    public void testBulkLoadPrioritiesSort() throws Exception {
        controller.bulkLoad(RequestKind.JSON, null, 10, null, true, null, null, true, true, null, response);

        assertEquals(200, response.getStatus());
        ArgumentCaptor<OffersFilter> requestCaptor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repository, times(1))
            .findOffers(requestCaptor.capture(), any(Consumer.class));
        OffersFilter offersFilters = requestCaptor.getValue();

        Assertions.assertThat(offersFilters.getOrders()).
            containsExactly(
                OffersFilter.Order.of(OffersFilter.Field.TICKET_DEADLINE,
                    OffersFilter.OrderType.DESC, true),
                OffersFilter.Order.of(OffersFilter.Field.DATE_FROM_PROCESSING_STATUS_MODIFIED),
                OffersFilter.Order.of(OffersFilter.Field.CATEGORY_ID),
                OffersFilter.Order.of(OffersFilter.Field.SUPPLIER_ID),
                OffersFilter.Order.of(OffersFilter.Field.ID));

        Assertions.assertThat(offersFilters.getLimit()).isEqualTo(10);
    }

    @Test
    public void testBulkLoadSortWithMatchedToModel() throws Exception {
        controller.bulkLoad(RequestKind.JSON, null, 10, null, true, null, null, true, true, true, response);

        assertEquals(200, response.getStatus());
        ArgumentCaptor<OffersFilter> requestCaptor = ArgumentCaptor.forClass(OffersFilter.class);
        Mockito.verify(repository, times(1))
            .findOffers(requestCaptor.capture(), any(Consumer.class));
        OffersFilter offersFilters = requestCaptor.getValue();

        Assertions.assertThat(offersFilters.getOrders()).
            containsExactly(OffersFilter.Order.of(OffersFilter.Field.MODEL_ID, OffersFilter.OrderType.ASC, true),
                OffersFilter.Order.of(OffersFilter.Field.TICKET_DEADLINE, OffersFilter.OrderType.DESC, true),
                OffersFilter.Order.of(OffersFilter.Field.DATE_FROM_PROCESSING_STATUS_MODIFIED),
                OffersFilter.Order.of(OffersFilter.Field.CATEGORY_ID),
                OffersFilter.Order.of(OffersFilter.Field.SUPPLIER_ID),
                OffersFilter.Order.of(OffersFilter.Field.ID));

        Assertions.assertThat(offersFilters.getLimit()).isEqualTo(10);
    }

    @Test
    public void testOverridenDeadline() {
        List<Offer> offers = repository.findOffers(new OffersFilter()
            .setProcessingStatuses(Offer.ProcessingStatus.IN_MODERATION));

        LocalDate ticketDeadline = LocalDate.now();
        ProcessingTicketInfo ticketInfo = processingTicketInfoService.createNewNoTicket(
            Offer.ProcessingStatus.IN_MODERATION,
            offers
        );
        repository.updateOffers(offers);
        ticketInfo.setDeadline(ticketDeadline);
        processingTicketInfoService.update(ticketInfo);

        DataPage<DisplayOffer> page = controller.list(
            new OffersWebFilter()
                .setProcessingStatus(Offer.ProcessingStatus.IN_MODERATION),
            OffsetFilter.firstPage());

        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getTicketDeadline)
            .containsExactly(ticketDeadline, ticketDeadline, ticketDeadline);

        Assertions.assertThat(controller.getOfferById(
            offers.get(0).getId(), offers.get(0).getServiceOffersSuppliers().get(0)
        )
            .getTicketDeadline())
            .isEqualTo(ticketDeadline);
    }

    @Test
    public void testDefaultDeadline() {
        Offer offer = repository.getOfferById(1);
        Assertions.assertThat(offer.getTicketDeadline()).isNull();
        LocalDate autoDeadline = YangPrioritiesUtil.countDefaultDeadline(offer);
        Assertions.assertThat(controller.getOfferById(offer.getId(), offer.getServiceOffersSuppliers().get(0))
            .getTicketDeadline())
            .isEqualTo(autoDeadline);

        DataPage<DisplayOffer> page = controller.list(
            new OffersWebFilter()
                .setOfferIds(Collections.singletonList(offer.getId())),
            OffsetFilter.firstPage());
        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getTicketDeadline)
            .containsExactly(autoDeadline);
    }

    @Test
    public void testGetAnyOfferByIdFiltersNotBlueSuppliers() {
        int supplierId = 2001;
        int otherSupplierId = 2002;

        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(2010, "dsbs").setType(MbocSupplierType.DSBS).setBusinessId(2000),
            new Supplier(supplierId, "biz child").setBusinessId(2000),
            new Supplier(otherSupplierId, "biz child").setBusinessId(2000).setType(MbocSupplierType.MARKET_SHOP)
        );

        long offerId = 10001;
        offerRepository.insertOffers(
            Offer.builder()
                .id(offerId)
                .title("1")
                .isOfferContentPresent(true)
                .shopCategoryName("c")
                .offerContent(OfferContent.builder().build())
                .shopSku("bizsku0")
                .businessId(2000)
                .serviceOffers(List.of(
                    new Offer.ServiceOffer(
                        otherSupplierId,
                        MbocSupplierType.MARKET_SHOP,
                        Offer.AcceptanceStatus.OK),
                    new Offer.ServiceOffer(
                        supplierId,
                        MbocSupplierType.THIRD_PARTY,
                        Offer.AcceptanceStatus.OK)))
                .build()
        );

        DisplayOffer found = controller.getAnyOfferById(offerId);
        Assert.assertEquals(offerId, found.getId());
        Assert.assertEquals(supplierId, (int) found.getSupplierId());
    }

    @Test
    public void testFindOnBusiness() {
        prepareForBusiness();
        DataPage<DisplayOffer> page = controller.list(
            new OffersWebFilter().setSupplierId(LINKED_SUPPLIER_ID),
            OffsetFilter.firstPage());
        Assertions.assertThat(page.getItems())
            .extracting(DisplayOffer::getId)
            .containsExactly(10001L);
    }

    private void prepareForBusiness() {
        supplierRepository.insertBatch(
            new Supplier(2000, "biz").setType(MbocSupplierType.BUSINESS),
            new Supplier(LINKED_SUPPLIER_ID, "biz child").setBusinessId(2000),
            new Supplier(OTHER_LINKED_SUPPLIER_ID, "biz child").setBusinessId(2000)
        );

        offerRepository.insertOffers(
            Offer.builder()
                .id(10001)
                .title("1")
                .shopCategoryName("c")
                .isOfferContentPresent(true)
                .offerContent(OfferContent.initEmptyContent())
                .shopSku("bizsku0")
                .businessId(2000)
                .serviceOffers(List.of(
                    new Offer.ServiceOffer(
                        LINKED_SUPPLIER_ID,
                        MbocSupplierType.THIRD_PARTY,
                        Offer.AcceptanceStatus.NEW),
                    new Offer.ServiceOffer(
                        OTHER_LINKED_SUPPLIER_ID,
                        MbocSupplierType.THIRD_PARTY,
                        Offer.AcceptanceStatus.NEW)))
                .build(),
            Offer.builder()
                .id(10002)
                .title("2")
                .shopCategoryName("c")
                .isOfferContentPresent(true)
                .offerContent(OfferContent.initEmptyContent())
                .shopSku("bizsku2")
                .businessId(2000)
                .serviceOffers(List.of(
                    new Offer.ServiceOffer(
                        OTHER_LINKED_SUPPLIER_ID,
                        MbocSupplierType.THIRD_PARTY,
                        Offer.AcceptanceStatus.NEW)))
                .build()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleProcessingStatuses() throws Exception {
        controller.bulkLoad(RequestKind.JSON, null, null, null, false,
            List.of(Offer.ProcessingStatus.IN_CLASSIFICATION, Offer.ProcessingStatus.IN_PROCESS), null,
            null, null, null, response);
    }

    @Test
    public void shouldFillDisplayOffersNeedSort() {
        var sampleOffers = offerRepository.findAll().stream()
            .limit(3)
            .collect(Collectors.toList());

        var offerWithNeedSortActive1 = sampleOffers.get(0);
        var offerWithNeedSortNotActive2 = sampleOffers.get(1);
        var offerWithNeedSortNotExists3 = sampleOffers.get(2);

        var activeSupplierOption1 = WarehouseService.builder()
            .supplierId(offerWithNeedSortActive1.getServiceOffers().get(0).getSupplierId())
            .shopSku(offerWithNeedSortActive1.getShopSku())
            .needSort(true)
            .build();

        var notActiveSupplierOption2 = WarehouseService.builder()
            .supplierId(offerWithNeedSortNotActive2.getServiceOffers().get(0).getSupplierId())
            .shopSku(offerWithNeedSortNotActive2.getShopSku())
            .needSort(false)
            .build();

        warehouseServiceRepository.saveOrUpdateAll(List.of(activeSupplierOption1, notActiveSupplierOption2));

        var displayOffers = controller.list(
            new OffersWebFilter().setOfferIds(List.of(
                offerWithNeedSortActive1.getId(),
                offerWithNeedSortNotActive2.getId(),
                offerWithNeedSortNotExists3.getId())
            ),
            OffsetFilter.firstPage()
        )
            .getItems();

        assertEquals(3, displayOffers.size());

        var offerIdToDisplayOfferMap = displayOffers.stream()
            .collect(Collectors.toMap(DisplayOffer::getBusinessOfferId, Function.identity()));

        var resultActiveNeedSort = offerIdToDisplayOfferMap.get(offerWithNeedSortActive1.getId());
        var resultNotActiveNeedSort = offerIdToDisplayOfferMap.get(offerWithNeedSortNotActive2.getId());
        var resultNotExistActiveNeedSort = offerIdToDisplayOfferMap.get(offerWithNeedSortNotExists3.getId());

        assertEquals(Boolean.TRUE, resultActiveNeedSort.getNeedSort());
        assertEquals(Boolean.FALSE, resultNotActiveNeedSort.getNeedSort());
        assertEquals(Boolean.FALSE, resultNotExistActiveNeedSort.getNeedSort());
    }

    @Test
    public void shouldSaveWarehouseServiceChanges() {
        var warehouseServiceForUpdate = WarehouseService.builder()
            .supplierId(1)
            .shopSku("shopsku1")
            .needSort(true)
            .build();

        var warehouseServiceUpdateRequest1 = OffersController.WarehouseServiceUpdate.builder()
            .supplierId(warehouseServiceForUpdate.getSupplierId())
            .shopSku(warehouseServiceForUpdate.getShopSku())
            .needSort(!warehouseServiceForUpdate.isNeedSort())
            .build();
        var shopSkuKey1 = new ShopSkuKey(
            warehouseServiceUpdateRequest1.getSupplierId(), warehouseServiceUpdateRequest1.getShopSku()
        );

        var newWarehouseServiceUpdateRequest2 = OffersController.WarehouseServiceUpdate.builder()
            .supplierId(2)
            .shopSku("shopsku2")
            .needSort(true)
            .build();
        var shopSkuKey2 = new ShopSkuKey(
            newWarehouseServiceUpdateRequest2.getSupplierId(), newWarehouseServiceUpdateRequest2.getShopSku()
        );

        when(warehouseServiceService.getAssortmentChildSskus(anyList(), anyBoolean()))
            .thenReturn(
                Map.of(
                    shopSkuKey1, new ChildSskuResult(shopSkuKey1, null, Set.of()),
                    shopSkuKey2, new ChildSskuResult(shopSkuKey2, null, Set.of())
                )
            );

        supplierRepository.insertBatch(
            OfferTestUtils.simpleSupplier(warehouseServiceForUpdate.getSupplierId()),
            OfferTestUtils.simpleSupplier(newWarehouseServiceUpdateRequest2.getSupplierId())
        );

        warehouseServiceRepository.saveOrUpdateAll(List.of(warehouseServiceForUpdate));

        var allOptionUpdateRequests = List.of(warehouseServiceUpdateRequest1, newWarehouseServiceUpdateRequest2);
        var result = controller.updateWarehouseServices(allOptionUpdateRequests, null);

        assertEquals("Услуги обновлены успешно", result.getMessage());

        var expectedWarehouseServices = allOptionUpdateRequests.stream()
            .map(warehouseServiceUpdate -> WarehouseService.builder()
                .supplierId(warehouseServiceUpdate.getSupplierId())
                .shopSku(warehouseServiceUpdate.getShopSku())
                .needSort(warehouseServiceUpdate.getNeedSort())
                .build()
            )
            .collect(Collectors.toList());

        Assertions.assertThat(warehouseServiceRepository.findAll())
            .containsAll(expectedWarehouseServices);
    }

    @Test
    public void shouldSaveOnlyValidWarehouseServices() {
        var shopSkuKey1 = new ShopSkuKey(1, "shopsku");
        var shopSkuKey2 = new ShopSkuKey(2, "");

        var validOfferWarehouseService = OffersController.WarehouseServiceUpdate.builder()
            .supplierId(shopSkuKey1.getSupplierId())
            .shopSku(shopSkuKey1.getShopSku())
            .needSort(true)
            .build();

        var invalidWarehouseServiceUpdateRequest2 = OffersController.WarehouseServiceUpdate.builder()
            .supplierId(shopSkuKey2.getSupplierId())
            .shopSku(shopSkuKey2.getShopSku())
            .needSort(true)
            .build();

        when(warehouseServiceService.getAssortmentChildSskus(anyList(), anyBoolean()))
            .thenReturn(
                Map.of(
                    shopSkuKey1, new ChildSskuResult(shopSkuKey1, null, Set.of("testChildSsku"))
                )
            );

        supplierRepository.insertBatch(
            OfferTestUtils.simpleSupplier(validOfferWarehouseService.getSupplierId()),
            OfferTestUtils.simpleSupplier(invalidWarehouseServiceUpdateRequest2.getSupplierId())
        );

        var allWarehouseServiceUpdateRequests = List.of(validOfferWarehouseService,
            invalidWarehouseServiceUpdateRequest2);
        var result = controller.updateWarehouseServices(allWarehouseServiceUpdateRequests, null);

        var warehouseServiceList = warehouseServiceRepository.findAll();
        assertEquals(result.getStatus(), Result.ResultStatus.SUCCESS);
        assertEquals(1, warehouseServiceList.size());

        var resultWarehouseService = warehouseServiceList.get(0);

        assertEquals(validOfferWarehouseService.getSupplierId().intValue(), resultWarehouseService.getSupplierId());
        assertEquals(validOfferWarehouseService.getShopSku(), resultWarehouseService.getShopSku());
        assertEquals(validOfferWarehouseService.getNeedSort(), resultWarehouseService.isNeedSort());
    }

    @Test
    public void shouldSaveOrUpdateFilteredWarehouseServices() {
        var warehouseService1 = WarehouseService.builder()
            .supplierId(43)
            .shopSku("ssku3")
            .needSort(false)
            .build();

        var newSupplierId = 45;
        var newShopSku = "qwerty123";
        var newShopSkuKey = new ShopSkuKey(newSupplierId, newShopSku);
        var neighborSupplierShopSkuKey = new ShopSkuKey(newShopSkuKey.getSupplierId(), "qwerty1234");

        warehouseServiceRepository.saveOrUpdateAll(List.of(warehouseService1));

        when(warehouseServiceService.getAssortmentChildSskus(anyList(), anyBoolean()))
            .thenReturn(
                Map.of(
                    warehouseService1.getShopSkuKey(), new ChildSskuResult(
                        warehouseService1.getShopSkuKey(), null, Set.of("testChildSsku1")
                    ),
                    newShopSkuKey, new ChildSskuResult(newShopSkuKey, null, Set.of("testChildSsku2"))
                )
            )
            .thenReturn(
                Map.of(
                    newShopSkuKey, new ChildSskuResult(newShopSkuKey, null, Set.of("testChildSsku2")),
                    neighborSupplierShopSkuKey, new ChildSskuResult(
                        neighborSupplierShopSkuKey, null, Set.of("testChildSsku3")
                    )
                )
            );

        OffersWebFilter filter =
            new OffersWebFilter().setSupplierId(warehouseService1.getSupplierId());
        OffersController.WarehouseServiceUpdateByFilter request = new OffersController.WarehouseServiceUpdateByFilter();
        request.setFilter(filter);
        request.setSupplierId(warehouseService1.getSupplierId());
        request.setNeedSort(true);

        var result1 = controller.updateWarehouseServicesByFilter(request);

        assertEquals("Услуги обновлены успешно", result1.getMessage());

        Assertions.assertThat(warehouseServiceRepository.findAll())
            .hasSize(1)
            .allMatch(WarehouseService::isNeedSort);

        var result2 = controller.updateWarehouseServicesByFilter(
            new OffersController.WarehouseServiceUpdateByFilter()
                .setFilter(filter.setSupplierId(newSupplierId))
                .setSupplierId(newSupplierId)
                .setNeedSort(false)
        );

        assertEquals("Услуги обновлены успешно", result2.getMessage());

        var updatedWarehouseService = warehouseServiceRepository
            .findByKey(warehouseService1.getSupplierId(), warehouseService1.getShopSku())
            .get();
        var savedWarehouseService = warehouseServiceRepository
            .findByKey(newSupplierId, newShopSku)
            .get();

        assertTrue(updatedWarehouseService.isNeedSort());
        assertFalse(savedWarehouseService.isNeedSort());
    }
}
