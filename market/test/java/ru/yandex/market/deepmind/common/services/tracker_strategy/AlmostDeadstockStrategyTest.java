package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.availability.ShopSkuWKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AlmostDeadstockStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.DeadstockStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.AlmostDeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.DeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.AlmostDeadstockMeta;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFacade;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.offers.model.AlmostDeadstockInfo;

import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.FIRST_PARTY;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.REAL_SUPPLIER;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.THIRD_PARTY;

public class AlmostDeadstockStrategyTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    private TrackerApproverFacade<ShopSkuWKey, AlmostDeadstockMeta, AlmostDeadstockInfo> facade;
    private AlmostDeadstockStrategy strategy;
    @Resource
    private AlmostDeadstockStatusRepository repository;
    @Resource
    private DeadstockStatusRepository deadstockStatusRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;

    @Before
    public void setUp() {
        super.setUp();

        var approveWithACHelper = new ApproveWithAssortmentCommitteeHelper(session,
            economicMetricsRepository, transactionHelper);
        var categoryManagerTeamService = new CategoryManagerTeamService(
            deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository,
            new DeepmindCategoryCachingServiceMock()
        );
        strategy = new AlmostDeadstockStrategy(
            session,
            approveWithACHelper,
            "TEST",
            repository,
            deadstockStatusRepository,
            sskuStatusRepository,
            serviceOfferReplicaRepository,
            categoryManagerTeamService,
            deepmindSupplierRepository,
            deepmindWarehouseRepository
        );
        factory.registerStrategy(strategy);
        facade = factory.getFacade(strategy.getType());
        initData();
    }

    @Test
    public void startTest() {
        start();
    }

    @Test
    public void ticketWithAlmostDeadstock_shouldNotBeClosed() {
        String ticket = start();
        executor.run();
        assertTicketStatus(ticket, TicketState.PREPROCESSED);
        assertTicketIsOpen(ticket);
    }

    @Test
    public void ticketWithAnyAlmostDeadstock_shouldNotBeClosed() {
        String ticket = start();
        var sskuwKeyForDelete = new ShopSkuWKey(1, "ssku1", 172);
        // нет дедстоков, но не все алмост дедстоки удалены
        deadstockStatusRepository.deleteAll();
        repository.deleteByFilter(new AlmostDeadstockStatusRepository.Filter().setKeys(List.of(sskuwKeyForDelete)));
        executor.run();
        assertTicketStatus(ticket, TicketState.PREPROCESSED);
        assertTicketIsOpen(ticket);
    }

    @Test
    public void ticketWithAnyDeadstock_shouldNotBeClosed() {
        String ticket = start();
        // нет алмост дедстоков, но есть дедсток
        repository.deleteAll();
        deadstockStatusRepository.save(new DeadstockStatus()
            .setSupplierId(1)
            .setShopSku("ssku1")
            .setDeadstockSince(LocalDate.now())
            .setWarehouseId(172L)
            .setImportTs(Instant.now()));

        executor.run();
        assertTicketStatus(ticket, TicketState.PREPROCESSED);
        assertTicketIsOpen(ticket);
    }

    @Test
    public void ticketWithNoAlmostDeadstockAndNoDeadstock_shouldBeClosed() {
        String ticket = start();
        repository.deleteAll();
        deadstockStatusRepository.deleteAll();
        executor.run();
        assertTicketStatus(ticket, TicketState.CLOSED);
        assertTicketIsClosed(ticket);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).isEqualTo("Закрыт — Не воспроизводится");
    }

    @Test
    public void ticketClosedByUser_shouldBeClosed() {
        String ticket = start();
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        assertTicketIsClosed(ticket);

        executor.run();
        assertTicketStatus(ticket, TicketState.CLOSED);
        assertTicketIsClosed(ticket);
    }

    @Test
    public void shouldCreateNewTickets_whenTheAlmostDeadstocksComeAgain() {
        ticketWithNoAlmostDeadstockAndNoDeadstock_shouldBeClosed();

        repository.save(List.of(
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku1")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 21)),
            new AlmostDeadstockStatus().setSupplierId(2).setShopSku("ssku2")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 22))
        ));

        var ticket = start();
        Assertions.assertThat(ticket).isEqualTo("TEST-2");
    }

    @Test
    public void shouldCreateNewTickets_whenClosedButStillHasAlmostDeadstocks() {
        String ticket = start();
        Assertions.assertThat(ticket).isEqualTo("TEST-1");
        var issue = session.issues().get(ticket);
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        assertTicketIsClosed(ticket);

        executor.run();
        assertTicketIsClosed(ticket);

        ticket = start();
        Assertions.assertThat(ticket).isEqualTo("TEST-2");
    }

    @Test
    public void testCatmanShouldBeSummoned_whenSskuBecomesInactive() {
        String ticket = start();
        Assertions.assertThat(ticket).isEqualTo("TEST-1");
        sskuStatusRepository.save(new SskuStatus()
            .setShopSku("ssku1")
            .setSupplierId(1)
            .setComment(".AP6.")
            .setAvailability(OfferAvailability.INACTIVE));
        sskuStatusRepository.save(new SskuStatus()
            .setShopSku("ssku3")
            .setSupplierId(1)
            .setComment("another reason")
            .setAvailability(OfferAvailability.INACTIVE_TMP));
        executor.run();
        String expectedComment = "оффер ssku1 переведен в статус INACTIVE, так как имеет признак deadstock уже более " +
            "30 дней на одном из московских складов и нет продаж";
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).isEqualTo(expectedComment);
        executor.run();
        //the same comment should not be added in the next iteration
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);
    }

    @Test
    public void testCatmanShouldNotBeSummoned_whenSskuInactiveButReasonInNotAP6() {
        String ticket = start();
        Assertions.assertThat(ticket).isEqualTo("TEST-1");
        sskuStatusRepository.save(new SskuStatus()
            .setShopSku("ssku1")
            .setSupplierId(1)
            .setComment(null)
            .setAvailability(OfferAvailability.INACTIVE));
        sskuStatusRepository.save(new SskuStatus()
            .setShopSku("ssku2")
            .setSupplierId(2)
            .setComment("another reason")
            .setAvailability(OfferAvailability.INACTIVE));
        sskuStatusRepository.save(new SskuStatus()
            .setShopSku("ssku3")
            .setSupplierId(1)
            .setComment("any reason")
            .setAvailability(OfferAvailability.INACTIVE_TMP));
        executor.run();
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).isEmpty();
    }

    @Test
    public void testCatmanShouldBeSummoned_multipleSSkus() {
        String ticket = "TEST-1";
        testCatmanShouldBeSummoned_whenSskuBecomesInactive();
        sskuStatusRepository.save(new SskuStatus()
            .setShopSku("ssku2")
            .setSupplierId(2)
            .setComment(".AP6.")
            .setAvailability(OfferAvailability.INACTIVE));
        executor.run();
        String expectedComment = "оффер ssku2 переведен в статус INACTIVE, так как имеет признак deadstock уже более " +
            "30 дней на одном из московских складов и нет продаж";
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).isEqualTo(expectedComment);
        executor.run();
        //the same comment should not be added in the next iteration
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(2);
    }

    @Test
    public void testCatmanShouldBeSummoned_whenSskuIsDaeadstock() {
        String ticket = start();
        deadstockStatusRepository.save(new DeadstockStatus()
            .setSupplierId(1)
            .setShopSku("ssku1")
            .setWarehouseId(172L)
            .setDeadstockSince(LocalDate.now())
            .setImportTs(Instant.now())
        );
        deadstockStatusRepository.save(new DeadstockStatus()
            .setSupplierId(2)
            .setShopSku("ssku2")
            .setWarehouseId(172L)
            .setDeadstockSince(LocalDate.now())
            .setImportTs(Instant.now())
        );
        executor.run();

        var statuses = deadstockStatusRepository.findAll();
        var offerMap = serviceOfferReplicaRepository.findOffersByKeysMap(
            List.of(new ServiceOfferKey(1, "ssku1"), new ServiceOfferKey(2, "ssku2"))
        );
        String expectedComment = strategy.getCommentTextForDeadstockSsku(statuses, offerMap);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).isEqualTo(expectedComment);

        //the same comment should not be added in the next iteration
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);
        executor.run();
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);
    }

    @Test
    public void testCatmanShouldBeSummoned_whenSskuIsDeadstockAndThenInactive() {
        String ticket = start();
        // появился признак дедстока
        deadstockStatusRepository.save(new DeadstockStatus()
            .setSupplierId(1)
            .setShopSku("ssku1")
            .setWarehouseId(172L)
            .setDeadstockSince(LocalDate.now())
            .setImportTs(Instant.now())
        );
        executor.run();

        var statuses = deadstockStatusRepository.findAll();
        var offerMap = serviceOfferReplicaRepository.findOffersByKeysMap(
            List.of(new ServiceOfferKey(1, "ssku1"))
        );
        String expectedComment = strategy.getCommentTextForDeadstockSsku(statuses, offerMap);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).isEqualTo(expectedComment);
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);

        // сскю стал INACTIVE
        sskuStatusRepository.save(new SskuStatus()
            .setShopSku("ssku1")
            .setSupplierId(1)
            .setComment("AP6.")
            .setAvailability(OfferAvailability.INACTIVE));
        executor.run();
        String expectedCommentInactive = "оффер ssku1 переведен в статус INACTIVE, так как имеет признак deadstock " +
            "уже более 30 дней на одном из московских складов и нет продаж";
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).isEqualTo(expectedCommentInactive);
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(2);
    }

    @Test
    public void testFilteringSskuBeforeSummone() {
        String ticket = start();
        var issue = session.issues().get(ticket);
        var existingComment = "Нижеперечисленным SSKU присвоен признак \"Deadstock\" ssku1";
        issue.comment(existingComment);
        deadstockStatusRepository.save(new DeadstockStatus()
            .setSupplierId(1)
            .setShopSku("ssku1")
            .setWarehouseId(172L)
            .setDeadstockSince(LocalDate.now())
            .setImportTs(Instant.now())
        );
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);
        executor.run();
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);
    }

    @Test
    public void testShouldNotAddCommentWhenTicketHasOversizedComment() {
        String ticket = start();
        var issue = session.issues().get(ticket);
        var existingComment = "Размер текста комментария превышает 500Kb, поэтому он был сохранён в виде аттача.";
        issue.comment(existingComment);
        deadstockStatusRepository.save(new DeadstockStatus()
            .setSupplierId(1)
            .setShopSku("ssku1")
            .setWarehouseId(172L)
            .setDeadstockSince(LocalDate.now())
            .setImportTs(Instant.now())
        );
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);
        executor.run();
        Assertions.assertThat(SessionUtils.getComments(session, ticket)).hasSize(1);
    }

    @Test
    public void testProcessShouldThrowExceptionWhenTicketHasOversizedComment() {
        String ticket = start();
        var issue = session.issues().get(ticket);
        var existingComment = "Размер текста комментария превышает 500Kb, поэтому он был сохранён в виде аттача.";
        issue.comment(existingComment);
        deadstockStatusRepository.save(new DeadstockStatus()
            .setSupplierId(1)
            .setShopSku("ssku1")
            .setWarehouseId(172L)
            .setDeadstockSince(LocalDate.now())
            .setImportTs(Instant.now())
        );
        var shopSkuKey = new ShopSkuWKey(1, "ssku1", 172L);
        ProcessRequest<ShopSkuWKey, AlmostDeadstockMeta, AlmostDeadstockInfo> request =
            ProcessRequest.of("TEST-1", List.of(shopSkuKey), new AlmostDeadstockMeta(), Map.of());
        Assertions.assertThatThrownBy(() -> strategy.process(request))
            .hasMessage("One of the comments has exceeded the 500Kb limit, which may cause problems when " +
                "checking already posted comments");
    }

    private void initData() {
        deepmindSupplierRepository.save(supplier(1, "supplier 1", "0001"));
        deepmindSupplierRepository.save(supplier(2, "supplier 2", "0002"));
        deepmindSupplierRepository.save(supplier(3, "supplier 3", "0003"));
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setStaffLogin("pupkin").setCategoryId(1L).setRole(CATMAN)
                .setFirstName("").setLastName(""),
            new CategoryManager().setStaffLogin("petrov").setCategoryId(2L).setRole(CATMAN)
                .setFirstName("").setLastName("")
        );
        deepmindWarehouseRepository.save(List.of(
            new Warehouse().setId(777L).setName("non-moscow warehouse")
                .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT)
                .setType(WarehouseType.FULFILLMENT)
        ));
        serviceOfferReplicaRepository.save(
            offer(1, "ssku3", 3L, FIRST_PARTY, 2L),
            offer(1, "ssku1", 1L, FIRST_PARTY, 1L),
            offer(1, "ssku7", 1L, FIRST_PARTY, 3L),
            offer(3, "ssku5", 5L, THIRD_PARTY, 2L),
            offer(2, "ssku4", 4L, REAL_SUPPLIER, 2L),
            offer(2, "ssku6", 6L, REAL_SUPPLIER, 2L),
            offer(2, "ssku2", 1L, REAL_SUPPLIER, 1L),
            offer(1, "ssku8", 1L, FIRST_PARTY, 1L)
        );

        repository.save(List.of(
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku1")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 21)),
            new AlmostDeadstockStatus().setSupplierId(2).setShopSku("ssku2")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 22)),
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku3")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 23)),
            new AlmostDeadstockStatus().setSupplierId(2).setShopSku("ssku4")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 24)),
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku5")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 25)),
            new AlmostDeadstockStatus().setSupplierId(2).setShopSku("ssku6")
                .setWarehouseId(777L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 26)),
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku7")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 27))
        ));
    }

    private Supplier supplier(int id, String name, String realSupplier) {
        return new Supplier().setId(id).setName(name).setRealSupplierId(realSupplier)
            .setSupplierType(SupplierType.REAL_SUPPLIER);
    }

    private String start() {
        var infos = getExpectedInfos();
        Map<ShopSkuWKey, AlmostDeadstockInfo> keyMetaMap = new HashMap<>();
        keyMetaMap.put(new ShopSkuWKey(1, "ssku1", 172), infos.get(0));
        keyMetaMap.put(new ShopSkuWKey(2, "ssku2", 172), infos.get(1));

        AlmostDeadstockMeta meta = new AlmostDeadstockMeta().setAssignee("pupkin");

        var startRequest = StartRequest.of(keyMetaMap.keySet(), meta, keyMetaMap);
        var ticket = facade.start(startRequest);

        var status = ticketRepository.findByTicket(ticket);
        Assertions.assertThat(status.getState()).isEqualTo(TicketState.NEW);
        var actualMeta = status.getMeta().toObject(AlmostDeadstockMeta.class);
        Assertions.assertThat(actualMeta.getAssignee()).isEqualTo(meta.getAssignee());

        var dataList = dataRepository.findByTicket(ticket);
        for (var data : dataList) {
            var actualSskuWKey = data.getKey().toObject(ShopSkuWKey.class);
            var actualMetaData = data.getMeta().toObject(AlmostDeadstockInfo.class);
            Assertions.assertThat(keyMetaMap.containsKey(actualSskuWKey)).isTrue();
            Assertions.assertThat(actualMetaData).isEqualTo(keyMetaMap.get(actualSskuWKey));
        }
        assertTicketIsOpen(ticket);
        return ticket;
    }

    private void assertTicketStatus(String ticket, TicketState state) {
        var status = ticketRepository.findByTicket(ticket);
        Assertions.assertThat(status.getState()).isEqualTo(state);
    }

    private ServiceOfferReplica offer(
        int supplierId, String shopSku, long msku, MbocSupplierType supplierType, Long categoryId) {
        return offer(supplierId, shopSku, msku, categoryId).setSupplierType(SupplierType.valueOf(supplierType.name()));
    }

    private List<AlmostDeadstockInfo> getExpectedInfos() {
        return List.of(
            //TEST-1
            new AlmostDeadstockInfo()
                .setSupplierId(1)
                .setCategoryId(1L)
                .setSupplierName("supplier 1")
                .setTitle("title ssku1")
                .setShopSku("ssku1")
                .setApprovedSkuMappingId(1L)
                .setWarehouseId(172L)
                .setWarehouseName("Яндекс.Маркет (Софьино)")
                .setRealSupplierId("0001")
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 21)),
            new AlmostDeadstockInfo()
                .setSupplierId(2)
                .setCategoryId(1L)
                .setSupplierName("supplier 2")
                .setTitle("title ssku2")
                .setShopSku("ssku2")
                .setApprovedSkuMappingId(1L)
                .setWarehouseId(172L)
                .setWarehouseName("Яндекс.Маркет (Софьино)")
                .setRealSupplierId("0002")
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 22)),
            //for TEST-2
            new AlmostDeadstockInfo()
                .setSupplierId(1)
                .setCategoryId(2L)
                .setSupplierName("supplier 1")
                .setTitle("title ssku3")
                .setShopSku("ssku3")
                .setApprovedSkuMappingId(3L)
                .setWarehouseId(172L)
                .setWarehouseName("Яндекс.Маркет (Софьино)")
                .setRealSupplierId("0001")
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 23)),
            new AlmostDeadstockInfo()
                .setSupplierId(2)
                .setCategoryId(2L)
                .setSupplierName("supplier 2")
                .setRealSupplierId("0002")
                .setTitle("title ssku4")
                .setShopSku("ssku4")
                .setApprovedSkuMappingId(4L)
                .setWarehouseId(172L)
                .setWarehouseName("Яндекс.Маркет (Софьино)")
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 24)),
            //for TEST-3
            new AlmostDeadstockInfo()
                .setSupplierId(1)
                .setCategoryId(1L)
                .setSupplierName("supplier 1")
                .setTitle("title ssku8")
                .setShopSku("ssku8")
                .setApprovedSkuMappingId(1L)
                .setWarehouseId(172L)
                .setWarehouseName("Яндекс.Маркет (Софьино)")
                .setRealSupplierId("0001")
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 28))
        );
    }

    private void assertTicketIsClosed(String ticket) {
        var issue = session.issues().get(ticket);
        Assertions.assertThat(TicketStatus.isClosedWithResolution(issue)).isTrue();
    }

    private void assertTicketIsOpen(String ticket) {
        var issue = session.issues().get(ticket);
        Assertions.assertThat(TicketStatus.isOpen(issue)).isTrue();
    }
}
