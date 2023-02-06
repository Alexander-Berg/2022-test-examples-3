package ru.yandex.market.mbo.mdm.common.masterdata.model.ssku;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.yt.ProviderProductInfoDiff;

/**
 * @author amaslak
 */
public class MdmSskuSearchFilterTest {
    private static final Logger log = LoggerFactory.getLogger(MdmSskuSearchFilterTest.class);

    private ShopSkuKey key1 = new ShopSkuKey(42, "123");
    private ShopSkuKey key2 = new ShopSkuKey(43, "QWE");
    private ShopSkuKey key3 = new ShopSkuKey(44, "000.123");
    private ShopSkuKey key4 = new ShopSkuKey(45, "000,123");
    private ShopSkuKey key1P = new ShopSkuKey(SupplierConverterServiceMock.BERU_ID, "01.234");

    @Test
    public void verifyShopSkuIds() {
        Assertions.assertThat(ProviderProductInfoDiff.verifyShopSkuId(key1.getShopSku())).isTrue();
        Assertions.assertThat(ProviderProductInfoDiff.verifyShopSkuId(key2.getShopSku())).isTrue();
        Assertions.assertThat(ProviderProductInfoDiff.verifyShopSkuId(key3.getShopSku())).isTrue();
        Assertions.assertThat(ProviderProductInfoDiff.verifyShopSkuId(key4.getShopSku())).isTrue();
        Assertions.assertThat(ProviderProductInfoDiff.verifyShopSkuId(key1P.getShopSku())).isTrue();
    }

    @Test
    public void whenParseSearchStringShouldGetSingleValidShopSkuKey() {
        List<ShopSkuKey> singleKey1 = List.of(key1);
        Assertions.assertThat(parseSearchString("42 123")).isEqualTo(singleKey1);
        Assertions.assertThat(parseSearchString("42\t123")).isEqualTo(singleKey1);
        Assertions.assertThat(parseSearchString(" 42 123\t")).isEqualTo(singleKey1);
        Assertions.assertThat(parseSearchString("\n\n\n\n42 123, ")).isEqualTo(singleKey1);
        Assertions.assertThat(parseSearchString("42 123, 42\t123, ")).isEqualTo(singleKey1);
    }

    @Test
    public void whenParseSearchStringShouldAcceptComma() {
        Assertions.assertThat(parseSearchString("42 123, 45 000,123")).isEqualTo(List.of(key1, key4));
    }

    @Test
    public void whenParseSearchStringShouldGetAllValidShopSkuKeys() {
        List<ShopSkuKey> keys = List.of(key1, key2, key3);
        Assertions.assertThat(parseSearchString("42 123, 43 QWE, 44 000.123")).isEqualTo(keys);
        Assertions.assertThat(parseSearchString(", 44 000.123\t 42 123\n\n43 QWE")).isEqualTo(keys);
        Assertions.assertThat(parseSearchString("43\tQWE, 42 123,\t\t44 000.123")).isEqualTo(keys);
    }

    @Test
    public void whenParse1PKeysShouldReturnShopSkuKeysWithBeruId() {
        Assertions.assertThat(parseSearchString("01.234")).isEqualTo(List.of(key1P));
        Assertions.assertThat(parseSearchString("01.234, 01.234, 465852\t01.234")).isEqualTo(List.of(key1P));
        Assertions.assertThat(parseSearchString("01.234, 01.234, 465852 01.234")).isEqualTo(List.of(key1P));
    }

    @Test
    public void whenParseSearchStringShouldAssumeInvalidShopSkuKeyAs1PKey() {
        List<ShopSkuKey> keys = List.of(key1, new ShopSkuKey(key1P.getSupplierId(), "42"));
        Assertions.assertThat(parseSearchString("42\n42 123, 42, 42,")).isEqualTo(keys);
    }

    @Test
    public void whenParseMskuIdsShouldGetAllValidMskIds() {
        Long id1 = 77777777L;
        Long id2 = 88888888L;
        Long id3 = 99999999L;
        Assertions.assertThat(parseMskuIds("77777777, 88888888, 99999999")).containsExactlyInAnyOrder(id1, id2, id3);
        Assertions.assertThat(parseMskuIds("77777777 88888888")).containsExactlyInAnyOrder(id1, id2);
        Assertions.assertThat(parseMskuIds("77777777, 88888888,,, 99999999")).containsExactlyInAnyOrder(id1, id2, id3);
        Assertions.assertThat(parseMskuIds(", 77777777\t, 88888888\n, 99999999"))
            .containsExactlyInAnyOrder(id1, id2, id3);
        Assertions.assertThat(parseMskuIds("77777777")).containsExactlyInAnyOrder(id1);
        Assertions.assertThat(parseMskuIds("-1,77777777,,0,-213,-890")).containsExactlyInAnyOrder(id1);
        Assertions.assertThat(parseMskuIds("abcd 77777777 0 88888888 ac130")).containsExactlyInAnyOrder(id1, id2);
        Assertions.assertThat(parseMskuIds("77777777 88888888\t99999999")).containsExactlyInAnyOrder(id1, id2, id3);
    }

    @Test
    public void whenParsingUnderlinigSymbolShouldNotAssumeAsSplitter() {
        Assertions.assertThat(parseSearchString("  23231232 id,of,ssku ;secondSsku; 8 my_ssku"))
            .containsExactlyInAnyOrder(
                new ShopSkuKey(23231232, "id,of,ssku"),
                new ShopSkuKey(SupplierConverterServiceMock.BERU_ID, "secondSsku"),
                new ShopSkuKey(8, "my_ssku")
            );
    }

    public List<ShopSkuKey> parseSearchString(String searchString) {
        MdmSskuSearchFilter filter = new MdmSskuSearchFilter();
        filter.setSearchString(searchString);
        return filter.getMergedShopSkuKeys(SupplierConverterServiceMock.BERU_ID);
    }

    private List<Long> parseMskuIds(String mskuIds) {
        MdmSskuSearchFilter filter = new MdmSskuSearchFilter();
        filter.setMskuIdSearchString(mskuIds);
        return filter.getMskuIds();
    }
}
