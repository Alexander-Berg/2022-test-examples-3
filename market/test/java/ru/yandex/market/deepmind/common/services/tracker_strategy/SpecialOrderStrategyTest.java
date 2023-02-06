package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.openapi.ReplenishmentService;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.pojo.SskuStatusReason;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichSpecialOrderExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderData;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderKeyMeta;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderMeta;
import ru.yandex.market.deepmind.common.services.yt.EnrichApproveToPendingLoader;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFacade;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.replenishment.autoorder.openapi.client.ApiException;
import ru.yandex.market.replenishment.autoorder.openapi.client.api.SpecialOrderApi;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApprovalItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreatedDemandIdsResponseDTO;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderApprovalStatus;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderType;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.StarTrekTicketUpdateRequest;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.UNDER_CONSIDERATION;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.WAITING_FOR_ENTER;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SAMARA_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

public class SpecialOrderStrategyTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    private SskuMskuStatusService sskuMskuStatusService;
    private SpecialOrderStrategy strategySpy;
    private TrackerApproverFacade<ServiceOfferKey, SpecialOrderMeta, SpecialOrderKeyMeta> facade;
    private ApproveWithAssortmentCommitteeHelper approveWithACHelperSpy;
    private ReplenishmentService replenishmentServiceSpy;
    private EnrichSpecialOrderExcelComposer excelComposer;

    @Before
    public void setUp() {
        super.setUp();
        offersConverter.clearCache();
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);

        var approveWithACHelper = new ApproveWithAssortmentCommitteeHelper(session,
            economicMetricsRepository, transactionHelper);
        approveWithACHelperSpy = Mockito.spy(approveWithACHelper);
        excelComposer = new EnrichSpecialOrderExcelComposer(deepmindMskuRepository,
                deepmindSupplierRepository, Mockito.mock(GlobalVendorsCachingService.class),
            Mockito.mock(MasterDataHelperService.class), serviceOfferReplicaRepository, categoryCachingService,
            Mockito.mock(DeepmindCategoryManagerRepository.class),
            Mockito.mock(DeepmindCategoryTeamRepository.class),
            Mockito.mock(MskuInfoRepository.class),
            Mockito.mock(SeasonRepository.class), Mockito.mock(SeasonalMskuRepository.class),
            Mockito.mock(EnrichApproveToPendingLoader.class), offersConverter,
            sskuStatusRepository
        );
        headerList = EnrichSpecialOrderExcelComposer.HEADERS;
        var replenishmentClient = new ReplenishmentService(Mockito.mock(SpecialOrderApi.class));
        replenishmentServiceSpy = Mockito.spy(replenishmentClient);
        strategySpy = Mockito.spy(new SpecialOrderStrategy(session,
                deepmindSupplierRepository,
                approveWithACHelperSpy,
                sskuMskuStatusService,
                sskuStatusRepository,
                "TEST",
                excelComposer,
                replenishmentServiceSpy,
                transactionHelper,
                offersConverter,
                deepmindWarehouseRepository,
                "https://url-to-demand.ru/{demandId}",
                deepmindRobotLogin
            )
        );
        factory.registerStrategy(strategySpy);
        facade = factory.getFacade(strategySpy.getType());
    }

    @Test
    public void startStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, WAITING_FOR_ENTER.getLiteral(), null)
        );
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new SpecialOrderMeta().setAuthor("author1").setDescription("description1");
        var ticketKey = facade.start(shopSkuKeys, meta);
        Assertions.assertThat(ticketKey).isEqualTo("TEST-1");
        Assertions.assertThat(session.issues().get(ticketKey).getComponents())
            .extracting(v -> v.load().getName())
            .containsExactlyInAnyOrder("Спец.закупка", "1P");
        Assertions
            .assertThat(session.issues().get(ticketKey).getSummary())
            .contains("Заявка на спец. заказ");
        var description = session.issues().get(ticketKey).getDescription();
        Assertions
            .assertThat(description.get())
            .contains(meta.getDescription(), "Прошу согласовать спец. заказ.",
                "shop-sku-222", "shop-sku-333");
        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getComment)
            .containsExactlyInAnyOrder(
                "comment1",
                "comment2",
                SskuStatusReason.UNDER_CONSIDERATION.getLiteral());
    }

    @Test
    public void enrichStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null)
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new SpecialOrderMeta().setAuthor("author1").setDescription("description1");
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        5L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1006.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        TOMILINO_ID,
                        5L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        ROSTOV_ID,
                        5L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0),
                        1,
                        LocalDate.of(2022, 1, 12),
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, meta, keyMetaMap));
        facade.enrich(ticket);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(1);
        Assertions.assertThat(session.issues().getSummonees(ticket))
            .isEmpty(); // because dev environment
    }

    @Test
    public void simpleRun() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(100L, 200L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        5L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0),
                        1,
                        LocalDate.now().minusDays(1),
                        null))
                .addSpecialOrderData(
                    new SpecialOrderData(
                        TOMILINO_ID,
                        18L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        1,
                        LocalDate.now().minusDays(8),
                        null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new SpecialOrderMeta(), keyMetaMap));

        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void checkPendingAndActiveSuccess() {
        var argument = ArgumentCaptor.forClass(StarTrekTicketUpdateRequest.class);
        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(10L, 25L, 123L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(argument.capture());
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.PENDING, "comment2", null)
        );
        var keys = List.of(new ServiceOfferKey(111, "shop-sku-111"), new ServiceOfferKey(222, "shop-sku-222"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        5L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(100.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(keys, new SpecialOrderMeta(), keyMetaMap));

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions
            .assertThat(sskuStatusRepository.find(keys))
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.PENDING, OfferAvailability.ACTIVE);
        var starTrekTicketUpdateRequest = argument.getValue();
        Assertions
            .assertThat(starTrekTicketUpdateRequest.getSskuMap().getAccepted())
            .containsExactlyInAnyOrder(
                new ApprovalItem().ssku("000111.shop-sku-111").warehouseId(SOFINO_ID),
                new ApprovalItem().ssku("000222.shop-sku-222").warehouseId(SOFINO_ID)
            );
    }

    @Test
    public void simpleRunWithNewFile() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(2000L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        LocalDate.now(),
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new SpecialOrderMeta(), keyMetaMap));

        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFileWithLegend(List.of(new ServiceOfferKey(111, "shop-sku-111")),
                excelComposer.getStaticHeaderList()), user);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .hasSize(1)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, SpecialOrderStrategy.TYPE));
    }

    @Test
    public void checkEconomicMetrics() throws IOException {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO())
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        LocalDate.now(),
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new SpecialOrderMeta(), keyMetaMap));

        executor.run();

        var builder = createCorrectExcelFileBuilderWithLegend(List.of(new ServiceOfferKey(111, "shop-sku-111")),
            excelComposer.getStaticHeaderList());
        builder.setValue(2, Headers.PAYMENT_DELAY_KEY, 123);
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            builder.build(), user);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, SpecialOrderStrategy.TYPE));
        Assertions.assertThat(metrics.get(0).getData())
            .containsEntry(Headers.SHOP_SKU_KEY, "shop-sku-111")
            .containsEntry(Headers.SUPPLIER_ID_KEY, "111")
            .containsEntry(Headers.PAYMENT_DELAY_KEY, "123");
    }

    @Test
    public void simpleRunWithNewFileWithRsId() {
        var issueArgument = ArgumentCaptor.forClass(Issue.class);
        var stringFilenameArgument = ArgumentCaptor.forClass(String.class);
        var excelArgument = ArgumentCaptor.forClass(ExcelFile.class);
        var stringAuthorArgument = ArgumentCaptor.forClass(String.class);
        var stringTagArgument = ArgumentCaptor.forClass(String.class);

        Mockito.doCallRealMethod().when(approveWithACHelperSpy)
            .commentWithAttachment(issueArgument.capture(),
                stringFilenameArgument.capture(),
                excelArgument.capture(),
                stringAuthorArgument.capture(),
                stringTagArgument.capture());
        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(67L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null));

        var keys = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(keys, new SpecialOrderMeta(), keyMetaMap));

        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        DeepmindAssertions.assertThat(excelArgument.getValue())
            .containsValue(2, Headers.REAL_SSKU_KEY, "000111.shop-sku-111");
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions
            .assertThat(sskuStatusRepository.find(keys))
            .extracting(SskuStatus::getAvailability)
            .containsExactly(OfferAvailability.PENDING);
    }

    @Test
    public void simpleRunWithReopenStep() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(20L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null));

        AtomicBoolean toReopenCall = new AtomicBoolean();
        Mockito.doAnswer(invok -> {
            // first run is failed
            if (!toReopenCall.get()) {
                ProcessRequest<ServiceOfferKey, SpecialOrderMeta, ?> request = invok.getArgument(0);
                // переводим статус в решен (чтобы сэмулировать готовность тикета)

                session.transitions().execute(request.getTicket(), "resolve");
                // заполняем ошибки
                var meta = request.getMeta();
                meta.setParsingErrors(List.of(MbocErrors.get().invalidValue("a", "b")));

                var keyMetaMap = request.getKeyMetaMap();

                toReopenCall.set(true);
                return ProcessResponse.of(ProcessResponse.Status.NOT_OK, meta, keyMetaMap);
            } else {
                // on second call run real method
                return invok.callRealMethod();
            }
        }).when(strategySpy).process(any());

        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new SpecialOrderMeta(), keyMetaMap));

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        // запускаем в первый раз
        executor.run();
        Mockito.verify(strategySpy, Mockito.times(1)).reopen(any());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFile(List.of(new ServiceOfferKey(111, "shop-sku-111")),
                excelComposer.getStaticHeaderList()), user);

        // second run process and close
        executor.run();

        // third run nothing to do
        Mockito.clearInvocations(strategySpy);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verifyNoMoreInteractions(strategySpy);
    }

    @Test
    public void checkStepTest() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(13L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new SpecialOrderMeta().setAuthor("author1").setDescription("description1");
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        5L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1006.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        TOMILINO_ID,
                        5L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        ROSTOV_ID,
                        5L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0),
                        1,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, meta, keyMetaMap));

        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                excelComposer.getStaticHeaderList()), user);
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
            createCorrectExcelFile(List.of(new ServiceOfferKey(111, "shop-sku-111")),
                excelComposer.getStaticHeaderList()), user);
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
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE,
                OfferAvailability.ACTIVE
            );
    }

    @Test
    public void processStepTest() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(225L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(100.0),
                        10,
                        null,
                        "1123437")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));
        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                excelComposer.getStaticHeaderList()), user);
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(any());

        //second run: correct sskus
        SessionUtils.check(session, ticket);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only supplier 222,333
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(222, "12345.shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333")), excelComposer.getStaticHeaderList()), user);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(4)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());

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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepReplenishmentFailedWithCommentTest() {
        String errorText = "Для поставщика ООО Марвел КТ и склада Яндекс.Маркет (Новосибирск) отсутствуют";
        Mockito.doThrow(new ApiException(400, errorText))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0),
                        10,
                        null,
                        "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        50L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        1000L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));
        executor.run();
        //attach correct sskus
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        //approve only supplier 222,333
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333")
            ), excelComposer.getStaticHeaderList()),
            user);
        executor.run();
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().cannotProcessSpecialOrderTicket(errorText).toString());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isZero();

        Mockito.verify(strategySpy, Mockito.times(2)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());

        Mockito.doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(30L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(2)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(2)).process(any());
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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepReplenishmentFailedTest() {
        String errorText = "some error occurred";
        Mockito.doThrow(new ApiException(errorText))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0),
                        10,
                        null,
                        "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        50L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        1000L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));
        executor.run();
        //attach correct sskus
        SessionUtils.check(session, ticket);
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        //approve only supplier 222,333
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", firstAttachCreatedAt,
            createCorrectExcelFileWithLegend(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333")
            ), excelComposer.getStaticHeaderList()), user);
        executor.run();
        Assertions.assertThat(session.issues().getSummonees(ticket))
            .doesNotContain(MbocErrors.get().cannotProcessSpecialOrderTicket(errorText).toString());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isOne();
        Assertions.assertThat(facade.process(ticket).getErrorMessage()).contains(errorText);

        Mockito.verify(strategySpy, Mockito.times(2)).process(any());

        Mockito.doReturn(new CreatedDemandIdsResponseDTO())
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(3)).process(any());
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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepReplenishmentClientWithExceptionTest() {
        var argument = ArgumentCaptor.forClass(StarTrekTicketUpdateRequest.class);
        doThrow(new RuntimeException())
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(argument.capture());

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0),
                        10,
                        null,
                        "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        50L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        1000L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));
        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                excelComposer.getStaticHeaderList()), user);
        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());

        //second run: correct sskus
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only supplier 222,333
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333")
            ), excelComposer.getStaticHeaderList()),
            user);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Mockito.verify(strategySpy, Mockito.times(4)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(2)).process(any());

        var starTrekTicketUpdateRequest = argument.getValue();
        Assertions
            .assertThat(starTrekTicketUpdateRequest.getSskuMap().getAccepted())
            .contains(
                new ApprovalItem().ssku("000222.shop-sku-222").warehouseId(SOFINO_ID),
                new ApprovalItem().ssku("000333.shop-sku-333").warehouseId(SOFINO_ID)
            );
        Assertions
            .assertThat(starTrekTicketUpdateRequest.getSskuMap().getDeclined())
            .contains(
                new ApprovalItem().ssku("000111.shop-sku-111").warehouseId(SOFINO_ID),
                new ApprovalItem().ssku("000444.shop-sku-444").warehouseId(SOFINO_ID)
            );
        Assertions.assertThat(starTrekTicketUpdateRequest.getId()).isEqualTo("TEST-1");
        Assertions.assertThat(starTrekTicketUpdateRequest.getStatus()).isEqualTo(SpecialOrderApprovalStatus.FINISHED);

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepReplenishmentClientDoubleCallTest() {

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        LocalDate.now().minusDays(8),
                        "12389787263681")
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(100.0),
                        10,
                        null,
                        "897376712641")
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));


        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                excelComposer.getStaticHeaderList()), user);
        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());

        //second run: correct sskus

        ArgumentCaptor<StarTrekTicketUpdateRequest> argument =
            ArgumentCaptor.forClass(StarTrekTicketUpdateRequest.class);
        doThrow(new RuntimeException("Тест выкидывает исключение при вызове метода specialOrderRequestFinalize"))
            .doReturn(new CreatedDemandIdsResponseDTO().demandIds(List.of(10L)))
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(argument.capture());

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only supplier 222,333
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(333, "shop-sku-333")
            ), excelComposer.getStaticHeaderList()),
            user);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Mockito.verify(strategySpy, Mockito.times(4)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(2)).process(any());

        var starTrekTicketUpdateRequest = argument.getValue();
        Assertions
            .assertThat(starTrekTicketUpdateRequest.getSskuMap().getAccepted())
            .usingRecursiveFieldByFieldElementComparator()
            .contains(
                new ApprovalItem().ssku("000222.shop-sku-222").warehouseId(SOFINO_ID),
                new ApprovalItem().ssku("000333.shop-sku-333").warehouseId(SOFINO_ID)
            );
        Assertions
            .assertThat(starTrekTicketUpdateRequest.getSskuMap().getDeclined())
            .contains(
                new ApprovalItem().ssku("000111.shop-sku-111").warehouseId(SOFINO_ID),
                new ApprovalItem().ssku("000444.shop-sku-444").warehouseId(SOFINO_ID)
            );
        Assertions.assertThat(starTrekTicketUpdateRequest.getId()).isEqualTo("TEST-1");
        Assertions.assertThat(starTrekTicketUpdateRequest.getStatus()).isEqualTo(SpecialOrderApprovalStatus.FINISHED);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(4)).preprocess(any());
        Mockito.verify(strategySpy, Mockito.times(3)).process(any());
        Mockito.verify(replenishmentServiceSpy, Mockito.times(2)).specialOrderRequestFinalize(any());

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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepHeadersNotMatchTest() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(1, "a", OfferAvailability.DELISTED, "comment1", null)
        );

        var shopSkus = List.of(new ServiceOfferKey(1, "a"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(1, "a"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SAMARA_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        LocalDate.now().minusDays(3),
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new SpecialOrderMeta(), keyMetaMap));

        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createNotCorrectExcelFile(shopSkus), user);
        // запускаем в первый раз
        executor.run();
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().excelRowParseError(3, "Отсутствует колонка 'Shop sku' в файле").toString()
        );
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);
    }

    @Test
    public void processStepClosedWithoutResolutionTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0),
                        10,
                        null,
                        "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        50L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0),
                        1,
                        null,
                        null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        1000L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));

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
    public void closeWithWontDoWillChangeToInactiveOnlyNewOffers() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO())
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.DELISTED, "comment3", null)
        );
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );

        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).isEmpty();
        SessionUtils.close(session, ticket, TicketResolution.WONT_DO);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.INACTIVE, OfferAvailability.INACTIVE_TMP,
                OfferAvailability.DELISTED);
    }

    @Test
    public void closeWithWontFixBeforeEnrichWillChangeToInactiveTmp() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO())
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.DELISTED, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE, "comment5", null)
        );

        var ticket = facade.start(shopSkuKeys, new SpecialOrderMeta());

        // тикет отменен после создания
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.INACTIVE_TMP, OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP, OfferAvailability.DELISTED, OfferAvailability.INACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getComment)
            .containsExactlyInAnyOrder("comment1", WAITING_FOR_ENTER.getLiteral(), WAITING_FOR_ENTER.getLiteral(),
                "comment4", "comment5");
    }

    @Test
    public void closeWithWontFixAfterEnrichWillChangeToInactiveTmp() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO())
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.DELISTED, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE, "comment5", null)
        );
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        ROSTOV_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        LocalDate.now().minusDays(7),
                        "98778467623")
                ),
            new ServiceOfferKey(555, "shop-sku-555"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        90L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(100.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new SpecialOrderMeta(), keyMetaMap));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        // тикет отменен после обогащения
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).hasSize(1);

        // all to inactive_tmp
        var result = sskuStatusRepository.find(shopSkuKeys);
        Assertions
            .assertThat(result)
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.INACTIVE_TMP, OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP, OfferAvailability.DELISTED, OfferAvailability.INACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getComment)
            .containsExactlyInAnyOrder("comment1", WAITING_FOR_ENTER.getLiteral(), WAITING_FOR_ENTER.getLiteral(),
                "comment4", "comment5");
    }

    @Test
    public void closeWithWontFixAfterReopenAndEnrich() {
        Mockito.doReturn(new CreatedDemandIdsResponseDTO())
            .when(replenishmentServiceSpy).specialOrderRequestFinalize(any());
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1").setSupplierType(SupplierType.FIRST_PARTY));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(1, "a", OfferAvailability.INACTIVE_TMP, WAITING_FOR_ENTER.getLiteral(), null)
        );

        AtomicBoolean toReopenCall = new AtomicBoolean();
        Mockito.doAnswer(invoke -> {
            // first run is failed
            if (!toReopenCall.get()) {
                ProcessRequest<ServiceOfferKey, SpecialOrderMeta, ?> request = invoke.getArgument(0);
                // переводим статус в решен (чтобы сэмулировать готовность тикета)

                session.transitions().execute(request.getTicket(), "resolve");
                // заполняем ошибки
                var meta = request.getMeta();
                meta.setParsingErrors(List.of(MbocErrors.get().invalidValue("a", "b")));

                toReopenCall.set(true);
                return ProcessResponse.of(ProcessResponse.Status.NOT_OK, meta);
            } else {
                // on second call run real method
                return invoke.callRealMethod();
            }
        }).when(strategySpy).process(any());

        List<ServiceOfferKey> shopSkuList = List.of(new ServiceOfferKey(1, "a"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(1, "a"),
            new SpecialOrderKeyMeta()
                .addSpecialOrderData(
                    new SpecialOrderData(
                        SOFINO_ID,
                        500L,
                        SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0),
                        10,
                        null,
                        null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuList, new SpecialOrderMeta(), keyMetaMap));

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        // запускаем в первый раз
        executor.run();
        Mockito.verify(strategySpy, Mockito.times(1)).reopen(any());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFile(List.of(new ServiceOfferKey(1, "a")),
                excelComposer.getStaticHeaderList()), user);

        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        // second run process and close
        executor.run();

        // third run nothing to do
        Mockito.clearInvocations(strategySpy);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(sskuStatusRepository.find(shopSkuList).stream()
            .filter(sskuStatus -> (sskuStatus.getAvailability() != OfferAvailability.INACTIVE_TMP
                || !sskuStatus.getComment().equals(WAITING_FOR_ENTER.getLiteral())))).isEmpty();
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).hasSize(2);
        Mockito.verifyNoMoreInteractions(strategySpy);
    }
}
