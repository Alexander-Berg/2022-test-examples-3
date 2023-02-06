package ru.yandex.market.vendors.analytics.tms.jobs.category.pricesegment;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.price.CategoryPriceSegments;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для джобы {@link ImportPriceSegmentsExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "SavePriceSegmentsTest.before.csv")
public class ImportPriceSegmentsExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<CategoryPriceSegments> ytTableReader;
    @Autowired
    private ImportPriceSegmentsExecutor importPriceSegmentsExecutor;

    @Test
    @DbUnitDataSet(after = "SavePriceSegmentsInfosTest.after.csv")
    void importPriceSegmentsExecutorTest() {
        //noinspection unchecked
        reset(ytTableReader);
        when(ytTableReader.loadInfoFromYtTable()).thenReturn(
                List.of(
                        new CategoryPriceSegments(1L,
                                100L,
                                200L,
                                300L,
                                400L,
                                500L,
                                600L,
                                700L
                        ),
                        new CategoryPriceSegments(2L,
                                1100L,
                                1200L,
                                1300L,
                                1400L,
                                1500L,
                                1600L,
                                1700L
                        )
                )
        );
        importPriceSegmentsExecutor.doJob(null);
    }
}
