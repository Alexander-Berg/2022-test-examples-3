package ru.yandex.market.mbo.templates.rendering;

import org.junit.Test;
import ru.yandex.market.mbo.core.templates.rendering.TemplateRenderingUtil;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.common.model.Language;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;

/**
 * @author york
 * @since 28.03.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TestForTemplateRendering {

    @Test
    public void testFiltering() {
        MboParameters.Category.Builder category = MboParameters.Category.newBuilder();

        MboParameters.Parameter numParameter = MboParameters.Parameter.newBuilder()
            .setId(10L)
            .addName(word("numeric"))
            .setXslName("num")
            .setValueType(MboParameters.ValueType.NUMERIC)
            .build();

        MboParameters.Parameter numEnumParameter = MboParameters.Parameter.newBuilder()
            .setId(11L)
            .addName(word("numeric-enum"))
            .setXslName("numnum")
            .addAllOption(Arrays.asList(
                option(1L, "1", true),
                option(2L, "2", false),
                option(3L, "3", true)
            ))
            .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
            .build();

        MboParameters.Parameter strParameter = MboParameters.Parameter.newBuilder()
            .setId(12L)
            .addName(word("string"))
            .setXslName("str")
            .setValueType(MboParameters.ValueType.STRING)
            .build();

        MboParameters.Parameter enumParameter = MboParameters.Parameter.newBuilder()
            .setId(13L)
            .addName(word("enum"))
            .setXslName("enum")
            .addAllOption(Arrays.asList(
                option(1L, "q", false),
                option(2L, "w", false),
                option(3L, "e", true)
            ))
            .setValueType(MboParameters.ValueType.ENUM)
            .build();

        category.addAllParameter(Arrays.asList(numParameter, numEnumParameter, strParameter, enumParameter));

        MboParameters.Category filtered = TemplateRenderingUtil.filterCategoryForRendering(category.build());

        Assert.assertEquals(filtered.getParameterCount(), 4);

        Optional<MboParameters.Parameter> pNEnum = filtered.getParameterList().stream()
            .filter(p -> p.getId() == numEnumParameter.getId()).findAny();

        Assert.assertTrue(pNEnum.isPresent());

        Assert.assertEquals(pNEnum.get().getOptionCount(), 2);

        Assert.assertTrue(getOptionIds(pNEnum.get()).containsAll(Arrays.asList(1L, 3L)));

        Optional<MboParameters.Parameter> pEnum = filtered.getParameterList().stream()
            .filter(p -> p.getId() == enumParameter.getId()).findAny();

        Assert.assertTrue(pEnum.isPresent());

        Assert.assertEquals(pEnum.get().getOptionCount(), 1);

        Assert.assertTrue(getOptionIds(pEnum.get()).containsAll(Arrays.asList(3L)));

    }

    private List<Long> getOptionIds(MboParameters.Parameter parameter) {
        return parameter.getOptionList().stream().map(MboParameters.Option::getId).collect(Collectors.toList());
    }

    private static MboParameters.Option option(Long id, String name, boolean published) {
        return MboParameters.Option.newBuilder().setId(id).addName(word(name)).setPublished(published).build();
    }

    private static MboParameters.Word word(String name) {
        return MboParameters.Word.newBuilder()
            .setName(name)
            .setLangId(Language.RUSSIAN.getId())
            .build();
    }
}
