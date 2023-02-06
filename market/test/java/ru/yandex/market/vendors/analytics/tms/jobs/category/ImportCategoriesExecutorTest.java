package ru.yandex.market.vendors.analytics.tms.jobs.category;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.categories.CategoryInfo;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtCategoryReader;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для джобы {@link ImportCategoriesExecutor}.
 *
 * @author ogonek
 */
public class ImportCategoriesExecutorTest extends FunctionalTest {

    @Autowired
    private YtCategoryReader ytCategoryReader;

    @Autowired
    private ImportCategoriesExecutor importCategoriesExecutor;

    /**
     * Проверяет работу {@link ImportCategoriesExecutor}.
     */
    @Test
    @DbUnitDataSet(after = "SaveCategoriesInfoTest.after.csv")
    void importCategoriesExecutorTest() {
        reset(ytCategoryReader);
        when(ytCategoryReader.loadInfoFromYtTable()).thenReturn(List.of(
                new CategoryInfo(12L, 0L, "Все", "All", null),
                new CategoryInfo(14L, 12L, "Автомобили", "Cars", "gurulight"),
                new CategoryInfo(15L, 12L, "Мобильные телефоны", "Phones", "guru")
        ));
        importCategoriesExecutor.doJob(null);
    }
}
