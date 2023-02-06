package ru.yandex.market.crm.campaign.yt;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;

import ru.yandex.market.crm.core.domain.categories.Category;

/**
 * @author dimkarp93
 */
public class CategoryTreeBuilder {
    private int hid;
    private int parent;

    private List<CategoryTreeBuilder> children = Lists.newArrayList();

    public CategoryTreeBuilder(int hid) {
        this.hid = hid;
    }

    public List<Category> build() {
        List<Category> categories = Lists.newArrayList();
        categories.add(new Category(hid, null, null, parent));

        for (CategoryTreeBuilder c : children) {
            categories.addAll(c.withParent(hid).build());
        }

        return categories;
    }

    public CategoryTreeBuilder child(@NotNull CategoryTreeBuilder child) {
        children.add(child);
        return this;
    }

    //Специально приавтный - родитель должен задаватся через структуру дерева
    private CategoryTreeBuilder withParent(int parent) {
        this.parent = parent;
        return this;
    }

}
