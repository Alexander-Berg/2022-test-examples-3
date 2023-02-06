package ru.yandex.market.api.comparisons;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.market.api.comparisons.ComparedParametersJsonParser.ParameterJsonParser;
import ru.yandex.market.api.domain.v2.comparisons.ComparedParameters.ComparedItem;
import ru.yandex.market.api.domain.v2.comparisons.ComparedParameters.Parameter;
import ru.yandex.market.api.domain.v2.comparisons.ComparedParameters.ProductValue;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 30.11.16.
 */
public class ParameterJsonParserTest extends UnitTestBase {

    private static ParameterJsonParser parser;

    @BeforeClass
    public static void setUpClass() {
        parser = new ParameterJsonParser();
    }

    @Test
    public void testParseEnumParameter() throws Exception {
        Parameter parameter = parser.parse(ResourceHelpers.getResource("compared-enum-param.json"));
        assertEquals("2142542726", parameter.getId());
        assertEquals("Тип", parameter.getName());
        assertEquals("ENUM", parameter.getType());
        assertNull(parameter.getUnit());
        assertFalse(parameter.isValuesEqual());

        List<ComparedItem> comparedItems = parameter.getComparedItems();
        assertEquals(2, comparedItems.size());

        assertEquals("12911822", comparedItems.get(0).getId());

        List<ProductValue> values = comparedItems.get(0).getValues();
        assertEquals(1, values.size());

        assertEquals("смартфон", values.get(0).getValue());
        assertEquals("1195192805", values.get(0).getId());

        values = comparedItems.get(1).getValues();
        assertEquals(1, values.size());
        assertEquals("смартфонище", values.get(0).getValue());
        assertEquals("1195192806", values.get(0).getId());
    }

    @Test
    public void testParseColorParameter() throws Exception {
        Parameter parameter = parser.parse(ResourceHelpers.getResource("compared-color-param.json"));

        assertEquals("13354415", parameter.getId());
        assertEquals("Цвет", parameter.getName());
        assertEquals("COLOR", parameter.getType());
        assertNull(parameter.getUnit());
        assertFalse(parameter.isValuesEqual());

        List<ComparedItem> comparedItems = parameter.getComparedItems();
        assertEquals(2, comparedItems.size());

        assertEquals("12911822", comparedItems.get(0).getId());
        List<ProductValue> values = comparedItems.get(0).getValues();
        assertEquals(6, values.size());

        assertEquals("желтый", values.get(1).getGroup());
        assertEquals("золотистый", values.get(1).getValue());
        assertEquals("#FFD700", values.get(1).getCode());
        assertEquals("13354528", values.get(1).getId());
    }

    @Test
    public void testParseBooleanParameter() throws Exception {
        Parameter parameter = parser.parse(ResourceHelpers.getResource("compared-boolean-param.json"));

        assertEquals("2136886660", parameter.getId());
        assertEquals("Оптическая стабилизация", parameter.getName());
        assertEquals("BOOLEAN", parameter.getType());

        List<ComparedItem> comparedItems = parameter.getComparedItems();
        assertEquals(3, comparedItems.size());

        assertEquals("12911822", comparedItems.get(0).getId());
        assertTrue(comparedItems.get(0).getValues().isEmpty());

        assertEquals("13934014", comparedItems.get(1).getId());
        assertEquals("true", comparedItems.get(1).getValues().get(0).getValue());

        assertEquals("13934015", comparedItems.get(2).getId());
        assertEquals("false", comparedItems.get(2).getValues().get(0).getValue());
    }

    @Test
    public void testParseNumericParameter() throws Exception {
        Parameter parameter = parser.parse(ResourceHelpers.getResource("compared-numeric-param.json"));

        assertEquals("4925691", parameter.getId());
        assertEquals("NUMBER", parameter.getType());
        assertEquals("мА·ч", parameter.getUnit());

        List<ComparedItem> comparedItems = parameter.getComparedItems();
        assertEquals(2, comparedItems.size());

        assertEquals(1, comparedItems.get(0).getValues().size());
        assertEquals("3450", comparedItems.get(0).getValues().get(0).getValue());

        assertEquals(1, comparedItems.get(1).getValues().size());
        assertEquals("3000", comparedItems.get(1).getValues().get(0).getValue());
    }
}
