package ru.yandex.market.loyalty.back.usecase;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.CategoryTree;
import ru.yandex.market.loyalty.core.service.CategoryTreeService;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class CategoryTreeServiceTest extends MarketLoyaltyBackProdDataMockedDbTest {
    @Autowired
    private CategoryTreeService categoryTreeService;

    @Test
    public void shouldCategoryTreeLoaded() {
        CategoryTree categoryTree = categoryTreeService.getCategoryTree();
        assertNotNull(categoryTree);

        CategoryTree.Node root = categoryTree.getRoot();

        assertNotNull(root);

        Set<CategoryTree.Node> allNodes = root.getPlane();

        assertThat(allNodes, hasItem(root));
    }
}
