package ru.yandex.direct.core.entity.creative.repository;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.dbschema.ppc.enums.PerfCreativesCreativeType;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.creative.repository.CreativeMappings.creativeTypeToDb;

@RunWith(JUnitParamsRunner.class)
public class CreativeMappingsCreativeTypeToDbTest {

    @Test
    @junitparams.Parameters(method = "values")
    public void creativeTypeToPerfCreativesCreativeType(String name,
                                                        CreativeType creativeType, PerfCreativesCreativeType dbType) {
        assertThat(creativeTypeToDb(creativeType), is(dbType));
    }

    Iterable<Object[]> values() {
        return asList(new Object[][]{
                {"null", null, null},

                {"html5", CreativeType.HTML5_CREATIVE, PerfCreativesCreativeType.html5_creative},
                {"performance", CreativeType.PERFORMANCE, PerfCreativesCreativeType.performance},
                {"bannerstorage", CreativeType.BANNERSTORAGE, PerfCreativesCreativeType.bannerstorage},
                {"canvas", CreativeType.CANVAS, PerfCreativesCreativeType.canvas},

                {"video addition", CreativeType.VIDEO_ADDITION_CREATIVE, PerfCreativesCreativeType.video_addition},
                {"cpm video", CreativeType.CPM_VIDEO_CREATIVE, PerfCreativesCreativeType.video_addition},
                {"cpc video", CreativeType.CPC_VIDEO_CREATIVE, PerfCreativesCreativeType.video_addition},
                {"cpm outdoor", CreativeType.CPM_OUTDOOR_CREATIVE, PerfCreativesCreativeType.video_addition},
        });
    }

}
