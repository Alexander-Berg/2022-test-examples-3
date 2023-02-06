package ru.yandex.market.billing.imports.shopsku;

import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.billing.dbschema.shops_web.Tables.SHOP_SKU_INFO;
import static ru.yandex.market.billing.imports.shopsku.ShopSkuInfoTestData.assertEquals;
import static ru.yandex.market.billing.imports.shopsku.ShopSkuInfoTestData.largeBox;
import static ru.yandex.market.billing.imports.shopsku.ShopSkuInfoTestData.mediumBox;
import static ru.yandex.market.billing.imports.shopsku.ShopSkuInfoTestData.smallBox;

@SuppressWarnings("rawtypes")
public class ShopSkuInfoDaoMergeShopSkuInfoTest extends FunctionalTest {

    private static final Long SUPPLIER_1 = 123L;
    private static final String SUPPLIER_1_SKU_1 = "123-1";
    private static final String SUPPLIER_1_SKU_2 = "123-2";

    private static final Long SUPPLIER_2 = 234L;
    private static final String SUPPLIER_2_SKU_1 = "234-1";

    @Autowired
    private ShopSkuInfoDao shopSkuInfoDao;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    public void beforeEach() {
        clear();
    }

    // не должно падать на пустом списке
    @Test
    public void addEmptyList() {
        shopSkuInfoDao.mergeShopSkuInfos(emptyList());
    }

    @Test
    public void addNewOneToEmptyTable() {
        ShopSkuInfo expected = smallBox(SUPPLIER_1, SUPPLIER_1_SKU_1);
        shopSkuInfoDao.mergeShopSkuInfos(List.of(expected));

        Map<SupplierShopSkuKey, ShopSkuInfo> actualMap = get();
        assertThat(actualMap).hasSize(1);

        ShopSkuInfo actual = actualMap.get(new SupplierShopSkuKey(SUPPLIER_1, SUPPLIER_1_SKU_1));
        assertEquals(actual, expected);
    }

    @Test
    public void addNewOneToFilledTable() {
        ShopSkuInfo expected1 = smallBox(SUPPLIER_1, SUPPLIER_1_SKU_2);
        ShopSkuInfo expected2 = mediumBox(SUPPLIER_2, SUPPLIER_2_SKU_1);
        ShopSkuInfo expected3 = largeBox(SUPPLIER_1, SUPPLIER_1_SKU_1);
        shopSkuInfoDao.mergeShopSkuInfos(List.of(
                expected1,
                expected2
        ));

        shopSkuInfoDao.mergeShopSkuInfos(List.of(expected3));

        Map<SupplierShopSkuKey, ShopSkuInfo> actualMap = get();
        assertThat(actualMap).hasSize(3);

        ShopSkuInfo actual1 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_1, SUPPLIER_1_SKU_2));
        ShopSkuInfo actual2 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_2, SUPPLIER_2_SKU_1));
        ShopSkuInfo actual3 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_1, SUPPLIER_1_SKU_1));
        assertEquals(actual1, expected1);
        assertEquals(actual2, expected2);
        assertEquals(actual3, expected3);
    }

    @Test
    public void addExistingOne() {
        shopSkuInfoDao.mergeShopSkuInfos(List.of(smallBox(SUPPLIER_1, SUPPLIER_1_SKU_1)));

        ShopSkuInfo expected = mediumBox(SUPPLIER_1, SUPPLIER_1_SKU_1);
        shopSkuInfoDao.mergeShopSkuInfos(List.of(expected));

        Map<SupplierShopSkuKey, ShopSkuInfo> actualMap = get();
        assertThat(actualMap).hasSize(1);

        ShopSkuInfo actual = actualMap.get(new SupplierShopSkuKey(SUPPLIER_1, SUPPLIER_1_SKU_1));
        assertEquals(actual, expected);
    }

    @Test
    public void addExistingOneWhenOthersPresent() {
        ShopSkuInfo expected1 = smallBox(SUPPLIER_1, SUPPLIER_1_SKU_1);
        ShopSkuInfo expected2 = mediumBox(SUPPLIER_2, SUPPLIER_2_SKU_1);
        shopSkuInfoDao.mergeShopSkuInfos(List.of(
                expected1,
                expected2
        ));

        expected2 = largeBox(SUPPLIER_2, SUPPLIER_2_SKU_1);

        shopSkuInfoDao.mergeShopSkuInfos(List.of(expected2));

        Map<SupplierShopSkuKey, ShopSkuInfo> actualMap = get();
        assertThat(actualMap).hasSize(2);

        ShopSkuInfo actual1 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_1, SUPPLIER_1_SKU_1));
        ShopSkuInfo actual2 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_2, SUPPLIER_2_SKU_1));
        assertEquals(actual1, expected1);
        assertEquals(actual2, expected2);
    }

    @Test
    public void addNewAndExistingOneWhenOthersPresent() {
        ShopSkuInfo expected1 = smallBox(SUPPLIER_1, SUPPLIER_1_SKU_1);
        ShopSkuInfo expected2 = mediumBox(SUPPLIER_2, SUPPLIER_2_SKU_1);
        shopSkuInfoDao.mergeShopSkuInfos(List.of(
                expected1,
                expected2
        ));

        expected2 = largeBox(SUPPLIER_2, SUPPLIER_2_SKU_1);
        ShopSkuInfo expected3 = mediumBox(SUPPLIER_1, SUPPLIER_1_SKU_2);

        shopSkuInfoDao.mergeShopSkuInfos(List.of(expected2, expected3));

        Map<SupplierShopSkuKey, ShopSkuInfo> actualMap = get();
        assertThat(actualMap).hasSize(3);

        ShopSkuInfo actual1 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_1, SUPPLIER_1_SKU_1));
        ShopSkuInfo actual2 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_2, SUPPLIER_2_SKU_1));
        ShopSkuInfo actual3 = actualMap.get(new SupplierShopSkuKey(SUPPLIER_1, SUPPLIER_1_SKU_2));
        assertEquals(actual1, expected1);
        assertEquals(actual2, expected2);
        assertEquals(actual3, expected3);
    }

    private void clear() {
        dslContext.deleteFrom(SHOP_SKU_INFO).execute();
    }

    private Map<SupplierShopSkuKey, ShopSkuInfo> get() {
        RecordMapper<Record, SupplierShopSkuKey> keyMapper =
                rec -> new SupplierShopSkuKey(
                        rec.getValue(SHOP_SKU_INFO.SUPPLIER_ID),
                        rec.getValue(SHOP_SKU_INFO.SHOP_SKU));
        return dslContext
                .select(shopSkuInfoDao.getShopSkuInfoMapper().getFieldsToRead())
                .from(SHOP_SKU_INFO)
                .fetchMap(keyMapper, shopSkuInfoDao.getShopSkuInfoMapper()::fromDb);
    }
}
