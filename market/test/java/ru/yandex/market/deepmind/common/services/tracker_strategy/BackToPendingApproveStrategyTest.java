package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.BackToPendingMeta;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.TrackerApproverValidationResult;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoader;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFacade;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;

import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.NO_PURCHASE_PRICE;

public class BackToPendingApproveStrategyTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    private SskuMskuStatusService sskuMskuStatusService;

    private BackToPendingApproveStrategy strategySpy;
    private TrackerApproverFacade<ServiceOfferKey, BackToPendingMeta, ?> facade;
    private ApproveWithAssortmentCommitteeHelper approveWithACHelperSpy;

    @Before
    public void setUp() {
        super.setUp();
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        var approveWithACHelper = new ApproveWithAssortmentCommitteeHelper(session,
            economicMetricsRepository, transactionHelper);
        approveWithACHelperSpy = Mockito.spy(approveWithACHelper);

        var exelComposer = new EnrichApproveToPendingExcelComposer(deepmindMskuRepository,
            deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class), Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class), Mockito.mock(SeasonRepository.class),
            Mockito.mock(SeasonalMskuRepository.class), Mockito.mock(EnrichApproveToPendingLoader.class),
            offersConverter, sskuStatusRepository
        );

        headerList = exelComposer.getStaticHeaderList();
        strategySpy = Mockito.spy(new BackToPendingApproveStrategy(session,
            deepmindSupplierRepository,
            approveWithACHelperSpy,
            sskuMskuStatusService,
            sskuStatusRepository,
            "TEST", exelComposer,
            transactionHelper,
            deepmindWarehouseRepository,
            deepmindRobotLogin));
        factory.registerStrategy(strategySpy);
        facade = factory.getFacade(strategySpy.getType());
    }

    @Test
    public void startStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null)
        );
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222")
        );
        var meta = new BackToPendingMeta().setAuthor("author1").setDescription("description1");
        var ticketKey = facade.start(shopSkuKeys, meta);
        Assertions.assertThat(ticketKey).isEqualTo("TEST-1");
        Assertions.assertThat(session.issues().get(ticketKey).getComponents())
            .extracting(v -> v.load().getName())
            .containsExactlyInAnyOrder("Возвращение ассортимента", "1P");
        Assertions
            .assertThat(session.issues().get(ticketKey).getSummary())
            .contains("Заявка на возвращение ассортимента");
        var description = session.issues().get(ticketKey).getDescription();
        Assertions
            .assertThat(description.get())
            .contains(meta.getDescription(), "Прошу согласовать возвращение ассортимента",
                "shop-sku-111", "shop-sku-222");
    }

    @Test
    public void enrichStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment2", null)
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222")
        );
        var meta = new BackToPendingMeta().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        facade.enrich(ticket);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(1);
        Assertions.assertThat(session.issues().getSummonees(ticket))
            .isEmpty(); // because dev environment
    }

    @Test
    public void simpleRun() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.ACTIVE, "comment1", null)
        );

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new BackToPendingMeta());

        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void simpleRunWithNewFile() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.ACTIVE, "comment1", null)
        );

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new BackToPendingMeta());

        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFileWithLegend(List.of(new ServiceOfferKey(1, "a")),
                headerList), user);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .hasSize(1)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, BackToPendingApproveStrategy.TYPE));
    }

    @Test
    public void checkEconomicMetrics() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1")
            .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000111"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.DELISTED, "comment1", null)
        );

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new BackToPendingMeta());

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        var builder = createCorrectExcelFileBuilderWithLegend(List.of(new ServiceOfferKey(1, "a")),
            headerList);
        builder.setValue(2, Headers.PAYMENT_DELAY_KEY, 123);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", Instant.now().plusSeconds(1),
            builder.build(), user);
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, BackToPendingApproveStrategy.TYPE));
        Assertions.assertThat(metrics.get(0).getData())
            .containsEntry(Headers.SHOP_SKU_KEY, "a")
            .containsEntry(Headers.SUPPLIER_ID_KEY, "1")
            .containsEntry(Headers.PAYMENT_DELAY_KEY, "123");
    }

    @Test
    public void simpleRunWithReopenStep() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.ACTIVE, "comment1", null)
        );

        AtomicBoolean toReopenCall = new AtomicBoolean();
        Mockito.doAnswer(invok -> {
            // first run is failed
            if (!toReopenCall.get()) {
                ProcessRequest<ServiceOfferKey, BackToPendingMeta, ?> request = invok.getArgument(0);
                // переводим статус в решен (чтобы сэмулировать готовность тикета)

                session.transitions().execute(request.getTicket(), "resolve");
                // заполняем ошибки
                var meta = request.getMeta();
                meta.setParsingErrors(List.of(MbocErrors.get().invalidValue("a", "b")));

                toReopenCall.set(true);
                return ProcessResponse.of(ProcessResponse.Status.NOT_OK, meta);
            } else {
                // on second call run real method
                return invok.callRealMethod();
            }
        }).when(strategySpy).process(Mockito.any());

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new BackToPendingMeta());

        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        // запускаем в первый раз
        executor.run();
        Mockito.verify(strategySpy, Mockito.times(1)).reopen(Mockito.any());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFile(List.of(new ServiceOfferKey(1, "a")),
                headerList), user);

        // second run process and close
        executor.run();

        // third run nothing to do
        Mockito.clearInvocations(strategySpy);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verifyNoMoreInteractions(strategySpy);
    }

    @Test
    public void processStepTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new BackToPendingMeta());

        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFileWithLegend(List.of(new ServiceOfferKey(2, "b")),
                headerList), user);
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(Mockito.any());

        //second run: correct sskus
        SessionUtils.check(session, ticket);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only supplier 222
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFileWithLegend(List.of(new ServiceOfferKey(222, "shop-sku-222")),
                headerList), user);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(4)).preprocess(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(Mockito.any());

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE);
    }

    @Test
    public void processStepWithInactiveTmpTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.INACTIVE_TMP, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null)
        );

        var ticket = facade.start(shopSkuKeys, new BackToPendingMeta());

        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                headerList), user);
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(Mockito.any());

        //second run: correct sskus
        SessionUtils.check(session, ticket);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only some suppliers
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), headerList),
            user
        );
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(4)).preprocess(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(Mockito.any());

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE,
                OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP
            );
    }

    @Test
    public void processStepHeadersNotMatchTest() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.DELISTED, "comment1", null)
        );

        List<ServiceOfferKey> keys = List.of(new ServiceOfferKey(1, "a"));
        var ticket = facade.start(keys, new BackToPendingMeta());

        executor.run();

        SessionUtils.check(session, ticket);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createNotCorrectExcelFile(keys), user);
        // запускаем в первый раз
        executor.run();
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().excelRowParseError(3, "Отсутствует колонка 'Shop sku' в файле").toString()
        );
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
    }

    @Test
    public void processStepBackwardCompatibilityTest() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.ACTIVE, "comment1", null)
        );

        List<ServiceOfferKey> keys = List.of(new ServiceOfferKey(1, "a"));
        var ticket = facade.start(keys, new BackToPendingMeta());

        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFile(keys, headerList), user);
        // запускаем в первый раз
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void checkStepTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.INACTIVE_TMP, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null)
        );

        var ticket = facade.start(shopSkuKeys, new BackToPendingMeta());

        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                headerList), user);
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(Mockito.any());

        //second run: wrong excel
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createNotCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            )),
            user
        );
        SessionUtils.check(session, ticket);


        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Mockito.verify(strategySpy, Mockito.times(5)).preprocess(Mockito.any());
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().excelRowParseError(3, "Отсутствует колонка 'Shop sku' в файле").toString()
        );

        //third run: correct excel data
        Instant thirdAttachCreatedAt = secondAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel3.xlsx", thirdAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), headerList),
            user
        );
        SessionUtils.check(session, ticket);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(6)).preprocess(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(Mockito.any());

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE,
                OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP
            );
    }

    @Test
    public void processStepClosedWithoutResolutionTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new BackToPendingMeta());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.NEW);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        // закрываем без резолюции
        SessionUtils.close(session, ticket);
        executor.run();
        // проверяем что после закрытия без резолюции тикет не уходит в CLOSED, а остается в ENRICHED
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
    }

    @Test
    public void closeWithWontDoWillChangeToPending() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new BackToPendingMeta());

        SessionUtils.close(session, ticket, TicketResolution.WONT_DO);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // nothing changed
        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
        Assertions.assertThat(sskuStatusRepository.findByKey(222, "shop-sku-222").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(333, "shop-sku-333").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void closeWithWontFixBeforeEnrichWillBeNoChanges() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new BackToPendingMeta());

        // тикет отменен после создания
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).isEmpty();

        // no changes performed
        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
        Assertions.assertThat(sskuStatusRepository.findByKey(222, "shop-sku-222").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(333, "shop-sku-333").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void closeWithWontFixAfterEnrichWillBeNoChanges() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new BackToPendingMeta());
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        // тикет отменен после обогащения
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).hasSize(1);

        // no changes performed
        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
        Assertions.assertThat(sskuStatusRepository.findByKey(222, "shop-sku-222").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(333, "shop-sku-333").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void validateSskuNotMatch() {
        String ticket = "TEST-1";
        String type = "test";
        ticketRepository.save(new TrackerApproverTicketRawStatus(ticket, type, TicketState.NEW));

        var sskuInRepo = List.of(
            new ServiceOfferKey(1, "a"),
            new ServiceOfferKey(2, "b"));

        var trackerApproverData = sskuInRepo.stream()
            .map(key -> new TrackerApproverRawData()
                .setTicket(ticket)
                .setKey(JsonWrapper.fromObject(key))
                .setType(type)
            ).collect(Collectors.toList());

        dataRepository.save(trackerApproverData);

        var sskuToTest = List.of(
            new ServiceOfferKey(1, "a"),
            new ServiceOfferKey(3, "c"));
        TrackerApproverValidationResult<ServiceOfferKey> result = strategySpy.validate(sskuToTest,
            sskuInRepo);
        Assertions.assertThat(result.getErrors().size()).isEqualTo(1);
        Assertions.assertThat(List.of(new ServiceOfferKey(1, "a"))).isEqualTo(result.getValidShopSkuKeys());
    }

    @Test
    public void validateSskuAllMatch() {
        String ticket = "TEST-1";
        String type = "test";
        ticketRepository.save(new TrackerApproverTicketRawStatus(ticket, type, TicketState.NEW));

        var sskuInRepo = List.of(
            new ServiceOfferKey(1, "a"),
            new ServiceOfferKey(2, "b"));

        var trackerApproverData = sskuInRepo.stream()
            .map(key -> new TrackerApproverRawData()
                .setTicket(ticket)
                .setKey(JsonWrapper.fromObject(key))
                .setType(type)
            ).collect(Collectors.toList());

        dataRepository.save(trackerApproverData);

        var sskuToTest = List.of(
            new ServiceOfferKey(1, "a"),
            new ServiceOfferKey(2, "b"));

        TrackerApproverValidationResult<ServiceOfferKey> result = strategySpy.validate(sskuToTest, sskuInRepo);
        Assert.assertEquals("Have no error", 0, result.getErrors().size());
        Assert.assertEquals("Have two valid entry", sskuToTest, result.getValidShopSkuKeys());
    }

    @Test // DEEPMIND-2638
    public void saveEconomicMetricsEvenIfColumnNameIsEmpty() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.ACTIVE, "comment1", null)
        );

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new BackToPendingMeta());

        executor.run();

        // Нам надо создать файл, у которого будет +1 новая пустая колонка
        var line = 2;
        var headerSize = headerList.size();
        var excelFile = createCorrectExcelFileWithLegend(List.of(new ServiceOfferKey(1, "a")),
            headerList);
        // Проверяем, что line - эта та линия, на которой данные находятся. Это важно.
        DeepmindAssertions.assertThat(excelFile)
            .containsValue(line, Headers.SHOP_SKU_KEY, "a")
            .hasHeaderSize(headerSize);
        // Добавляем значение при пустой колонке
        excelFile = excelFile.toBuilder()
            .setValue(line, headerSize + 1, "3.1415926535")
            .build();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            excelFile, user);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .hasSize(1)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, BackToPendingApproveStrategy.TYPE));
    }
}
