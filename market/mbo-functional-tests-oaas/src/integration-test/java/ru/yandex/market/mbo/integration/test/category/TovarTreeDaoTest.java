package ru.yandex.market.mbo.integration.test.category;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.gwt.models.visual.CategoryHistoricalData;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.utils.RandomTestUtils;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class TovarTreeDaoTest extends BaseIntegrationTest {
    public static final long UID1 = 777L;
    public static final long UID2 = 888L;

    private static final String SQL_CREATE_SCHEMA_PG = "CREATE SCHEMA IF NOT EXISTS site_catalog";

    private static final String SQL_CREATE_SEQUENCE_PG =
        "CREATE SEQUENCE IF NOT EXISTS SITE_CATALOG.TOVAR_TREE_ID MINVALUE 1 MAXVALUE 1000000";

    private static final String SQL_SET_SEARCH_PATH = "SET search_path TO site_catalog";

    @Inject
    private TovarTreeDao tovarTreeDao;
    @Inject
    private JdbcTemplate siteCatalogPgJdbcTemplate;

    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = RandomTestUtils.createNewRandom(1);

        //init required state in local pg database
        siteCatalogPgJdbcTemplate.execute(SQL_CREATE_SCHEMA_PG);
        siteCatalogPgJdbcTemplate.execute(SQL_SET_SEARCH_PATH);
        siteCatalogPgJdbcTemplate.execute(SQL_CREATE_SEQUENCE_PG);
    }

    @Test
    public void createAndUpdateCategory() {
        TovarCategory tovarCategory = createNewTovarCategory();
        tovarCategory.setLeaf(true);
        long hid = tovarTreeDao.createCategory(tovarCategory, UID1);

        TovarCategory created = tovarTreeDao.loadCategoryByHid(hid);

        TovarCategory createdNoIds = assertAndCleanIds(created);
        tovarCategory.setLastModificationUid(UID1);
        assertThat(createdNoIds).isEqualTo(tovarCategory);

        created.setLinkedCategories("qwerqwerwqerqwerwqe");
        tovarTreeDao.updateCategory(created, UID2);

        TovarCategory updated = tovarTreeDao.loadCategoryByHid(hid);
        created.setLastModificationUid(UID2);

        assertThat(assertAndCleanIds(updated)).isEqualTo(cleanIds(created));
    }

    @Test
    public void setGuruCategoryId() {
        TovarCategory tovarCategory = createNewTovarCategory();
        tovarCategory.setGuruCategoryId(TovarCategory.NO_ID);

        long hid = tovarTreeDao.createCategory(tovarCategory, UID1);

        TovarCategory created = tovarTreeDao.loadCategoryByHid(hid);

        boolean categoryIdSet = tovarTreeDao.setGuruCategoryId(created.getHid(), 100500L);

        assertThat(categoryIdSet).isTrue();

        TovarCategory updated = tovarTreeDao.loadCategoryByHid(hid);
        assertThat(updated).extracting(TovarCategory::getGuruCategoryId).isEqualTo(100500L);

        categoryIdSet = tovarTreeDao.setGuruCategoryId(created.getHid(), 200500L);

        assertThat(categoryIdSet).isFalse();

        updated = tovarTreeDao.loadCategoryByHid(hid);
        assertThat(updated).extracting(TovarCategory::getGuruCategoryId).isEqualTo(100500L);
    }

    private TovarCategory createNewTovarCategory() {
        TovarCategory category = random.nextObject(TovarCategory.class);
        cleanIds(category);
        return category;
    }

    private TovarCategory cleanIds(TovarCategory category) {
        category.setId(TovarCategory.NO_ID);
        category.setTovarId((int) TovarCategory.NO_ID);
        category.getNames().forEach(n -> n.setId(TovarCategory.NO_ID));
        category.getAliases().forEach(n -> n.setId(TovarCategory.NO_ID));
        category.getClassificationParams().clear();
        List<CategoryHistoricalData> historicalData = category.getHistoricalData().stream()
            .sorted(Comparator.comparing(CategoryHistoricalData::getModificationDate).reversed())
            .collect(Collectors.toList());
        category.setHistoricalData(historicalData);
        return category;
    }

    private TovarCategory assertAndCleanIds(TovarCategory category) {
        assertThat(category).extracting(TovarCategory::getId).isNotEqualTo(TovarCategory.NO_ID);
        assertThat(category).extracting(TovarCategory::getHid).isNotEqualTo(TovarCategory.NO_ID);
        assertThat(category).extracting(TovarCategory::getTovarId).isNotEqualTo(TovarCategory.NO_ID);
        TovarCategory copy = category.getDeepCopy();
        cleanIds(copy);
        return copy;
    }
}
