package ru.yandex.market.deepmind.common.services.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.category.models.CategoryParameterValue;

/**
 * Мок.
 * Есть также мок данных для impl класса, но использовать сложнее - надо создавать больше промежуточных сущностей,
 * использовать кэш. Мок самого сервиса проще.
 *
 * @author yuramalinov
 * @created 20.08.18
 */
public class DeepmindCategoryCachingServiceMock implements DeepmindCategoryCachingService {
    private Map<Long, Category> categoryMap = new HashMap<>();
    private boolean enableAuto;
    private boolean goodContentDefault;

    public DeepmindCategoryCachingServiceMock() {
        Category rootCategory = new Category()
            .setCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setName(ROOT)
            .setParentCategoryId(CategoryTree.NO_ROOT_ID)
            .setPublished(true)
            .setParameterValues(List.of(
                new CategoryParameterValue().setId(17278736L).setType(CategoryParameterValue.Type.ENUM)
                    .setValue(17278740).setXslName("heavyGoodOverride"),
                new CategoryParameterValue().setId(17840566L).setType(CategoryParameterValue.Type.ENUM)
                    .setValue(17840572).setXslName("heavyGood20Override")
            ));
        addCategory(rootCategory);
    }

    public DeepmindCategoryCachingServiceMock enableAuto() {
        return enableAuto(true);
    }

    public DeepmindCategoryCachingServiceMock enableAuto(boolean enableAuto) {
        this.enableAuto = enableAuto;
        return this;
    }

    public DeepmindCategoryCachingServiceMock setGoodContentDefault(boolean goodContentDefault) {
        this.goodContentDefault = goodContentDefault;
        return this;
    }

    @Override
    public CategoryTree getCategoryTree() {
        return CategoryTree.computeTree(categoryMap.values());
    }

    @Override
    public List<Category> getAllCategories() {
        return new ArrayList<>(categoryMap.values());
    }

    @Override
    @Nonnull
    public Optional<Category> getCategory(long categoryId) {
        Category category = categoryMap.get(categoryId);
        if (enableAuto && category == null) {
            return Optional.of(new Category()
                .setCategoryId(categoryId)
                .setName("auto-category #" + categoryId)
                .setParentCategoryId(Category.ROOT_PARENT)
            );
        }
        return Optional.ofNullable(category);
    }

    public DeepmindCategoryCachingServiceMock addCategory(Category category) {
        categoryMap.put(category.getCategoryId(), category);
        return this;
    }

    public DeepmindCategoryCachingServiceMock addCategory(long id) {
        return addCategory(id, Category.ROOT_PARENT);
    }

    public DeepmindCategoryCachingServiceMock addCategory(long id, long parentId) {
        return addCategory(id, "Category #" + id, parentId);
    }

    public DeepmindCategoryCachingServiceMock addCategory(long id, String name) {
        return addCategory(id, name, Category.ROOT_PARENT);
    }

    public DeepmindCategoryCachingServiceMock addCategory(long id, String name, long parentId) {
        return addCategory(new Category().setCategoryId(id).setName(name).setParentCategoryId(parentId)
            .setPublished(true).setParameterValues(List.of()));
    }

    public DeepmindCategoryCachingServiceMock addCategories(Category... categories) {
        return addCategories(Arrays.asList(categories));
    }

    public DeepmindCategoryCachingServiceMock addCategories(Collection<Category> categories) {
        categories.forEach(this::addCategory);
        return this;
    }

    public void removeCategory(long categoryId) {
        categoryMap.remove(categoryId);
    }
}
