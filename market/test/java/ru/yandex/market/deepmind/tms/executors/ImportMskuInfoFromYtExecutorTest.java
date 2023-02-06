package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuInfo;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.msku.info.MskuInfoRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.common.utils.YqlOverPgUtils;

/**
 * @author rbizhanov
 */
public class ImportMskuInfoFromYtExecutorTest extends DeepmindBaseDbTestClass {
    @Resource(name = TestYqlOverPgDatasourceConfig.YQL_OVER_PG_NAMED_TEMPLATE)
    private NamedParameterJdbcTemplate namedYqlJdbcTemplate;
    @Resource
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private MskuInfoRepository mskuInfoRepository;

    private ImportMskuInfoFromYtExecutor executor;
    private final long mskuId1 = 111L;
    private final long mskuId2 = 222L;
    private final long mskuId3 = 333L;
    private final long mskuId4 = 444L;
    private final long mskuId5 = 555L;
    private YPath mskuInfoYt;
    private YPath pricebandYt;

    @Before
    public void setUp() {
        YqlOverPgUtils.setTransformYqlToSql(true);
        mskuInfoYt = YPath.simple("//temp_yt_msku_info");
        pricebandYt = YPath.simple("//temp_yt_priceband");

        // tables in yt
        namedYqlJdbcTemplate.getJdbcTemplate().execute("" +
            "CREATE TABLE " + mskuInfoYt + "(" +
            "   msku bigint, " +
            "   in_target_assortment boolean " +
            ")"
        );

        namedYqlJdbcTemplate.getJdbcTemplate().execute("" +
            "CREATE TABLE " + pricebandYt + "(" +
            "   msku_id bigint, " +
            "   drinks_volume double precision, " +
            "   package_num integer, " +
            "   price double precision, " +
            "   price_per_one double precision, " +
            "   priceband_id bigint, " +
            "   priceband_label text, " +
            "   weight double precision " +
            ")"
        );

        // executor init
        executor = new ImportMskuInfoFromYtExecutor(
            namedYqlJdbcTemplate, jdbcTemplate, transactionTemplate,
            mskuInfoYt, pricebandYt, "pool");
        executor.setBatchSize(20);

        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        insertOffer(77, "sku-1", mskuId1);
        insertOffer(77, "sku-2", mskuId2);
        insertOffer(77, "sku-3", mskuId3);
        insertOffer(77, "sku-4", mskuId4);
    }

    @After
    public void tearDown() {
        YqlOverPgUtils.setTransformYqlToSql(false);
    }

    @Test
    public void testImport() {
        insertIntoMskuInfoYt(mskuId3);

        insertIntoPricebandYt(mskuId1, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0);
        insertIntoPricebandYt(mskuId2, 1.0, 3, 3.0, 1.0, 1L, "label2", 2.0);
        insertIntoPricebandYt(mskuId3, 1.0, 4, 3.0, 1.0, 1L, "label3", 3.0);

        executor.execute();

        Assertions.assertThat(mskuInfoRepository.findAll()).containsExactlyInAnyOrder(
            mskuInfo(mskuId1, false, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0),
            mskuInfo(mskuId2, false, 1.0, 3, 3.0, 1.0, 1L, "label2", 2.0),
            mskuInfo(mskuId3, true, 1.0, 4, 3.0, 1.0, 1L, "label3", 3.0)
        );
    }

    @Test
    public void testUpdate() {
        insertIntoPg(mskuInfo(mskuId1, true, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0));
        insertIntoPg(mskuInfo(mskuId2, true, 1.0, 3, 3.0, 1.0, 1L, "label2", 2.0));
        insertIntoMskuInfoYt(mskuId1);
        insertIntoPricebandYt(mskuId1, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0);
        insertIntoPricebandYt(mskuId2, 2.0, 3, 4.0, 1.0, 1L, "label22", 3.0);

        executor.execute();

        Assertions.assertThat(mskuInfoRepository.findAll()).containsExactlyInAnyOrder(
            mskuInfo(mskuId1, true, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0),
            mskuInfo(mskuId2, false, 2.0, 3, 4.0, 1.0, 1L, "label22", 3.0)
        );
    }

    @Test
    public void testDelete() {
        insertIntoPg(mskuInfo(mskuId1, false, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0));
        insertIntoPg(mskuInfo(mskuId2, false, 1.0, 3, 3.0, 1.0, 1L, "label2", 2.0));
        insertIntoPricebandYt(mskuId1, 3.0, 2, 4.0, 1.0, 1L, "label1", 1.0);

        executor.execute();

        Assertions.assertThat(mskuInfoRepository.findAll()).containsExactly(
            mskuInfo(mskuId1, false, 3.0, 2, 4.0, 1.0, 1L, "label1", 1.0)
        );
    }

    @Test
    public void testUpdateWithNullValues() {
        insertIntoMskuInfoYt(mskuId1);
        insertIntoPricebandYt(mskuId2, null, null,  null, null, null, null, null);
        insertIntoPricebandYt(mskuId3, 1.0, 4, 3.0, 1.0, 1L, "label3", 3.0);

        executor.execute();

        Assertions.assertThat(mskuInfoRepository.findAll()).containsExactlyInAnyOrder(
            mskuInfo(mskuId1, true, null, null,  null, null, null, null, null),
            mskuInfo(mskuId2, false, null, null,  null, null, null, null, null),
            mskuInfo(mskuId3, false, 1.0, 4, 3.0, 1.0, 1L, "label3", 3.0)
        );
    }

    @Test
    public void comboTest() {
        insertIntoPg(mskuInfo(mskuId1, false, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0));
        insertIntoPg(mskuInfo(mskuId2, false, 1.0, 3, 3.0, 1.0, 1L, "label2", 2.0));
        insertIntoPg(mskuInfo(mskuId3, false, 1.0, 4, 3.0, 1.0, 1L, "label3", 3.0));
        insertIntoMskuInfoYt(mskuId2);
        insertIntoPricebandYt(mskuId1, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0);
        insertIntoPricebandYt(mskuId2, 3.0, 3, 4.0, 1.0, 1L, "label2223", 3.0);
        insertIntoPricebandYt(mskuId5, 5.0, 5, 5.0, 2.0, 1L, "label5", 3.0);

        executor.execute();

        Assertions.assertThat(mskuInfoRepository.findAll()).containsExactly(
            mskuInfo(mskuId1, false, 1.0, 2, 3.0, 1.0, 1L, "label1", 1.0),
            mskuInfo(mskuId2, true, 3.0, 3, 4.0, 1.0, 1L, "label2223", 3.0),
            mskuInfo(mskuId5, false, 5.0, 5, 5.0, 2.0, 1L, "label5", 3.0)
        );
    }

    private void insertOffer(int supplierId, String shopSku, long mskuId) {
        insertOffer(supplierId, shopSku, SupplierType.REAL_SUPPLIER, mskuId, 0);
    }

    private void insertOffer(int supplierId, String shopSku, SupplierType supplierType, long mskuId, long categoryId) {
        var supplier = new Supplier().setId(supplierId)
            .setName("test_supplier_" + supplierId).setSupplierType(supplierType);
        if (supplierType == SupplierType.REAL_SUPPLIER) {
            supplier.setRealSupplierId("00004" + supplierId);
        }
        var offer = new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(categoryId)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
        var suppliers = deepmindSupplierRepository.findByIds(List.of(supplierId));
        if (suppliers.isEmpty()) {
            deepmindSupplierRepository.save(supplier);
        }
        serviceOfferReplicaRepository.save(offer);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private MskuInfo mskuInfo(long mskuId, boolean inTargetAssortment, Double drinksVolume, Integer packageNum,
                              Double price, Double pricePerOne, Long pricebandId, String pricebandLabel,
                              Double weight) {
        return new MskuInfo()
            .setMarketSkuId(mskuId)
            .setInTargetAssortment(inTargetAssortment)
            .setDrinksVolume(drinksVolume)
            .setPackageNum(packageNum)
            .setPrice(price)
            .setPricePerOne(pricePerOne)
            .setPricebandId(pricebandId)
            .setPricebandLabel(pricebandLabel)
            .setWeight(weight);
    }

    private void insertIntoMskuInfoYt(long mskuId) {
        namedYqlJdbcTemplate.getJdbcTemplate().update(
            "INSERT INTO " + mskuInfoYt + "(msku, in_target_assortment) " +
                "VALUES (?, ?)",
            mskuId, true);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void insertIntoPricebandYt(long mskuId, Double drinksVolume, Integer packageNum, Double price,
                                       Double pricePerOne, Long pricebandId, String pricebandLabel, Double weight) {
        namedYqlJdbcTemplate.getJdbcTemplate().update(
            "INSERT INTO " + pricebandYt + " (msku_id, drinks_volume, package_num, price, price_per_one, " +
                "priceband_id, priceband_label, weight) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            mskuId, drinksVolume, packageNum, price, pricePerOne, pricebandId, pricebandLabel, weight);
    }

    private void insertIntoPg(MskuInfo mskuInfo) {
        jdbcTemplate.getJdbcTemplate().update(
            "INSERT INTO msku.msku_info(market_sku_id, in_target_assortment, drinks_volume, package_num, price, " +
                "price_per_one, priceband_id, priceband_label, weight) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            mskuInfo.getMarketSkuId(), mskuInfo.getInTargetAssortment(), mskuInfo.getDrinksVolume(),
            mskuInfo.getPackageNum(), mskuInfo.getPrice(), mskuInfo.getPricePerOne(), mskuInfo.getPricebandId(),
            mskuInfo.getPricebandLabel(), mskuInfo.getWeight());
    }
}
