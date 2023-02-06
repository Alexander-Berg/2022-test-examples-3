package ru.yandex.market.vendors.analytics.tms.jobs.category;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.categories.CategoryWithDepatment;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для джобы {@link ImportManagerWhitelistExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "ImportManagerWhitelistExecutorTest.before.csv")
public class ImportManagerWhitelistExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<CategoryWithDepatment> ytManagerWhitelistReader;
    @Autowired
    private ImportManagerWhitelistExecutor importManagerWhitelistExecutor;

    @Test
    @DbUnitDataSet(after = "ImportManagerWhitelistExecutorTest.after.csv")
    void importPriceSegmentsExecutorTest() {
        //noinspection unchecked
        reset(ytManagerWhitelistReader);
        when(ytManagerWhitelistReader.loadInfoFromYtTable()).thenReturn(
                List.of(
                        new CategoryWithDepatment(91491, "test1", "department1"),
                        new CategoryWithDepatment(91013, "test2", "department2"),
                        new CategoryWithDepatment(999, "test3", "department3"),
                        new CategoryWithDepatment(1000, "test4", "department4")
                )
        );
        importManagerWhitelistExecutor.doJob(null);
    }

    @Test
    void importBadPriceSegmentsExecutorTest() {
        //noinspection unchecked
        reset(ytManagerWhitelistReader);
        when(ytManagerWhitelistReader.loadInfoFromYtTable()).thenReturn(
                List.of(
                        new CategoryWithDepatment(91491, "test1", "department1")
                )
        );
        assertThrows(IllegalStateException.class, () -> importManagerWhitelistExecutor.doJob(null));
    }
}
