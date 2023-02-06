package ru.yandex.market.mboc.common.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.test.YamlTestUtil;

import static ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator.DEFAULT_CLASSIFIER_TRUST_THRESHOLD;
import static ru.yandex.market.mboc.common.offers.model.Offer.MappingConfidence.CONTENT;

/**
 * @author yuramalinov
 * @created 07.09.18
 */
public class OfferTestUtils {
    public static final long TEST_CATEGORY_INFO_ID = 91497;
    public static final int TEST_VENDOR_ID = 100500;
    public static final long TEST_MODEL_ID = 100600;
    public static final long TEST_SKU_ID = 100700;
    public static final int TEST_SUPPLIER_ID = 42;
    public static final int BLUE_SUPPLIER_ID_1 = 143;
    public static final int BLUE_SUPPLIER_ID_2 = 144;
    public static final int WHITE_SUPPLIER_ID = 243;
    public static final int FMCG_SUPPLIER_ID = 301;
    public static final int DSBS_SUPPLIER_ID = 443;
    public static final int REAL_SUPPLIER_ID = 555;
    public static final int BIZ_ID_SUPPLIER = 1000;
    public static final String PARAM_NAME = "PARAM";
    public static final int PARAM_ID = 10000;
    public static final int PARAM_VAL = 10001;
    public static final int GROUP_ID = 10;
    public static final String DEFAULT_GROUP_NAME = "model";
    public static final String DEFAULT_SHOP_SKU = "shop-sku";
    public static final String DEFAULT_TITLE = "title";
    public static final String DEFAULT_DESCRIPTION = "description";
    public static final String DEFAULT_CATEGORY = "shopCategory";
    public static final String DEFAULT_URL = "http://url";
    public static final String DEFAULT_PIC_URL = "http://picurl";
    public static final String DEFAULT_BARCODE = "somebarcode";
    public static final String DEFAULT_VENDORCODE = "somevendorcode";
    public static final String DEFAULT_SHOP_CATEGORY_NAME = "shop-category-name";

    public static final String DEFAULT_CATEGORY_NAME = "marketCategory";
    public static final String DEFAULT_VENDOR_NAME = "vendor";
    public static final String DEFAULT_MODEL_NAME = "marketModel";
    public static final String DEFAULT_SKU_NAME = "marketSku";

    public static final String MAPPING_NAME_PREFIX = "Mapping #";

    private static int counter = 0;

    private OfferTestUtils() {
    }

    /**
     * Returns minimal offer with required fields filled.
     */
    public static Offer simpleOffer(Supplier supplier) {
        return new Offer()
            .setBusinessId(TEST_SUPPLIER_ID)
            .setShopSku(DEFAULT_SHOP_SKU)
            .setTitle(DEFAULT_TITLE)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName(DEFAULT_SHOP_CATEGORY_NAME)
            .addNewServiceOfferIfNotExistsForTests(supplier);
    }

    public static Offer realOffer(Supplier supplier) {
        return new Offer()
            .setBusinessId(REAL_SUPPLIER_ID)
            .setShopSku(DEFAULT_SHOP_SKU)
            .setTitle(DEFAULT_TITLE)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName(DEFAULT_SHOP_CATEGORY_NAME)
            .addNewServiceOfferIfNotExistsForTests(supplier);
    }

    public static Offer simpleOkOffer(Supplier supplier) {
        return simpleOffer(supplier).updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
    }

    public static Offer simpleOffer() {
        return simpleOffer(simpleSupplier());
    }

    public static Offer firstPartyOffer() {
        return realOffer(firstPartySupplier());
    }

    public static Offer simpleOkOffer() {
        return simpleOkOffer(simpleSupplier());
    }

    public static OfferForService simpleOfferForService() {
        Supplier supplier = simpleSupplier();
        Offer offer = simpleOffer(supplier);
        return OfferForService.from(offer, supplier.getId());
    }

    public static Offer simpleOffer(long id) {
        return simpleOffer()
            .setId(id)
            .setShopSku(DEFAULT_SHOP_SKU + "-" + String.format("%05d", id))
            .setIsOfferContentPresent(true);
    }

    public static Offer firstPartyOffer(long id) {
        return firstPartyOffer()
            .setId(id)
            .setShopSku(DEFAULT_SHOP_SKU + "-" + String.format("%05d", id))
            .setIsOfferContentPresent(true);
    }

    /**
     * Returns minimal offer with required fields filled. With autoincremented shop_sku to skip conflicts.
     */
    public static Offer nextOffer() {
        return simpleOffer()
            .setShopSku(DEFAULT_SHOP_SKU + "-" + String.format("%05d", counter++));
    }

    public static Offer next1pOffer() {
        return firstPartyOffer()
            .setShopSku(DEFAULT_SHOP_SKU + "-" + String.format("%05d", counter++));
    }

    /**
     * Returns minimal offer with required fields filled. With autoincremented shop_sku to skip conflicts.
     */
    public static Offer nextOffer(Supplier supplier) {
        return simpleOffer(supplier)
            .setShopSku(DEFAULT_SHOP_SKU + "-" + String.format("%05d", counter++));
    }

    public static Supplier simpleSupplier() {
        return simpleSupplier(TEST_SUPPLIER_ID);
    }

    public static Supplier firstPartySupplier() {
        return firstPartySupplier(REAL_SUPPLIER_ID);
    }

    public static Supplier simpleSupplier(int supplierId) {
        return new Supplier(supplierId, "test")
            .setType(MbocSupplierType.THIRD_PARTY);
    }

    public static Supplier firstPartySupplier(int supplierId) {
        return new Supplier(supplierId, "real-sup-test")
            .setType(MbocSupplierType.REAL_SUPPLIER);
    }

    public static Supplier blueSupplierUnderBiz1() {
        return blueSupplierUnderBiz(BLUE_SUPPLIER_ID_1);
    }

    public static Supplier blueSupplierUnderBiz2() {
        return blueSupplierUnderBiz(BLUE_SUPPLIER_ID_2);
    }

    public static Supplier blueSupplierUnderBiz(int supplierId) {
        return new Supplier(supplierId, "test under biz " + supplierId)
            .setMbiBusinessId(BIZ_ID_SUPPLIER)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setDatacamp(true)
            .setType(MbocSupplierType.THIRD_PARTY);
    }

    public static Supplier whiteSupplierUnderBiz() {
        return whiteSupplier()
            .setId(WHITE_SUPPLIER_ID)
            .setName("white under biz")
            .setMbiBusinessId(BIZ_ID_SUPPLIER)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setDatacamp(true);
    }

    public static Supplier dsbsSupplierUnderBiz() {
        return whiteSupplier()
            .setId(DSBS_SUPPLIER_ID)
            .setType(MbocSupplierType.DSBS)
            .setName("dsbs under biz")
            .setMbiBusinessId(BIZ_ID_SUPPLIER)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setDatacamp(true);
    }

    public static Supplier whiteSupplier() {
        return new Supplier(TEST_SUPPLIER_ID, "white")
            .setType(MbocSupplierType.MARKET_SHOP);
    }

    public static Supplier fmcgSupplier() {
        return new Supplier(FMCG_SUPPLIER_ID, "fmcg")
            .setNewContentPipeline(true)
            .setType(MbocSupplierType.FMCG);
    }

    public static Supplier realSupplier() {
        return new Supplier(REAL_SUPPLIER_ID, "real")
            .setType(MbocSupplierType.REAL_SUPPLIER)
            .setBusinessId(REAL_SUPPLIER_ID)
            .setRealSupplierId("real_" + REAL_SUPPLIER_ID);
    }

    public static Supplier businessSupplier() {
        return new Supplier(BIZ_ID_SUPPLIER, "biznez")
            .setType(MbocSupplierType.BUSINESS)
            .setNewContentPipeline(true);
    }

    public static Supplier fulfillmentSupplier() {
        return new Supplier(TEST_SUPPLIER_ID, "fulfillment")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setFulfillment(true);
    }

    public static Supplier crossdockSupplier() {
        return new Supplier(TEST_SUPPLIER_ID, "crossdock")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setCrossdock(true);
    }

    public static Supplier dropshipBySellerSupplier() {
        return new Supplier(TEST_SUPPLIER_ID, "dropshipBySeller")
            .setType(MbocSupplierType.DSBS)
            .setDropshipBySeller(true);
    }

    public static Supplier dropshipSupplier() {
        return new Supplier(TEST_SUPPLIER_ID, "dropship")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setDropship(true);
    }

    public static Supplier clickAndCollectSupplier() {
        return new Supplier(TEST_SUPPLIER_ID, "clickAndCollect")
            .setType(MbocSupplierType.THIRD_PARTY)
            .setClickAndCollect(true);
    }

    public static String defaultMappingName(long skuId) {
        return MAPPING_NAME_PREFIX + skuId;
    }

    public static Offer.Mapping mapping(long skuId) {
        return mapping(skuId, defaultMappingName(skuId));
    }

    public static Offer.Mapping mapping(long skuId, String title) {
        return new Offer.Mapping(skuId, DateTimeUtils.dateTimeNow(), null);
    }

    public static Offer.Mapping mapping(long skuId, Offer.SkuType skuType) {
        return new Offer.Mapping(skuId, DateTimeUtils.dateTimeNow(), skuType);
    }

    public static CategoryInfo categoryInfoWithManualAcceptance() {
        CategoryInfo categoryInfo = new CategoryInfo(TEST_CATEGORY_INFO_ID);
        categoryInfo.setManualAcceptance(true);
        categoryInfo.setClassifierTrustThreshold((float) DEFAULT_CLASSIFIER_TRUST_THRESHOLD);
        return categoryInfo;
    }

    public static Category defaultCategory() {
        return new Category().setCategoryId(TEST_CATEGORY_INFO_ID)
            .setName(DEFAULT_CATEGORY_NAME)
            .setHasKnowledge(true)
            .setAcceptGoodContent(true)
            .setAcceptContentFromWhiteShops(true)
            .setAllowPskuWithoutBarcode(false);
    }

    public static DocumentOfferRelation generateDocumentOfferRelation(long seed) {
        return TestDataUtils.defaultRandom(seed)
            .nextObject(DocumentOfferRelation.class, "modifiedTimestamp", "shopSkuKey");
    }

    public static List<Offer> generateTestOffers(int count) {
        List<Offer> result = new ArrayList<>();
        Offer sampleOffer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        for (int i = 1; i <= count; i++) {
            sampleOffer.setId(i);
            sampleOffer.setShopSku("ssku" + i);
            sampleOffer.setIsOfferContentPresent(true);
            result.add(sampleOffer.copy());
        }
        return result;
    }

    public static Offer createOffer(int id, int supplierId, String ssku, long mskuId) {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setSupplierId(supplierId);
        offer.setShopSku(ssku);
        offer.setTitle("title " + ssku);
        offer.setCategoryIdForTests(99L, Offer.BindingKind.APPROVED);
        offer.updateApprovedSkuMapping(new Offer.Mapping(mskuId, LocalDateTime.now()), CONTENT);
        offer.setShopCategoryName("shop category");
        offer.setIsOfferContentPresent(true);
        offer.storeOfferContent(OfferContent.builder().build());
        offer.setServiceOffers(
            List.of(new Offer.ServiceOffer(supplierId).setSupplierType(MbocSupplierType.THIRD_PARTY)));
        return offer;
    }

    public static void hardSetYtStamp(NamedParameterJdbcTemplate jdbcTemplate, long offerId, Long ytStamp) {
        jdbcTemplate.update("update " + OfferRepositoryImpl.OFFER_TABLE
                + " set upload_to_yt_stamp = :yt_stamp where id = :id",
            new MapSqlParameterSource()
                .addValue("id", offerId)
                .addValue("yt_stamp", ytStamp)
        );
    }
}
