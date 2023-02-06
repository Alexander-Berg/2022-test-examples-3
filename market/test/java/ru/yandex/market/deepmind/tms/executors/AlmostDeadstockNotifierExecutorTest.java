package ru.yandex.market.deepmind.tms.executors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.availability.ShopSkuWKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AlmostDeadstockStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.AlmostDeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.DeadstockStatusRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.tracker_approver.configurations.AlmostDeadstockTrackerApproverConfiguration;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.AlmostDeadstockMeta;
import ru.yandex.market.deepmind.common.services.tracker_strategy.AlmostDeadstockStrategy;
import ru.yandex.market.deepmind.common.services.tracker_strategy.ApproveWithAssortmentCommitteeHelper;
import ru.yandex.market.deepmind.common.services.tracker_strategy.TicketStatus;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverExecutionContext;
import ru.yandex.market.deepmind.tracker_approver.service.TrackerApproverFactory;
import ru.yandex.market.deepmind.tracker_approver.service.enhanced.EnhancedTrackerApproverExecutor;
import ru.yandex.market.deepmind.tracker_approver.utils.CurrentThreadExecutorService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.offers.model.AlmostDeadstockInfo;
import ru.yandex.market.tracker.tracker.MockSession;

import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.ASSORTMENT_MANAGER;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;
import static ru.yandex.market.deepmind.tms.executors.AlmostDeadstockNotifierExecutor.ALMOST_DEADSTOCK_NOTIFIER_ENABLED;
import static ru.yandex.market.deepmind.tms.executors.AlmostDeadstockNotifierExecutor.ALMOST_DEADSTOCK_NOTIFIER_LAST_RUN_TS;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.FIRST_PARTY;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.REAL_SUPPLIER;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.THIRD_PARTY;

public class AlmostDeadstockNotifierExecutorTest extends DeepmindBaseDbTestClass {
    private AlmostDeadstockNotifierExecutor executor;
    private EnhancedTrackerApproverExecutor trackerApproverExecutor;

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private AlmostDeadstockStatusRepository almostDeadstockStatusRepository;
    @Resource
    private DeadstockStatusRepository deadstockStatusRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private MskuInfoRepository mskuInfoRepository;
    @Resource
    private TrackerApproverTicketRepository ticketStatusRepository;
    @Resource
    private TrackerApproverDataRepository ticketDataRepository;
    @Resource
    private StorageKeyValueService deepmindStorageKeyValueService;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private ObjectMapper trackerApproverObjectMapper;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private StorageKeyValueService storageKeyValueService;
    @Resource
    private TrackerApproverTicketRepository ticketRepository;

    private MockSession session;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        var factory = new TrackerApproverFactory(
            ticketDataRepository,
            ticketStatusRepository,
            transactionTemplate,
            trackerApproverObjectMapper);
        var categoryManagerTeamService = new CategoryManagerTeamService(
            deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository,
            new DeepmindCategoryCachingServiceMock()
        );
        session = new MockSession();
        var open = session.statuses().add("open");
        var closed = session.statuses().add("closed");
        session.transitions().add(List.of(open), "close", closed);

        factory.registerStrategy(new AlmostDeadstockStrategy(
            session,
            Mockito.mock(ApproveWithAssortmentCommitteeHelper.class),
            "TEST",
            almostDeadstockStatusRepository,
            deadstockStatusRepository,
            sskuStatusRepository,
            serviceOfferReplicaRepository,
            categoryManagerTeamService,
            deepmindSupplierRepository,
            deepmindWarehouseRepository
        ));
        executor = new AlmostDeadstockNotifierExecutor(
            factory,
            almostDeadstockStatusRepository,
            categoryManagerTeamService,
            deepmindStorageKeyValueService
        );
        var executionContext = new TrackerApproverExecutionContext()
            .setThreadCount(1);

        var storageKeyValueServiceSpy = Mockito.spy(storageKeyValueService);
        Mockito.doReturn(null).when(storageKeyValueServiceSpy).getOffsetDateTime(Mockito.any(), Mockito.any());

        var configuration = new AlmostDeadstockTrackerApproverConfiguration(
            "",
            factory,
            executionContext
        );
        trackerApproverExecutor = new EnhancedTrackerApproverExecutor(
            ticketRepository,
            configuration,
            null,
            transactionTemplate,
            storageKeyValueServiceSpy
        );
        trackerApproverExecutor.setExecutorService(new CurrentThreadExecutorService());
    }

    @Test
    public void simpleRun() {
        executor.execute();
    }

    @Test
    public void runOnMonday() {
        var now = Instant.parse("2021-11-29T08:00:00.00Z");
        executor.setClock(Clock.fixed(now, ZoneOffset.UTC));

        initData();
        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(1L));
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(ASSORTMENT_MANAGER).setStaffLogin("ivanov")
                .setFirstName("").setLastName("")
        );

        //изначально нет данных
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(0);

        //после запуска эксекютора, появились 2 тикета
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);

        var newTicketMeta1 = new AlmostDeadstockMeta().setAssignee("pupkin")
            .addFollower("pupkin").addFollower("ivanov");
        assertTicket(tickets.get(0), newTicketMeta1);

        var newTicketMeta2 = new AlmostDeadstockMeta().setAssignee("petrov").addFollower("petrov");
        assertTicket(tickets.get(1), newTicketMeta2);

        var expectedInfos = getExpectedInfos();
        assertData("TEST-1", expectedInfos.get(0).setInTargetAssortment(true));
        assertData("TEST-1", expectedInfos.get(1).setInTargetAssortment(true));
        assertData("TEST-2", expectedInfos.get(2));
        assertData("TEST-2", expectedInfos.get(3));

        //при повторном запуске эксекютора ничего не изменилось
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);

        //появился новый признак, после запуска эксекютора появился новый тикет
        almostDeadstockStatusRepository.save(new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku8")
            .setWarehouseId(172L).setAlmostDeadstockSince(LocalDate.of(2021, 10, 28)));
        executor.execute();

        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(3);

        var newTicketMeta3 = new AlmostDeadstockMeta().setAssignee("pupkin")
            .addFollower("pupkin").addFollower("ivanov");
        assertTicket(tickets.get(2), newTicketMeta3);

        assertData("TEST-3", expectedInfos.get(4).setInTargetAssortment(true));
    }

    @Test
    public void runOnAnyDayExceptMonday() {
        var now = Instant.parse("2021-11-30T08:00:00.00Z");
        executor.setClock(Clock.fixed(now, ZoneOffset.UTC));

        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(1L));
        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(4L));
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(ASSORTMENT_MANAGER).setStaffLogin("ivanov")
                .setFirstName("").setLastName("")
        );
        initData();

        //изначально нет данных
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(0);

        //после запуска эксекютора, появились 2 тикета
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);

        var newTicketMeta1 = new AlmostDeadstockMeta().setAssignee("pupkin")
            .addFollower("pupkin").addFollower("ivanov");
        assertTicket(tickets.get(0), newTicketMeta1);

        var expectedInfos = getExpectedInfos();
        assertData("TEST-1", expectedInfos.get(0).setInTargetAssortment(true));
        assertData("TEST-1", expectedInfos.get(1).setInTargetAssortment(true));

        //при повторном запуске эксекютора ничего не изменилось
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);
        assertTicket(tickets.get(0), newTicketMeta1);
        assertData("TEST-1", expectedInfos.get(0).setInTargetAssortment(true));
        assertData("TEST-1", expectedInfos.get(1).setInTargetAssortment(true));

        //появился новый признак, после запуска эксекютора тикет не создается, потому что некорфикс мскю
        almostDeadstockStatusRepository.save(new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku9")
            .setWarehouseId(172L).setAlmostDeadstockSince(LocalDate.of(2021, 10, 28)));
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);
        assertTicket(tickets.get(0), newTicketMeta1);
        assertData("TEST-1", expectedInfos.get(0).setInTargetAssortment(true));
        assertData("TEST-1", expectedInfos.get(1).setInTargetAssortment(true));
    }

    @Test
    public void callAssortmentManagerToCorefix() {
        var now = Instant.parse("2021-11-29T08:00:00.00Z");
        executor.setClock(Clock.fixed(now, ZoneOffset.UTC));
        initData();
        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(1L));
        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(4L));
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(ASSORTMENT_MANAGER).setStaffLogin("ivanov")
                .setFirstName("").setLastName("")
        );

        executor.execute();
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);

        var newTicketMeta1 = new AlmostDeadstockMeta().setAssignee("pupkin")
            .addFollower("pupkin").addFollower("ivanov");
        assertTicket(tickets.get(0), newTicketMeta1);

        var expectedInfos = getExpectedInfos();
        assertData("TEST-1", expectedInfos.get(0).setInTargetAssortment(true));
        assertData("TEST-1", expectedInfos.get(1).setInTargetAssortment(true));

        // товар corefix, но нет ассортиментного менеджера
        var newTicketMeta2 = new AlmostDeadstockMeta().setAssignee("petrov").addFollower("petrov");
        assertTicket(tickets.get(1), newTicketMeta2);
        assertData("TEST-2", expectedInfos.get(2));
        assertData("TEST-2", expectedInfos.get(3).setInTargetAssortment(true));
    }

    @Test
    public void whenNewAlmostDeadstocksCome_shouldBeFilteredBeforeCreate() {
        var now = Instant.parse("2021-11-29T08:00:00.00Z");
        executor.setClock(Clock.fixed(now, ZoneOffset.UTC));
        //создались два тикета
        initData();
        executor.execute();
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);
        assertTicketIsOpen("TEST-1", "TEST-2");

        //некоторые признаки пропали, но тикет не закрылся
        almostDeadstockStatusRepository.deleteByFilter(new AlmostDeadstockStatusRepository.Filter()
            .setKeys(List.of(new ShopSkuWKey(1, "ssku1", 172L))));
        trackerApproverExecutor.run();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);
        assertTicketIsOpen("TEST-1", "TEST-2");

        //появились новые признаки, но не на все из них создался тикет
        almostDeadstockStatusRepository.save(List.of(
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku1")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 21)),
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku8")
                .setWarehouseId(172L).setAlmostDeadstockSince(LocalDate.of(2021, 10, 28))
        ));
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(3);
        var newTicketMeta3 = new AlmostDeadstockMeta().setAssignee("pupkin").addFollower("pupkin");
        assertTicket(tickets.get(2), newTicketMeta3);
        assertData("TEST-3", getExpectedInfos().get(4));
        var issue = session.issues().get("TEST-3");
        Assertions.assertThat(TicketStatus.isOpen(issue)).isTrue();
    }

    @Test
    public void testShouldRetryByDefault() {
        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(1L));
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(ASSORTMENT_MANAGER).setStaffLogin("ivanov")
                .setFirstName("").setLastName("")
        );
        initData();
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isEmpty();
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isNotEmpty();
        var dt = LocalDateTime.ofInstant(getLastRunTs(), ZoneId.systemDefault()).toLocalDate();
        Assertions.assertThat(dt).isEqualTo(LocalDate.now());
    }

    @Test
    public void testExecutorDisabled() {
        deepmindStorageKeyValueService.putValue(ALMOST_DEADSTOCK_NOTIFIER_ENABLED, false);
        initData();
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isEmpty();
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isEmpty();
        Assertions.assertThat(getLastRunTs()).isNull();
    }

    @Test
    public void testShouldRetryIfLastTryWasYesterday() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        deepmindStorageKeyValueService.putValue(ALMOST_DEADSTOCK_NOTIFIER_LAST_RUN_TS, yesterday);
        //чтобы учитывать и пн добавим немного корфиксовых товаров
        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(1L));
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(ASSORTMENT_MANAGER).setStaffLogin("ivanov")
                .setFirstName("").setLastName("")
        );
        initData();
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isEmpty();
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isNotEmpty();
        var dt = LocalDateTime.ofInstant(getLastRunTs(), ZoneId.systemDefault()).toLocalDate();
        Assertions.assertThat(dt).isEqualTo(LocalDate.now());
    }

    @Test
    public void testShouldNotRetry() {
        var today = Instant.now();
        deepmindStorageKeyValueService.putValue(ALMOST_DEADSTOCK_NOTIFIER_LAST_RUN_TS, today);
        initData();
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isEmpty();
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).isEmpty();
        Assertions.assertThat(today).isEqualTo(getLastRunTs());
    }

    @Test
    public void testSskusShouldBeSeparatedByMaxSskuCount() {
        var now = Instant.parse("2021-11-29T08:00:00.00Z");
        executor.setClock(Clock.fixed(now, ZoneOffset.UTC));

        deepmindSupplierRepository.save(
            new Supplier()
                .setId(1)
                .setName("supplier 1")
                .setSupplierType(SupplierType.REAL_SUPPLIER)
                .setRealSupplierId("0001")
        );
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(CATMAN).setStaffLogin("ivanov")
                .setFirstName("").setLastName("")
        );
        for (int i = 0; i < 800 + 20; i++) {
            var ssku = "ssku" + i;
            serviceOfferReplicaRepository.save(
                offer(1, ssku, 1L, FIRST_PARTY, 1L)
            );
            almostDeadstockStatusRepository.save(List.of(
                new AlmostDeadstockStatus().setSupplierId(1).setShopSku(ssku)
                    .setWarehouseId(172L)
                    .setAlmostDeadstockSince(LocalDate.of(2021, 10, 21))
            ));
        }

        //изначально нет данных
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(0);

        //после запуска эксекютора, появились 2 тикета - потому что максимум ssku count в тикете = 50
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(5);
        Assertions.assertThat(ticketDataRepository.findByTicket("TEST-1")).hasSize(200);
        Assertions.assertThat(ticketDataRepository.findByTicket("TEST-2")).hasSize(200);
        Assertions.assertThat(ticketDataRepository.findByTicket("TEST-3")).hasSize(200);
        Assertions.assertThat(ticketDataRepository.findByTicket("TEST-4")).hasSize(200);
        Assertions.assertThat(ticketDataRepository.findByTicket("TEST-5")).hasSize(20);
    }

    @Test
    public void testResetFollowersForEachTicket() {
        var now = Instant.parse("2021-11-29T08:00:00.00Z");
        executor.setClock(Clock.fixed(now, ZoneOffset.UTC));

        deepmindSupplierRepository.save(
            new Supplier()
                .setId(1)
                .setName("supplier 1")
                .setSupplierType(SupplierType.REAL_SUPPLIER)
                .setRealSupplierId("0001")
        );
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(CATMAN).setStaffLogin("pupkin")
                .setFirstName("").setLastName(""),
            new CategoryManager().setCategoryId(1L).setRole(ASSORTMENT_MANAGER).setStaffLogin("assortman1")
                .setFirstName("").setLastName(""),
            new CategoryManager().setCategoryId(2L).setRole(CATMAN).setStaffLogin("pupkin")
                .setFirstName("").setLastName(""),
            new CategoryManager().setCategoryId(2L).setRole(ASSORTMENT_MANAGER).setStaffLogin("assortman2")
                .setFirstName("").setLastName("")
        );
        mskuInfoRepository.save(new MskuInfo().setInTargetAssortment(true).setMarketSkuId(1L));

        for (int i = 0; i < 220; i++) {
            var ssku = "ssku" + i;
            long categoryId = i < 200 ? 1L : 2L;
            serviceOfferReplicaRepository.save(
                offer(1, ssku, 1L, FIRST_PARTY, categoryId)
            );
            almostDeadstockStatusRepository.save(List.of(
                new AlmostDeadstockStatus().setSupplierId(1).setShopSku(ssku)
                    .setWarehouseId(172L)
                    .setAlmostDeadstockSince(LocalDate.of(2021, 10, 21))
            ));
        }

        //изначально нет данных
        var tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(0);

        //после запуска эксекютора, появились 2 тикета - потому что максимум ssku count в тикете = 50
        executor.execute();
        tickets = ticketStatusRepository.findAll();
        Assertions.assertThat(tickets).hasSize(2);

        Assertions.assertThat(ticketDataRepository.findByTicket("TEST-1")).hasSize(200);
        assertTicket(tickets.get(0),
            new AlmostDeadstockMeta().setAssignee("pupkin").addFollower("pupkin").addFollower("assortman1"));

        Assertions.assertThat(ticketDataRepository.findByTicket("TEST-2")).hasSize(20);
        assertTicket(tickets.get(1),
            new AlmostDeadstockMeta().setAssignee("pupkin").addFollower("pupkin").addFollower("assortman2"));
    }

    private Instant getLastRunTs() {
        return deepmindStorageKeyValueService.getInstant(ALMOST_DEADSTOCK_NOTIFIER_LAST_RUN_TS,
            null);
    }

    void assertTicketIsOpen(String... tickets) {
        for (var ticket : tickets) {
            var issue = session.issues().get(ticket);
            Assertions.assertThat(TicketStatus.isOpen(issue)).isTrue();
        }
    }

    void assertData(String ticket, AlmostDeadstockInfo expectedInfo) {
        var key = new ShopSkuWKey(
            expectedInfo.getSupplierId(),
            expectedInfo.getShopSku(),
            expectedInfo.getWarehouseId());
        var actualData = ticketDataRepository.findByKey(AlmostDeadstockStrategy.TYPE, key);
        Assertions.assertThat(actualData.getTicket()).isEqualTo(ticket);
        var meta = actualData.getMeta().toObject(AlmostDeadstockInfo.class);
        Assertions.assertThat(meta).isEqualTo(expectedInfo);
    }

    private void initData() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("supplier 1")
            .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("0001"));
        deepmindSupplierRepository.save(new Supplier().setId(2).setName("supplier 2")
            .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("0002"));
        deepmindSupplierRepository.save(new Supplier().setId(3).setName("supplier 3")
            .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("0003"));
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setCategoryId(1L).setRole(CATMAN).setStaffLogin("pupkin")
                .setFirstName("").setLastName(""),
            new CategoryManager().setCategoryId(2L).setRole(CATMAN).setStaffLogin("petrov")
                .setFirstName("").setLastName("")
        );
        serviceOfferReplicaRepository.save(
            offer(1, "ssku3", 3L, FIRST_PARTY, 2L),
            offer(1, "ssku1", 1L, FIRST_PARTY, 1L),
            offer(1, "ssku7", 1L, FIRST_PARTY, 3L),
            offer(3, "ssku5", 5L, THIRD_PARTY, 2L),
            offer(2, "ssku4", 4L, REAL_SUPPLIER, 2L),
            offer(2, "ssku6", 6L, REAL_SUPPLIER, 2L),
            offer(2, "ssku2", 1L, REAL_SUPPLIER, 1L),
            offer(1, "ssku8", 1L, FIRST_PARTY, 1L),
            offer(1, "ssku9", 5L, FIRST_PARTY, 1L)
        );

        almostDeadstockStatusRepository.save(List.of(
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
            new AlmostDeadstockStatus().setSupplierId(1).setShopSku("ssku7")
                .setWarehouseId(172L)
                .setAlmostDeadstockSince(LocalDate.of(2021, 10, 27))
        ));
    }

    private void assertTicket(TrackerApproverTicketRawStatus ticket, AlmostDeadstockMeta expectedMeta) {
        Assertions.assertThat(ticket.getType()).isEqualTo(AlmostDeadstockStrategy.TYPE);
        Assertions.assertThat(ticket.getState()).isEqualTo(TicketState.NEW);
        var ticketMeta = ticket.getMeta().toObject(AlmostDeadstockMeta.class);
        Assertions.assertThat(ticketMeta.getAssignee()).isEqualTo(expectedMeta.getAssignee());
        Assertions.assertThat(ticketMeta.getFollowers())
            .isEqualTo(expectedMeta.getFollowers());
    }

    private ServiceOfferReplica offer(
        int supplierId, String shopSku, long msku, MbocSupplierType supplierType, Long categoryId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title " + shopSku)
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(msku)
            .setSupplierType(SupplierType.valueOf(supplierType.name()))
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
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
}
