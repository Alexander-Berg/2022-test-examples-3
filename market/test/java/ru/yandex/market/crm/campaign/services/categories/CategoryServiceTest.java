package ru.yandex.market.crm.campaign.services.categories;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.yt.CategoryTreeBuilder;
import ru.yandex.market.crm.core.domain.categories.Category;
import ru.yandex.market.crm.core.domain.categories.CategoryCacheData;
import ru.yandex.market.crm.core.services.categories.CategoryService;
import ru.yandex.market.crm.core.services.categories.CategoryServiceImpl;
import ru.yandex.market.crm.core.suppliers.CategoriesDataSupplierImpl;
import ru.yandex.market.crm.util.CrmInts;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

/**
 * @author dimkarp93
 */
@RunWith(MockitoJUnitRunner.class)
public class CategoryServiceTest {

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Mock
    private CategoriesDataSupplierImpl categoriesData;

    private Int2ObjectMap<Category> categoriesMap =
            CrmInts.index(
                    new CategoryTreeBuilder(CategoryService.ROOT_HID)
                            .child(
                                    new CategoryTreeBuilder(1)
                                            .child(new CategoryTreeBuilder(11))
                                            .child(new CategoryTreeBuilder(12))
                            )
                            .child(
                                    new CategoryTreeBuilder(2)
                                            .child(new CategoryTreeBuilder(21))
                                            .child(
                                                    new CategoryTreeBuilder(22)
                                                            .child(new CategoryTreeBuilder(221))
                                                            .child(new CategoryTreeBuilder(222))
                                            )
                                            .child(new CategoryTreeBuilder(23))
                            )
                            .build(),
                    Category::getHid
            );


    private static void assertEquals(Category expected, Category actual) {
        Assert.assertFalse("Both categories must be null or must be not null",
                Boolean.logicalXor(expected == null, actual == null));

        if (expected == null || actual == null) {
            return;
        }

        Assert.assertEquals("Both categories must have equal hid",
                expected.getHid(), actual.getHid());
        Assert.assertEquals("Both categories must have equal name",
                expected.getName(), actual.getName());
        Assert.assertEquals("Both categories must have equal uniq_name",
                expected.getUniqName(), actual.getUniqName());
        Assert.assertEquals("Both categories must have same parent",
                expected.getParent(), actual.getParent());
    }

    @Test
    public void findParentForLeaf() {
        assertEquals(getCategory(22), categoryService.getParent(getCategory(222)).get());
    }

    @Test
    public void findParentForNode() {
        assertEquals(root(), categoryService.getParent(getCategory(2)).get());
    }

    @Test
    public void findParentForRoot() {
        assertFalse(categoryService.getParent(root()).isPresent());
    }

    @Test
    public void lcaForChildAndImmediateParent() {
        lcaTest(getCategory(1), getCategory(1), getCategory(12));
        lcaTest(getCategory(1), getCategory(12), getCategory(1));
    }

    @Test
    public void lcaForLeafWithRootLCA() {
        lcaTest(root(), getCategory(23), getCategory(11));
    }

    @Test
    public void lcaForOneNode() {
        lcaTest(getCategory(22), getCategory(22));
    }

    @Test
    public void lcaForRootAndLeaf() {
        lcaTest(root(), getCategory(CategoryService.ROOT_HID), getCategory(12));
    }

    @Test
    public void lcaForSeveral() {
        lcaTest(getCategory(2), getCategory(222), getCategory(221), getCategory(21));
    }

    @Test
    public void lcaForSiblings() {
        lcaTest(getCategory(22), getCategory(221), getCategory(222));
    }

    @Before
    public void setUp() {
        when(categoriesData.get()).thenReturn(new CategoryCacheData(categoriesMap));
    }

    private Category getCategory(int hid) {
        return categoriesData.get().getCategories().get(hid);
    }

    private void lcaTest(Category expected, Category... data) {
        assertEquals(
                expected,
                categoryService.getLeastCommonAncestor(Arrays.asList(data))
        );
    }

    private Category root() {
        return getCategory(CategoryService.ROOT_HID);
    }
}
