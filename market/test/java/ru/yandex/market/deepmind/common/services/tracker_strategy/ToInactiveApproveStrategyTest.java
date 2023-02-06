package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
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
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToInactiveExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Header;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.ToInactiveMeta;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.TrackerApproverValidationResult;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFacade;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.utils.SecurityUtil;

import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.NO_PURCHASE_PRICE;

/**
 * Tests of {@link ToInactiveApproveStrategy}.
 */
public class ToInactiveApproveStrategyTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    private ToInactiveApproveStrategy strategySpy;
    private TrackerApproverFacade<ServiceOfferKey, ToInactiveMeta, ?> facade;
    private SskuMskuStatusService sskuMskuStatusService;

    @Before
    public void setUp() {
        super.setUp();

        var approveWithACHelper = new ApproveWithAssortmentCommitteeHelper(session,
            economicMetricsRepository, transactionHelper);
        var excelComposer = new EnrichApproveToInactiveExcelComposer(deepmindMskuRepository, deepmindSupplierRepository,
            serviceOfferReplicaRepository, mskuInfoRepository);
        headerList = EnrichApproveToInactiveExcelComposer.HEADERS;
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);
        strategySpy = Mockito.spy(new ToInactiveApproveStrategy(
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

        factory.registerStrategy(strategySpy);
        facade = factory.getFacade(strategySpy.getType());
    }

    @Test
    public void startStepTest() {
        List<ServiceOfferKey> keys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var attachment1 = session.attachments().upload("a.pdf", new ByteArrayInputStream(new byte[]{1}));
        var attachment2 = session.attachments().upload("b.pdf", new ByteArrayInputStream(new byte[]{2}));

        var meta = new ToInactiveMeta()
            .setAuthor("author1")
            .setDescription("description1")
            .setAttachmentIds(List.of(attachment1.getId(), attachment2.getId()));

        var ticketKey = facade.start(keys, meta);
        Assertions.assertThat(ticketKey).isEqualTo("TEST-1");

        Assertions.assertThat(session.attachments().getAll(ticketKey)).toIterable().hasSize(2);
        Assertions.assertThat(session.issues().get(ticketKey).getComponents())
            .extracting(v -> v.load().getName())
            .containsExactlyInAnyOrder("Вывод ассортимента", "1P");
        Assertions.assertThat(session.issues().get(ticketKey).getSummary())
            .contains("Заявка на вывод из ассортимента");
        var description = session.issues().get(ticketKey).getDescription();
        Assertions.assertThat(description.get())
            .contains(meta.getDescription(), "Прошу согласовать вывод из ассортимента.",
                "shop-sku-111", "shop-sku-222", "shop-sku-333");
    }

    @Test
    public void enrichStepTest() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new ToInactiveMeta().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(serviceOfferKeys, meta);
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
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new ToInactiveMeta());

        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        SecurityUtil.wrapWithLogin(user.getLogin(), () -> executor.run());

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void simpleRunWithNewFile() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new ToInactiveMeta());

        manualStepAttachExcelAndResolve(List.of(new ServiceOfferKey(1, "a")), ticket);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .hasSize(1)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, ToInactiveApproveStrategy.TYPE));
    }

    @Test
    public void checkEconomicMetrics() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new ToInactiveMeta());

        manualStepAttachExcelAndResolve(List.of(new ServiceOfferKey(1, "a")), ticket);

        executor.run();

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, ToInactiveApproveStrategy.TYPE));
        Assertions.assertThat(metrics.get(0).getData())
            .containsEntry(Headers.SHOP_SKU_KEY, "a")
            .containsEntry(Headers.SUPPLIER_ID_KEY, "1");
    }

    @Test
    public void simpleRunWithReopenStep() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));

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
                return ProcessResponse.of(ProcessResponse.Status.NOT_OK, meta);
            } else {
                // on second call run real method
                return invok.callRealMethod();
            }
        }).when(strategySpy).process(Mockito.any());

        var ticket = facade.start(List.of(new ServiceOfferKey(1, "a")), new ToInactiveMeta());

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        // запускаем в первый раз
        executor.run();
        Mockito.verify(strategySpy, Mockito.times(1)).reopen(Mockito.any());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);

        manualStepAttachExcelAndResolve(List.of(new ServiceOfferKey(1, "a")), ticket);
        // second run process attached file
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        //third run, nothing to do
        Mockito.clearInvocations(strategySpy);
        executor.run();
        Mockito.verifyNoMoreInteractions(strategySpy);
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void checkStepTest() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
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

        var ticket = facade.start(serviceOfferKeys, new ToInactiveMeta());

        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                EnrichApproveToInactiveExcelComposer.HEADERS), user);
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
            )), user
        );
        SessionUtils.check(session, ticket);


        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Mockito.verify(strategySpy, Mockito.times(5)).preprocess(Mockito.any());
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );

        //third run: correct excel data
        Instant thirdAttachCreatedAt = secondAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel3.xlsx", thirdAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), EnrichApproveToInactiveExcelComposer.HEADERS), user
        );
        SessionUtils.check(session, ticket);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(6)).preprocess(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(Mockito.any());

        var sortedBySupplierResult = sskuStatusRepository.find(serviceOfferKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.INACTIVE,
                OfferAvailability.ACTIVE,
                OfferAvailability.INACTIVE,
                OfferAvailability.INACTIVE
            );
    }

    @Test
    public void excelHeadersValidationTest() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
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

        var ticket = facade.start(serviceOfferKeys, new ToInactiveMeta());

        executor.run();

        //first run: added header
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", firstAttachCreatedAt,
            createNotCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            )), user
        );
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(Mockito.any());
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );

        //second run: deleted header
        var headers = new ArrayList<>(EnrichApproveToInactiveExcelComposer.HEADERS);
        headers.remove(Headers.SHOP_SKU);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), headers), user
        );
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Mockito.verify(strategySpy, Mockito.times(5)).preprocess(Mockito.any());
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderDeletionError(Headers.SHOP_SKU_KEY)
                .toString()
        );

        //third run: renamed header
        headers = new ArrayList<>(EnrichApproveToInactiveExcelComposer.HEADERS);
        headers.remove(Headers.SHOP_SKU);
        var renamedHeader = Headers.SHOP_SKU_KEY + "_sth";
        headers.add(new Header(renamedHeader));
        Instant thirdAttachCreatedAt = secondAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", thirdAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), headers), user
        );
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        Mockito.verify(strategySpy, Mockito.times(7)).preprocess(Mockito.any());
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderRenameError(renamedHeader).toString()
        );

        //fourth run: correct excel data
        Instant fourthAttachCreatedAt = thirdAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel3.xlsx", fourthAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), EnrichApproveToInactiveExcelComposer.HEADERS), user
        );
        SessionUtils.check(session, ticket);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(8)).preprocess(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(Mockito.any());

        var sortedBySupplierResult = sskuStatusRepository.find(serviceOfferKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.INACTIVE,
                OfferAvailability.ACTIVE,
                OfferAvailability.INACTIVE,
                OfferAvailability.INACTIVE
            );
    }

    @Test
    public void processStepTest() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );
        var ticket = facade.start(serviceOfferKeys, new ToInactiveMeta());

        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(2, "b")),
                EnrichApproveToInactiveExcelComposer.HEADERS), user);
        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());

        Mockito.verify(strategySpy, Mockito.times(3)).preprocess(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(1)).process(Mockito.any());

        //second run: correct sskus
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFile(List.of(new ServiceOfferKey(111, "shop-sku-111"),
                new ServiceOfferKey(222, "shop-sku-222")),
                EnrichApproveToInactiveExcelComposer.HEADERS), user);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(4)).preprocess(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(2)).process(Mockito.any());

        Assertions.assertThat(dataRepository.findByTicket(ticket)).isEmpty();

        var sortedBySupplierResult = sskuStatusRepository.find(serviceOfferKeys).stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.INACTIVE,
                OfferAvailability.INACTIVE,
                OfferAvailability.ACTIVE);
    }

    @Test
    public void processStepHeadersNotMatchTest() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));

        List<ServiceOfferKey> keys = List.of(new ServiceOfferKey(1, "a"));
        var ticket = facade.start(keys, new ToInactiveMeta());

        // запускаем в первый раз
        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createNotCorrectExcelFile(keys), user);

        // во второй
        executor.run();
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);
    }

    @Test
    public void processStepDuplicateHeaders() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));

        List<ServiceOfferKey> keys = List.of(new ServiceOfferKey(1, "a"));
        var ticket = facade.start(keys, new ToInactiveMeta());

        // запускаем в первый раз
        executor.run();

        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        var stream = this.getClass().getClassLoader().getResourceAsStream("file_with_duplicate_colums.xlsx");
        session.attachments().add(ticket, "excel.xlsx", stream, Instant.now().plusSeconds(1), user);

        // во второй
        executor.run();

        Assertions.assertThat(session.issues().getSummonees(ticket)).doesNotContain(
            MbocErrors.get().excelDuplicateHeader("duplicate_column").toString()
        );
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.REOPENED);
    }

    @Test
    public void processStepClosedWithoutResolutionTest() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, "comment3", null)
        );

        var ticket = facade.start(serviceOfferKeys, new ToInactiveMeta());
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
    public void closeWithWontDoWillChangeWontChangeAnything() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.PENDING, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, "comment3", null)
        );

        var ticket = facade.start(serviceOfferKeys, new ToInactiveMeta());

        SessionUtils.close(session, ticket, TicketResolution.WONT_DO);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // nothing changed
        var sortedBySupplierResult = sskuStatusRepository.find(serviceOfferKeys)
            .stream()
            .sorted(Comparator.comparing(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.ACTIVE,
                OfferAvailability.PENDING,
                OfferAvailability.INACTIVE_TMP);
    }

    @Test
    public void closeWithWontFixBeforeEnrichWillBeNoChanges() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.PENDING, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, "comment3", null)
        );

        var ticket = facade.start(serviceOfferKeys, new ToInactiveMeta());

        // тикет отменен после создания
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).isEmpty();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // nothing changed
        var sortedBySupplierResult = sskuStatusRepository.find(serviceOfferKeys)
            .stream()
            .sorted(Comparator.comparing(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.ACTIVE,
                OfferAvailability.PENDING,
                OfferAvailability.INACTIVE_TMP);
    }

    @Test
    public void closeWithWontFixAfterEnrichWillBeNoChanges() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.PENDING, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, "comment3", null)
        );

        var ticket = facade.start(serviceOfferKeys, new ToInactiveMeta());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        // тикет отменен после обогащения
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).hasSize(1);

        // nothing changed
        var sortedBySupplierResult = sskuStatusRepository.find(serviceOfferKeys)
            .stream()
            .sorted(Comparator.comparing(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.ACTIVE,
                OfferAvailability.PENDING,
                OfferAvailability.INACTIVE_TMP);
    }

    private void manualStepAttachExcelAndResolve(List<ServiceOfferKey> keys, String ticket) {
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createCorrectExcelFile(keys, EnrichApproveToInactiveExcelComposer.HEADERS), user);
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

    // DEEPMIND-2125
    @Test
    public void dontDealWithEmptyLines() {
        deepmindSupplierRepository.save(new Supplier().setId(2303663).setName("2303663"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(
            offer(2303663, "10101", 1, 1),
            offer(2303663, "10201", 1, 1),
            offer(2303663, "10301", 1, 1)
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(2303663, "10101", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(2303663, "10201", OfferAvailability.ACTIVE, "comment2", null),
            sskuStatus(2303663, "10301", OfferAvailability.ACTIVE, "comment3", null)
        );

        List<ServiceOfferKey> keys = List.of(
            new ServiceOfferKey(2303663, "10101"),
            new ServiceOfferKey(2303663, "10201"),
            new ServiceOfferKey(2303663, "10301")
        );
        var ticket = facade.start(keys, new ToInactiveMeta());

        executor.run();

        var stream = this.getClass().getClassLoader().getResourceAsStream("file_with_empty_lines.xlsx");
        session.attachments().add(ticket, "excel.xlsx", stream, Instant.now().plusSeconds(1), user);
        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        // во второй
        executor.run();

        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains("Согласовано");
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(sskuStatusRepository.findByKeys(keys))
            .hasSize(3)
            .allMatch(s -> s.getAvailability() == OfferAvailability.INACTIVE);
    }

    // DEEPMIND-2125
    @Test
    public void dontDealWithSkipLines() {
        deepmindSupplierRepository.save(new Supplier().setId(2303663).setName("2303663"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(
            offer(2303663, "10101", 1, 1),
            offer(2303663, "10201", 1, 1),
            offer(2303663, "10301", 1, 1)
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(2303663, "10101", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(2303663, "10201", OfferAvailability.ACTIVE, "comment2", null),
            sskuStatus(2303663, "10301", OfferAvailability.ACTIVE, "comment3", null)
        );

        List<ServiceOfferKey> keys = List.of(
            new ServiceOfferKey(2303663, "10101"),
            new ServiceOfferKey(2303663, "10201"),
            new ServiceOfferKey(2303663, "10301")
        );
        var ticket = facade.start(keys, new ToInactiveMeta());

        executor.run();

        var stream = this.getClass().getClassLoader().getResourceAsStream("file_with_skip_lines.xlsx");
        session.attachments().add(ticket, "excel.xlsx", stream, Instant.now().plusSeconds(1), user);
        SessionUtils.close(session, ticket, TicketResolution.FIXED);

        // во второй
        executor.run();

        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains("Согласовано");
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(sskuStatusRepository.findByKeys(keys))
            .hasSize(3)
            .allMatch(s -> s.getAvailability() == OfferAvailability.INACTIVE);
    }
}
