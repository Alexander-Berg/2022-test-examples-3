package ru.yandex.market.api.internal.report.parsers;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.market.api.category.*;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
public class GuruLightFiltersParserTest extends UnitTestBase {

    private static String EMPTY_STRING = "";

    @NotNull
    private List<Filter> loadFiltersFromXml(String resourcePath) throws Exception {
        List<Filter> filters = new GuruLightFiltersParser().parse(
            ResourceHelpers.getResource(resourcePath)
        );

        assertNotNull(filters);
        return filters;
    }

    @Test
    public void shouldParseEnumsWithColorXml() throws Exception {
        List<Filter> filters = loadFiltersFromXml("gl-filters-color-part.xml");

        assertEquals(1, filters.size());

        assertEquals(FilterType.ENUMERATOR, filters.get(0).getType());
        assertEquals(FilterSubType.COLOR, filters.get(0).getSubType());
        assertEquals("7925349", filters.get(0).getId());
        assertEquals("Цвет", filters.get(0).getName());
        assertEquals(null, filters.get(0).getShortname());
        assertEquals(EMPTY_STRING, filters.get(0).getUnit());
        assertEquals(null, filters.get(0).getFilterProperty());

        EnumFilter asEnumFilter = (EnumFilter) filters.get(0);
        assertNotNull(asEnumFilter);
        assertEquals(EnumFilterType.COLOR, asEnumFilter.getEnumFilterType());

        assertNotNull(asEnumFilter.getOptions());
        assertEquals(1, asEnumFilter.getOptions().size());

        EnumValueColor color = (EnumValueColor) asEnumFilter.getOptions().get(0);
        assertNotNull(color);
        assertEquals("#103090", color.getCode());
        assertEquals("синий", color.getTag());
        assertEquals("темно-синий", color.getValueText());
        assertEquals("7925377", color.getValueId());
    }

    @Test
    public void shouldParseEnumsWithSizesXml() throws Exception {
        List<Filter> filters = loadFiltersFromXml("gl-filters-size-part.xml");

        assertEquals(1, filters.size());

        assertEquals(FilterType.ENUMERATOR, filters.get(0).getType());
        assertEquals(FilterSubType.SIZE, filters.get(0).getSubType());
        assertEquals("8224548", filters.get(0).getId());
        assertEquals("Размер", filters.get(0).getName());
        assertEquals(null, filters.get(0).getShortname());
        assertEquals("см", filters.get(0).getUnit());
        assertEquals(null, filters.get(0).getFilterProperty());

        EnumFilter asEnumFilter = (EnumFilter) filters.get(0);
        assertNotNull(asEnumFilter);
        assertEquals("дюймы", asEnumFilter.getDefaultUnit());
        assertEquals(EnumFilterType.SIZE, asEnumFilter.getEnumFilterType());

        assertNotNull(asEnumFilter.getOptions());
        assertEquals(3, asEnumFilter.getOptions().size());

        EnumValueSize size = (EnumValueSize) asEnumFilter.getOptions().get(0);
        assertEquals("18446744073709551407", size.getValueId());
        assertEquals("48", size.getValueText());
        assertEquals("RU", size.getUnit());

        size = (EnumValueSize) asEnumFilter.getOptions().get(1);
        assertEquals("18446744073709551387", size.getValueId());
        assertEquals("S", size.getValueText());
        assertEquals("INT", size.getUnit());

        size = (EnumValueSize) asEnumFilter.getOptions().get(2);
        assertEquals("18446744073709550838", size.getValueId());
        assertEquals("32", size.getValueText());
        assertEquals("дюймы", size.getUnit());
    }

    @Test
    public void shouldParsePartialXml() throws Exception {
        List<Filter> filters = loadFiltersFromXml("gl-filters-part.xml");
        assertEquals(3, filters.size());

        assertEquals(FilterType.ENUMERATOR, filters.get(0).getType());
        assertEquals(FilterSubType.COLOR, filters.get(0).getSubType());

        assertEquals(FilterType.ENUMERATOR, filters.get(1).getType());
        assertEquals(FilterSubType.SIZE, filters.get(1).getSubType());

        assertEquals(FilterType.ENUMERATOR, filters.get(2).getType());
        assertEquals(FilterSubType.SIZE, filters.get(2).getSubType());
    }

    @Test
    public void shouldParseNoSubtypeEnumXml() throws Exception {
        List<Filter> filters = loadFiltersFromXml("gl-filters-enum-no-subtype-part.xml");

        assertEquals(2, filters.size());

        EnumFilter firstFilter = (EnumFilter) filters.get(0);

        assertEquals(FilterType.ENUMERATOR, firstFilter.getType());
        assertEquals(null, firstFilter.getSubType());
        assertEquals(null, firstFilter.getDefaultUnit());

        assertEquals(10, firstFilter.getOptions().size());

        EnumValue value = firstFilter.getOptions().get(0);

        assertEquals("7286382", value.getValueId());
        assertEquals("Wella", value.getValueText());
    }

    @Test
    public void shouldParseNumericXml() throws Exception {
        List<Filter> filters = loadFiltersFromXml("gl-filters-numeric-part.xml");

        assertEquals(1, filters.size());
        NumericFilter numeric = (NumericFilter) filters.get(0);

        assertEquals(FilterType.NUMERIC, numeric.getType());
        assertEquals("6189060", numeric.getId());
        assertEquals("Ширина", numeric.getName());
        assertEquals("см", numeric.getUnit());
        assertEquals(true, numeric.getExactly());
        assertEquals("300", numeric.getMaxValue());
        assertEquals("40", numeric.getMinValue());
    }

    @Test
    public void shouldParseBooleanXml() throws Exception {
        List<Filter> filters = loadFiltersFromXml("gl-filters-boolean-part.xml");

        assertEquals(1, filters.size());
        BoolFilter boolFilter = (BoolFilter) filters.get(0);

        assertEquals(FilterType.BOOL, boolFilter.getType());
        assertEquals("6188821", boolFilter.getId());
        assertEquals("С зеркалом", boolFilter.getName());
        assertEquals(null, boolFilter.getUnit());
        assertEquals(true, boolFilter.isHasBoolNo());
    }
}
