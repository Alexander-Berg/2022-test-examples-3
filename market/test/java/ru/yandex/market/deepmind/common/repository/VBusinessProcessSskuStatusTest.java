package ru.yandex.market.deepmind.common.repository;

import java.time.Instant;
import java.util.List;
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
import ru.yandex.market.deepmind.common.availability.ShopSkuWKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.VBusinessProcessSskuStatus;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Header;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.AlmostDeadstockMeta;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.BackToPendingMeta;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.ToInactiveMeta;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.ToPendingMeta;
import ru.yandex.market.deepmind.common.services.tracker_strategy.AlmostDeadstockStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ApproveWithAssortmentCommitteeHelper;
import ru.yandex.market.deepmind.common.services.tracker_strategy.BackToPendingApproveStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.TicketResolution;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ToInactiveApproveStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ToPendingApproveStrategy;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoader;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables.V_BUSINESS_PROCESS_SSKU_STATUS;

@DbUnitDataBaseConfig({@DbUnitDataBaseConfig.Entry(
    name = "tableType",
    value = "TABLE,VIEW"
)})
public class VBusinessProcessSskuStatusTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    protected static final ToPendingMeta TO_PENDING_META = new ToPendingMeta();
    protected static final ToInactiveMeta TO_INACTIVE_META = new ToInactiveMeta();
    protected static final BackToPendingMeta BACK_TO_PENDING_META = new BackToPendingMeta();

    private ToInactiveApproveStrategy toInactiveStrategy;
    private ToPendingApproveStrategy toPendingStrategy;
    private BackToPendingApproveStrategy backToPendingStrategy;
    private AlmostDeadstockStrategy almostDeadstockStrategy;

    private SskuMskuStatusService sskuMskuStatusService;

    @Resource(name = "deepmindDsl")
    private DSLContext dsl;
    @Resource
    private AlmostDeadstockStatusRepository almostDeadstockRepository;
    @Resource
    private DeadstockStatusRepository deadstockStatusRepository;

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
        var exelComposer = new EnrichApproveToPendingExcelComposer(deepmindMskuRepository,
                deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class), Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class), Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class), Mockito.mock(EnrichApproveToPendingLoader.class),
            offersConverter, sskuStatusRepository
        );
        headerList = excelComposer.HEADERS;

        toPendingStrategy = Mockito.spy(new ToPendingApproveStrategy(session,
                deepmindSupplierRepository,
            approveWithACHelperSpy,
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST", exelComposer,
            transactionHelper,
            deepmindWarehouseRepository,
            deepmindRobotLogin));
        factory.registerStrategy(toPendingStrategy);

        backToPendingStrategy = Mockito.spy(new BackToPendingApproveStrategy(session,
                deepmindSupplierRepository,
            approveWithACHelperSpy,
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST", exelComposer,
            transactionHelper,
            deepmindWarehouseRepository,
            deepmindRobotLogin));
        factory.registerStrategy(backToPendingStrategy);

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

        TO_INACTIVE_META.setAuthor("test_login");
        TO_INACTIVE_META.setDescription("some description");
        TO_PENDING_META.setAuthor("test_login");
        TO_PENDING_META.setDescription("some description");
        BACK_TO_PENDING_META.setAuthor("test_login");
        BACK_TO_PENDING_META.setDescription("some description");
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
        }).when(toInactiveStrategy).process(Mockito.any());

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
                createData(111, "shop-sku-111", ACTIVE, INACTIVE, ticket, toInactiveStrategy.getType()),
                createData(222, "shop-sku-222", PENDING, INACTIVE, ticket, toInactiveStrategy.getType())
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
        }).when(toInactiveStrategy).process(Mockito.any());

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
                createData(111, "shop-sku-111", ACTIVE, INACTIVE, ticket, toInactiveStrategy.getType())
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
    public void multipleTicketAndStrategyTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null),
            sskuStatus(333, "shop-sku-333", INACTIVE, "comment", null),
            sskuStatus(444, "shop-sku-444", DELISTED, "comment", null),
            sskuStatus(555, "shop-sku-555", INACTIVE_TMP, "comment", null)
        ));

        var ticket1 = factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        var ticket2 = factory.getFacade(backToPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            BACK_TO_PENDING_META);

        var ticket3 = factory.getFacade(toPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            TO_PENDING_META);

        var ticket4 = factory.getFacade(almostDeadstockStrategy.getType()).start(
            List.of(
                new ShopSkuWKey(1, "ssku1", 172),
                new ShopSkuWKey(2, "ssku2", 172)
            ),
            new AlmostDeadstockMeta().setAssignee("test_asignee")
        );

        executor.run();

        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            EnrichApproveToInactiveExcelComposer.HEADERS,
            ticket1);
        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket2);
        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket3);
        // second run process attached file
        executor.run();
        //third run, nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", ACTIVE, INACTIVE, ticket1, toInactiveStrategy.getType()),
                createData(222, "shop-sku-222", PENDING, INACTIVE, ticket1, toInactiveStrategy.getType()),
                createData(333, "shop-sku-333", INACTIVE, PENDING, ticket2, backToPendingStrategy.getType()),
                createData(444, "shop-sku-444", DELISTED, PENDING, ticket2, backToPendingStrategy.getType()),
                createData(555, "shop-sku-555", INACTIVE_TMP, PENDING, ticket3, toPendingStrategy.getType())
            );
    }


    @Test
    public void oldStyleAndNewStyleTicketStrategyTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null),
            sskuStatus(333, "shop-sku-333", INACTIVE, "comment", null),
            sskuStatus(444, "shop-sku-444", DELISTED, "comment", null),
            sskuStatus(555, "shop-sku-555", INACTIVE_TMP, "comment", null)
        ));

        var ticket1 = factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        // обнуляем keyMeta для ticket1, создавая ситуацию когда есть старые еще открытые тикеты без keyMeta
        var data = dataRepository.findByTicket(ticket1);
        data.stream().forEach(item -> item.setMeta(null));
        dataRepository.save(data);

        var ticket2 = factory.getFacade(backToPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            BACK_TO_PENDING_META);

        var ticket3 = factory.getFacade(toPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            TO_PENDING_META);

        var ticket4 = factory.getFacade(almostDeadstockStrategy.getType()).start(
            List.of(
                new ShopSkuWKey(1, "ssku1", 172),
                new ShopSkuWKey(2, "ssku2", 172)
            ),
            new AlmostDeadstockMeta().setAssignee("test_asignee")
        );

        executor.run();

        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            EnrichApproveToInactiveExcelComposer.HEADERS,
            ticket1);
        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket2);
        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket3);
        // second run process attached file
        executor.run();
        //third run, nothing to do
        executor.run();

        // тикеты созданные по старому образцу не попадут в выгрузку
        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(333, "shop-sku-333", INACTIVE, PENDING, ticket2, backToPendingStrategy.getType()),
                createData(444, "shop-sku-444", DELISTED, PENDING, ticket2, backToPendingStrategy.getType()),
                createData(555, "shop-sku-555", INACTIVE_TMP, PENDING, ticket3, toPendingStrategy.getType())
            );
    }

    @Test
    public void toPendingDifferentEndStatusTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", INACTIVE_TMP, "comment", null),
            sskuStatus(222, "shop-sku-222", INACTIVE_TMP, "comment", null),
            sskuStatus(333, "shop-sku-333", INACTIVE_TMP, "comment", null),
            sskuStatus(444, "shop-sku-444", INACTIVE_TMP, "comment", null),
            sskuStatus(555, "shop-sku-555", INACTIVE_TMP, "comment", null)
        ));

        var ticket = factory.getFacade(toPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")),
            TO_PENDING_META);

        // запускаем в первый раз
        executor.run();

        manualStepAttachExcelAndResolve(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket);
        // second run process attached file
        executor.run();
        //third run, nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", INACTIVE_TMP, PENDING, ticket, toPendingStrategy.getType()),
                createData(222, "shop-sku-222", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType()),
                createData(333, "shop-sku-333", INACTIVE_TMP, PENDING, ticket, toPendingStrategy.getType()),
                createData(444, "shop-sku-444", INACTIVE_TMP, PENDING, ticket, toPendingStrategy.getType()),
                createData(555, "shop-sku-555", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType())
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

        var ticket = factory.getFacade(toPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")),
            TO_PENDING_META);

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
                createData(111, "shop-sku-111", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType()),
                createData(222, "shop-sku-222", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType()),
                createData(333, "shop-sku-333", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType()),
                createData(444, "shop-sku-444", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType()),
                createData(555, "shop-sku-555", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType())
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

        var ticket = factory.getFacade(toPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")),
            TO_PENDING_META);

        // запускаем в первый раз
        executor.run();

        manualStepAttachExcelAndCheck(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket);
        // second run process attached file
        executor.run();
        //third run, nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", INACTIVE_TMP, PENDING, ticket, toPendingStrategy.getType()),
                createData(222, "shop-sku-222", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType()),
                createData(333, "shop-sku-333", INACTIVE_TMP, PENDING, ticket, toPendingStrategy.getType()),
                createData(444, "shop-sku-444", INACTIVE_TMP, PENDING, ticket, toPendingStrategy.getType()),
                createData(555, "shop-sku-555", INACTIVE_TMP, INACTIVE, ticket, toPendingStrategy.getType())
            );
    }


    @Test
    public void multipleTicketAndCheckStrategyTest() {
        sskuStatusRepository.save(List.of(
            sskuStatus(111, "shop-sku-111", ACTIVE, "comment", null),
            sskuStatus(222, "shop-sku-222", PENDING, "comment", null),
            sskuStatus(333, "shop-sku-333", INACTIVE, "comment", null),
            sskuStatus(444, "shop-sku-444", DELISTED, "comment", null),
            sskuStatus(555, "shop-sku-555", INACTIVE_TMP, "comment", null)
        ));

        var ticket1 = factory.getFacade(toInactiveStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            TO_INACTIVE_META);

        var ticket2 = factory.getFacade(backToPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            BACK_TO_PENDING_META);

        var ticket3 = factory.getFacade(toPendingStrategy.getType()).start(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            TO_PENDING_META);

        var ticket4 = factory.getFacade(almostDeadstockStrategy.getType()).start(
            List.of(
                new ShopSkuWKey(1, "ssku1", 172),
                new ShopSkuWKey(2, "ssku2", 172)
            ),
            new AlmostDeadstockMeta().setAssignee("test_asignee")
        );

        executor.run();

        manualStepAttachExcelAndCheck(
            List.of(
                new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
            EnrichApproveToInactiveExcelComposer.HEADERS,
            ticket1);
        manualStepAttachExcelAndCheck(
            List.of(
                new ServiceOfferKey(333, "shop-sku-333"),
                new ServiceOfferKey(444, "shop-sku-444")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket2);
        manualStepAttachExcelAndCheck(
            List.of(
                new ServiceOfferKey(555, "shop-sku-555")),
            EnrichApproveToPendingExcelComposer.HEADERS,
            ticket3);
        // second run process attached file
        executor.run();
        //third run, nothing to do
        executor.run();

        var view = dsl.selectFrom(V_BUSINESS_PROCESS_SSKU_STATUS).fetchInto(VBusinessProcessSskuStatus.class);
        Assertions.assertThat(view)
            .usingElementComparatorIgnoringFields("startTime", "endTime")
            .containsExactlyInAnyOrder(
                createData(111, "shop-sku-111", ACTIVE, INACTIVE, ticket1, toInactiveStrategy.getType()),
                createData(222, "shop-sku-222", PENDING, INACTIVE, ticket1, toInactiveStrategy.getType()),
                createData(333, "shop-sku-333", INACTIVE, PENDING, ticket2, backToPendingStrategy.getType()),
                createData(444, "shop-sku-444", DELISTED, PENDING, ticket2, backToPendingStrategy.getType()),
                createData(555, "shop-sku-555", INACTIVE_TMP, PENDING, ticket3, toPendingStrategy.getType())
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
        String strategyType
    ) {
        return new VBusinessProcessSskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setInitialStatus(initialStatus)
            .setEndStatus(endStatus)
            .setTicket(ticket)
            .setStrategyType(strategyType)
            .setUserLogin("test_login")
            .setDescription("some description");
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

    private void manualStepAttachExcelAndCheck(List<ServiceOfferKey> keys, List<Header> headers, String ticket) {
        SessionUtils.check(session, ticket);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFile(keys, headers), user);
    }
}
