package ru.yandex.market.crm.campaign.services.categories;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.campaign.yt.CategoryTreeBuilder;
import ru.yandex.market.crm.core.domain.categories.Category;
import ru.yandex.market.crm.core.domain.categories.CategoryCacheData;
import ru.yandex.market.crm.core.services.categories.CategoryService;
import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.CrmInts;

import static org.junit.Assert.assertEquals;

/**
 * @author dimkarp93
 */
public class CategoriesCreateChildrenTest {

    @Test
    public void emptyTree() {
        assertChildren(empty(), children(make(new CategoryTreeBuilder(CategoryService.ROOT_HID)),
                CategoryService.ROOT_HID));
    }

    @Test
    public void leafHasNothing() {
        CategoryTreeBuilder tree = new CategoryTreeBuilder(CategoryService.ROOT_HID)
                .child(new CategoryTreeBuilder(1));
        assertChildren(empty(), children(make(tree), 1));
    }

    @Test
    public void twoChildren() {
        CategoryTreeBuilder tree = new CategoryTreeBuilder(CategoryService.ROOT_HID)
                .child(new CategoryTreeBuilder(1))
                .child(new CategoryTreeBuilder(2));
        assertChildren(categories(1, 2), children(make(tree), CategoryService.ROOT_HID));
    }

    private void assertChildren(List<Integer> expected, List<Integer> actual) {
        Assert.assertEquals("children must have same count", expected.size(), actual.size());

        Collections.sort(expected);
        Collections.sort(actual);

        CrmCollections.zip(expected, actual, (x, y) -> assertEquals("child must be equals", x, y));
    }

    private List<Integer> categories(int... categories) {
        return new IntArrayList(categories);
    }

    private List<Integer> children(CategoryCacheData data, int hid) {
        return Lists.newArrayList(data.getChildren().get(hid));
    }

    private List<Integer> empty() {
        return Collections.emptyList();
    }

    private CategoryCacheData make(CategoryTreeBuilder builder) {
        return new CategoryCacheData(CrmInts.index(builder.build(), Category::getHid));
    }
}
