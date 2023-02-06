package ru.yandex.market.mbo.mdm.common.masterdata.services;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;

import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

@SuppressWarnings("checkstyle:magicnumber")
public class SupplierConverterServiceImplTest {

    private static final String TABLE_PATH = "test-path";
    private static final String TABLE_CLUSTER = "test-cluster";
    private static final int TEST_BERU_ID = 1_234;
    private static final ShopSkuKey REAL_SHOP_SKU1 = new ShopSkuKey(TEST_BERU_ID, "2.shopSku1");
    private static final ShopSkuKey REAL_SHOP_SKU2 = new ShopSkuKey(TEST_BERU_ID, "4.shopSku2");
    private static final ShopSkuKey INTERNAL_SHOP_SKU1 = new ShopSkuKey(1, "shopSku1");
    private static final ShopSkuKey INTERNAL_SHOP_SKU2 = new ShopSkuKey(3, "shopSku2");
    private SupplierConverterServiceImpl supplierConverterService;
    @Mock
    private JdbcOperations jdbcOperations;

    @Before
    public void setUp() {
        supplierConverterService = Mockito.spy(
            new SupplierConverterServiceImpl(jdbcOperations, new BeruIdMock(TEST_BERU_ID),
                SupplierConverterService.TableName.MDM_SUPPLIER));

        Mockito.doReturn(ImmutableMap.of(1, "2", 3, "4"))
            .when(supplierConverterService).getInternalSupplierToRealMap();
        Mockito.doReturn(ImmutableMap.of("2", 1, "4", 3))
            .when(supplierConverterService).getRealSupplierToInternalMap();
    }

    @Test
    public void convertRealToInternal() {
        ShopSkuKey result = supplierConverterService.convertRealToInternal(REAL_SHOP_SKU1);
        Assertions.assertThat(result).isEqualTo(INTERNAL_SHOP_SKU1);
    }

    @Test
    public void convertInternalToReal() {
        ShopSkuKey result = supplierConverterService.convertInternalToReal(INTERNAL_SHOP_SKU1);
        Assertions.assertThat(result).isEqualTo(REAL_SHOP_SKU1);
    }

    @Test
    public void convertRealToInternalList() {
        List<ShopSkuKey> result = supplierConverterService
            .convertRealToInternal(Arrays.asList(REAL_SHOP_SKU1, REAL_SHOP_SKU2));
        Assertions.assertThat(result).containsExactly(INTERNAL_SHOP_SKU1, INTERNAL_SHOP_SKU2);
    }

    @Test
    public void convertInternalToRealList() {
        List<ShopSkuKey> result = supplierConverterService
            .convertInternalToReal(Arrays.asList(INTERNAL_SHOP_SKU1, INTERNAL_SHOP_SKU2));
        Assertions.assertThat(result).containsExactly(REAL_SHOP_SKU1, REAL_SHOP_SKU2);
    }
}
