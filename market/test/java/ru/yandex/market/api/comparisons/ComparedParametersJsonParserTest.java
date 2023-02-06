package ru.yandex.market.api.comparisons;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.comparisons.ComparedParameters;
import ru.yandex.market.api.domain.v2.comparisons.ComparedParameters.ParameterGroup;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 30.11.16.
 */
public class ComparedParametersJsonParserTest extends UnitTestBase {

    private static ComparedParametersJsonParser parser;

    @BeforeClass
    public static void setUpClass() {
        parser = new ComparedParametersJsonParser();
    }

    @Test
    public void testParseComparedParametersList() throws Exception {
        ComparedParameters parameters = parser.parse(ResourceHelpers.getResource("compared-parameters.json"));

        assertEquals(1, parameters.getGroups().size());

        ParameterGroup group = parameters.getGroups().get(0);
        assertEquals(3, group.getParams().size());

        assertEquals("2142542726", group.getParams().get(0).getId());
        assertEquals("2134007594", group.getParams().get(1).getId());
        assertEquals("2134007476", group.getParams().get(2).getId());
    }

    @Test
    public void testParseTitledParamGroups() throws Exception {
        ComparedParameters parameters = parser.parse(ResourceHelpers.getResource("compared-parameters2.json"));
        assertEquals(2, parameters.getGroups().size());

        assertEquals("Общие характеристики", parameters.getGroups().get(0).getTitle());
        assertFalse(parameters.getGroups().get(0).isValuesEqual());

        assertEquals("Звонки", parameters.getGroups().get(1).getTitle());
        assertTrue(parameters.getGroups().get(1).isValuesEqual());
    }
}
