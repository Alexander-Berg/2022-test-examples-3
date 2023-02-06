package ru.yandex.market.deepmind.common.services.lifecycle;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditRecorder;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditService;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepository;
import ru.yandex.market.mboc.common.users.UserRepository;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;


public class LifecycleStatusesStatsCalculatorTest extends DeepmindBaseDbTestClass {
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Resource
    private UserRepository userRepository;
    @Resource(name = "deepmindDsl")
    private DSLContext dsl;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private CategoryManagerTeamService categoryManagerService;
    private LifecycleStatusesStatsCalculator lifecycleStatusesStatsCalculator;

    private MskuStatusAuditService mskuStatusAuditService;

    @Before
    public void setUp() {
        categoryManagerService = new CategoryManagerTeamService(deepmindCategoryManagerRepository,
            deepmindCategoryTeamRepository, new DeepmindCategoryCachingServiceMock());

        var mboAuditServiceMock = new MboAuditServiceMock();
        mskuStatusAuditService = new MskuStatusAuditService(mboAuditServiceMock);
        var mskuStatusAuditRecorder = new MskuStatusAuditRecorder(mboAuditServiceMock,
            Mockito.mock(MboUsersRepository.class));
        mskuStatusAuditRecorder.setAuditEnabled(true);
        mskuStatusRepository = new MskuStatusRepository(dsl);
        mskuStatusRepository.addObserver(mskuStatusAuditRecorder);

        var yqlAutoClusterMock = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(yqlAutoClusterMock.query(Mockito.anyString(), Mockito.anyMap(),
            Mockito.any(ResultSetExtractor.class))).thenReturn(List.of());
        lifecycleStatusesStatsCalculator = new LifecycleStatusesStatsCalculator(namedParameterJdbcTemplate,
            deepmindMskuRepository, serviceOfferReplicaRepository, deepmindCategoryRepository,
            categoryManagerService, mskuStatusAuditService, userRepository, yqlAutoClusterMock,
            YPath.simple("//tmp"), "pool");
        seasonRepository.save(new Season().setId(111L).setName("season_111"));
    }

    @Test
    public void mskusReturnedToSaleTest() {
        // prepare data
        deepmindMskuRepository.save(msku(111L, 111L), msku(222L, 222L), msku(333L, 333L), msku(444L, 444L));
        mskuStatusRepository.save(
            mskuStatus(111L, MskuStatusValue.ARCHIVE),
            mskuStatus(222L, MskuStatusValue.REGULAR),
            mskuStatus(333L, MskuStatusValue.END_OF_LIFE),
            mskuStatus(444L, MskuStatusValue.SEASONAL)
        );
        deepmindCategoryRepository.insertBatch(category(111L), category(222L), category(333L), category(444L));
        deepmindSupplierRepository.save(
            supplier(111, SupplierType.FIRST_PARTY),
            supplier(222, SupplierType.REAL_SUPPLIER).setRealSupplierId("222"),
            supplier(333, SupplierType.REAL_SUPPLIER).setRealSupplierId("333"),
            supplier(3333, SupplierType.REAL_SUPPLIER).setRealSupplierId("3333"),
            supplier(444, SupplierType.REAL_SUPPLIER).setRealSupplierId("4444"),
            supplier(555)
        );
        serviceOfferReplicaRepository.save(
            offer(111, "ssku-111", SupplierType.FIRST_PARTY, 111L, 111),
            offer(222, "ssku-222", SupplierType.REAL_SUPPLIER, 222L, 222),
            offer(333, "ssku-333", SupplierType.REAL_SUPPLIER, 333L, 333),
            offer(3333, "ssku-3333", SupplierType.REAL_SUPPLIER, 333L, 333),
            offer(444, "ssku-444", SupplierType.REAL_SUPPLIER, 444L, 444),
            offer(555, "ssku-555", 111L, 111)
        );
        deepmindCategoryManagerRepository.save(
            categoryManager(111, "catman_111")
        );
        // test batch processing
        var fromTs = Instant.now().minusSeconds(100);
        var untilTs = Instant.now().plusSeconds(10);
        var auditList = mskuStatusAuditService
            .getMskuStatusAuditInfoByProperty("mskuStatus", 0, 100, fromTs, untilTs);
        // check audit records
        Assertions
            .assertThat(auditList.getAuditInfoList())
            .extracting(MboAudit.MboAction::getNewValue)
            .containsExactlyInAnyOrder(
                MskuStatusValue.ARCHIVE.getLiteral(), MskuStatusValue.REGULAR.getLiteral(),
                MskuStatusValue.END_OF_LIFE.getLiteral(), MskuStatusValue.SEASONAL.getLiteral()
            );
        lifecycleStatusesStatsCalculator.setAuditBatchSize(2);
        // firstly expecting nothing returned from sale
        Assertions
            .assertThat(lifecycleStatusesStatsCalculator.mskusReturnedToSale(
                Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS)))
            .isEmpty();
        // return 2 msku to sale
        var statuses = mskuStatusRepository.findAllMap();
        mskuStatusRepository.save(
            statuses.get(111L).setMskuStatus(MskuStatusValue.REGULAR),
            statuses.get(333L).setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(111L)
        );
        Assertions
            .assertThat(lifecycleStatusesStatsCalculator.mskusReturnedToSale(
                Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS)))
            .hasSize(3)
            .usingElementComparatorIgnoringFields("returnDate", "purchaser")
            .containsExactlyInAnyOrder(
                mskuReturnedToSale(111, "ssku-111", 111L, "", 111L,
                    "catman_111", MskuStatusValue.REGULAR),
                mskuReturnedToSale(333, "ssku-333", 333L, "", 333L,
                    null, MskuStatusValue.SEASONAL),
                mskuReturnedToSale(3333, "ssku-3333", 333L, "", 333L,
                    null, MskuStatusValue.SEASONAL)
            );
    }

    @Test
    public void preNpdMskusUnavailableOnStockTest() {
        deepmindMskuRepository.save(msku(111), msku(222), msku(333), msku(444, 444),
            msku(555, 555), msku(666, 666), msku(777, 777), msku(888, 888));
        mskuStatusRepository.save(
            mskuStatus(111L, MskuStatusValue.REGULAR),
            mskuStatus(222L, MskuStatusValue.REGULAR),
            mskuStatus(333L, MskuStatusValue.REGULAR),
            mskuStatus(444L, MskuStatusValue.PRE_NPD),
            mskuStatus(555L, MskuStatusValue.PRE_NPD),
            mskuStatus(666L, MskuStatusValue.PRE_NPD),
            mskuStatus(777L, MskuStatusValue.PRE_NPD),
            mskuStatus(888L, MskuStatusValue.PRE_NPD)
        );
        var startAt444 = Instant.now().minus(17, ChronoUnit.DAYS);
        var startAt555 = Instant.now().minus(20, ChronoUnit.DAYS);
        var startAt777 = Instant.now().minus(25, ChronoUnit.DAYS);
        jdbcTemplate.update("update msku.msku_status set status_start_at = :dt where market_sku_id = :id",
            Map.of("id", 444, "dt", Timestamp.from(startAt444))
        );
        jdbcTemplate.update("update msku.msku_status set status_start_at = :dt where market_sku_id = :id",
            Map.of("id", 555, "dt", Timestamp.from(startAt555))
        );
        jdbcTemplate.update("update msku.msku_status set status_start_at = :dt where market_sku_id = :id",
            Map.of("id", 777, "dt", Timestamp.from(startAt777))
        );

        deepmindCategoryRepository.insertBatch(category(444L), category(555L), category(666L), category(777L),
            category(888L));
        deepmindSupplierRepository.save(
            supplier(444, SupplierType.REAL_SUPPLIER).setRealSupplierId("444"),
            supplier(4444, SupplierType.FIRST_PARTY),
            supplier(44444, SupplierType.FIRST_PARTY), supplier(555, SupplierType.FIRST_PARTY),
            supplier(5555, SupplierType.REAL_SUPPLIER).setRealSupplierId("5555"), supplier(55555),
            supplier(666, SupplierType.FIRST_PARTY), supplier(6666, SupplierType.FIRST_PARTY),
            supplier(66666, SupplierType.FIRST_PARTY), supplier(777, SupplierType.FIRST_PARTY),
            supplier(7777, SupplierType.FIRST_PARTY), supplier(77777, SupplierType.FIRST_PARTY),
            supplier(888, SupplierType.FIRST_PARTY)
        );
        serviceOfferReplicaRepository.save(
            offer(444, "ssku-444", SupplierType.REAL_SUPPLIER, 444L, 444),
            offer(4444, "ssku-4444", SupplierType.FIRST_PARTY, 444L, 444),
            offer(44444, "ssku-44444", SupplierType.FIRST_PARTY, 444L, 444),
            offer(555, "ssku-555", SupplierType.FIRST_PARTY, 555L, 555),
            offer(5555, "ssku-5555", SupplierType.REAL_SUPPLIER, 555L, 555),
            offer(55555, "ssku-55555", SupplierType.THIRD_PARTY, 555L, 555),
            offer(666, "ssku-666", SupplierType.FIRST_PARTY, 666L, 666),
            offer(6666, "ssku-6666", SupplierType.FIRST_PARTY, 666L, 666),
            offer(66666, "ssku-66666", SupplierType.FIRST_PARTY, 666L, 666),
            offer(777, "ssku-777", SupplierType.FIRST_PARTY, 777L, 777),
            offer(7777, "ssku-7777", SupplierType.FIRST_PARTY, 777L, 777),
            offer(77777, "ssku-77777", SupplierType.FIRST_PARTY, 777L, 777),
            offer(888, "ssku-888", SupplierType.FIRST_PARTY, 888L, 888)
        );
        insertStocks(444, "ssku-444", 2);
        insertStocks(4444, "ssku-4444", 0);
        insertStocks(44444, "ssku-44444", 2);
        insertStocks(555, "ssku-555", 0);
        insertStocks(5555, "ssku-5555", 0);
        insertStocks(55555, "ssku-55555", 0);
        insertStocks(666, "ssku-666", 0);
        insertStocks(6666, "ssku-6666", 2);
        insertStocks(66666, "ssku-66666", 0);
        insertStocks(777, "ssku-777", 0);
        // add several warehouses to check duplicates
        mskuStockRepository.insertOrUpdate(new MskuStockInfo()
            .setShopSkuKey(new ServiceOfferKey(777, "ssku-777"))
            .setFitInternal(0)
            .setWarehouseId((int) SOFINO_ID)
        );
        insertStocks(777, "ssku-777", 0);
        insertStocks(7777, "ssku-7777", 2);
        insertStocks(77777, "ssku-77777", 0);
        insertStocks(888, "ssku-888", 2);

        deepmindCategoryManagerRepository.save(
            categoryManager(444, "catman_444"),
            categoryManager(777, "catman_777")
        );

        var tmp = lifecycleStatusesStatsCalculator.preNpdMskusUnavailableOnStock();
        Assertions
            .assertThat(tmp)
            .usingElementComparatorIgnoringFields("startDate", "purchaser")
            .containsExactlyInAnyOrder(
                requiredSskuWithNoStock(444,
                    startAt444.toString(), 444, "catman_444", 17),
                requiredSskuWithNoStock(555,
                    startAt555.toString(), 555, null, 20),
                requiredSskuWithNoStock(777,
                    startAt777.toString(), 777, "catman_777", 25)
            );
    }

    private LifecycleStatusesStat.MskuReturnedToSale mskuReturnedToSale(
        int supplierId, String shopSku, long mskuId, String returnedDate, long category, String catman,
        MskuStatusValue status) {
        return new LifecycleStatusesStat.MskuReturnedToSale()
            .setReturnDate(returnedDate)
            .setMskuStatus(status.getLiteral())
            .setSskuMskuPair(new ServiceOfferKey(supplierId, shopSku), mskuId)
            .setCategoryId(category)
            .setCategory("name_" + category, category)
            .setCatmanLogin(catman);
    }

    private LifecycleStatusesStat.RequiredSskuWithNoStock requiredSskuWithNoStock(
        long mskuId, String startAt, long category, String catman, int delay) {
        return new LifecycleStatusesStat.RequiredSskuWithNoStock()
            .setStartDate(startAt)
            .setDelayDays(delay)
            .setMskuId(mskuId)
            .setCategoryId(category)
            .setCategory("name_" + category, category)
            .setCatmanLogin(catman);
    }

    private Supplier supplier(Integer id) {
        return new Supplier().setId(id).setName(id.toString()).setSupplierType(THIRD_PARTY);
    }

    private Supplier supplier(Integer id, SupplierType supplierType) {
        return new Supplier().setId(id).setName(id.toString()).setSupplierType(supplierType);
    }

    private CategoryManager categoryManager(long categoryId, String login) {
        return new CategoryManager()
            .setCategoryId(categoryId)
            .setStaffLogin(login)
            .setRole(CATMAN)
            .setFirstName(login)
            .setLastName(login);
    }

    private Category category(Long id) {
        return new Category()
            .setCategoryId(id)
            .setName("name_" + id);
    }

    private Msku msku(long id) {
        return new Msku()
            .setId(id)
            .setTitle("Msku #" + id)
            .setDeleted(false)
            .setVendorId(1L)
            .setModifiedTs(Instant.now())
            .setCategoryId(1L)
            .setSkuType(SkuTypeEnum.SKU);
    }

    private Msku msku(long id, long categoryId) {
        return msku(id)
            .setCategoryId(categoryId);
    }

    private MskuStatus mskuStatus(long mskuId, MskuStatusValue status) {
        var mskuStatus = new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(status)
            .setStatusStartAt(Instant.now())
            .setNpdStartDate(LocalDate.now());
        if (status == MskuStatusValue.NPD) {
            mskuStatus.setNpdStartDate(LocalDate.now());
        }
        if (status == MskuStatusValue.SEASONAL) {
            mskuStatus.setSeasonId(111L);
        }
        if (status == MskuStatusValue.IN_OUT) {
            mskuStatus.setInoutStartDate(LocalDate.now());
            mskuStatus.setInoutFinishDate(LocalDate.now().plusDays(60));
        }
        return mskuStatus;
    }

    private ServiceOfferReplica offer(
        int supplierId, String shopSku, long mskuId, long categoryId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ServiceOfferReplica offer(
        int supplierId, String shopSku, SupplierType supplierType, long mskuId, long categoryId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private void insertStocks(int supplierId, String shopSku, int fit) {
        mskuStockRepository.insertOrUpdate(new MskuStockInfo()
            .setShopSkuKey(new ServiceOfferKey(supplierId, shopSku))
            .setFitInternal(fit)
            .setWarehouseId((int) ROSTOV_ID)
        );
    }
}
