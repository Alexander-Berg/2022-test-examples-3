package ru.yandex.market.bootcamp.deepdive.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.bootcamp.deepdive.dto.Category;
import ru.yandex.market.bootcamp.deepdive.dto.CategoryTreeResponse;
import ru.yandex.market.bootcamp.deepdive.exceptions.IdNotFoundException;
import ru.yandex.market.bootcamp.deepdive.exceptions.RootNotFoundException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class YtServiceTest {

    private YtService ytService;
    private List<Category> categories;

    @Before
    public void init() {
        ytService = new YtService();
        categories = createCategories();
    }

    @Test
    public void buildTree() throws NoSuchFieldException, IllegalAccessException {
        ytService.buildTree(categories);
        Field treeField = ytService.getClass().getDeclaredField("tree");
        treeField.setAccessible(true);
        Map<Long, List<Category>> tree = (Map<Long, List<Category>>) treeField.get(ytService);
        Assert.assertEquals(tree.size(), 2);
        Assert.assertEquals(tree.get(1L).size(), 4);
        Assert.assertEquals(tree.get(2L).size(), 1);
        Assert.assertTrue(tree.get(1L).contains(categories.get(1)));
        Assert.assertTrue(tree.get(1L).contains(categories.get(2)));
        Assert.assertTrue(tree.get(1L).contains(categories.get(3)));
        Assert.assertTrue(tree.get(1L).contains(categories.get(5)));
        Assert.assertTrue(tree.get(2L).contains(categories.get(4)));

        Field rootField = ytService.getClass().getDeclaredField("root");
        rootField.setAccessible(true);
        Category root = (Category) rootField.get(ytService);
        Assert.assertEquals(root.getParentHid().longValue(), -1L);
        Assert.assertEquals(root, categories.get(0));
    }

    @Test(expected = IdNotFoundException.class)
    public void getChildrenWrongId() {
        ytService.buildTree(categories);
        ytService.getChildren(categories.size() + 1L);
    }

    @Test(expected = RootNotFoundException.class)
    public void getChildrenRootWithoutRoot() {
        ytService.buildTree(createCategoriesWithoutRoot());
        ytService.getRootChildren();
    }

    @Test
    public void getChildren() {
        ytService.buildTree(categories);
        CategoryTreeResponse response = ytService.getChildren(1L);
        Assert.assertEquals(response.getChilds().size(), 4);
        Assert.assertEquals(response.getParent(), categories.get(0));
        Assert.assertEquals(response.getParent().getNids(), categories.get(0).getNids());
        Assert.assertEquals(response.getParent().getHid(), categories.get(0).getHid());
    }

    private List<Category> createCategoriesWithoutRoot() {
        List<Category> categories = new ArrayList<>();
        Category.CategoryBuilder builder = Category.builder();
        categories.add(builder
            .setName("child1")
            .setHid(2L)
            .setParentHid(2L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child2")
            .setHid(3L)
            .setParentHid(2L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child3")
            .setHid(4L)
            .setParentHid(2L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child4")
            .setHid(5L)
            .setParentHid(2L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child5")
            .setHid(6L)
            .setParentHid(2L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        return categories;
    }

    private List<Category> createCategories() {
        List<Category> categories = new ArrayList<>();
        Category.CategoryBuilder builder = Category.builder();
        categories.add(builder
            .setName("parent")
            .setHid(1L)
            .setParentHid(-1L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child1")
            .setHid(2L)
            .setParentHid(1L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child2")
            .setHid(3L)
            .setParentHid(1L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child3")
            .setHid(4L)
            .setParentHid(1L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child4")
            .setHid(5L)
            .setParentHid(2L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        categories.add(builder
            .setName("child5")
            .setHid(6L)
            .setParentHid(1L)
            .setNids(Arrays.asList(1l, 2l, 3l)).build());
        return categories;
    }
}
