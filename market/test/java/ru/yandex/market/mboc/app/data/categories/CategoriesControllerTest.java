package ru.yandex.market.mboc.app.data.categories;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mboc.app.data.categories.web.CategoryWebFilter;
import ru.yandex.market.mboc.app.data.categories.web.DisplayCategory;
import ru.yandex.market.mboc.app.data.categories.web.Publication;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.logisticsparams.repository.SkuLogisticParamsRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryImpl;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category.models.CategoryParameterValue;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

public class CategoriesControllerTest extends BaseMbocAppTest {

    private CategoriesController controller;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private CategoryTree mockCategoryTree;
    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private SkuLogisticParamsRepository skuLogisticParamsRepository;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private OfferStatService offerStatService;

    @Before
    public void setUp() {
        var categoryCachingService = Mockito.mock(CategoryCachingService.class);
        mockCategoryTree = Mockito.mock(CategoryTree.class);
        Mockito.when(categoryCachingService.getCategoryTree()).thenReturn(mockCategoryTree);
        categoryCachingServiceMock = new CategoryCachingServiceMock();

        offerStatService = new OfferStatService(namedParameterJdbcTemplate, namedParameterJdbcTemplate,
            skuLogisticParamsRepository, transactionHelper, offerRepository, storageKeyValueService);
        offerStatService.subscribe();

        supplierRepository
            .insertBatch(YamlTestUtil.readSuppliersFromResource("app-offers/app-test-suppliers.yml"));

        controller = new CategoriesController(categoryCachingServiceMock, offerStatService);
    }

    @Test
    public void allCallShouldReturnConvertedCategories() {
        categoryCachingServiceMock.addCategory(1, "name 1", CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(2, "name 2", 1);

        final var categories = controller.all(Publication.ALL);
        Assertions.assertThat(categories)
            .usingElementComparatorIgnoringFields("cargoTypesOverride")
            .containsExactlyInAnyOrder(
                new DisplayCategory(CategoryTree.ROOT_CATEGORY_ID, CategoryTree.NO_ROOT_ID, "Все товары", true),
                new DisplayCategory(1, CategoryTree.ROOT_CATEGORY_ID, "name 1", true),
                new DisplayCategory(2, 1, "name 2", true)
            );
    }

    @Test
    public void testFilterByPublicity() {
        categoryCachingServiceMock.addCategory(1, "name 1", CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(2).setName("name 2").setParentCategoryId(1)
            .setPublished(false));

        List<DisplayCategory> published = controller.all(Publication.ONLY_PUBLISHED);
        Assertions.assertThat(published)
            .usingElementComparatorIgnoringFields("cargoTypesOverride")
            .containsExactlyInAnyOrder(
                new DisplayCategory(CategoryTree.ROOT_CATEGORY_ID, CategoryTree.NO_ROOT_ID, "Все товары", true),
                new DisplayCategory(1, CategoryTree.ROOT_CATEGORY_ID, "name 1", true)
            );

        List<DisplayCategory> nonpublished = controller.all(Publication.ONLY_NONPUBLISHED);
        Assertions.assertThat(nonpublished)
            .usingElementComparatorIgnoringFields("cargoTypesOverride")
            .containsExactlyInAnyOrder(
                new DisplayCategory(2, 1, "name 2", false)
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

        final var all = controller.all(Publication.ALL);

        Assertions.assertThat(all)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new DisplayCategory(CategoryTree.ROOT_CATEGORY_ID, CategoryTree.NO_ROOT_ID, "Все товары", true),
                new DisplayCategory(1, CategoryTree.ROOT_CATEGORY_ID, "name 1", true),
                new DisplayCategory(2, 1, "name 2", true)
            );
    }

    @Test
    public void testGetDepartment() {
        String department = "department";
        categoryCachingServiceMock.addCategory(1, department, CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(2).setName("name 2")
            .setParameterValues(List.of())
            .setParentCategoryId(1)
            .setPublished(false));
        Assertions.assertThat(controller.getDepartments())
            .isEqualTo(Collections.singletonList(department));
    }

    @Test
    public void testList() {
        String department = "department";
        categoryCachingServiceMock.addCategory(1, department, CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(2).setName("cat2")
            .setParameterValues(List.of())
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setPublished(false));
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(3).setName("cat3")
            .setParameterValues(List.of())
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setPublished(false));
        offerRepository.insertOffers(OfferTestUtils.simpleOffer().setCategoryIdInternal(2L));

        offerStatService.updateOfferStat();

        var list =
            controller.list(new CategoryWebFilter().setSupplierId(OfferTestUtils.TEST_SUPPLIER_ID));
        Assertions.assertThat(list).extracting(DisplayCategory::getId).containsExactlyInAnyOrder(2L,
            CategoryTree.ROOT_CATEGORY_ID);
    }

    @Test
    public void testListOnBusiness() {
        String department = "department";
        categoryCachingServiceMock.addCategory(1, department, CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(2).setName("cat2")
            .setParameterValues(List.of())
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setPublished(false));
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(3).setName("cat3")
            .setParameterValues(List.of())
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setPublished(false));
        categoryCachingServiceMock.addCategory(new Category().setCategoryId(4).setName("cat4")
            .setParameterValues(List.of())
            .setParentCategoryId(CategoryTree.ROOT_CATEGORY_ID)
            .setPublished(false));


        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer()
                .setShopSku("1")
                .setCategoryIdInternal(2L)
                .setBusinessId(10000)
                .setServiceOffers(10001, 10002),
            OfferTestUtils.simpleOffer()
                .setShopSku("2")
                .setCategoryIdInternal(3L)
                .setBusinessId(10000)
                .setServiceOffers(10002),
            OfferTestUtils.simpleOffer()
                .setShopSku("3")
                .setCategoryIdInternal(4L)
                .setBusinessId(10000)
                .setServiceOffers(10001)
        );

        offerStatService.updateOfferStat();

        var list =
            controller.list(new CategoryWebFilter().setSupplierId(10002));
        Assertions.assertThat(list).extracting(DisplayCategory::getId).containsExactlyInAnyOrder(2L, 3L,
            CategoryTree.ROOT_CATEGORY_ID);
    }
}
