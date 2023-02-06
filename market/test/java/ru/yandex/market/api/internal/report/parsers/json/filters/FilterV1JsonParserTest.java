package ru.yandex.market.api.internal.report.parsers.json.filters;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.category.BoolFilter;
import ru.yandex.market.api.category.EnumFilter;
import ru.yandex.market.api.category.EnumFilterType;
import ru.yandex.market.api.category.EnumValue;
import ru.yandex.market.api.category.EnumValueColor;
import ru.yandex.market.api.category.EnumValueShop;
import ru.yandex.market.api.category.EnumValueSize;
import ru.yandex.market.api.category.Filter;
import ru.yandex.market.api.category.FilterSubType;
import ru.yandex.market.api.category.FilterType;
import ru.yandex.market.api.category.NumericFilter;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.parsers.json.filters.v1.FilterV1Factory;
import ru.yandex.market.api.internal.report.parsers.json.filters.v1.FilterV1JsonParser;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by anton0xf on 09.02.17.
 */
@WithContext
@WithMocks
public class FilterV1JsonParserTest extends BaseTest {

    @Mock
    ClientHelper clientHelper;

    private FilterV1JsonParser parser;

    MockClientHelper mockClientHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        parser = new FilterV1JsonParser(new FilterV1Factory());
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void testParseNumberFilter() {
        NumericFilter filter = (NumericFilter) parse("number-filter.json");
        assertEquals(FilterType.NUMERIC, filter.getType());
        assertEquals("Длина спального места", filter.getName());
        assertEquals("12442823", filter.getId());

        assertEquals("160", filter.getMinValue());
        assertEquals("220", filter.getMaxValue());
        assertEquals("см", filter.getUnit());
    }

    @Test
    public void testParseRangeFilter() {
        NumericFilter filter = (NumericFilter) parse("range-filter.json");
        assertEquals(FilterType.NUMERIC, filter.getType());
        assertEquals("Ширина", filter.getName());
        assertEquals("6187317", filter.getId());

        assertEquals("50", filter.getMinValue());
        assertEquals("241", filter.getMaxValue());
    }

    @Test
    public void testParseDeliveryIntervalFilter() {
        EnumFilter filter = (EnumFilter) parse("offer-delivery-interval-filter.json");

        assertEquals("Срок доставки курьером", filter.getName());
        assertEquals("-14", filter.getId());
        assertEquals(FilterType.ENUMERATOR, filter.getType());

        List<? extends EnumValue> values = filter.getOptions();

        assertEquals(3, values.size());

        assertEquals("0", values.get(0).getValueId());
        assertEquals("Сегодня", values.get(0).getValueText());

        assertEquals("1", values.get(1).getValueId());
        assertEquals("Завтра", values.get(1).getValueText());

        assertEquals("5", values.get(2).getValueId());
        assertEquals("До 5 дней", values.get(2).getValueText());
    }

    @Test
    public void shouldParsePriceFilter() {
        NumericFilter filter = (NumericFilter) parse("price-filter.json");

        assertEquals("-1", filter.getId());
        assertEquals(FilterType.NUMERIC, filter.getType());
        assertEquals("Цена", filter.getName());
        assertEquals("price", filter.getShortname());
        assertEquals("RUR", filter.getUnit());
        assertEquals("15498648", filter.getMaxValue());
        assertEquals("16167.9078", filter.getMinValue());
        assertFalse(filter.getExactly());
    }

    @Test
    public void testParseColorFilter() {
        EnumFilter filter = (EnumFilter) parse("color-filter.json");

        assertEquals("13354415", filter.getId());
        assertEquals(FilterType.ENUMERATOR, filter.getType());
        assertEquals(FilterSubType.COLOR, filter.getSubType());
        assertEquals(EnumFilterType.COLOR, filter.getEnumFilterType());
        assertEquals("Цвет", filter.getName());
        assertTrue(filter.isExactly());

        List<EnumValueColor> values = (List<EnumValueColor>) filter.getOptions();
        assertEquals(3, values.size());

        assertEquals("13354443", values.get(0).getValueId());
        assertEquals("белый", values.get(0).getValueText());
        assertEquals("#FFFFFF", values.get(0).getCode());

        assertEquals("13354528", values.get(1).getValueId());
        assertEquals("золотистый", values.get(1).getValueText());
        assertEquals("#FFD700", values.get(1).getCode());

        assertEquals("13354600", values.get(2).getValueId());
        assertEquals("розовый", values.get(2).getValueText());
        assertEquals("#FF00BB", values.get(2).getCode());
    }

    @Test
    public void testParseShopsFilter() {
        EnumFilter filter = (EnumFilter) parse("shops-filter.json");

        assertEquals("-6", filter.getId());
        assertEquals(FilterType.ENUMERATOR, filter.getType());
        assertEquals("Магазины", filter.getName());
        assertEquals("shop", filter.getShortname());

        List<EnumValueShop> values = (List<EnumValueShop>) filter.getOptions();
        assertEquals(2, values.size());

        assertEquals("262", values.get(0).getValueId());
        assertEquals("Shop 1", values.get(0).getValueText());
        assertEquals("5", values.get(0).getCount());

        assertEquals("391925", values.get(1).getValueId());
        assertEquals("Shop 2", values.get(1).getValueText());
        assertEquals("6", values.get(1).getCount());
    }

    @Test
    public void testParseEnumFilter() {
        EnumFilter filter = (EnumFilter) parse("enum-filter.json");

        assertEquals("4940921", filter.getId());
        assertEquals(FilterType.ENUMERATOR, filter.getType());
        assertEquals("Тип", filter.getName());

        List<? extends EnumValue> values = filter.getOptions();
        assertEquals(4, values.size());

        assertEquals("13475069", values.get(0).getValueId());
        assertEquals("смартфон", values.get(0).getValueText());

        assertEquals("12105575", values.get(1).getValueId());
        assertEquals("телефон", values.get(1).getValueText());

        assertEquals("13475319", values.get(2).getValueId());
        assertEquals("телефон для детей", values.get(2).getValueText());

        assertEquals("13475071", values.get(3).getValueId());
        assertEquals("телефон для пожилых", values.get(3).getValueText());
    }

    @Test
    public void testParseSizeFilter() {
        EnumFilter filter = (EnumFilter) parse("size-filter.json");

        assertEquals("8224549", filter.getId());
        assertEquals(FilterType.ENUMERATOR, filter.getType());
        assertEquals("Рост", filter.getName());
        assertEquals("см", filter.getUnit());
        assertEquals("дюймы", filter.getDefaultUnit());

        List<EnumValueSize> values = (List<EnumValueSize>) filter.getOptions();
        assertEquals(4, values.size());

        assertEquals("18446744073709549374", values.get(0).getValueId());
        assertEquals("28", values.get(0).getValueText());
        assertEquals("дюймы", values.get(0).getUnit());

        assertEquals("18446744073709549373", values.get(1).getValueId());
        assertEquals("30", values.get(1).getValueText());
        assertEquals("дюймы", values.get(1).getUnit());

        assertEquals("18446744073709549626", values.get(2).getValueId());
        assertEquals("38", values.get(2).getValueText());
        assertEquals("RU", values.get(2).getUnit());

        assertEquals("18446744073709549625", values.get(3).getValueId());
        assertEquals("40", values.get(3).getValueText());
        assertEquals("RU", values.get(3).getUnit());
    }

    @Test
    public void testParseSimpleBooleanFilter() {
        BoolFilter filter = (BoolFilter) parse("boolean-filter.json");

        assertEquals("7924631", filter.getId());
        assertEquals(FilterType.BOOL, filter.getType());
        assertEquals("Рваные", filter.getName());
    }

    @Test
    public void testParseWarrantyFilter() {
        BoolFilter filter = (BoolFilter) parse("warranty-filter.json");

        assertEquals("-2", filter.getId());
        assertEquals(FilterType.BOOL, filter.getType());
        assertEquals("Гарантия производителя", filter.getName());
        assertEquals("manufacturer_warranty", filter.getShortname());
    }

    @Test
    public void testParseDiscountFilter() {
        BoolFilter filter = (BoolFilter) parse("discount-filter.json");

        assertEquals("-9", filter.getId());
        assertEquals(FilterType.BOOL, filter.getType());
        assertEquals("Скидки", filter.getName());
        assertEquals("has_discount", filter.getShortname());
    }

    @Test
    public void testSkipFilterOfUnknownType() {
        EnumFilter filter = (EnumFilter) parse("unknown-type-filter.json");
        assertNull(filter);
    }

    @Test
    public void testParseShortname() {
        EnumFilter filter = (EnumFilter) parse("enum-filter.json");
        assertEquals("type", filter.getShortname());
    }

    @Test
    public void testParseVendorFilter() {
        EnumFilter filter = (EnumFilter) parse("vendor-filter.json");
        assertEquals("-11", filter.getId());
        assertEquals("Производитель", filter.getName());
        assertEquals(EnumFilterType.VENDOR, filter.getEnumFilterType());
    }

    private Filter parse(String path) {
        return parser.parse(ResourceHelpers.getResource(path));
    }
}
