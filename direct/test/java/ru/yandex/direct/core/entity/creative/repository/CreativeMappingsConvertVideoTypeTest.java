package ru.yandex.direct.core.entity.creative.repository;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.creative.model.CreativeType;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.creative.repository.CreativeMappings.convertVideoType;

@RunWith(JUnitParamsRunner.class)
public class CreativeMappingsConvertVideoTypeTest {

    @Test
    @junitparams.Parameters(method = "validValues")
    public void convertVideoType_LayotIdToCreativeType(String name, Long layoutId, CreativeType creativeType) {
        assertThat(convertVideoType(layoutId), is(creativeType));
    }

    Iterable<Object[]> validValues() {
        return asList(new Object[][]{
                {"min - video", 1L, CreativeType.VIDEO_ADDITION_CREATIVE},
                {"middle - video", 3L, CreativeType.VIDEO_ADDITION_CREATIVE},
                {"max - video", 5L, CreativeType.VIDEO_ADDITION_CREATIVE},
                {"min - cpm", 6L, CreativeType.CPM_VIDEO_CREATIVE},
                {"max - cpm", 6L, CreativeType.CPM_VIDEO_CREATIVE},
                {"min - mobile_content - video", 11L, CreativeType.VIDEO_ADDITION_CREATIVE},
                {"middle - mobile_content - video", 13L, CreativeType.VIDEO_ADDITION_CREATIVE},
                {"max - mobile_content - video", 15L, CreativeType.VIDEO_ADDITION_CREATIVE},
                {"min - cpc", 51L, CreativeType.CPC_VIDEO_CREATIVE},
                {"middle - cpc", 61L, CreativeType.CPC_VIDEO_CREATIVE},
                {"max - cpc", 65L, CreativeType.CPC_VIDEO_CREATIVE},
                {"min - cpm_outdoor", 101L, CreativeType.CPM_OUTDOOR_CREATIVE},
                {"middle - cpm_outdoor", 125L, CreativeType.CPM_OUTDOOR_CREATIVE},
                {"max - cpm_outdoor", 150L, CreativeType.CPM_OUTDOOR_CREATIVE},
        });
    }

    @Test(expected = IllegalStateException.class)
    @junitparams.Parameters(method = "invalidValues")
    public void convertVideoType_IllegalLayoutId(String name, Long layoutId) {
        convertVideoType(layoutId);
    }

    Iterable<Object[]> invalidValues() {
        return asList(new Object[][]{
                {"null", null},
                {"negative", -1L},
                {"zero", 0L},
                {"greater than max", 451L}
        });
    }
}
