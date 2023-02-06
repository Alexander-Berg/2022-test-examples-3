package ru.yandex.market.mboc.common.dict;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 17.04.18
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SupplierRepositoryImplTest extends BaseDbTestClass {
    private static final int BIZ_ID = 20000;

    private static final Supplier MARKET_SHOP = new Supplier(12, "a MARKET_SHOP", "MARKET_SHOP.ru", "OOO MARKET_SHOP")
        .setType(MbocSupplierType.MARKET_SHOP);

    private static final Supplier MARKET_SHOP_2 = new Supplier(13, "a MARKET_SHOP 12", "MARKET_SHOP.ru", "OOO " +
        "MARKET_SHOP")
        .setType(MbocSupplierType.MARKET_SHOP);

    private static final Supplier REAL_SUPPLIER = new Supplier(3, "a test", "ozon.ru", "OOO romashka")
        .setRealSupplierId("000044")
        .setType(MbocSupplierType.REAL_SUPPLIER);

    private static final Supplier NULL_TYPE_SUPPLIER = new Supplier(1, "test", null, null);
    private static final Supplier THIRD_PARTY_SUPPLIER_1 = new Supplier(5, "a 3P 1", "THIRD_PARTY.ru", "OOO 3P 1")
        .setType(MbocSupplierType.THIRD_PARTY)
        .setFulfillment(true)
        .setCrossdock(true)
        .setDropship(false)
        .setClickAndCollect(false)
        .setDropshipBySeller(false);
    @SuppressWarnings("unused")
    private static final Supplier THIRD_PARTY_SUPPLIER_2 = new Supplier(6, "a 3P 2", "THIRD_PARTY.ru", "OOO 3P 2")
        .setType(MbocSupplierType.THIRD_PARTY)
        .setFulfillment(false)
        .setCrossdock(false)
        .setDropship(true)
        .setClickAndCollect(true)
        .setDropshipBySeller(false);
    @SuppressWarnings("unused")
    private static final Supplier THIRD_PARTY_SUPPLIER_3 = new Supplier(7, "a 3P 3", "THIRD_PARTY.ru", "OOO 3P 3")
        .setType(MbocSupplierType.THIRD_PARTY)
        .setFulfillment(false)
        .setCrossdock(false)
        .setDropship(true)
        .setClickAndCollect(false)
        .setDropshipBySeller(true);

    private static final Supplier FMCG = new Supplier(23, "a FMCG", "FMCG.ru", "OOO FMCG")
        .setType(MbocSupplierType.FMCG);

    private static final Supplier DSBS = new Supplier(99, "a DSBS", "DSBS.ru", "DSBS")
        .setType(MbocSupplierType.DSBS);

    private static final Supplier BIZ = new Supplier(BIZ_ID, "biz_id", "market4-0.ru", "new era")
        .setType(MbocSupplierType.BUSINESS);

    private static final Supplier LINKED_TO_BIZ1 = new Supplier(20001, "sup1", "market4-0.ru", "new era")
        .setRealSupplierId("000042")
        .setType(MbocSupplierType.REAL_SUPPLIER)
        .setBusinessId(BIZ_ID)
        .setMbiBusinessId(BIZ_ID);

    private static final Supplier SUPPLIER_WITH_NULL_BUSINESS_ID = new Supplier(20301, "sup301", "market4-0.ru", "new" +
        " era")
        .setRealSupplierId("000047")
        .setType(MbocSupplierType.REAL_SUPPLIER);

    private static final Supplier SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID = new Supplier(20321, "sup302", "market4-0" +
        ".ru", "new era")
        .setRealSupplierId("000048")
        .setMbiBusinessId(BIZ_ID)
        .setType(MbocSupplierType.REAL_SUPPLIER);

    private static final Supplier LINKED_TO_BIZ2 = new Supplier(20002, "sup2", "market4-0.ru", "new era")
        .setType(MbocSupplierType.THIRD_PARTY)
        .setBusinessId(BIZ_ID)
        .setMbiBusinessId(BIZ_ID);


    private static final Supplier DSBS_WITH_BIZ = new Supplier(101, "Business DSBS", "DSBS.biz", "DSBS BIZ")
        .setType(MbocSupplierType.DSBS)
        .setBusinessId(BIZ_ID)
        .setMbiBusinessId(BIZ_ID);

    private static final Supplier FIRST_PARTY = new Supplier(8, "a FIRST_PARTY", "FIRST_PARTY.ru", "OOO FIRST_PARTY")
        .setType(MbocSupplierType.FIRST_PARTY);

    private static final List<Supplier> allSuppliers = List.of(
        NULL_TYPE_SUPPLIER,
        THIRD_PARTY_SUPPLIER_1,
        REAL_SUPPLIER,
        FIRST_PARTY,
        MARKET_SHOP,
        MARKET_SHOP_2,
        FMCG,
        DSBS,
        BIZ,
        LINKED_TO_BIZ1,
        SUPPLIER_WITH_NULL_BUSINESS_ID,
        SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID,
        LINKED_TO_BIZ2,
        DSBS_WITH_BIZ
    );

    @Resource
    private SupplierRepositoryImpl supplierRepository;
    @Resource
    private OfferRepositoryImpl offerRepository;

    private List<Supplier> suppliers;

    private static Offer offer(Supplier supplier, String ssku) {
        return new Offer()
            .setBusinessId(supplier.getId())
            .setShopSku(ssku)
            .setTitle("title")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("category");
    }

    @Before
    public void setup() {
        suppliers = allSuppliers;

        supplierRepository.insertBatch(suppliers);
    }

    @Test
    public void testFindAll() {
        List<Supplier> selectedSuppliers = supplierRepository.findAll();
        assertThat(selectedSuppliers).containsExactlyInAnyOrderElementsOf(suppliers);
    }

    @Test
    public void testFilterWithMappings() {
        offerRepository.insertOffers(
            offer(REAL_SUPPLIER, "sku-real1"),
            offer(REAL_SUPPLIER, "sku-real2"),
            offer(THIRD_PARTY_SUPPLIER_1, "sku-third3")
        );

        List<Supplier> withMappings = supplierRepository.find(SupplierFilter.builder().withMappings(true).build());
        assertThat(withMappings).containsExactlyInAnyOrder(
            REAL_SUPPLIER,
            THIRD_PARTY_SUPPLIER_1
        );

        List<Supplier> withoutMappings = supplierRepository.find(SupplierFilter.builder().withMappings(false).build());
        assertThat(withoutMappings).containsExactlyInAnyOrder(
            NULL_TYPE_SUPPLIER,
            FIRST_PARTY,
            MARKET_SHOP,
            MARKET_SHOP_2,
            FMCG,
            DSBS,
            BIZ,
            LINKED_TO_BIZ1,
            LINKED_TO_BIZ2,
            DSBS_WITH_BIZ,
            SUPPLIER_WITH_NULL_BUSINESS_ID,
            SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID
        );
    }

    @Test
    public void testUpdate() {
        Supplier test = suppliers.get(0);
        test.setName("Something other");
        test.setDomain("domain.com");
        test.setOrganizationName("ooo romashka");

        supplierRepository.update(test);

        Supplier updated = supplierRepository.findById(test.getId());
        assertThat(updated).isEqualTo(test);
    }

    @Test(expected = NoSuchElementException.class)
    public void testDelete() {
        Supplier test = suppliers.get(0);
        supplierRepository.delete(test);

        // Will throw NoSuchElementException due to absence of such row.
        supplierRepository.findById(test.getId());
    }

    @Test
    public void testFindByType() {
        List<Supplier> real = supplierRepository.findByType(SupplierRepository.ByType.REAL);
        assertThat(real).containsExactlyInAnyOrderElementsOf(List.of(REAL_SUPPLIER, LINKED_TO_BIZ1,
            SUPPLIER_WITH_NULL_BUSINESS_ID, SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID));

        List<Supplier> unreal = supplierRepository.findByType(SupplierRepository.ByType.THIRD_PARTY);
        assertThat(unreal).containsExactlyInAnyOrderElementsOf(List.of(NULL_TYPE_SUPPLIER, THIRD_PARTY_SUPPLIER_1,
            LINKED_TO_BIZ2));

        assertThat(supplierRepository.findByType(SupplierRepository.ByType.FMCG)).containsOnlyOnce(FMCG);

        assertThat(supplierRepository.findByType(SupplierRepository.ByType.BUSINESS)).containsOnlyOnce(BIZ);

        assertThat(supplierRepository.findByType(SupplierRepository.ByType.DSBS))
            .containsExactlyInAnyOrder(DSBS, DSBS_WITH_BIZ);

        assertThat(supplierRepository.findByType(SupplierRepository.ByType.ALL))
            .containsExactlyInAnyOrderElementsOf(suppliers);

        // проверяем, что для любого значения реализован switch с фильтром и тестовый элемент
        Arrays.stream(SupplierRepository.ByType.values())
            .forEach(t -> assertThat(supplierRepository.findByType(t)).isNotEmpty());
    }

    @Test
    public void testFindByTypes() {
        List<Supplier> real =
            supplierRepository.find(SupplierFilter.builder().types(List.of(SupplierRepository.ByType.REAL)).build());
        assertThat(real).containsExactlyInAnyOrderElementsOf(List.of(REAL_SUPPLIER, LINKED_TO_BIZ1,
            SUPPLIER_WITH_NULL_BUSINESS_ID, SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID));

        List<Supplier> unreal =
            supplierRepository.find(SupplierFilter.builder().types(List.of(SupplierRepository.ByType.THIRD_PARTY)).build());
        assertThat(unreal).containsExactlyInAnyOrderElementsOf(List.of(NULL_TYPE_SUPPLIER, THIRD_PARTY_SUPPLIER_1,
            LINKED_TO_BIZ2));

        assertThat(supplierRepository.find(SupplierFilter.builder().types(List.of(SupplierRepository.ByType.FMCG)).build()))
            .containsOnlyOnce(FMCG);

        assertThat(supplierRepository.find(SupplierFilter.builder().types(List.of(SupplierRepository.ByType.BUSINESS)).build()))
            .containsOnlyOnce(BIZ);

        assertThat(supplierRepository.find(SupplierFilter.builder().types(List.of(SupplierRepository.ByType.ALL)).build()))
            .containsExactlyInAnyOrderElementsOf(suppliers);

        List<Supplier> mixed =
            supplierRepository.find(SupplierFilter.builder().types(List.of(SupplierRepository.ByType.REAL,
                SupplierRepository.ByType.THIRD_PARTY)).build());
        assertThat(mixed).containsExactlyInAnyOrderElementsOf(List.of(REAL_SUPPLIER, LINKED_TO_BIZ1, NULL_TYPE_SUPPLIER,
            THIRD_PARTY_SUPPLIER_1, LINKED_TO_BIZ2, SUPPLIER_WITH_NULL_BUSINESS_ID,
            SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID));

        // проверяем, что для любого значения реализован switch с фильтром и тестовый элемент
        Arrays.stream(SupplierRepository.ByType.values())
            .forEach(t -> assertThat(supplierRepository.find(SupplierFilter.builder().types(List.of(t)).build())).isNotEmpty());
    }

    @Test
    public void testFindBlue() {
        supplierRepository.insert(new Supplier(1001, "test").setType(MbocSupplierType.MARKET_SHOP));

        SupplierFilter filter = SupplierFilter.builder().blueSuppliers(true).build();
        List<Supplier> selectedSuppliers = supplierRepository.find(filter);

        assertThat(selectedSuppliers).containsExactlyInAnyOrderElementsOf(List.of(
            NULL_TYPE_SUPPLIER,
            THIRD_PARTY_SUPPLIER_1,
            REAL_SUPPLIER,
            FIRST_PARTY,
            FMCG,
            BIZ,
            LINKED_TO_BIZ1,
            LINKED_TO_BIZ2,
            SUPPLIER_WITH_NULL_BUSINESS_ID,
            SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID));
    }

    @Test
    public void testFindByRealIds() {
        var suppliersList = supplierRepository.findByRealSupplierIds(Collections.singleton("000042"));
        assertThat(suppliersList.values()).containsExactlyInAnyOrder(LINKED_TO_BIZ1);

        suppliersList = supplierRepository.findByRealSupplierIds(Collections.singleton("wrong test"));
        assertThat(suppliersList).isEmpty();

        suppliersList = supplierRepository.findByRealSupplierIds(Collections.emptySet());
        assertThat(suppliersList).isEmpty();
    }

    @Test
    public void testFindAllFMCG() {
        assertThat(supplierRepository.findAllFmcg()).containsOnlyOnce(FMCG);
    }

    @Test
    public void testFindByBusinessId() {
        var suppliersBindedToBusiness = supplierRepository.findByBusinessId(BIZ_ID);
        var suppliersWithNullBusinessId = supplierRepository.findByBusinessId(null);
        var suppliersThatDoesntExist = supplierRepository.findByBusinessId(123);

        assertThat(suppliersBindedToBusiness)
            .containsExactlyInAnyOrder(LINKED_TO_BIZ1, LINKED_TO_BIZ2, DSBS_WITH_BIZ);

        assertThat(suppliersWithNullBusinessId)
            .contains(SUPPLIER_WITH_NULL_BUSINESS_ID)
            .doesNotContain(LINKED_TO_BIZ1, LINKED_TO_BIZ2, DSBS_WITH_BIZ);

        assertThat(suppliersThatDoesntExist).isEmpty();
    }

    @Test
    public void testLimit() {
        var suppliers = supplierRepository.find(new SupplierFilter(), new OffsetFilter().setLimit(1), null);
        assertThat(suppliers).hasSize(1);
    }

    @Test
    public void testOffset() {
        var suppliers1 = supplierRepository.find(new SupplierFilter(), OffsetFilter.offset(0, 2), null);
        assertThat(suppliers1).hasSize(2);
        var suppliers2 = supplierRepository.find(new SupplierFilter(), OffsetFilter.offset(1, 1), null);
        assertThat(suppliers2).hasSize(1);
        assertThat(suppliers1)
            .extracting(Supplier::getId)
            .contains(suppliers2.get(0).getId());
    }

    @Test
    public void testFindByNameOrId() {
        var suppliers = supplierRepository.find(SupplierFilter.builder().nameOrId("a FMCG").build());
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(FMCG);

        suppliers = supplierRepository.find(SupplierFilter.builder().nameOrId("12").build());
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(
            MARKET_SHOP,
            MARKET_SHOP_2
        );

        suppliers = supplierRepository.find(SupplierFilter.builder().name("a MARKET_SHOP 12").nameOrId("12").build());
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(
            MARKET_SHOP_2
        );
    }

    @Test
    public void testFindByName() {
        var suppliers = supplierRepository.find(SupplierFilter.builder().name("a FMCG").build());
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(FMCG);

        suppliers = supplierRepository.find(SupplierFilter.builder().name("12").build());
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(
            MARKET_SHOP_2
        );

        suppliers = supplierRepository.find(SupplierFilter.builder().name("a MARKET_SHOP").build());
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(
            MARKET_SHOP,
            MARKET_SHOP_2
        );
    }

    @Test
    public void findWithFilterWorksFine() {
        SupplierFilter filter = SupplierFilter.builder()
            .mbiBusinessIds(Collections.singleton(BIZ_ID))
            .build();
        List<Supplier> suppliers = supplierRepository.find(filter);
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(LINKED_TO_BIZ1, LINKED_TO_BIZ2, DSBS_WITH_BIZ,
            SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID);

        SupplierFilter nullBusinessIdFilter = SupplierFilter.builder()
            .isBusinessIdNull(true)
            .build();

        suppliers = supplierRepository.find(nullBusinessIdFilter);
        Assertions.assertThat(suppliers)
            .contains(SUPPLIER_WITH_NULL_BUSINESS_ID, SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID)
            .doesNotContain(LINKED_TO_BIZ1, LINKED_TO_BIZ2, DSBS_WITH_BIZ);

        SupplierFilter nullBusinessAndMbiFilter = SupplierFilter.builder()
            .mbiBusinessIds(Collections.singleton(BIZ_ID))
            .isBusinessIdNull(true)
            .build();

        suppliers = supplierRepository.find(nullBusinessAndMbiFilter);
        Assertions.assertThat(suppliers).containsExactly(SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID);
        SupplierFilter keywordFilter = SupplierFilter.builder()
            .keyword("MARKET")
            .build();
        suppliers = supplierRepository.find(keywordFilter);
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(MARKET_SHOP, MARKET_SHOP_2);
        keywordFilter = SupplierFilter.builder()
            .keyword("1")
            .build();
        suppliers = supplierRepository.find(keywordFilter);
        Assertions.assertThat(suppliers).containsExactlyInAnyOrder(MARKET_SHOP, MARKET_SHOP_2, NULL_TYPE_SUPPLIER,
            THIRD_PARTY_SUPPLIER_1, LINKED_TO_BIZ1, SUPPLIER_WITH_NULL_BUSINESS_ID,
            SUPPLIER_WITH_MBI_AND_NULL_BUSINESS_ID, DSBS_WITH_BIZ);
        keywordFilter = SupplierFilter.builder()
            .keyword("")
            .build();
        suppliers = supplierRepository.find(keywordFilter);
        Assertions.assertThat(suppliers).containsExactlyInAnyOrderElementsOf(allSuppliers);
    }
}
