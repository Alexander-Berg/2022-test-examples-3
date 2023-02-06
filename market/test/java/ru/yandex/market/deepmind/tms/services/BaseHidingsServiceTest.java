package ru.yandex.market.deepmind.tms.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import com.google.common.base.Preconditions;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.jooq.DSLContext;
import org.jooq.impl.TableRecordImpl;
import org.junit.After;
import org.junit.Before;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.common.utils.YqlOverPgUtils;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables.HIDING_REASON_DESCRIPTION;

public abstract class BaseHidingsServiceTest extends DeepmindBaseDbTestClass {
    public static final int BERU_ID = 10264169;

    public static final MboUser USER_1 = new MboUser(12345, "Вася пупкин", "agent007@y.ru");
    public static final MboUser USER_2 = new MboUser(54321, "Мася", "masya@y.ru");
    public static final MboUser USER_3 = new MboUser(98765, "Тиша", "tisha@y.ru");

    @Resource(name = "deepmindDsl")
    protected DSLContext dslContext;
    @Resource
    protected HidingRepository hidingRepository;
    @Resource
    protected HidingReasonDescriptionRepository hidingReasonDescriptionRepository;
    @Resource
    protected JdbcTemplate jdbcTemplate;
    @Resource
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource(name = TestYqlOverPgDatasourceConfig.YQL_OVER_PG_TEMPLATE)
    protected JdbcTemplate yqlJdbcTemplate;
    @Resource(name = TestYqlOverPgDatasourceConfig.YQL_OVER_PG_NAMED_TEMPLATE)
    protected NamedParameterJdbcTemplate namedYqlJdbcTemplate;
    @Resource
    protected SupplierRepository deepmindSupplierRepository;
    @Resource
    protected MskuRepository deepmindMskuRepository;
    @Resource
    protected StorageKeyValueService deepmindStorageKeyValueService;
    @Resource
    protected ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    protected DeepmindWarehouseRepository deepmindWarehouseRepository;

    protected EnhancedRandom random;
    protected String mapReducePool;

    protected YPath stopWordsTable;
    protected YPath mbiHidingsTable;
    protected YPath offersTable;
    protected YPath hiddenSuppliersTable;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        YqlOverPgUtils.setTransformYqlToSql(true);

        random = TestUtils.createMskuRandom();
        mapReducePool = "unit-test";

        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));

        stopWordsTable = YPath.simple("//tmp/stop_words_folder/20200101_0101");
        mbiHidingsTable = YPath.simple("//tmp/mbi_hiddings_folder/20200101");
        offersTable = YPath.simple("//tmp/mboc/offers_table");
        hiddenSuppliersTable = YPath.simple("//tmp/hidden_suppliers_folder/20200101_0101");

        mockYtOffersTableInPg(yqlJdbcTemplate);
        mockYtMbiHidingTableInPg(yqlJdbcTemplate);
        mockYtStopWordsTableInPg(yqlJdbcTemplate);
        mockYtHiddenSuppliersTableInPg(yqlJdbcTemplate);
    }

    @After
    public void tearDown() {
        YqlOverPgUtils.setTransformYqlToSql(false);
    }

    protected void mockYtOffersTableInPg(JdbcOperations yqlOnPgTemplate) {
        yqlOnPgTemplate.execute("create view " + offersTable + "\n" +
            " (supplier_id, shop_sku, raw_supplier_id, raw_shop_sku, supplier_type) AS\n" +
            "SELECT CASE\n" +
            "           WHEN s.supplier_type = 'REAL_SUPPLIER'::mbo_category.supplier_type THEN\n" +
            "                " + BERU_ID + "\n" +
            "           ELSE o.supplier_id\n" +
            "           END AS supplier_id,\n" +
            "       CASE\n" +
            "           WHEN s.supplier_type = 'REAL_SUPPLIER'::mbo_category.supplier_type\n" +
            "               THEN concat(s.real_supplier_id, '.', o.shop_sku)\n" +
            "           ELSE o.shop_sku\n" +
            "           END AS shop_sku,\n" +
            "           o.supplier_id AS raw_supplier_id,\n" +
            "       o.shop_sku AS raw_shop_sku,\n" +
            "       s.supplier_type AS supplier_type\n," +
            "       o.msku_id as approved_market_sku_id\n" +
            "FROM msku.offer o\n" +
            "         JOIN msku.supplier s ON o.supplier_id = s.id");
    }

    protected void mockYtMbiHidingTableInPg(JdbcOperations yqlOnPgTemplate) {
        yqlOnPgTemplate.execute("" +
            "create table " + mbiHidingsTable + " (\n" +
            "  supplier_id  bigint,\n" +
            "  shop_sku     text,\n" +
            "  hidden_at    text,\n" +
            "  timeout      text,\n" +
            "  reason       text,\n" +
            "  subreason    text,\n" +
            "  mustache_template text,\n" +
            "  source       text,\n" +
            "  date         text" +
            ");");
    }

    protected void mockYtStopWordsTableInPg(JdbcOperations yqlOnPgTemplate) {
        yqlOnPgTemplate.execute("" +
            "create table " + stopWordsTable + " (\n" +
            "  msku_id      bigint,\n" +
            "  title        text,\n" +
            "  stop_word    text,\n" +
            "  category_id  bigint,\n" +
            "  user_id      bigint,\n" +
            "  user_name    text,\n" +
            "  comment      text,\n" +
            "  creation_time bigint,\n" +
            "  mbo_stuff_version text" +
            ");");
    }

    protected void mockYtHiddenSuppliersTableInPg(JdbcOperations yqlOnPgTemplate) {
        yqlOnPgTemplate.execute("" +
            "create table " + hiddenSuppliersTable + " (\n" +
            "  shop_id      bigint, " +
            "  from_time    text " +
            ");");
    }

    protected List<Hiding> getAllHidings() {
        return hidingRepository.findAll();
    }

    protected void insertHidings(Hiding... hidings) {
        hidingRepository.save(hidings);
    }

    protected void insertHidingsReasonDescriptions(HidingReasonDescription... reasonDescriptions) {
        Arrays.stream(reasonDescriptions)
            .map(description -> dslContext.newRecord(Tables.HIDING_REASON_DESCRIPTION, description))
            .forEach(TableRecordImpl::insert);
    }

    protected Map<String, HidingReasonDescription> insertHidingsReasonDescriptionsWithRes(
        HidingReasonDescription... reasonDescriptions) {
        return insertHidingsReasonDescriptionsWithRes(List.of(reasonDescriptions));
    }

    protected Map<String, HidingReasonDescription> insertHidingsReasonDescriptionsWithRes(
        Collection<HidingReasonDescription> reasonDescriptions) {
        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExists(reasonDescriptions);
        return hidingReasonDescriptionRepository.findByReasonKeysMap(
            reasonDescriptions.stream().map(HidingReasonDescription::getReasonKey).collect(Collectors.toSet()));
    }

    protected Set<String> getExistingReasonKeys() {
        return dslContext.select(HIDING_REASON_DESCRIPTION.REASON_KEY)
            .from(HIDING_REASON_DESCRIPTION)
            .fetchSet(HIDING_REASON_DESCRIPTION.REASON_KEY);
    }

    protected void addMbiHiding(HidingReason reason, HidingReason subreason, ServiceOfferReplica offer,
                                String hiddenAt) {
        addMbiHiding(offer.getBusinessId(), offer.getShopSku(), hiddenAt, reason, subreason);
    }

    protected void addMbiHiding(String reason, String subreason, ServiceOfferReplica offer, String hiddenAt) {
        addMbiHiding(offer.getBusinessId(), offer.getShopSku(), hiddenAt, reason, subreason);
    }

    protected void addMbiHiding(HidingReason reason, HidingReason subreason,
                                ru.yandex.market.mboc.common.offers.model.Offer offer, String hiddenAt) {
        addMbiHiding(offer.getBusinessId(), offer.getShopSku(), hiddenAt, reason, subreason);
    }

    protected void addMbiHiding(
        String reason, String subreason, ru.yandex.market.mboc.common.offers.model.Offer offer, String hiddenAt) {
        addMbiHiding(offer.getBusinessId(), offer.getShopSku(), hiddenAt, reason, subreason);
    }

    protected void addMbiHiding(int supplierId, String ssku, String hiddenAt,
                                HidingReason reason, HidingReason subreason) {
        Preconditions.checkArgument(reason.isReason());
        Preconditions.checkArgument(subreason.isSubReason());
        yqlJdbcTemplate.update("insert into `" + mbiHidingsTable + "` " +
                "(supplier_id, shop_sku, hidden_at, reason, subreason)\n" +
                "values (?,?,?,?,?)",
            supplierId, ssku, hiddenAt, reason.toString(), subreason.toString());
    }

    protected void addMbiHiding(int supplierId, String ssku, String hiddenAt,
                                String reason, String subreason) {
        yqlJdbcTemplate.update("insert into `" + mbiHidingsTable + "` " +
                "(supplier_id, shop_sku, hidden_at, reason, subreason)\n" +
                "values (?,?,?,?,?)",
            supplierId, ssku, hiddenAt, reason, subreason);
    }

    protected void addStopWordHiding(Msku msku, MboUser user, String stopWord, String comment, String creationTime) {
        addStopWordHiding(msku.getId(), msku.getTitle(), msku.getCategoryId(),
            user.getUid(), user.getFullName(),
            stopWord,
            comment, creationTime == null ? null : Instant.parse(creationTime),
            "20200313_1012");
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    protected void addStopWordHiding(long mskuId, String title, long categoryId,
                                     long userId, String userName,
                                     String stopWord,
                                     @Nullable String comment, @Nullable Instant creationTime,
                                     String mboStuffVersion) {
        long creationTimeTs = creationTime != null
            ? creationTime.toEpochMilli()
            : 0;
        yqlJdbcTemplate.update("insert into `" + stopWordsTable + "` " +
                "(msku_id, title, stop_word, category_id, user_id, user_name, comment, creation_time," +
                " mbo_stuff_version)\n" +
                "values (?,?,?,?,?,?,?,?,?)",
            mskuId, title, stopWord, categoryId, userId, userName, comment, creationTimeTs, mboStuffVersion);
    }

    protected void addHiddenSupplier(int supplierId, LocalDate hiddenAt) {
        yqlJdbcTemplate.update("insert into `" + hiddenSuppliersTable + "`" +
                "(shop_id, from_time) values (?, ?) ",
            supplierId, hiddenAt.toString());
    }

    protected HidingReasonDescription createReasonDescription(String reasonKey) {
        return createReasonDescription(reasonKey, HidingReasonType.REASON_KEY);
    }

    protected HidingReasonDescription createReasonDescription(String reasonKey, HidingReasonType type) {
        return new HidingReasonDescription().setReasonKey(reasonKey).setExtendedDesc("")
            .setType(type).setReplaceWithDesc("");
    }

    protected HidingReasonDescription createReasonDescription(String reasonKey, HidingReasonType type,
                                                              String replaceDesc) {
        return new HidingReasonDescription().setReasonKey(reasonKey).setExtendedDesc("")
            .setType(type).setReplaceWithDesc(replaceDesc);
    }

    protected HidingReasonDescription createReasonDescription(String reasonKey, String replaceDesc) {
        return new HidingReasonDescription().setReasonKey(reasonKey).setExtendedDesc("")
            .setType(HidingReasonType.REASON_KEY).setReplaceWithDesc(replaceDesc);
    }

    protected HidingReasonDescription createReasonDescription(String reasonKey, String description,
                                                              String replaceDesc) {
            return new HidingReasonDescription().setReasonKey(reasonKey).setExtendedDesc(description)
                .setType(HidingReasonType.REASON_KEY).setReplaceWithDesc(replaceDesc);
    }

    protected HidingReasonDescription createReasonDescription(String reasonKey, HidingReasonType type,
                                                              String description, String replaceDesc) {
        return new HidingReasonDescription().setReasonKey(reasonKey).setExtendedDesc(description)
            .setType(type).setReplaceWithDesc(replaceDesc);
    }

    protected Hiding createHiding(Long reasonKeyId, HidingReason subreason, ServiceOfferReplica offer,
                                  String hidingAt) {
        return createHiding(reasonKeyId, subreason.toString(), offer, null, hidingAt, null);
    }

    protected Hiding createHiding(Long reasonKeyId, String subreasonId, ServiceOfferReplica offer,
                                  MboUser user, @Nullable String hidingAt, @Nullable String comment) {
        return new Hiding()
            .setReasonKeyId(reasonKeyId)
            .setSubreasonId(subreasonId)
            .setSupplierId(offer.getSupplierId())
            .setShopSku(offer.getShopSku())
            .setUserId(user != null ? user.getUid() : null)
            .setUserName(user != null ? user.getFullName() : null)
            .setHiddenAt(hidingAt == null ? null : Instant.parse(hidingAt))
            .setComment(comment);
    }

    protected Hiding createHiding(Long reasonKeyId, HidingReason subreason, ServiceOfferReplica offer,
                                  LocalDateTime hidingAt) {
        return createHiding(reasonKeyId, subreason.toString(), offer, null, null, null)
            .setHiddenAt(hidingAt.atZone(ZoneId.systemDefault()).toInstant());
    }

    protected Hiding createHiding(Long reasonKeyId, HidingReason subreason, ServiceOfferReplica offer, int supplierId,
                                  String hidingAt) {
        return createHiding(reasonKeyId, subreason.toString(), offer, null, hidingAt, null)
            .setSupplierId(supplierId);
    }

    protected Hiding createHiding(Long reasonKeyId, HidingReason subreason, ServiceOfferReplica offer) {
        return createHiding(reasonKeyId, subreason.toString(), offer, null, null, null);
    }

    protected Msku createMsku(long mskuId) {
        return random.nextObject(Msku.class)
            .setId(mskuId)
            .setTitle("Msku #" + mskuId)
            .setDeleted(false);
    }

    protected ServiceOfferReplica offer(int supplierId, String ssku) {
        var supplier = deepmindSupplierRepository.findById(supplierId).orElseThrow();
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(1L)
            .setSupplierType(supplier.getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    protected ServiceOfferReplica offer(
        int supplierId, String ssku, long mskuId) {
        return offer(supplierId, ssku).setMskuId(mskuId);
    }

    protected ServiceOfferReplica offer(
        int supplierId, String ssku, Msku msku) {
        return offer(supplierId, ssku, msku.getId()).setCategoryId(msku.getCategoryId());
    }

    protected SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability availability) {
        return new SskuStatus()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setAvailability(availability);
    }
}
