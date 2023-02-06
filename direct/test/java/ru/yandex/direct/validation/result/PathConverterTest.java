package ru.yandex.direct.validation.result;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class PathConverterTest {

    @Test
    public void convert_SuccessSimpleSubstitution() {
        PathConverter converter = noNamePathConverterBuilder()
                .add("AAA", "aaa")
                .add("BBB", "bbb")
                .build();

        Path original = new Path(asList(field("AAA"), index(0), field("BBB")));
        Path actual = converter.convert(original);

        assertThat(actual.toString(), is("aaa[0].bbb"));
    }

    @Test
    public void convert_SuccessComplexSubstitution() {
        PathConverter converter = noNamePathConverterBuilder()
                .add("AAA", "aaa.ccc")
                .add("BBB", "bbb")
                .build();

        Path original = new Path(asList(field("AAA"), index(0), field("BBB")));
        Path actual = converter.convert(original);

        assertThat(actual.toString(), is("aaa.ccc[0].bbb"));
    }

    @Test(expected = IllegalStateException.class)
    public void convert_ExceptionOnUnknownPathNode() {
        PathConverter emptyConverter = noNamePathConverterBuilder().build();

        Path original = new Path(singletonList(field("CCC")));
        emptyConverter.convert(original);
    }

    @Test
    public void convert_ExceptionOnUnknownPathNodeContainsConverterName() {
        String name = "testConverterName";
        PathConverter emptyConverter = MappingPathConverter.builder(name).build();

        Path original = new Path(singletonList(field("CCC")));

        assertThatThrownBy(() -> emptyConverter.convert(original)).hasMessageContaining(name);
    }

    @Test
    public void convert_SuccessWhenInitByMap() {
        PathConverter pathConverter = noNamePathConverterBuilder()
                .add(ImmutableMap.of("CCC", "ccc"))
                .build();

        Path original = new Path(singletonList(field("CCC")));
        Path actual = pathConverter.convert(original);

        assertThat(actual.toString(), is("ccc"));
    }

    @Nonnull
    private MappingPathConverter.Builder noNamePathConverterBuilder() {
        return MappingPathConverter.builder("");
    }

}
