package ru.yandex.market.ydb.integration;

import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ydb.integration.table.IndexColumn;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

public class IndexColumnTest {

    @Test
    void shouldSortColumnsByWeight() {
        assertThat(Stream.of(
                new IndexColumn("c", 3),
                new IndexColumn("a", 1),
                new IndexColumn("b", 1)
        ).sorted()
                .collect(toUnmodifiableList()), Matchers.contains(
                hasProperty("name", is("a")),
                hasProperty("name", is("b")),
                hasProperty("name", is("c"))
        ));
    }
}
