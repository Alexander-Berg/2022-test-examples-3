package ru.yandex.market.mbo.db.params;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.GuruParamFilter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

@SuppressWarnings("checkstyle:magicNumber")
public class ParameterLoaderServiceTest {

    @Test
    public void testMatchByParamAlias() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC,
            "testParam",
            ImmutableList.of("testSecondName", "тестовый алиас"),
            ImmutableMap.of());
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("алиас");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchByParamAlias() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC,
            "testParam",
            ImmutableList.of(),
            ImmutableMap.of());
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("некая строка");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testMatchByOptionName() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName", "тестовый алиас"),
            ImmutableMap.of(
                "тут некая опция", Collections.emptyList(),
                "еще опция", Collections.emptyList()
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("некая опция");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchByOptionName() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName", "тестовый алиас"),
            ImmutableMap.of(
                "еще опция", Collections.emptyList()
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("некая опция");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testMatchByOptionAlias() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName"),
            ImmutableMap.of(
                "опция", ImmutableList.of("алиас опции какой то")
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("алиас опции");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchByOptionAlias() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName"),
            ImmutableMap.of(
                "опция", ImmutableList.of("алиас опции какой то")
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("алиас не подходящий");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testMatchNumericEnumOption() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC_ENUM,
            "testParam",
            ImmutableList.of("testSecondName"),
            ImmutableMap.of(
                "200", ImmutableList.of(),
                "1000", ImmutableList.of()
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("100");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchNumericEnumOption() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC_ENUM,
            "testParam",
            ImmutableList.of("100"),
            ImmutableMap.of(
                "1000", ImmutableList.of()
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("200");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testMatchParamId() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC_ENUM,
            "testParam",
            ImmutableList.of("100"),
            ImmutableMap.of(
                "1000", ImmutableList.of()
            )
        );
        param.setId(123);
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("123");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchParamId() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC_ENUM,
            "testParam",
            ImmutableList.of("100"),
            ImmutableMap.of(
                "1000", ImmutableList.of()
            )
        );
        param.setId(1234);
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("123");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testMatchStrictByParamAlias() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC,
            "testParam",
            ImmutableList.of("testSecondName", "тестовый алиас"),
            ImmutableMap.of());
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("тестовый алиас");
        flt.setStrict(1);
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchStrictByParamAlias() {
        CategoryParam param = createParam(
            Param.Type.NUMERIC,
            "testParam",
            ImmutableList.of("testSecondName", "тестовый алиас"),
            ImmutableMap.of());
        GuruParamFilter flt = new GuruParamFilter();
        flt.setText("алиас");
        flt.setStrict(1);
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testMatchStrictByOptionName() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName", "тестовый алиас"),
            ImmutableMap.of(
                "тут некая опция", Collections.emptyList(),
                "еще опция", Collections.emptyList()
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setStrict(1);
        flt.setText("еще опция");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchStrictByOptionName() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName", "тестовый алиас"),
            ImmutableMap.of(
                "тут некая опция", Collections.emptyList(),
                "еще опция", Collections.emptyList()
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setStrict(1);
        flt.setText("некая опция");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testMatchStrictByOptionAlias() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName"),
            ImmutableMap.of(
                "опция", ImmutableList.of("алиас опции какой то")
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setStrict(1);
        flt.setText("алиас опции какой то");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isTrue();
    }

    @Test
    public void testNotMatchStrictByOptionAlias() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName"),
            ImmutableMap.of(
                "опция", ImmutableList.of("алиас опции какой то")
            )
        );
        GuruParamFilter flt = new GuruParamFilter();
        flt.setStrict(1);
        flt.setText("алиас опции");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    @Test
    public void testNullXslName() {
        CategoryParam param = createParam(
            Param.Type.ENUM,
            "testParam",
            ImmutableList.of("testSecondName"),
            ImmutableMap.of(
                "опция", ImmutableList.of("алиас опции какой то")
            )
        );
        param.setXslName(null);
        GuruParamFilter flt = new GuruParamFilter();
        flt.setStrict(1);
        flt.setText("алиас опции");
        Assertions.assertThat(ParameterLoaderService.isMatch(param, flt)).isFalse();
    }

    private static CategoryParam createParam(Param.Type type, String name, List<String> aliases,
                                             Map<String, List<String>> options) {
        Parameter param = new Parameter();
        param.setType(type);
        param.setXslName(name);
        param.setLocalizedNames(Collections.singletonList(WordUtil.defaultWord(name)));
        param.setLocalizedAliases(aliases.stream()
            .map(WordUtil::defaultWord)
            .collect(Collectors.toList()));

       options.forEach((k, v) -> {
           Option opt = new OptionImpl(k);
           opt.setAliases(v.stream()
               .map(s -> new EnumAlias(0, Language.RUSSIAN.getId(), s))
               .collect(Collectors.toList()));
           param.addOption(opt);
       });

        return param;
    }
}
