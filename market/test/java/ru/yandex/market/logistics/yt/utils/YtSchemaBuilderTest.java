package ru.yandex.market.logistics.yt.utils;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

class YtSchemaBuilderTest {
    @RegisterExtension
    protected JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Test
    public void testCreateSchema() {
        YTreeNode schema = new YtSchemaBuilder()
            .field("a", YtWriter.INT_64, false)
            .field("b", YtWriter.STRING, true)
            .build();

        softly.assertThat(schema).isEqualTo(new YTreeBuilder()
            .beginList()
            .value(
                YTree.mapBuilder()
                    .key("name").value("a")
                    .key("type").value(YtWriter.INT_64)
                    .buildMap()
            )
            .value(
                YTree.mapBuilder()
                    .key("name").value("b")
                    .key("type").value(YtWriter.STRING)
                    .key("required").value(true)
                    .buildMap()
            )
            .endList()
            .build());
    }

    @Test
    public void testAnyNotRequired() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new YtSchemaBuilder()
                .field("a", YtWriter.ANY, true)
                .build()
        );
    }
}
