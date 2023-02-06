package ru.yandex.market.deepmind.common.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.VBusinessProcessSskuStatus;
import ru.yandex.market.deepmind.common.openapi.ReplenishmentService;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.ExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Header;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.KeyMetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.MetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderData;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.ToInactiveMeta;
import ru.yandex.market.deepmind.common.services.tracker_strategy.AlmostDeadstockStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ApproveWithAssortmentCommitteeHelper;
import ru.yandex.market.deepmind.common.services.tracker_strategy.BackToPendingApproveStrategyV2;
import ru.yandex.market.deepmind.common.services.tracker_strategy.BaseApproveStrategyV2;
import ru.yandex.market.deepmind.common.services.tracker_strategy.FromUserExcelComposerMock;
import ru.yandex.market.deepmind.common.services.tracker_strategy.SpecialOrderStrategyV2;
import ru.yandex.market.deepmind.common.services.tracker_strategy.TicketResolution;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ToInactiveApproveStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ToPendingApproveStrategyV2;
import ru.yandex.market.deepmind.common.services.yt.AssortmentResponsiblesLoader;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoader;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoaderMock;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.replenishment.autoorder.openapi.client.api.SpecialOrderApi;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderResponse;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApprovedSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderResponse;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.DeclinedSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.MessageDTO;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderCreateKey;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderDateType;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderType;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables.V_BUSINESS_PROCESS_SSKU_STATUS;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;

@DbUnitDataBaseConfig({@DbUnitDataBaseConfig.Entry(
    name = "tableType",
    value = "TABLE,VIEW"
)})
@SuppressWarnings("checkstyle:LineLength")
public class VBusinessProcessSskuStatusV2Test extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    protected static final ToInactiveMeta TO_INACTIVE_META = new ToInactiveMeta();
    protected static final MetaV2 META_V2 = new MetaV2();

    private ToInactiveApproveStrategy toInactiveStrategy;
    private AlmostDeadstockStrategy almostDeadstockStrategy;

    private ToPendingApproveStrategyV2 toPendingApproveStrategyV2;
    private BackToPendingApproveStrategyV2 backToPendingApproveStrategyV2;
    private SpecialOrderStrategyV2 specialOrderStrategyV2;
    private ReplenishmentService replenishmentServiceSpy;
    @Resource
    private SupplierRepository deepmindSupplierRepository;

    private SskuMskuStatusService sskuMskuStatusService;

    @Resource(name = "deepmindDsl")
    private DSLContext dsl;
    @Resource
    private AlmostDeadstockStatusRepository almostDeadstockRepository;
    @Resource
    private DeadstockStatusRepository deadstockStatusRepository;
    @Resource
    private OffersConverter offersConverter;
    protected ExcelComposer fromUserExcelComposer;

    @Before
    public void setUp() {
        super.setUp();
        var approveWithACHelper = new ApproveWithAssortmentCommitteeHelper(session,
            Mockito.mock(BusinessProcessEconomicMetricsRepository.class), transactionHelper);
        var excelComposer = new EnrichApproveToInactiveExcelComposer(deepmindMskuRepository, deepmindSupplierRepository,
            serviceOfferReplicaRepository, mskuInfoRepository);
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);
        toInactiveStrategy = Mockito.spy(new ToInactiveApproveStrategy(
            session,
                deepmindSupplierRepository,
            approveWithACHelper,
            excelComposer,
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST",
            transactionHelper,
            deepmindRobotLogin,
            storageKeyValueService
        ));
        factory.registerStrategy(toInactiveStrategy);

        var approveWithACHelperSpy = Mockito.spy(approveWithACHelper);
        var enrichApproveToPendingExcelComposer = new EnrichApproveToPendingExcelComposer(deepmindMskuRepository,
                deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class), Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class), Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class), Mockito.mock(EnrichApproveToPendingLoader.class),
            offersConverter, sskuStatusRepository
        );
        headerList = excelComposer.HEADERS;

        almostDeadstockStrategy = new AlmostDeadstockStrategy(
            session,
            approveWithACHelper,
            "TEST",
            almostDeadstockRepository,
            deadstockStatusRepository,
            sskuStatusRepository,
            serviceOfferReplicaRepository,
            Mockito.mock(CategoryManagerTeamService.class),
                deepmindSupplierRepository,
            deepmindWarehouseRepository
        );
        factory.registerStrategy(almostDeadstockStrategy);

        var service = new ReplenishmentService(Mockito.mock(SpecialOrderApi.class));
        replenishmentServiceSpy = Mockito.spy(service);

        toPendingApproveStrategyV2 = Mockito.spy(new ToPendingApproveStrategyV2(session,
            approveWithACHelperSpy,
            "TEST",
            enrichApproveToPendingExcelComposer,
            transactionHelper,
            replenishmentServiceSpy,
            "demand1PLink",
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
        ));
        factory.registerStrategy(toPendingApproveStrategyV2);

        backToPendingApproveStrategyV2 = Mockito.spy(new BackToPendingApproveStrategyV2(session,
            approveWithACHelperSpy,
            "TEST",
            enrichApproveToPendingExcelComposer,
            transactionHelper,
            replenishmentServiceSpy,
            "demand1PLink",
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
        ));
        factory.registerStrategy(backToPendingApproveStrategyV2);

        specialOrderStrategyV2 = Mockito.spy(new SpecialOrderStrategyV2(session,
            approveWithACHelperSpy,
            "TEST",
            enrichApproveToPendingExcelComposer,
            transactionHelper,
            replenishmentServiceSpy,
            "demand1PLink",
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
        ));
        factory.registerStrategy(specialOrderStrategyV2);

        TO_INACTIVE_META.setAuthor("test_login");
        TO_INACTIVE_META.setDescription("some description");
        META_V2.setAuthor("test_login");
        META_V2.setDescription("some description");
        META_V2.setAuthor("test_login");
        META_V2.setDescription("some description");
        MasterDataHelperService masterDataHelperService = Mockito.mock(MasterDataHelperService.class);
        Mockito.when(masterDataHelperService.findSskuMasterData(any()))
            .thenReturn(List.of(
                new MasterData().setSupplierId(111).setShopSku("shop-sku-111").setMinShipment(1).setQuantumOfSupply(1),
                new MasterData().setSupplierId(222).setShopSku("shop-sku-222").setMinShipment(2).setQuantumOfSupply(2),
                new MasterData().setSupplierId(333).setShopSku("shop-sku-333").setMinShipment(3).setQuantumOfSupply(3),
                new MasterData().setSupplierId(444).setShopSku("shop-sku-444").setMinShipment(4).setQuantumOfSupply(4)
            ));
        var enrichApproveToPendingLoader = new EnrichApproveToPendingLoaderMock(getEnrichApproveToPendingYtInfo());
        fromUserExcelComposer = new FromUserExcelComposerMock(deepmindMskuRepository,
            deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            masterDataHelperService, serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class), Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class), Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class), enrichApproveToPendingLoader,
            offersConverter, sskuStatusRepository, false
        );
    }

    @Test
    public void toInactiveWithReopenAndApproveAllTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null)
        ));

        AtomicBoolean toReopenCall = new AtomicBoolean();
        Mockito.doAnswer(invok -> {
            // first run is failed
            if (!toReopenCall.get()) {
                ProcessRequest<ServiceOfferKey, ToInactiveMeta, ?> request = invok.getArgument(0);
                // переводим статус в решен (чтобы сэмулировать готовность тикета)
                session.transitions().execute(request.getTicket(), "resolve");

                // заполняем ошибки
                var meta = request.getMeta();
                meta.setParsingErrors(List.of(MbocErrors.get().invalidValue("a", "b")));

                toReopenCall.set(true);
                return ProcessResponse.of(ProcessResponse.Status.NOT_OK, meta, request.getKeyMetaMap());
            } else {
                // on second call run real method
                return invok.callRealMethod();
            }
        }).when(toInactiveStrategy).process(any());

        var ticket = factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        // запускаем в первый раз
        executor.run();

        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            EnrichApproveToInactiveExcelComposer.HEADERS,
            ticket);
        // second run: check (closed + resolved) & process & reopen
        executor.run();

        session.transitions().execute(ticket, "check");

        //third run: check & process
        executor.run();

        //forth run: nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", ACTIVE, INACTIVE, ticket, toInactiveStrategy.getType(), null),
                createData(222, "shop-sku-222", PENDING, INACTIVE, ticket, toInactiveStrategy.getType(), null)
            );
    }

    @Test
    public void toInactiveWithReopenAndApproveOneTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null)
        ));

        AtomicBoolean toReopenCall = new AtomicBoolean();
        Mockito.doAnswer(invok -> {
            // first run is failed
            if (!toReopenCall.get()) {
                ProcessRequest<ServiceOfferKey, ToInactiveMeta, ?> request = invok.getArgument(0);
                // переводим статус в решен (чтобы сэмулировать готовность тикета)
                session.transitions().execute(request.getTicket(), "resolve");

                // заполняем ошибки
                var meta = request.getMeta();
                meta.setParsingErrors(List.of(MbocErrors.get().invalidValue("a", "b")));

                toReopenCall.set(true);
                return ProcessResponse.of(ProcessResponse.Status.NOT_OK, meta, request.getKeyMetaMap());
            } else {
                // on second call run real method
                return invok.callRealMethod();
            }
        }).when(toInactiveStrategy).process(any());

        var ticket = factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        // запускаем в первый раз
        executor.run();

        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111")),
            EnrichApproveToInactiveExcelComposer.HEADERS,
            ticket);
        // second run: check (closed + resolved) & process & reopen
        executor.run();

        session.transitions().execute(ticket, "check");

        //third run: check & process
        executor.run();

        //forth run: nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", ACTIVE, INACTIVE, ticket, toInactiveStrategy.getType(), null)
            );
    }

    @Test
    public void toInactiveCancelTicketTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null)
        ));

        var ticket = factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        executor.run();
        // тикет отменен после обогащения
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view).isEmpty();
    }

    @Test
    public void toInactiveNotClosedTicketTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null)
        ));

        factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void multipleTicketAndStrategyTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null),
            sskuStatus(333, "shop-sku-333", INACTIVE, "comment", null),
            sskuStatus(444, "shop-sku-444", DELISTED, "comment", null),
            sskuStatus(555, "shop-sku-555", INACTIVE_TMP, "comment", null),
            sskuStatus(666, "shop-sku-666", ACTIVE, "comment", null),
            sskuStatus(777, "shop-sku-777", PENDING, "comment", null),
            sskuStatus(888, "shop-sku-888", INACTIVE_TMP, "comment", null)
        ));

        var ticket1 = factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        var ticket2 = factory.getFacade(backToPendingApproveStrategyV2.getType()).start(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            META_V2);

        var ticket3 = factory.getFacade(toPendingApproveStrategyV2.getType()).start(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            META_V2);

        var sskuForSpecialOrder = List.of(
            new ServiceOfferKey(666, "shop-sku-666"),
            new ServiceOfferKey(777, "shop-sku-777"),
            new ServiceOfferKey(888, "shop-sku-888"));
        var ticket4 = factory.getFacade(specialOrderStrategyV2.getType()).start(
            new StartRequest(sskuForSpecialOrder, META_V2, getKeyMetaMap(sskuForSpecialOrder)));

        executor.run();

        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            EnrichApproveToInactiveExcelComposer.HEADERS,
            ticket1);
        manualStepAttachExcelAndCheckV2(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            ticket2,
            backToPendingApproveStrategyV2);
        manualStepAttachExcelAndCheckV2(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            ticket3,
            toPendingApproveStrategyV2);
        manualStepAttachExcelAndCheckV2(
            List.of(
                new ServiceOfferKey(666, "shop-sku-666"),
                new ServiceOfferKey(777, "shop-sku-777"),
                new ServiceOfferKey(888, "shop-sku-888")),
            ticket4,
            specialOrderStrategyV2);

        // возвращается из реплена только 000333.shop-sku-333, поэтому у 000444.shop-sku-444 во вьюхе demandIdToWarehouseId - null
        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .declinedItems(List.of(
                        new DeclinedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                    )))
            .when(replenishmentServiceSpy).specialOrderRequestCreateAll(
                new CreateSpecialOrderRequest().ticketId(ticket2)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem().key(
                                new SpecialOrderCreateKey().ssku("000333.shop-sku-333").warehouseId(SOFINO_ID)
                            ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(1),
                        new CreateSpecialOrderItem().key(
                                new SpecialOrderCreateKey().ssku("000444.shop-sku-444").warehouseId(SOFINO_ID)
                            ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(2)
                    )));

        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                        .demandId(123L)
                    )))
            .when(replenishmentServiceSpy).specialOrderRequestCreateAny(
                new CreateSpecialOrderRequest().ticketId(ticket2)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem().key(
                                new SpecialOrderCreateKey().ssku("000333.shop-sku-333").warehouseId(SOFINO_ID)
                            ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(1),
                        new CreateSpecialOrderItem().key(
                                new SpecialOrderCreateKey().ssku("000444.shop-sku-444").warehouseId(SOFINO_ID)
                            ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(2)
                    )));

        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000555.shop-sku-555"))
                            .demandId(321L)
                    )))
            .when(replenishmentServiceSpy).specialOrderRequestCreateAll(
                new CreateSpecialOrderRequest().ticketId(ticket3)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem().key(
                                new SpecialOrderCreateKey().ssku("000555.shop-sku-555").warehouseId(SOFINO_ID)
                            ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(1)
                    )));

        Mockito.doReturn(
                new ApproveSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000666.shop-sku-666"))
                            .demandId(1L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000777.shop-sku-777"))
                            .demandId(2L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000888.shop-sku-888"))
                            .demandId(3L)
                    )))
            .when(replenishmentServiceSpy).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket4)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000666.shop-sku-666"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000777.shop-sku-777"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000888.shop-sku-888")
                    )));

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentServiceSpy).specialOrderRequestDeclineRest(any());
        // second run process attached file
        executor.run();
        SessionUtils.awaitsActivation(session, ticket2);
        // third run, nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", ACTIVE, INACTIVE, ticket1, toInactiveStrategy.getType(), null),
                createData(222, "shop-sku-222", PENDING, INACTIVE, ticket1, toInactiveStrategy.getType(), null),
                createData(333, "shop-sku-333", INACTIVE, PENDING, ticket2, backToPendingApproveStrategyV2.getType(), "{123: 172}"),
                createData(444, "shop-sku-444", DELISTED, PENDING, ticket2, backToPendingApproveStrategyV2.getType(), null),
                createData(555, "shop-sku-555", INACTIVE_TMP, PENDING, ticket3, toPendingApproveStrategyV2.getType(), "{321: 172}"),
                createData(666, "shop-sku-666", ACTIVE, ACTIVE, ticket4, specialOrderStrategyV2.getType(), "{1: 172}"),
                createData(777, "shop-sku-777", PENDING, PENDING, ticket4, specialOrderStrategyV2.getType(), "{2: 172}"),
                createData(888, "shop-sku-888", INACTIVE_TMP, PENDING, ticket4, specialOrderStrategyV2.getType(), "{3: 172}")
            );
    }

    @Test
    public void toPendingWontDoTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", INACTIVE_TMP, "comment", null),
            sskuStatus(222, "shop-sku-222", INACTIVE_TMP, "comment", null),
            sskuStatus(333, "shop-sku-333", INACTIVE_TMP, "comment", null),
            sskuStatus(444, "shop-sku-444", INACTIVE_TMP, "comment", null),
            sskuStatus(555, "shop-sku-555", INACTIVE_TMP, "comment", null)
        ));

        var ticket = factory.getFacade(toPendingApproveStrategyV2.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")),
            META_V2
           );

        // запускаем в первый раз
        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.WONT_DO);

        // second run process attached file
        executor.run();
        //third run, nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingRecursiveFieldByFieldElementComparator(recursiveComparison())
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", INACTIVE_TMP, INACTIVE, ticket, toPendingApproveStrategyV2.getType(), null),
                createData(222, "shop-sku-222", INACTIVE_TMP, INACTIVE, ticket, toPendingApproveStrategyV2.getType(), null),
                createData(333, "shop-sku-333", INACTIVE_TMP, INACTIVE, ticket, toPendingApproveStrategyV2.getType(), null),
                createData(444, "shop-sku-444", INACTIVE_TMP, INACTIVE, ticket, toPendingApproveStrategyV2.getType(), null),
                createData(555, "shop-sku-555", INACTIVE_TMP, INACTIVE, ticket, toPendingApproveStrategyV2.getType(), null)
            );
    }

    @Test
    public void toPendingDifferentEndStatusCheckStrategyTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", INACTIVE_TMP, "comment", null),
            sskuStatus(222, "shop-sku-222", INACTIVE_TMP, "comment", null),
            sskuStatus(333, "shop-sku-333", INACTIVE_TMP, "comment", null),
            sskuStatus(444, "shop-sku-444", INACTIVE_TMP, "comment", null),
            sskuStatus(555, "shop-sku-555", INACTIVE_TMP, "comment", null)
        ));

        var ticket = factory.getFacade(toPendingApproveStrategyV2.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")),
            META_V2
           );

        // запускаем в первый раз
        executor.run();

        manualStepAttachExcelAndCheckV2(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            ticket,
            toPendingApproveStrategyV2);

        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .demandId(1L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                            .demandId(2L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000444.shop-sku-444"))
                            .demandId(3L)
                    )))
            .when(replenishmentServiceSpy).specialOrderRequestCreateAll(any());
        // second run process attached file
        executor.run();
        //third run, nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", INACTIVE_TMP, PENDING, ticket, toPendingApproveStrategyV2.getType(), "{1: 172}"),
                createData(222, "shop-sku-222", INACTIVE_TMP, INACTIVE, ticket, toPendingApproveStrategyV2.getType(), null),
                createData(333, "shop-sku-333", INACTIVE_TMP, PENDING, ticket, toPendingApproveStrategyV2.getType(), "{2: 172}"),
                createData(444, "shop-sku-444", INACTIVE_TMP, PENDING, ticket, toPendingApproveStrategyV2.getType(), "{3: 172}"),
                createData(555, "shop-sku-555", INACTIVE_TMP, INACTIVE, ticket, toPendingApproveStrategyV2.getType(), null)
            );
    }

    @Test
    @DbUnitDataSet(
        dataSource = "deepmindDataSource",
        before = "VBusinessProcessSskuStatusTest.before.csv",
        after = "VBusinessProcessSskuStatusTest.after.csv")
    public void vBusinessProcessSskuStatusDemandIdToWarehouseIdTest() {

    }

    protected VBusinessProcessSskuStatus createData(
        Integer supplierId,
        String shopSku,
        OfferAvailability initialStatus,
        OfferAvailability endStatus,
        String ticket,
        String strategyType,
        String demandIdToWarehouseId
    ) {
        return new VBusinessProcessSskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setInitialStatus(initialStatus)
            .setEndStatus(endStatus)
            .setTicket(ticket)
            .setStrategyType(strategyType)
            .setUserLogin("test_login")
            .setDescription("some description")
            .setDemandidWarehouseid(demandIdToWarehouseId);
    }


    public RecursiveComparisonConfiguration recursiveComparison() {
        var configuration = new RecursiveComparisonConfiguration();
        configuration.ignoreFields("startTime", "endTime");
        return configuration;
    }

    private void manualStepAttachExcelAndResolve(List<ServiceOfferKey> keys, List<Header> headers, String ticket) {
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFile(keys, headers), user);
    }

    private void manualStepAttachExcelAndCheckV2(List<ServiceOfferKey> keys,
                                                 String ticket,
                                                 BaseApproveStrategyV2<MetaV2, KeyMetaV2> strategy) {
        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = getKeyMetaMap(keys);
        SessionUtils.check(session, ticket);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(keys, strategy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);
    }

    private Map<ServiceOfferKey, KeyMetaV2> getKeyMetaMap(List<ServiceOfferKey> keys) {
        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            keyMetaMap.put(
                keys.get(i),
                new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(
                            SOFINO_ID,
                            10L,
                            null,
                            null,
                            i + 1,
                            LocalDate.now(),
                            null)
                    )));
        }
        return keyMetaMap;
    }
}
