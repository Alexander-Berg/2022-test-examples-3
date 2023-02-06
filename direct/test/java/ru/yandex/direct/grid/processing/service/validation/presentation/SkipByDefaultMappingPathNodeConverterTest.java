package ru.yandex.direct.grid.processing.service.validation.presentation;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.validation.result.PathNode;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.index;

@ParametersAreNonnullByDefault
public class SkipByDefaultMappingPathNodeConverterTest {

    @Test
    public void convert_SuccessSimpleSubstitution() {
        SkipByDefaultMappingPathNodeConverter converter = SkipByDefaultMappingPathNodeConverter.builder()
                .replace("aaa", "AAA")
                .build();

        PathNode.Field node = new PathNode.Field("aaa");
        String result = converter.convert(node).toString();
        assertThat(result, equalTo("AAA"));
    }

    @Test
    public void convert_SuccessOneToManySubstitution() {
        SkipByDefaultMappingPathNodeConverter converter = SkipByDefaultMappingPathNodeConverter.builder()
                .replace("aaa", asList("AAA", "BBB"))
                .build();

        PathNode.Field node = new PathNode.Field("aaa");
        String result = converter.convert(node).toString();
        assertThat(result, equalTo("AAA.BBB"));
    }

    @Test
    public void convert_SuccessWithIndex() {
        SkipByDefaultMappingPathNodeConverter converter = SkipByDefaultMappingPathNodeConverter.builder()
                .replace("items", "itemsToWeb")
                .build();

        PathNode.Field node = new PathNode.Field("items");
        String result = converter.convert(node, index(1)).toString();
        assertThat(result, equalTo("[1]"));
    }

    @Test
    public void skip_whenUnknownPathNode() {
        SkipByDefaultMappingPathNodeConverter converter = SkipByDefaultMappingPathNodeConverter.builder()
                .replace("bbb", "BBB")
                .build();

        PathNode.Field node = new PathNode.Field("aaa");
        String result = converter.convert(node).toString();
        assertThat(result, isEmptyString());

        result = converter.convert(node, index(0)).toString();
        assertThat(result, isEmptyString());
    }

}
