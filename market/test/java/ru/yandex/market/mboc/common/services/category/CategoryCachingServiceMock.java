package ru.yandex.market.mboc.common.services.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category.models.CategoryParameterValue;

/**
 * Мок.
 * Есть также мок данных для impl класса, но использовать сложнее - надо создавать больше промежуточных сущностей,
 * использовать кэш. Мок самого сервиса проще.
 *
 * @author yuramalinov
 * @created 20.08.18
 */
public class CategoryCachingServiceMock implements CategoryCachingService {
    private Map<Long, Category> categoryMap = new HashMap<>();
    private boolean enableAuto;
    private boolean goodContentDefault;

    public CategoryCachingServiceMock() {
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

    public CategoryCachingServiceMock enableAuto() {
        return enableAuto(true);
    }

    public CategoryCachingServiceMock enableAuto(boolean enableAuto) {
        this.enableAuto = enableAuto;
        return this;
    }

    public CategoryCachingServiceMock setGoodContentDefault(boolean goodContentDefault) {
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
                .setAcceptGoodContent(goodContentDefault)
                .setParentCategoryId(Category.ROOT_PARENT)
            );
        }
        return Optional.ofNullable(category);
    }

    public CategoryCachingServiceMock addCategory(Category category) {
        // TODO: until https://st.yandex-team.ru/MBO-35667
        if (category.getCategoryId() == ROOT_BOOK_CATEGORY_ID) {
            category.setHasKnowledge(true);
        }
        categoryMap.put(category.getCategoryId(), category);
        return this;
    }

    public CategoryCachingServiceMock addCategory(long id) {
        return addCategory(id, Category.ROOT_PARENT);
    }

    public CategoryCachingServiceMock addCategory(long id, long parentId) {
        return addCategory(id, "Category #" + id, parentId);
    }

    public CategoryCachingServiceMock addCategory(long id, String name) {
        return addCategory(id, name, Category.ROOT_PARENT);
    }

    public CategoryCachingServiceMock addCategory(long id, String name, long parentId) {
        return addCategory(new Category().setCategoryId(id).setName(name).setParentCategoryId(parentId)
            .setPublished(true).setParameterValues(List.of()));
    }

    public CategoryCachingServiceMock addCategories(Category... categories) {
        return addCategories(Arrays.asList(categories));
    }

    public CategoryCachingServiceMock addCategories(Collection<Category> categories) {
        categories.forEach(this::addCategory);
        return this;
    }

    public void setCategoryAcceptGoodContent(long id, boolean acceptGoodContent) {
        categoryMap.get(id).setAcceptGoodContent(acceptGoodContent);
    }

    public void setAcceptContentFromWhiteShops(long id, boolean acceptContentFromWhiteShops) {
        categoryMap.get(id).setAcceptContentFromWhiteShops(acceptContentFromWhiteShops);
    }

    public void setVendorExcluded(long id, Integer vendorId) {
        categoryMap.get(id).setVendorGoodContentExclusion(List.of(vendorId));
    }

    public void setCategoryHasKnowledge(long id, boolean hasKnowledge) {
        categoryMap.get(id).setHasKnowledge(hasKnowledge);
    }

    public void removeCategory(long categoryId) {
        categoryMap.remove(categoryId);
    }
}
