package ru.yandex.market.deepmind.app.openapi;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.deepmind.app.model.BusinessProcess;
import ru.yandex.market.deepmind.app.model.SpecialOrderItem;
import ru.yandex.market.deepmind.app.model.SpecialOrderType;
import ru.yandex.market.deepmind.app.model.StartAndFinishForSpecialOrderRequest;
import ru.yandex.market.deepmind.app.model.StartBusinessProcessRequest;
import ru.yandex.market.deepmind.app.model.TicketInfo;
import ru.yandex.market.deepmind.app.openapi.exception.ApiResponseEntityExceptionHandler;
import ru.yandex.market.deepmind.app.services.SskuMskuStatusHelperServiceImpl;
import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.openapi.ReplenishmentService;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.BusinessProcessEconomicMetricsRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichSpecialOrderExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.KeyMetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.MetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderData;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderKeyMeta;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ApproveWithAssortmentCommitteeHelper;
import ru.yandex.market.deepmind.common.services.tracker_strategy.FromUserExcelComposerMock;
import ru.yandex.market.deepmind.common.services.tracker_strategy.SpecialOrderStrategyV2;
import ru.yandex.market.deepmind.common.services.yt.AssortmentResponsiblesLoader;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoader;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFacade;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.replenishment.autoorder.openapi.client.api.SpecialOrderApi;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderResponse;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApprovedSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.MessageDTO;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderCreateKey;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.deepmind.common.config.TrackerApproverConfig.DEEPMIND_USE_NEW_FLOW_STRATEGY_ENABLED;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderType.CURRENCY;
import static ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderType.LOT;
import static ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderType.NEW;

/**
 * Tests of {@link SpecialOrderApiController}.
 */
public class SpecialOrderApiControllerTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    private static final String SPECIAL_ORDER_FAST_URL = "/api/v1/tracker-approver/start-and-finish-for-special-order";
    private static final String SPECIAL_ORDER_LONG_URL = "/api/v1/tracker-approver/start-and-run-for-special-order";

    private SpecialOrderApiController controller;
    private MockMvc mockMvc;
    private TrackerApproverFacade<ServiceOfferKey, MetaV2, KeyMetaV2> specialOrderFacade;
    private SpecialOrderStrategyV2 strategy;
    private ReplenishmentService replenishmentServiceSpy;
    private FromUserExcelComposerMock excelComposer;
    private StorageKeyValueServiceMock storageKeyValueServiceMock;

    @Before
    public void setUp() {
        super.setUp();
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        offersConverter.clearCache();
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        var sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        var sskuMskuStatusHelperService = new SskuMskuStatusHelperServiceImpl(serviceOfferReplicaRepository,
            new BackgroundServiceMock(), sskuMskuStatusService, sskuMskuStatusValidationService, sskuStatusRepository,
            deepmindMskuRepository, mskuStatusRepository, transactionTemplate);

        // config tracker_approver
        excelComposer = new FromUserExcelComposerMock(
            deepmindMskuRepository,
            deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class),
            Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class),
            Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class),
            Mockito.mock(EnrichApproveToPendingLoader.class),
            offersConverter,
            sskuStatusRepository,
            true
        );
        headerList = EnrichSpecialOrderExcelComposer.HEADERS;
        var approveWithACHelper = new ApproveWithAssortmentCommitteeHelper(session,
            Mockito.mock(BusinessProcessEconomicMetricsRepository.class), transactionHelper);
        var approveWithACHelperSpy = Mockito.spy(approveWithACHelper);
        var service = new ReplenishmentService(Mockito.mock(SpecialOrderApi.class));

        replenishmentServiceSpy = Mockito.spy(service);
        strategy = new SpecialOrderStrategyV2(
            session,
            approveWithACHelperSpy,
            "TEST",
            excelComposer,
            transactionHelper,
            replenishmentServiceSpy,
            "https://url-to-demand.ru/{demandId}",
            deepmindSupplierRepository,
            sskuMskuStatusService,
            sskuStatusRepository,
            mskuStatusRepository,
            mskuInfoRepository,
            assortSskuRepository,
            offerRepository,
            deepmindWarehouseRepository,
            offersConverter,
            namedParameterJdbcTemplate,
            Mockito.mock(AssortmentResponsiblesLoader.class),
            deepmindRobotLogin,
            storageKeyValueService,
            "corefixTrackerField"
        );
        factory.registerStrategy(strategy);
        specialOrderFacade = factory.getFacade(strategy.getType(), strategy.getStrategyVersion());

        storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        storageKeyValueServiceMock.putValue(DEEPMIND_USE_NEW_FLOW_STRATEGY_ENABLED, true);
        controller = new SpecialOrderApiController(
            sskuMskuStatusHelperService,
            offersConverter,
            null,
            specialOrderFacade,
            storageKeyValueServiceMock);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiResponseEntityExceptionHandler()).build();
    }

    @Test
    public void apiV1TrackerApproverStartAndFinishForSpecialOrderPostTest() throws Exception {
        serviceOfferReplicaRepository.save(
            createOffer(111, "shopSku-111"),
            createOffer(222, "shopSku-222"),
            createOffer(333, "shopSku-333")
        );
        sskuStatusRepository.save(
            sskuStatus(111, "shopSku-111", OfferAvailability.ACTIVE),
            sskuStatus(222, "shopSku-222", OfferAvailability.INACTIVE),
            sskuStatus(333, "shopSku-333", OfferAvailability.INACTIVE_TMP, Instant.now())
        );

        var keys = List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        );
        var data = new StartAndFinishForSpecialOrderRequest().keys(keys).author("author1");
        var request = prepareSpecialOrderFastRequest(data);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andDo(MockMvcResultHandlers.print());
        var statuses = sskuStatusRepository.find(keys);
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsOnly(OfferAvailability.PENDING);
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getModifiedLogin)
            .containsOnly("author1");
    }

    @Test
    public void apiV1TrackerApproverStartAndFinishForSpecialOrderPost1PTest() throws Exception {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            createOffer(1002, "shopSku-111")
        );
        sskuStatusRepository.save(
            sskuStatus(1002, "shopSku-111", OfferAvailability.INACTIVE)
        );
        var data = new StartAndFinishForSpecialOrderRequest()
            .keys(List.of(new ServiceOfferKey(465852, "001234.shopSku-111"))).author("author1");
        var request = prepareSpecialOrderFastRequest(data);
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andDo(MockMvcResultHandlers.print());

        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(1002, "shopSku-111")
        ));
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(OfferAvailability.PENDING);
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getModifiedLogin)
            .containsExactly("author1");
    }

    @Test
    public void apiV1TrackerApproverStartAndRunForSpecialOrderPostTest() throws Exception {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, Instant.now())
        );
        var data = new StartBusinessProcessRequest()
            .businessProcess(BusinessProcess.SPECIAL_ORDER)
            .author("author")
            .items(List.of(
                new SpecialOrderItem()
                    .supplierId(111)
                    .shopSku("shop-sku-111")
                    .warehouseId(SOFINO_ID)
                    .specialOrderType(SpecialOrderType.NEW)
                    .price(BigDecimal.TEN)
                    .quantity(5L)
                    .orderDate(LocalDate.now())
                    .account("1112222233345"),
                new SpecialOrderItem()
                    .supplierId(222)
                    .shopSku("shop-sku-222")
                    .warehouseId(SOFINO_ID)
                    .specialOrderType(SpecialOrderType.NEW)
                    .price(BigDecimal.TEN)
                    .quantity(5L)
                    .orderDate(LocalDate.now())
                    .account("1112222233345"),
                new SpecialOrderItem()
                    .supplierId(333)
                    .shopSku("shop-sku-333")
                    .warehouseId(SOFINO_ID)
                    .specialOrderType(SpecialOrderType.NEW)
                    .price(BigDecimal.TEN)
                    .quantity(5L)
                    .orderDate(LocalDate.now())
                    .account("1112222233345")
            ))
            .comment("comment1");
        var request = prepareSpecialOrderLongRequest(data);
        var result = mockMvc.perform(request)
            .andExpect(status().isOk())
            .andDo(MockMvcResultHandlers.print())
            .andReturn();


        var ticket = new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), TicketInfo.class)
            .getTicketName();
        Assertions
            .assertThat(ticket)
            .isEqualTo("TEST-1");

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L),
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                        .demandId(113154600L),
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                        .demandId(113154600L)
                )))
            .when(replenishmentServiceSpy).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333")
                    ))
            );

        executor.run();
        SessionUtils.check(session, ticket);
        Instant attachCreatedAt = Instant.now().plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", attachCreatedAt,
            createCorrectFile(List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333")
            ), SOFINO_ID),
            user
        );
        Mockito.doReturn(new MessageDTO())
            .when(replenishmentServiceSpy).specialOrderRequestDeclineRest(any());
        executor.run();
        Assertions.assertThat(specialOrderFacade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        ));
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsOnly(OfferAvailability.PENDING);
    }

    @Test
    public void apiV1TrackerApproverStartAndRunForSpecialOrderPost1PTest() throws Exception {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            createOffer(1002, "shopSku-111").setCategoryId(111L)
        );
        sskuStatusRepository.save(
            sskuStatus(1002, "shopSku-111", OfferAvailability.INACTIVE)
        );
        var data = new StartBusinessProcessRequest()
            .businessProcess(BusinessProcess.SPECIAL_ORDER)
            .author("author")
            .items(List.of(
                new SpecialOrderItem()
                    .supplierId(1002)
                    .shopSku("shopSku-111")
                    .warehouseId(TOMILINO_ID)
                    .specialOrderType(SpecialOrderType.NEW)
                    .price(BigDecimal.TEN)
                    .quantity(5L)
                    .orderDate(LocalDate.now())
                    .account("1112222233345")))
            .comment("comment1");
        var request = prepareSpecialOrderLongRequest(data);
        var result = mockMvc.perform(request)
            .andExpect(status().isOk())
            .andDo(MockMvcResultHandlers.print())
            .andReturn();
        var ticket = new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), TicketInfo.class)
            .getTicketName();
        Assertions
            .assertThat(ticket)
            .isEqualTo("TEST-1");

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("001234.shopSku-111"))
                        .demandId(113154600L)
                )))
            .when(replenishmentServiceSpy).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("001234.shopSku-111")
                    ))
            );

        executor.run();
        SessionUtils.check(session, ticket);
        Instant attachCreatedAt = Instant.now().plusSeconds(10);

        var sskus = List.of(new ServiceOfferKey(1002, "shopSku-111"));
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", attachCreatedAt,
            createCorrectFile(sskus, TOMILINO_ID), user);
        Mockito.doReturn(new MessageDTO())
            .when(replenishmentServiceSpy).specialOrderRequestDeclineRest(any());
        executor.run();
        Assertions.assertThat(specialOrderFacade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(1002, "shopSku-111")
        ));
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsOnly(OfferAvailability.PENDING);
    }

    @Test
    public void apiV1TrackerApproverStartAndRunForSpecialOrderShouldThrowException() throws Exception {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED)
        );
        var data = new StartBusinessProcessRequest()
            .businessProcess(BusinessProcess.SPECIAL_ORDER)
            .items(List.of(
                new SpecialOrderItem()
                    .supplierId(111)
                    .shopSku("shop-sku-111")
                    .warehouseId(SOFINO_ID)
                    .specialOrderType(SpecialOrderType.NEW)
                    .price(BigDecimal.TEN)
                    .quantity(5L)
                    .orderDate(LocalDate.now())
                    .account("1112222233345"),
                new SpecialOrderItem()
                    .supplierId(222)
                    .shopSku("shop-sku-222")
                    .warehouseId(SOFINO_ID)
                    .specialOrderType(SpecialOrderType.NEW)
                    .price(BigDecimal.TEN)
                    .quantity(5L)
                    .orderDate(LocalDate.now())
                    .account("1112222233345"),
                new SpecialOrderItem()
                    .supplierId(333)
                    .shopSku("shop-sku-333")
                    .warehouseId(SOFINO_ID)
                    .specialOrderType(SpecialOrderType.NEW)
                    .price(BigDecimal.TEN)
                    .quantity(5L)
                    .orderDate(LocalDate.now())
                    .account("1112222233345")
            ))
            .comment("comment1");
        var request = prepareSpecialOrderLongRequest(data);
        mockMvc.perform(request)
            .andExpect(status().isBadRequest())
            .andExpect(content().string("{\"code\":400,\"error\":\"BAD_REQUEST\",\"message\":\"Invalid sskus: " +
                "supplier_id: 222; shop_sku: shop-sku-222\\nsupplier_id: 333; shop_sku: shop-sku-333\"}"));
    }

    @Test
    public void createKeyMetaMapGroupedByKeyAllItemsInOneRegionTest() {
        var date = LocalDate.now();
        List<SpecialOrderItem> list = List.of(
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(TOMILINO_ID)
                .specialOrderType(SpecialOrderType.NEW)
                .price(BigDecimal.valueOf(1000))
                .quantity(800L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(SOFINO_ID)
                .specialOrderType(SpecialOrderType.LOT)
                .price(BigDecimal.valueOf(1000))
                .quantity(80L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(ROSTOV_ID)
                .specialOrderType(SpecialOrderType.CURRENCY)
                .price(BigDecimal.valueOf(1000))
                .quantity(8L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467")
        );
        var result = controller.createKeyMetaMapGroupedByKey(list);
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result).containsKey(new ServiceOfferKey(111, "shop_sku-111"));
        Assertions.assertThat(result.values())
            .usingRecursiveComparison()
            .isEqualTo(
                new SpecialOrderKeyMeta(List.of(
                    new SpecialOrderData(TOMILINO_ID, 880L, NEW,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(SOFINO_ID, 80L, LOT,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(MARSHRUT_ID, 8L, CURRENCY,
                        BigDecimal.valueOf(1000), 1, date, "12467")
                ),
                    null,
                    null));
    }

    @Test
    public void createKeyMetaMapGroupedByKeyAllItemsInDifferentRegionsTest() {
        var date = LocalDate.now();
        List<SpecialOrderItem> list = List.of(
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(TOMILINO_ID)
                .specialOrderType(SpecialOrderType.NEW)
                .price(BigDecimal.valueOf(1000))
                .quantity(800L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(SOFINO_ID)
                .specialOrderType(SpecialOrderType.LOT)
                .price(BigDecimal.valueOf(1000))
                .quantity(80L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(ROSTOV_ID)
                .specialOrderType(SpecialOrderType.CURRENCY)
                .price(BigDecimal.valueOf(1000))
                .quantity(8L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467")
        );
        var result = controller.createKeyMetaMapGroupedByKey(list);
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result).containsKey(new ServiceOfferKey(111, "shop_sku-111"));
        Assertions.assertThat(result.values())
            .usingRecursiveComparison()
            .isEqualTo(
                new SpecialOrderKeyMeta(List.of(
                    new SpecialOrderData(TOMILINO_ID, 880L, NEW,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(SOFINO_ID, 80L, LOT,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(MARSHRUT_ID, 8L, CURRENCY,
                        BigDecimal.valueOf(1000), 1, date, "12467")
                ),
                    null,
                    null));
    }

    @Test
    public void createKeyMetaMapGroupedByKeySomeItemsHaveNoQuantumTest() {
        var date = LocalDate.now();
        List<SpecialOrderItem> list = List.of(
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(TOMILINO_ID)
                .specialOrderType(SpecialOrderType.NEW)
                .price(BigDecimal.valueOf(1000))
                .quantity(8L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(TOMILINO_ID)
                .specialOrderType(SpecialOrderType.LOT)
                .price(BigDecimal.valueOf(1000))
                .quantity(8L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(ROSTOV_ID)
                .specialOrderType(SpecialOrderType.CURRENCY)
                .price(BigDecimal.valueOf(1000))
                .quantity(8L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467")
        );
        var result = controller.createKeyMetaMapGroupedByKey(list);
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result).containsKey(new ServiceOfferKey(111, "shop_sku-111"));
        Assertions.assertThat(result.values())
            .usingRecursiveComparison()
            .isEqualTo(
                new SpecialOrderKeyMeta(List.of(
                    new SpecialOrderData(TOMILINO_ID, 8L, NEW,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(SOFINO_ID, 8L, LOT,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(MARSHRUT_ID, 8L, CURRENCY,
                        BigDecimal.valueOf(1000), 1, date, "12467")
                ),
                    null,
                    null));
    }

    @Test
    public void createKeyMetaMapGroupedByKeyTest() {
        var date = LocalDate.now();
        List<SpecialOrderItem> list = List.of(
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(TOMILINO_ID)
                .specialOrderType(SpecialOrderType.NEW)
                .price(BigDecimal.valueOf(1000))
                .quantity(800L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(SOFINO_ID)
                .specialOrderType(SpecialOrderType.LOT)
                .price(BigDecimal.valueOf(1000))
                .quantity(80L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467"),
            new SpecialOrderItem()
                .supplierId(111)
                .shopSku("shop_sku-111")
                .warehouseId(ROSTOV_ID)
                .specialOrderType(SpecialOrderType.CURRENCY)
                .price(BigDecimal.valueOf(1000))
                .quantity(8L)
                .shipmentQuantum(1)
                .orderDate(date)
                .account("12467")
        );
        var result = controller.createKeyMetaMapGroupedByKey(list);
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result).containsKey(new ServiceOfferKey(111, "shop_sku-111"));
        Assertions.assertThat(result.values())
            .usingRecursiveComparison()
            .isEqualTo(
                new SpecialOrderKeyMeta(List.of(
                    new SpecialOrderData(TOMILINO_ID, 800L, NEW,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(SOFINO_ID, 80L, LOT,
                        BigDecimal.valueOf(1000), 1, date, "12467"),
                    new SpecialOrderData(MARSHRUT_ID, 8L, CURRENCY,
                        BigDecimal.valueOf(1000), 1, date, "12467")
                ),
                    null,
                    null));
    }

    private MockHttpServletRequestBuilder prepareSpecialOrderFastRequest(StartAndFinishForSpecialOrderRequest request)
        throws JsonProcessingException {
        var jsonContent = JsonMapper.DEFAULT_OBJECT_MAPPER
            .writeValueAsString(request);

        return MockMvcRequestBuilders.post(SPECIAL_ORDER_FAST_URL)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonContent);
    }

    private MockHttpServletRequestBuilder prepareSpecialOrderLongRequest(StartBusinessProcessRequest request)
        throws JsonProcessingException {
        var jsonContent = JsonMapper.DEFAULT_OBJECT_MAPPER
            .writeValueAsString(request);

        return MockMvcRequestBuilders.post(SPECIAL_ORDER_LONG_URL)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonContent);
    }

    private SskuStatus sskuStatus(int supId, String shopSku, OfferAvailability availability) {
        return new SskuStatus().setSupplierId(supId).setShopSku(shopSku)
            .setAvailability(availability)
            .setStatusStartAt(Instant.now());
    }

    private SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability availability,
                                  Instant statusFinishAt) {
        return sskuStatus(supplierId, shopSku, availability)
            .setComment("comment")
            .setStatusFinishAt(statusFinishAt)
            .setModifiedByUser(false);
    }

    private ServiceOfferReplica createOffer(int supplierId, String ssku) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(33L)
            .setSeqId(0L)
            .setMskuId(1L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ExcelFile createCorrectFile(List<ServiceOfferKey> sskus, long warehouseId) {
        var keyMetaMap = sskus.stream()
            .collect(Collectors.toMap(ssku -> ssku, ssku -> {
                return new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(
                            warehouseId,
                            10L,
                            null,
                            null,
                            2,
                            null,
                            null)
                    ));
            }));
        return excelComposer.processKeys(sskus, strategy.getNotCreatedSpecialOrderData(keyMetaMap));
    }
}
