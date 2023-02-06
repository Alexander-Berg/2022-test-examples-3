package ru.yandex.market.abo.tms.resupply;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.TreeTraverser;

import ru.yandex.market.abo.core.category.CategoryInfo;
import ru.yandex.market.abo.core.category.CategoryService;
import ru.yandex.market.abo.core.category.SubcategoryKey;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
class TreeNodeCategoryService implements CategoryService {
    private final Map<Integer, CategoryTreeNode>
            nodes = new HashMap<>();
    private final CategoryTreeNode root;

    TreeNodeCategoryService(CategoryTreeNode root) {
        this.root = root;
        for (CategoryTreeNode node : TreeTraverser.using(CategoryTreeNode::subcategories).preOrderTraversal(root)) {
            CategoryTreeNode sameIdNode = nodes.putIfAbsent(node.id(), node);
            if (sameIdNode != null) {
                throw new IllegalStateException("Same id used twice: " + sameIdNode + ", " + node);
            }
        }
    }

    @Override
    public CategoryInfo getRoot() {
        return root.categoryInfo();
    }

    @Override
    public Optional<CategoryInfo> getSubcategory(SubcategoryKey key) {
        return Optional.ofNullable(nodes.get(key.parentId()))
                .stream()
                .flatMap(node -> node.subcategories().stream())
                .filter(node -> node.name().equals(key.name()))
                .map(CategoryTreeNode::categoryInfo)
                .findFirst();
    }

    @Override
    public List<CategoryInfo> getPath(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCategoryInfo(int id, String name, @Nullable Integer parentId) {
        throw new UnsupportedOperationException();
    }
}
