package ru.yandex.market.clab.common.service.category;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.common.service.ConcurrentModificationException;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.clab.common.test.assertions.CategoryAssert.assertThatCategory;

/**
 * @author anmalysh
 * @since 1/19/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class CategoryRepositoryTestPgaas extends BasePgaasIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    public void testSaveAndReadCategory() {
        Category category = createCategory();

        Category createdCategory = categoryRepository.create(category);

        assertThatCategory(createdCategory).isValueEqualTo(category);

        Category storedCategory = categoryRepository.getByHid(category.getId());

        assertThat(storedCategory).isEqualTo(createdCategory);
    }

    @Test
    public void testOptimisticLocking() {
        Category category = createCategory();

        Category createdCategory = categoryRepository.create(category);

        createdCategory.setMinRawPhotos(100);

        categoryRepository.update(createdCategory);

        assertThatThrownBy(() -> {
            categoryRepository.update(createdCategory);
        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testGetActiveCategoryTree() {
        Category category1 = createCategory()
            .setDeleted(false);
        Category category2 = createCategory()
            .setDeleted(true);
        categoryRepository.create(category1);
        categoryRepository.create(category2);

        List<Category> categoryTree = categoryRepository.getActiveCategoryTree();
        assertThat(categoryTree).extracting(Category::getId).containsExactly(category1.getId());
    }

    @Test
    public void testGetEffectiveCategory() {
        Category rootCategory = createCategory()
            .setParentHid(null);
        Category intermediateCategory = createCategory("photoInstruction", "minRawPhotos")
            .setParentHid(rootCategory.getId());
        Category leafCategory = createCategory(
            "data", "photoInstruction", "photoEditInstruction", "minRawPhotos", "goodTypeId")
            .setParentHid(intermediateCategory.getId());

        rootCategory = categoryRepository.create(rootCategory);
        intermediateCategory = categoryRepository.create(intermediateCategory);
        leafCategory = categoryRepository.create(leafCategory);

        Category leafEffectiveCategory = categoryRepository.getEffectiveByHid(leafCategory.getId());

        // From leaf always
        assertThat(leafEffectiveCategory.getId()).isEqualTo(leafCategory.getId());
        assertThat(leafEffectiveCategory.getParentHid()).isEqualTo(leafCategory.getParentHid());
        assertThat(leafEffectiveCategory.getName()).isEqualTo(leafCategory.getName());
        assertThat(leafEffectiveCategory.getModifiedDate()).isEqualTo(leafCategory.getModifiedDate());
        assertThat(leafEffectiveCategory.getData()).isNull();
        assertThat(leafEffectiveCategory.getSizeMeasureData()).isEqualTo(leafCategory.getSizeMeasureData());

        // Defined in leaf
        assertThat(leafEffectiveCategory.getEditorInstruction()).isEqualTo(leafCategory.getEditorInstruction());
        assertThat(leafEffectiveCategory.getMinProcessedPhotos()).isEqualTo(leafCategory.getMinProcessedPhotos());

        // Defined in intermediate
        assertThat(leafEffectiveCategory.getPhotoEditInstruction())
            .isEqualTo(intermediateCategory.getPhotoEditInstruction());
        assertThat(leafEffectiveCategory.getGoodTypeId()).isEqualTo(intermediateCategory.getGoodTypeId());

        //Defined in root
        assertThat(leafEffectiveCategory.getPhotoInstruction()).isEqualTo(rootCategory.getPhotoInstruction());
        assertThat(leafEffectiveCategory.getMinRawPhotos()).isEqualTo(rootCategory.getMinRawPhotos());
    }

    @Test
    public void testGetActiveEffectiveCategoryTree() {
        Category rootCategory = createCategory()
            .setDeleted(false)
            .setParentHid(null);
        Category intermediateCategory = createCategory("goodTypeId")
            .setDeleted(false)
            .setParentHid(rootCategory.getId());
        Category leafCategory = createCategory()
            .setDeleted(false)
            .setParentHid(intermediateCategory.getId());
        Category leafCategory2 = createCategory("goodTypeId")
            .setDeleted(false)
            .setParentHid(intermediateCategory.getId());
        Category leafDeletedCategory = createCategory()
            .setParentHid(intermediateCategory.getId())
            .setDeleted(true);
        Category intDeletedCategory = createCategory()
            .setDeleted(true)
            .setParentHid(rootCategory.getId());
        Category leafAfterDeletedCategory = createCategory()
            .setDeleted(false)
            .setParentHid(intDeletedCategory.getId());

        categoryRepository.create(rootCategory);
        categoryRepository.create(intermediateCategory);
        categoryRepository.create(leafCategory);
        categoryRepository.create(leafCategory2);
        categoryRepository.create(leafDeletedCategory);
        categoryRepository.create(intDeletedCategory);
        categoryRepository.create(leafAfterDeletedCategory);

        List<Category> activeEffectiveTree = categoryRepository.getEffectiveCategoryTree();

        List<Long> activeCategories = Stream.of(rootCategory, intermediateCategory, leafCategory, leafCategory2)
            .map(Category::getId)
            .collect(Collectors.toList());
        assertThat(activeEffectiveTree).extracting(Category::getId)
            .containsExactlyElementsOf(activeCategories);
        assertThat(activeEffectiveTree)
            .filteredOn(c -> c.getId().equals(rootCategory.getId()))
            .extracting(Category::getGoodTypeId)
            .containsExactly(rootCategory.getGoodTypeId());
        assertThat(activeEffectiveTree)
            .filteredOn(c -> c.getId().equals(intermediateCategory.getId()))
            .extracting(Category::getGoodTypeId)
            .containsExactly(rootCategory.getGoodTypeId());
        assertThat(activeEffectiveTree)
            .filteredOn(c -> c.getId().equals(leafCategory.getId()))
            .extracting(Category::getGoodTypeId)
            .containsExactly(leafCategory.getGoodTypeId());
        assertThat(activeEffectiveTree)
            .filteredOn(c -> c.getId().equals(leafCategory2.getId()))
            .extracting(Category::getGoodTypeId)
            .containsExactly(rootCategory.getGoodTypeId());
    }

    @Test
    public void testGetCategoryIdWithParents() {
        categoryRepository.create(new Category().setId(1L));
        categoryRepository.create(new Category().setId(2L).setParentHid(1L));
        categoryRepository.create(new Category().setId(3L).setParentHid(2L));
        categoryRepository.create(new Category().setId(4L).setParentHid(2L));

        List<Long> categoryIdWithParents = categoryRepository.getCategoryIdWithParents(3L);

        assertThat(categoryIdWithParents).containsExactly(3L, 2L, 1L);
    }

    private Category createCategory(String... ignore) {
        return RandomTestUtils.randomObject(Category.class, ignore);
    }
}
