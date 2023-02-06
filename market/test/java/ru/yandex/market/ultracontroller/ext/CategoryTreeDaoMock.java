package ru.yandex.market.ultracontroller.ext;

import ru.yandex.market.CategoryTree;
import ru.yandex.market.dao.CategoryTreeDao;

import java.util.HashMap;
import java.util.Map;

public class CategoryTreeDaoMock implements CategoryTreeDao {
    private final Map<CategoryTree.CategoryTreeNode, Integer> result = new HashMap<>();

    @Override
    public Map<CategoryTree.CategoryTreeNode, Integer> loadCategoryTree() {
        return result;
    }

    public void addNode(CategoryTree.CategoryTreeNode node, int tovarId) {
        result.put(node, tovarId);
    }
}
