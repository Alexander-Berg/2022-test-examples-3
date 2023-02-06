package ru.yandex.market.deepmind.app.controllers;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.deepmind.app.pojo.DisplayMskuCategory;
import ru.yandex.market.deepmind.app.pojo.PublicationStatus;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.category.models.CategoryParameterValue;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingService;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.services.category_availability.CategoryCargoTypeService;

public class DeepmindCategoriesControllerTest extends DeepmindBaseDbTestClass {
    private DeepmindCategoriesController controller;
    private DeepmindCategoryCachingServiceMock categoryCachingServiceMock;
    private CategoryTree mockCategoryTree;

    @Before
    public void setUp() {
        var categoryCachingService = Mockito.mock(DeepmindCategoryCachingService.class);
        mockCategoryTree = Mockito.mock(CategoryTree.class);
        Mockito.when(categoryCachingService.getCategoryTree()).thenReturn(mockCategoryTree);
        categoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();
        var categoryCargoTypeService = new CategoryCargoTypeService(categoryCachingService);
        controller = new DeepmindCategoriesController(categoryCachingServiceMock, categoryCargoTypeService);
    }

    @Test
    public void allCallShouldReturnConvertedCategories() {
        categoryCachingServiceMock.addCategory(1, "name 1", CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(2, "name 2", 1);

        final var categories = controller.all(PublicationStatus.ALL);
        Assertions.assertThat(categories)
            .usingElementComparatorIgnoringFields("cargoTypesOverride")
            .containsExactlyInAnyOrder(
                new DisplayMskuCategory(CategoryTree.ROOT_CATEGORY_ID, CategoryTree.NO_ROOT_ID,
                    "Все товары", true, Map.of()),
                new DisplayMskuCategory(1, CategoryTree.ROOT_CATEGORY_ID, "name 1", true, Map.of()),
                new DisplayMskuCategory(2, 1, "name 2", true, Map.of())
            );
    }

    @Test
    public void testFilterByPublicity() {
        categoryCachingServiceMock.addCategory(1, "name 1", CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(2).setName("name 2").setParentCategoryId(1)
            .setPublished(false));

        List<DisplayMskuCategory> published = controller.all(PublicationStatus.ONLY_PUBLISHED);
        Assertions.assertThat(published)
            .usingElementComparatorIgnoringFields("cargoTypesOverride")
            .containsExactlyInAnyOrder(
                new DisplayMskuCategory(CategoryTree.ROOT_CATEGORY_ID, CategoryTree.NO_ROOT_ID,
                    "Все товары", true, Map.of()),
                new DisplayMskuCategory(1, CategoryTree.ROOT_CATEGORY_ID, "name 1", true, Map.of())
            );

        List<DisplayMskuCategory> nonpublished = controller.all(PublicationStatus.ONLY_NONPUBLISHED);
        Assertions.assertThat(nonpublished)
            .usingElementComparatorIgnoringFields("cargoTypesOverride")
            .containsExactlyInAnyOrder(
                new DisplayMskuCategory(2, 1, "name 2", false, Map.of())
            );
    }

    @Test
    public void testCargoTypesOverride() {
        categoryCachingServiceMock.addCategory(1, "name 1", CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(2, "name 2", 1);
        Mockito.when(mockCategoryTree.getCategoryParamValues()).thenReturn(
            Map.of(
                CategoryTree.ROOT_CATEGORY_ID, Map.of(),
                1L, Map.of(
                    17278736L, new CategoryParameterValue().setValue(17278738),
                    17840566L, new CategoryParameterValue().setValue(17840570)
                ),
                2L, Map.of(
                    17278736L, new CategoryParameterValue().setValue(17278739),
                    17840566L, new CategoryParameterValue().setValue(17840571)
                )
            )
        );

        final var all = controller.all(PublicationStatus.ALL);

        Assertions.assertThat(all)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new DisplayMskuCategory(CategoryTree.ROOT_CATEGORY_ID, CategoryTree.NO_ROOT_ID,
                    "Все товары", true,
                    Map.of("heavyGoodOverride", "вычислять индивидуально", "heavyGood20Override",
                        "вычислять индивидуально")),
                new DisplayMskuCategory(1, CategoryTree.ROOT_CATEGORY_ID, "name 1", true,
                    Map.of("heavyGoodOverride", "форсировать КГТ", "heavyGood20Override",
                        "форсировать не КГТ20")),
                new DisplayMskuCategory(2, 1, "name 2", true,
                    Map.of("heavyGoodOverride", "форсировать не КГТ", "heavyGood20Override",
                        "форсировать КГТ20"))
            );
    }
}
