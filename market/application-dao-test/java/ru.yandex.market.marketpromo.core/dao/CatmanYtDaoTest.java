package ru.yandex.market.marketpromo.core.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.data.source.yt.YtTableClient;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.utils.YtTestHelper;
import ru.yandex.market.marketpromo.model.Category;
import ru.yandex.market.marketpromo.model.CategoryManager;
import ru.yandex.market.marketpromo.model.CategoryTree;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author kl1san
 */
public class CatmanYtDaoTest extends ServiceTestBase {

    @Autowired
    private YtTestHelper ytTestHelper;

    @Autowired
    private YtTableClient ytTableClient;

    private CatmanYtDao catmanYtDao;

    @BeforeEach
    public void setUp() {
        catmanYtDao = new CatmanYtDao(ytTableClient,
                "//home/market/prestable/mstat/dictionaries/mbo/catman_categories/latest",
                "//home/market/production/mstat/dictionaries/mbo/category_tree_with_names/latest"
        );
    }

    @Test
    public void shouldMapCatmansToModel() {
        List<CategoryManager> categoryManagers = buildCatmanList();
        ytTestHelper.mockCatmanCategoriesResponse(categoryManagers);
        List<CategoryManager> loadedCatmans = catmanYtDao.loadCategoryManagers();

        assertThat(loadedCatmans, allOf(
                hasItem(allOf(
                        hasProperty("id", nullValue()),
                        hasProperty("staffLogin", is("vasya_pupkin")),
                        hasProperty("firstName", is("Василий")),
                        hasProperty("lastName", is("Пупкин")),
                        hasProperty("categoryIds", containsInAnyOrder(241435L, 242699L, 12515112L,
                                14234999L, 17392030L)))),
                hasItem(allOf(
                        hasProperty("id", nullValue()),
                        hasProperty("staffLogin", is("frodor")),
                        hasProperty("firstName", is("Федор")),
                        hasProperty("lastName", is("Федоров")),
                        hasProperty("categoryIds", contains(989040L))))
        ));
    }

    @Test
    public void shouldMapCategoryTreeToModel() {
        ytTestHelper.mockCategoryTreeResponse();
        Map<Long, Category> categoryTree = catmanYtDao.loadCategoryTree().getCategoryMap();

        assertThat(categoryTree, allOf(
                hasEntry(is(90401L), allOf(
                        hasProperty("parentId", is(0L)),
                        hasProperty("leaf", is(true)),
                        hasProperty("published", is(true)),
                        hasProperty("name", is("Все товары")),
                        hasProperty("fullName", is("\tВсе товары\t")))),
                hasEntry(is(90402L), allOf(
                        hasProperty("parentId", is(90401L)),
                        hasProperty("leaf", is(true)),
                        hasProperty("published", is(true)),
                        hasProperty("name", is("Товары для авто- и мототехники")),
                        hasProperty("fullName", is("\tВсе товары\tТовары для авто- и мототехники\t")))),
                hasEntry(is(90403L), allOf(
                        hasProperty("parentId", is(90402L)),
                        hasProperty("leaf", is(true)),
                        hasProperty("published", is(true)),
                        hasProperty("name", is("Автомобильная аудио- и видеотехника")),
                        hasProperty("fullName", is("\tВсе товары\tТовары для авто- и мототехники\tАвтомобильная " +
                                "аудио- и видеотехника\t"))))
        ));
    }

    @Test
    void shouldGetCorrectSubcategories() {
        ytTestHelper.mockCategoryTreeResponse();
        CategoryTree categoryTree = catmanYtDao.loadCategoryTree();
        Set<Long> fromRoot = categoryTree.getAllSubcategories(90401L);
        Set<Long> fromSubcategory = categoryTree.getAllSubcategories(90403L);
        Set<Long> fromLeaf = categoryTree.getAllSubcategories(90407L);

        assertThat(fromRoot, hasSize(5));
        assertThat(fromSubcategory, hasSize(3));
        assertThat(fromLeaf, hasSize(1));
    }

    private List<CategoryManager> buildCatmanList() {
        return List.of(
                new CategoryManager(null, "vasya_pupkin", "Василий", "Пупкин",
                        Set.of(241435L, 242699L, 12515112L, 14234999L, 17392030L)),
                new CategoryManager(null, "frodor", "Федор", "Федоров",
                        Set.of(989040L))
        );
    }

}
