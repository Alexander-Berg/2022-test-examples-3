package ru.yandex.market.mbo.catalogue.category;

import ru.yandex.market.mbo.user.MboUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author s-ermakov
 */
public class CategoryManagersManagerMock implements CategoryManagersManager {

    private List<CategoryManagers> categoryManagers = new ArrayList<>();

    @Override
    public MboUser getCategoryContentManagerUser(long guruCategoryId) {
        return null;
    }

    @Override
    public MboUser getCategoryInputManagerUser(long guruCategoryId) {
        return null;
    }

    @Override
    public List<CategoryManagers> getManagersForAllCategories() {
        return Collections.unmodifiableList(categoryManagers);
    }

    public void addCategoryManagers(CategoryManagers categoryManagers) {
        this.categoryManagers.add(categoryManagers);
    }

    @Override
    public void setCategoryContentManagerUser(long uid, long guruCategoryId) {

    }

    @Override
    public void setCategoryInputManagerUser(long uid, long guruCategoryId) {

    }

    @Override
    public void deleteCategory(long guruCategoryId) {
        categoryManagers.removeIf(cm -> cm.getGuruCategoryId() == guruCategoryId);
    }
}
