package ru.yandex.market.deepmind.common.hiding.ticket;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryTeam;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketHistory;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketProcessing;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.HidingTicketHistoryRepository;
import ru.yandex.market.deepmind.common.repository.HidingTicketProcessingRepository;
import ru.yandex.market.deepmind.common.repository.HidingTicketSskuRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.category.DeepmindCategoryRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.CategoryManagerTeamService;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATDIR;
import static ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository.CATMAN;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;


public abstract class BaseHidingTicketTest extends DeepmindBaseDbTestClass {
    @Resource
    protected NamedParameterJdbcTemplate jdbcTemplate;
    @Resource
    protected SupplierRepository deepmindSupplierRepository;
    @Resource
    protected MskuRepository deepmindMskuRepository;
    @Resource
    protected MskuStatusRepository mskuStatusRepository;
    @Resource
    protected HidingRepository hidingRepository;
    @Resource
    protected HidingTicketSskuRepository hidingTicketSskuRepository;
    @Resource
    protected HidingTicketHistoryRepository hidingTicketHistoryRepository;
    @Resource
    protected HidingTicketProcessingRepository hidingTicketProcessingRepository;
    @Resource
    protected MskuStockRepository mskuStockRepository;
    @Resource
    protected DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    protected DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    protected SskuStatusRepository sskuStatusRepository;
    @Resource(name = "deepmindTransactionHelper")
    protected TransactionHelper transactionHelper;
    @Resource
    protected DeepmindCategoryRepository deepmindCategoryRepository;
    @Resource
    protected HidingReasonDescriptionRepository hidingReasonDescriptionRepository;
    @Resource
    protected ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    protected EnhancedRandom random;
    protected CategoryManagerTeamService categoryManagerTeamService;
    protected DeepmindCategoryCachingServiceMock categoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();

    @Before
    public void beforeBase() {
        categoryManagerTeamService = new CategoryManagerTeamService(
            deepmindCategoryManagerRepository, deepmindCategoryTeamRepository, categoryCachingServiceMock
        );
        random = new EnhancedRandomBuilder().seed(1).build();
    }

    protected void insertOffer(int supplierId, String shopSku, long mskuId, long categoryId) {
        insertOffer(supplierId, shopSku, SupplierType.REAL_SUPPLIER, OfferAvailability.ACTIVE, mskuId, categoryId);
    }

    protected void insertOffer(int supplierId, String shopSku, SupplierType supplierType,
                               OfferAvailability availability, int mskuId) {
        insertOffer(supplierId, shopSku, supplierType, availability, mskuId, 0);
    }

    protected void insertOffer(int supplierId, String shopSku, SupplierType supplierType,
                               OfferAvailability availability, long mskuId, long categoryId) {
        var supplier = new Supplier().setId(supplierId)
            .setName("test_supplier_" + supplierId)
            .setSupplierType(supplierType);
        if (supplierType == SupplierType.REAL_SUPPLIER) {
            var rsId = "000000" + supplierId;
            supplier.setRealSupplierId(rsId.substring(rsId.length() - 6));
        }
        serviceOfferReplicaRepository.save(
            new ServiceOfferReplica()
                .setBusinessId(supplierId)
                .setSupplierId(supplierId)
                .setShopSku(shopSku)
                .setTitle("Offer: " + shopSku)
                .setCategoryId(categoryId)
                .setSeqId(0L)
                .setMskuId(mskuId)
                .setSupplierType(supplierType)
                .setAcceptanceStatus(OfferAcceptanceStatus.OK)
                .setModifiedTs(Instant.now())
        );
        var suppliers = deepmindSupplierRepository.findByIds(List.of(supplierId));
        if (suppliers.isEmpty()) {
            deepmindSupplierRepository.save(supplier);
        }
        insertStocks(supplierId, shopSku, 10);
        sskuStatusRepository.save(new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(OfferAvailability.valueOf(availability.name()))
        );
    }

    protected void insertStocks(int supplierId, String shopSku, int fit) {
        mskuStockRepository.insertOrUpdate(new MskuStockInfo()
            .setShopSkuKey(new ServiceOfferKey(supplierId, shopSku))
            .setFitInternal(fit)
            .setWarehouseId((int) ROSTOV_ID)
        );
    }

    protected void insertCatman(long categoryId, String staffLogin) {
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setStaffLogin(staffLogin).setCategoryId(categoryId).setRole(CATMAN)
                .setFirstName(staffLogin).setLastName(staffLogin)
        );
    }

    protected void insertCatDir(long categoryId, String staffLogin) {
        deepmindCategoryManagerRepository.save(
            new CategoryManager().setStaffLogin(staffLogin).setCategoryId(categoryId).setRole(CATDIR)
                .setFirstName(staffLogin).setLastName(staffLogin)
        );
    }

    protected void insertCatteam(long categoryId, String catteam) {
        deepmindCategoryTeamRepository.save(new CategoryTeam().setCategoryId(categoryId).setCatteam(catteam));
    }

    protected List<Category> insertCategories(Collection<Category> categories) {
        return deepmindCategoryRepository.insertOrUpdateAll(categories);
    }

    protected void insertMskuStatus(long... mskuIds) {
        for (long mskuId : mskuIds) {
            insertMskuStatus(mskuId, MskuStatusValue.REGULAR);
        }
    }

    protected void insertMskuStatus(long mskuId, MskuStatusValue status) {
        var msku = TestUtils.newMsku(mskuId);
        var mskuStatus = TestUtils.randomMskuStatus(random)
            .setMarketSkuId(mskuId)
            .setMskuStatus(status)
            .setSeasonId(null);
        deepmindMskuRepository.save(msku);
        mskuStatusRepository.save(mskuStatus);
    }

    protected void insertHiding(int supplierId, String shopSku, String reasonKey) {
        insertHiding(supplierId, shopSku, reasonKey, reasonKey);
    }

    protected void insertHiding(int supplierId, String shopSku, String reasonKey, String subreasonId) {
        hidingRepository.save(createHiding(supplierId, shopSku, reasonKey, subreasonId));
    }

    protected Hiding createHiding(int supplierId, String shopSku, String reasonKey, String subreasonId) {
        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExists(new HidingReasonDescription()
            .setReasonKey(reasonKey).setExtendedDesc("").setType(HidingReasonType.REASON_KEY));
        var description = hidingReasonDescriptionRepository.findByReasonKeys(reasonKey).get(0);
        return new Hiding(supplierId, shopSku, Instant.now(), "test",
            "test_", subreasonId, -1L, null, description.getId());
    }

    protected void insertHidingTicket(int supplierId, String shopSku, String reason) {
        insertHidingTicket(supplierId, shopSku, reason, true);
    }

    protected void insertHidingTicket(int supplierId, String shopSku, String reason, boolean isEffectivelyHidden) {
        var row = hidingTicketSsku(reason, null, supplierId, shopSku)
            .setIsEffectivelyHidden(isEffectivelyHidden);
        hidingTicketSskuRepository.save(row);
    }

    protected HidingTicketSsku hidingTicketSsku(String reasonKey, @Nullable String ticket, int supplierId, String sku) {
        return new HidingTicketSsku()
            .setReasonKey(reasonKey)
            .setTicket(ticket)
            .setSupplierId(supplierId)
            .setShopSku(sku)
            .setIsEffectivelyHidden(true)
            .setCreationTs(Instant.now());
    }

    protected static HidingTicketProcessing ticketProcessing(String reasonKey, String time) {
        return new HidingTicketProcessing().setReasonKey(reasonKey).setLastRunTs(Instant.parse(time))
            .setEnabled(true).setForceRun(false);
    }

    protected static HidingTicketHistory ticketHistory(String reasonKey, String ticket, int supplierId, String ssku) {
        return new HidingTicketHistory()
            .setTicket(ticket)
            .setReasonKey(reasonKey)
            .setSupplierId(supplierId)
            .setShopSku(ssku);
    }

    protected void insertCorefix(long mskuId) {
        jdbcTemplate.update("INSERT INTO msku.msku_info(market_sku_id, in_target_assortment)" +
                " VALUES (:mskuId, true)", Map.of("mskuId", mskuId)
        );
    }
}
