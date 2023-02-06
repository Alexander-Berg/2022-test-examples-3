package ru.yandex.direct.core.entity.creative.repository;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.dbschema.ppc.enums.PerfCreativesCreativeType;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.creative.repository.CreativeMappings.creativeTypeByTypeAndLayoutIdFromDb;

@RunWith(JUnitParamsRunner.class)
public class CreativeMappingsCreativeTypeFromDbTest {

    @Test
    @junitparams.Parameters(method = "values")
    public void creativeTypeByTypeAndLayoutIdFromDb_DbTypeAndLayoutIdToCreativeType(String name,
                                                                                    PerfCreativesCreativeType dbType, Long layoutId,
                                                                                    CreativeType creativeType) {
        assertThat(creativeTypeByTypeAndLayoutIdFromDb(dbType, layoutId), is(creativeType));
    }

    Iterable<Object[]> values() {
        return asList(new Object[][]{
                {"html", PerfCreativesCreativeType.html5_creative, null, CreativeType.HTML5_CREATIVE},
                {"canvas", PerfCreativesCreativeType.canvas, null, CreativeType.CANVAS},
                {"performance", PerfCreativesCreativeType.performance, null, CreativeType.PERFORMANCE},
                {"bannerstorage", PerfCreativesCreativeType.bannerstorage, null, CreativeType.BANNERSTORAGE},
                {"canvas with layout", PerfCreativesCreativeType.canvas, 100L, CreativeType.CANVAS},
                {"video addition", PerfCreativesCreativeType.video_addition, 1L, CreativeType.VIDEO_ADDITION_CREATIVE},
                {"cpm video", PerfCreativesCreativeType.video_addition, 6L, CreativeType.CPM_VIDEO_CREATIVE},
                {"cpc video", PerfCreativesCreativeType.video_addition, 51L, CreativeType.CPC_VIDEO_CREATIVE},
                {"cpm outdoor", PerfCreativesCreativeType.video_addition, 101L, CreativeType.CPM_OUTDOOR_CREATIVE},
        });
    }

}
