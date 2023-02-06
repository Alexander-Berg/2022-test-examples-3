package ru.yandex.market.mbi.affiliate.promo.dao;

import java.util.List;

import org.dbunit.database.DatabaseConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.model.CatalogPromoCategories;
import ru.yandex.market.mbi.affiliate.promo.model.Category;
import ru.yandex.market.mbi.affiliate.promo.model.CategoryTreeNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.iterableWithSize;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class CatalogCategoriesDaoTest {

    @Autowired
    private CatalogCategoriesDao dao;

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_categories_dao_after.csv")
    public void testGetAllCategories() {
        var result = dao.getAllCategories();
        assertThat(result, containsInAnyOrder(
                new CategoryTreeNode(1133, "Книги", List.of()),
                new CategoryTreeNode(23, "Техника",
                        List.of(new CategoryTreeNode(2323, "Бытовая техника", List.of()))),
                new CategoryTreeNode(45, "Дом",
                        List.of(new CategoryTreeNode(4545, "Товары для дома", List.of()),
                                new CategoryTreeNode(4546, "Товары для дачи", List.of()))),
                new CategoryTreeNode(90, "Для красоты",
                        List.of(new CategoryTreeNode(9090, "Косметика", List.of()))),
                new CategoryTreeNode(68, "Спортивное",
                        List.of(new CategoryTreeNode(6869, "Спорт", List.of())))
        ));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_categories_dao_before.csv",
            after = "catalog_categories_dao_after.csv")
    public void testSetCategoriesByPromo() {
        dao.setCategoriesByPromo(
                List.of(
                        new CatalogPromoCategories(
                                "111",
                                List.of(new Category(2323, "Бытовая техника",
                                                    new Category(23, "Техника", null)))),
                        new CatalogPromoCategories(
                                "222",
                                List.of(new Category(4545, "Товары для дома",
                                                    new Category(45, "Дом", null)),
                                        new Category(4546, "Товары для дачи",
                                                new Category(45, "Дом", null)))),
                        new CatalogPromoCategories(
                                "444",
                                List.of(new Category(9090, "Косметика",
                                                new Category(90, "Для красоты", null)))
                        ),
                        new CatalogPromoCategories(
                                "555",
                                List.of(new Category(1133, "Книги", null))
                        )));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_categories_dao_before.csv",
            after = "catalog_categories_dao_before.csv")
    public void testSetCategoriesIdentical() {
        dao.setCategoriesByPromo(
                List.of(
                        new CatalogPromoCategories(
                                "111",
                                List.of(new Category(2323, "Бытовая техника",
                                            new Category(23, "Техника", null)))),
                        new CatalogPromoCategories(
                                "222",
                                List.of(new Category(4545, "Товары для дома",
                                                new Category(45, "Дом", null)),
                                        new Category(2323, "Бытовая техника",
                                                new Category(23, "Техника", null)),
                                        new Category(9898, "Обувь",
                                                new Category(98, "Одежда и обувь", null))
                        )),
                        new CatalogPromoCategories(
                                "444",
                                List.of(new Category(2323, "Бытовая техника",
                                                new Category(23, "Техника", null)))
                        ),
                        new CatalogPromoCategories(
                                "333", List.of(new Category(6869, "Спорт",
                                                                new Category(68, "Спортивное", null)
                        )))));
    }
}
