package ru.yandex.market.api.comparisons;

import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.comparisons.PersComparisonList;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 29.11.16.
 */
public class ComparisonListsJsonParserTest extends UnitTestBase {

    private static ComparisonListsJsonParser parser;

    @BeforeClass
    public static void setUpClass() {
        parser = new ComparisonListsJsonParser();
    }

    @Test
    public void testParseSimpleComparisonList() throws Exception {
        List<PersComparisonList> lists = parser.parse(ResourceHelpers.getResource("comparison-lists.json"));

        assertEquals(2, lists.size());

        PersComparisonList list1 = lists.get(0);
        assertEquals(91491, list1.getCategoryId());
        assertEquals(1480418262000L, list1.getLastUpdate());

        assertEquals(1, list1.getItems().size());
        assertEquals(13188751, list1.getItems().get(0).getId());
        assertEquals(1480418262000L, (long) list1.getItems().get(0).getLastUpdate());

        PersComparisonList list2 = lists.get(1);
        assertEquals(91148, list2.getCategoryId());
        assertEquals(1480418140000L, list2.getLastUpdate());

        assertEquals(2, list2.getItems().size());
        assertEquals(7888705, list2.getItems().get(0).getId());
        assertEquals(1480418140000L, (long) list2.getItems().get(0).getLastUpdate());
        assertEquals(10545588, list2.getItems().get(1).getId());
        assertEquals(1480418072000L, (long) list2.getItems().get(1).getLastUpdate());
    }

    @Test
    public void testParseListWithInvalidModelId() throws Exception {
        List<PersComparisonList> lists = parser.parse(ResourceHelpers.getResource("lists-with-invalid-id.json"));

        assertEquals(1, lists.size());

        PersComparisonList list = lists.get(0);

        assertEquals(2, list.getItems().size());
        assertEquals(7888705, list.getItems().get(0).getId());
        assertEquals(10545588, list.getItems().get(1).getId());
    }
}
