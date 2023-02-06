package ru.yandex.market.api.internal.cataloger;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by fettsery on 17.09.18.
 */
public class NidHierarchyParserTest {
    @Test
    public void shouldParseNavigationTrees() {
        Int2ObjectMap<IntList> result = new NidHierarchyParser().parse(
            ResourceHelpers.getResource("navigation-trees.xml")
        );

        Assert.assertEquals(9, result.size());

        assertThat(result.get(20322), Matchers.containsInAnyOrder(20381, 0));
        assertThat(result.get(20326), Matchers.containsInAnyOrder(20409, 20406));
        assertThat(result.get(20003), Matchers.containsInAnyOrder(20010, 20011, 20012));
        assertThat(result.get(20001), Matchers.contains(20003));
    }
}
