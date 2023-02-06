package ru.yandex.ir.modelsclusterizer;

import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.CategoryTree;
import ru.yandex.market.dao.CategoryTreeDao;

import java.util.Map;

/**
 * @author a-shar.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:clusterizer-test.xml"})
public class FixedCategoriesTest {

    @Autowired
    private FixedCategoryIdMetaInfo fixedCategoryIdMetaInfo;
    @Autowired
    private CategoryTreeDao categoryTreeDao;

    private Map<CategoryTree.CategoryTreeNode, Integer> constructMap() {
        Map<CategoryTree.CategoryTreeNode, Integer> map = Maps.newHashMap();
        CategoryTree.CategoryTreeNodeBuilder builder = CategoryTree.newCategoryTreeNodeBuilder();
        CategoryTree.CategoryTreeNode node = builder.setName("Все товары")
            .setUniqName("Все товары")
            .setHyperId(90401)
            .setTovarId(0)
            .setVisible(false)
            .setPublished(false)
            .setClusterize(false)
            .setFixedCluster(false)
            .build();

        map.put(node, 0);

        builder = CategoryTree.newCategoryTreeNodeBuilder();
        node = builder.setName("Компьютерная техника")
            .setUniqName("Компьютерная техника")
            .setHyperId(91009)
            .setTovarId(10)
            .setVisible(false)
            .setPublished(false)
            .setClusterize(false)
            .setFixedCluster(false)
            .build();

        map.put(node, 0);

        builder = CategoryTree.newCategoryTreeNodeBuilder();
        node = builder.setName("Зимние комбинезоны для мальчиков")
            .setUniqName("Зимние комбинезоны для мальчиков")
            .setHyperId(8495423)
            .setTovarId(22768)
            .setVisible(true)
            .setPublished(true)
            .setClusterize(true)
            .setFixedCluster(true)
            .build();

        map.put(node, 0);
        return map;
    }

    @Before
    public void init() {
        Mockito.when(categoryTreeDao.loadCategoryTree()).then(invocation -> constructMap());
    }

    @Test
    public void testFixed() {
        Assert.assertFalse(fixedCategoryIdMetaInfo.isCategoryFixed(91009));
        Assert.assertTrue(fixedCategoryIdMetaInfo.isCategoryFixed(8495423));
        Mockito.verify(categoryTreeDao, Mockito.times(1)).loadCategoryTree();

        fixedCategoryIdMetaInfo.reload();

        Assert.assertFalse(fixedCategoryIdMetaInfo.isCategoryFixed(91009));
        Mockito.verify(categoryTreeDao, Mockito.times(2)).loadCategoryTree();
    }
}
