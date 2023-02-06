package ru.yandex.market.abo.tms.resupply;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.abo.core.category.CategoryInfo;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
class CategoryTreeNode {
    private final CategoryInfo categoryInfo;
    private final List<CategoryTreeNode> subcategories;

    private CategoryTreeNode(CategoryInfo categoryInfo, List<CategoryTreeNode> subcategories) {
        this.categoryInfo = categoryInfo;
        this.subcategories = List.copyOf(subcategories);
    }

    int id() {
        return categoryInfo.id();
    }

    CategoryInfo categoryInfo() {
        return categoryInfo;
    }

    String name() {
        return categoryInfo.name();
    }

    @Nonnull
    List<CategoryTreeNode> subcategories() {
        return subcategories;
    }

    static CategoryTreeNode of(int id, String name, List<CategoryTreeNode> subcategories) {
        return new CategoryTreeNode(CategoryInfo.of(id, name), subcategories);
    }

    static CategoryTreeNode of(int id, String name) {
        return CategoryTreeNode.of(id, name, Collections.emptyList());
    }
}
