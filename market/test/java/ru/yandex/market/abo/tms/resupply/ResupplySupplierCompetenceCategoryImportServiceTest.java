package ru.yandex.market.abo.tms.resupply;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;

import ru.yandex.market.abo.core.category.CategoryService;
import ru.yandex.market.abo.core.resupply.stock.ResupplySupplierCompetenceCategoryEntity;
import ru.yandex.market.abo.core.resupply.stock.ResupplySupplierCompetenceCategoryRepository;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
class ResupplySupplierCompetenceCategoryImportServiceTest {
    private static final String TEST_CSV_RESOURCE_PATH =
            "/ru/yandex/market/abo/tms/resupply/test-supplier-service-categories.csv";

    @Test
    void smokeTest() {
        CategoryTreeNode categories =
                CategoryTreeNode.of(3, "Все товары", List.of(
                        CategoryTreeNode.of(4, "Продукты", List.of(
                                CategoryTreeNode.of(5, "Безалкогольное пиво и вино")
                        )),
                        CategoryTreeNode.of(6, "Электроника", List.of(
                                CategoryTreeNode.of(7, "Портативная техника", List.of(
                                        CategoryTreeNode.of(8, "Аксессуары для наушников и гарнитур")
                                ))
                        )),
                        CategoryTreeNode.of(9, "Бытовая техника", List.of(
                                CategoryTreeNode.of(10, "Мелкая техника для кухни", List.of(
                                        CategoryTreeNode.of(11, "Приготовление напитков", List.of(
                                                CategoryTreeNode.of(12, "Аксессуары для кофемашин и кофеварок")
                                        ))
                                )),
                                CategoryTreeNode.of(13, "Техника для красоты", List.of(
                                        CategoryTreeNode.of(14, "Приборы для ухода за телом"),
                                        CategoryTreeNode.of(15, "Уход за полостью рта", List.of(
                                                CategoryTreeNode.of(16, "Аксессуары для зубных щеток и ирригаторов")
                                        ))
                                ))
                        ))
                ));
        CategoryService categoryService = new TreeNodeCategoryService(categories);
        ResupplySupplierCompetenceCategoryRepository repository =
                Mockito.mock(ResupplySupplierCompetenceCategoryRepository.class);
        ResupplySupplierCompetenceCategoryImportService importer =
                new ResupplySupplierCompetenceCategoryImportService(categoryService, repository);
        importer.updateCompetenceCategories(() -> getClass().getResourceAsStream(TEST_CSV_RESOURCE_PATH));
        Mockito.verify(repository).deleteAll();
        Mockito.verify(repository).save(MockitoHamcrest.argThat(hasCategoryId(5)));
        Mockito.verify(repository).save(MockitoHamcrest.argThat(hasCategoryId(8)));
        Mockito.verify(repository).save(MockitoHamcrest.argThat(hasCategoryId(11)));
        Mockito.verify(repository).save(MockitoHamcrest.argThat(hasCategoryId(14)));
        Mockito.verify(repository).save(MockitoHamcrest.argThat(hasCategoryId(15)));
        Mockito.verifyNoMoreInteractions(repository);
    }

    @Nonnull
    private static Matcher<ResupplySupplierCompetenceCategoryEntity> hasCategoryId(int categoryId) {
        return Matchers.hasProperty("categoryId", Matchers.is(Integer.valueOf(categoryId)));
    }
}
