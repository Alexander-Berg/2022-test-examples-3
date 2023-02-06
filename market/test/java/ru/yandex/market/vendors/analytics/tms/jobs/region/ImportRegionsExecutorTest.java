package ru.yandex.market.vendors.analytics.tms.jobs.region;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.region.RegionInfo;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.yt.YtTableReader;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "SaveRegionInfosTest.before.csv")
public class ImportRegionsExecutorTest extends FunctionalTest {

    @Autowired
    private YtTableReader<RegionInfo> ytRegionReader;
    @Autowired
    private ImportRegionsExecutor importRegionsExecutor;

    @Test
    @DbUnitDataSet(after = "SaveRegionInfosTest.after.csv")
    void importRegionsExecutorTest() {
        //noinspection unchecked
        reset(ytRegionReader);
        when(ytRegionReader.loadInfoFromYtTable()).thenReturn(
                List.of(
                        new RegionInfo(1L, "Россия", "Russia", 3, 0L),
                        new RegionInfo(2L, "ЦФО", "Central federal district", 4, 1L),
                        new RegionInfo(3L, "ПФО", "Volga federal district", 4, 1L),
                        new RegionInfo(4L, "Москва", "Moscow", 5, 2L),
                        new RegionInfo(5L, "Самара", "Samara", 5, 3L)
                )
        );
        importRegionsExecutor.doJob(null);
    }
}
