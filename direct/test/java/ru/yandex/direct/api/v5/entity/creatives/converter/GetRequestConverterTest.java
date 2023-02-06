package ru.yandex.direct.api.v5.entity.creatives.converter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.creatives.CreativeTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.creative.model.CreativeType;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.creatives.converter.GetRequestConverter.convertTypes;

@Api5Test
@RunWith(Parameterized.class)
public class GetRequestConverterTest {

    @Parameterized.Parameter(0)
    public List<CreativeTypeEnum> creativeTypeEnums;

    @Parameterized.Parameter(1)
    public Set<CreativeType> convertedTypes;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {singletonList(CreativeTypeEnum.IMAGE_CREATIVE),
                        ImmutableSet.of(CreativeType.CANVAS)},
                {singletonList(CreativeTypeEnum.HTML_5_CREATIVE),
                        ImmutableSet.of(CreativeType.HTML5_CREATIVE)},
                {singletonList(CreativeTypeEnum.VIDEO_EXTENSION_CREATIVE),
                        ImmutableSet.of(CreativeType.VIDEO_ADDITION_CREATIVE)},
                {singletonList(CreativeTypeEnum.CPC_VIDEO_CREATIVE),
                        ImmutableSet.of(CreativeType.CPC_VIDEO_CREATIVE)},
                {singletonList(CreativeTypeEnum.SMART_CREATIVE),
                        ImmutableSet.of(CreativeType.PERFORMANCE)},

                {asList(CreativeTypeEnum.IMAGE_CREATIVE, CreativeTypeEnum.IMAGE_CREATIVE),
                        ImmutableSet.of(CreativeType.CANVAS)},

                {asList(CreativeTypeEnum.IMAGE_CREATIVE, CreativeTypeEnum.HTML_5_CREATIVE),
                        ImmutableSet.of(CreativeType.CANVAS, CreativeType.HTML5_CREATIVE)},

                {asList(CreativeTypeEnum.VIDEO_EXTENSION_CREATIVE, CreativeTypeEnum.CPC_VIDEO_CREATIVE),
                        ImmutableSet.of(CreativeType.VIDEO_ADDITION_CREATIVE, CreativeType.CPC_VIDEO_CREATIVE)},
        };

        return asList(data);
    }

    @Test
    public void convert() {
        assertThat(convertTypes(creativeTypeEnums)).containsExactlyInAnyOrderElementsOf(convertedTypes);
    }

}
