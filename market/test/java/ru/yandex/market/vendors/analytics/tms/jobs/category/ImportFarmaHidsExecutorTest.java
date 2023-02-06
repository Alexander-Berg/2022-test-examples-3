package ru.yandex.market.vendors.analytics.tms.jobs.category;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.jpa.entity.FarmaHid;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для джобы {@link ImportFarmaHidsExecutor}.
 *
 * @author ogonek
 */
@DbUnitDataSet(before = "SaveFarmaHids.before.csv")
public class ImportFarmaHidsExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<FarmaHid> ytFarmaHidReader;
    @Autowired
    private ImportFarmaHidsExecutor importFarmaHidsExecutor;

    @Test
    @DbUnitDataSet(after = "SaveFarmaHids.after.csv")
    void importBrandsExecutorTest() {
        //noinspection unchecked
        reset(ytFarmaHidReader);
        when(ytFarmaHidReader.loadInfoFromYtTable()).thenReturn(
                ImmutableList.of(
                        new FarmaHid(774L),
                        new FarmaHid(228L)
                )
        );
        importFarmaHidsExecutor.doJob(null);
    }
}
