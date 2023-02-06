package ru.yandex.direct.validation.result;

import javax.annotation.Nonnull;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MappingPathNodeConverterTest {
    @Test
    public void convert_SuccessSimpleSubstitution() {
        MappingPathNodeConverter converter = noNameConverterBuilder()
                .replace("aaa", "AAA")
                .build();

        PathNode.Field node = new PathNode.Field("aaa");
        String result = converter.convert(node).toString();
        assertThat(result, equalTo("AAA"));
    }

    @Test
    public void convert_SuccessOneToManySubstitution() {
        MappingPathNodeConverter converter = noNameConverterBuilder()
                .replace("aaa", asList("AAA", "BBB"))
                .build();

        PathNode.Field node = new PathNode.Field("aaa");
        String result = converter.convert(node).toString();
        assertThat(result, equalTo("AAA.BBB"));
    }

    @Test
    public void convert_SuccessSkipping() {
        MappingPathNodeConverter converter = noNameConverterBuilder()
                .skip("aaa")
                .build();

        PathNode.Field node = new PathNode.Field("aaa");
        String result = converter.convert(node).toString();
        assertThat(result, equalTo(""));
    }

    @Test
    public void convert_returnGivenNode_whenUnknownPathNode() {
        MappingPathNodeConverter converter = noNameConverterBuilder()
                .build();

        PathNode.Field node = new PathNode.Field("aaa");
        String result = converter.convert(node).toString();
        assertThat(result, equalTo("aaa"));
    }

    @Nonnull
    private MappingPathNodeConverter.Builder noNameConverterBuilder() {
        return MappingPathNodeConverter.builder("noNameConverter");
    }
}
