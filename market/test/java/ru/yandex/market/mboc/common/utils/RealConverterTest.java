package ru.yandex.market.mboc.common.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Java6Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class RealConverterTest {

    private static final int BERU_ID = 909;

    @Test
    public void testConvertInternalToReal() {
        ShopSkuKey key1 = new ShopSkuKey(100500, "3P!");
        ShopSkuKey key2 = new ShopSkuKey(222, "1P!");
        Map<Integer, String> mapping = ImmutableMap.of(
            222, "000222"
        );
        List<ShopSkuKey> converted = RealConverter.convertInternalToReal(Arrays.asList(key1, key2), mapping, BERU_ID);
        assertThat(converted).containsExactlyInAnyOrder(
            key1,
            new ShopSkuKey(BERU_ID, "000222.1P!")
        );
    }

    @Test
    public void testConvertRealToInternal() {
        ShopSkuKey key1 = new ShopSkuKey(100500, "3P!");
        ShopSkuKey key2 = new ShopSkuKey(BERU_ID, "000222.1P!");
        Map<String, Integer> mapping = ImmutableMap.of(
            "000222", 222
        );
        List<ShopSkuKey> converted = RealConverter.convertRealToInternal(Arrays.asList(key1, key2), mapping, BERU_ID);
        assertThat(converted).containsExactlyInAnyOrder(
            key1,
            new ShopSkuKey(222, "1P!")
        );
    }

    @Test
    public void isValidRealSupplierId() {
        assertThat(RealConverter.isValidRealSupplierIdString("0")).isFalse();
        assertThat(RealConverter.isValidRealSupplierIdString("")).isFalse();

        assertThat(RealConverter.isValidRealSupplierIdString("100")).isTrue();
        assertThat(RealConverter.isValidRealSupplierIdString("00100")).isTrue();
        assertThat(RealConverter.isValidRealSupplierIdString("100a1")).isTrue();
    }
}
