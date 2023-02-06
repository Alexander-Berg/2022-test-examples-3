package ru.yandex.market.mbo.category;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.db.pg.BasePgTestClass;
import ru.yandex.market.mbo.export.category.CategoryAvgTitleSize;
import ru.yandex.market.mbo.export.category.CategoryAvgTitleSizePgService;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryAvgTitleSizePgServiceTest extends BasePgTestClass {

    public static final CategoryAvgTitleSize VALID_RECORD_1 = CategoryAvgTitleSize.record(1L, 101, 11);
    public static final CategoryAvgTitleSize VALID_RECORD_2 = CategoryAvgTitleSize.record(2L, 102, 12);
    public static final CategoryAvgTitleSize VALID_RECORD_3 = CategoryAvgTitleSize.record(3L, 103, 13);

    public static final List<CategoryAvgTitleSize> VALID_RECORDS = Arrays.asList(
        VALID_RECORD_1, VALID_RECORD_2, VALID_RECORD_3
    );

    public static final List<CategoryAvgTitleSize> INVALID_RECORDS = Arrays.asList(
        CategoryAvgTitleSize.record(1L, -100, 10),
        CategoryAvgTitleSize.record(2L, 100, -10),
        CategoryAvgTitleSize.record(3L, 10, 100)
    );

    @Inject
    private DataSource siteCatalogPgDb;
    private CategoryAvgTitleSizePgService pgService;

    @Before
    public void setUp() {
        pgService = new CategoryAvgTitleSizePgService(
            new TransactionTemplate(new DataSourceTransactionManager(siteCatalogPgDb)),
            new JdbcTemplate(siteCatalogPgDb)
        );
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testFailureOnNegativeCharsAmount() {
        pgService.update(Collections.singletonList(INVALID_RECORDS.get(0)));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testFailureOnNegativeWordsAmount() {
        pgService.update(Collections.singletonList(INVALID_RECORDS.get(1)));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testFailureOnNegativeCharsAmountLessThanWordsAmount() {
        pgService.update(Collections.singletonList(INVALID_RECORDS.get(2)));
    }

    @Test
    public void testInsertAndGetAll() {
        pgService.update(VALID_RECORDS);
        List<CategoryAvgTitleSize> sizes = pgService.getCategoryAvgTitleSizes();
        assertThat(sizes).size().isEqualTo(VALID_RECORDS.size());
        assertThat(sizes).containsExactlyInAnyOrderElementsOf(VALID_RECORDS);
    }

    @Test
    public void testInsertAndGetByCategory() {
        pgService.update(VALID_RECORDS);
        CategoryAvgTitleSize size = pgService.getCategoryAvgTitleSize(2L);
        assertThat(size).isNotNull();
        assertThat(size.getCharsAmount()).isEqualTo(102);
        assertThat(size.getWordsAmount()).isEqualTo(12);
    }

    @Test
    public void testGetByCategoryEmptyResult() {
        pgService.update(Collections.singletonList(VALID_RECORD_2));
        CategoryAvgTitleSize size = pgService.getCategoryAvgTitleSize(2L);
        assertThat(size).isNotNull();
        assertThat(size.getCharsAmount()).isEqualTo(102);
        assertThat(size.getWordsAmount()).isEqualTo(12);

        size = pgService.getCategoryAvgTitleSize(3L);
        assertThat(size).isNull();
    }

    @Test
    public void testInsertAndAfterInsertEmptyOrNull() {
        pgService.update(VALID_RECORDS);
        List<CategoryAvgTitleSize> sizes = pgService.getCategoryAvgTitleSizes();
        assertThat(sizes).size().isEqualTo(VALID_RECORDS.size());
        assertThat(sizes).containsExactlyInAnyOrderElementsOf(VALID_RECORDS);

        // Insert empty list
        pgService.update(Collections.emptyList());
        sizes = pgService.getCategoryAvgTitleSizes();
        assertThat(sizes).size().isEqualTo(VALID_RECORDS.size());
        assertThat(sizes).containsExactlyInAnyOrderElementsOf(VALID_RECORDS);

        // Insert NULL
        pgService.update(null);
        sizes = pgService.getCategoryAvgTitleSizes();
        assertThat(sizes).size().isEqualTo(VALID_RECORDS.size());
        assertThat(sizes).containsExactlyInAnyOrderElementsOf(VALID_RECORDS);
    }

    @Test
    public void testInsertAndAfterInsert() {
        pgService.update(VALID_RECORDS);
        List<CategoryAvgTitleSize> sizes = pgService.getCategoryAvgTitleSizes();
        assertThat(sizes).size().isEqualTo(VALID_RECORDS.size());
        assertThat(sizes).containsExactlyInAnyOrderElementsOf(VALID_RECORDS);

        pgService.update(Collections.singletonList(VALID_RECORD_3));
        sizes = pgService.getCategoryAvgTitleSizes();
        assertThat(sizes).size().isEqualTo(1);
        assertThat(sizes).containsExactlyInAnyOrder(VALID_RECORD_3);
    }

}
