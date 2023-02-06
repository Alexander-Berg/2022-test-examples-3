package ru.yandex.direct.grid.processing.processor;

import java.util.List;
import java.util.Set;

import graphql.execution.ResultPath;
import one.util.streamex.StreamEx;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class GridTracingInstrumentationPredicatesTest {

    private static class Root {
    }

    private static class TypeA {
    }

    private static class TypeB {
    }

    @Test
    public void getPathSet_simpleLinearScheme() {
        Set<String> pathSet = getPathSet(asList(
                new GridTracingInstrumentationPredicates.QueryData("root", Root.class, null),
                new GridTracingInstrumentationPredicates.QueryData("fieldA", TypeA.class, Root.class),
                new GridTracingInstrumentationPredicates.QueryData("fieldB", TypeB.class, Root.class)
        ));

        assertThat(pathSet).containsExactlyInAnyOrder(
                "/root",
                "/root/fieldA",
                "/root/fieldB");
    }

    @Test
    public void getPathSet_manyParents() {
        Set<String> pathSet = getPathSet(asList(
                new GridTracingInstrumentationPredicates.QueryData("root", Root.class, null),
                new GridTracingInstrumentationPredicates.QueryData("fieldA", TypeA.class, Root.class),
                new GridTracingInstrumentationPredicates.QueryData("fieldB", TypeB.class, Root.class),
                new GridTracingInstrumentationPredicates.QueryData("fieldB", TypeB.class, TypeA.class)
        ));

        assertThat(pathSet).containsExactlyInAnyOrder(
                "/root",
                "/root/fieldA",
                "/root/fieldB",
                "/root/fieldA/fieldB");
    }

    private Set<String> getPathSet(List<GridTracingInstrumentationPredicates.QueryData> queryData) {
        return StreamEx.of(GridTracingInstrumentationPredicates.getPathSet(queryData))
                .map(ResultPath::toString)
                .toSet();
    }
}
