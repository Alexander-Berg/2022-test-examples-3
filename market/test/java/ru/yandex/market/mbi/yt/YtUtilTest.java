package ru.yandex.market.mbi.yt;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.formula.functions.T;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YtUtilTest {

    public static boolean yPathHasPath(@Nullable YPath yPath, String expectedPath) {
        if (yPath == null) {
            return false;
        }
        return expectedPath.equals(yPath.toString());
    }

    @Nonnull
    public static YTreeMapNode treeMapNode(final Map<String, YTreeNode> attributes) {
        final YTreeMapNodeImpl entries = new YTreeMapNodeImpl(null);
        attributes.forEach(entries::put);
        return entries;
    }

    @Nonnull
    public static YTreeListNode treeListNode(final List<? extends YTreeNode> values) {
        final YTreeListNodeImpl yTreeListNode = new YTreeListNodeImpl(new OpenHashMap<>());
        values.forEach(yTreeListNode::add);
        return yTreeListNode;
    }

    @Nonnull
    public static YTreeIntegerNodeImpl intNode(int value) {
        return new YTreeIntegerNodeImpl(false, value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeIntegerNodeImpl longNode(long value) {
        return new YTreeIntegerNodeImpl(false, value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeStringNodeImpl stringNode(String value) {
        return new YTreeStringNodeImpl(value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeDoubleNodeImpl floatNode(float value) {
        return new YTreeDoubleNodeImpl(value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeBooleanNodeImpl booleanNode(boolean value) {
        return new YTreeBooleanNodeImpl(value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeStringNode tableNode(String name) {
        YTreeStringNode node = stringNode(name);
        node.putAttribute("type", stringNode("table"));
        return node;
    }

    @Nonnull
    public static YTreeEntityNodeImpl nullNode() {
        return new YTreeEntityNodeImpl(null);
    }

    public static Yt mockYt(final List<YTreeMapNode> tableData) {
        final Yt yt = mock(Yt.class);
        final YtTables tables = mock(YtTables.class);
        when(yt.tables()).thenReturn(tables);
        if (CollectionUtils.isNotEmpty(tableData)) {
            doAnswer(invocation -> {
                final Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
                tableData.forEach(consumer);
                return null;
            }).when(tables).read(any(), any(), any(Consumer.class));

            /* mock iterator call as well for {@link YtHelper#readTableAsStream} method */
            doAnswer(invocation -> {
                final Function<Iterator<YTreeMapNode>, T> callback = invocation.getArgument(2);
                return callback.apply(tableData.iterator());
            }).when(tables).read(any(), any(), any(Function.class));
        }
        return yt;
    }

    @Nonnull
    private static Stream<Arguments> provideArgumentsForBuildTablePath() {
        return Stream.of(
                Arguments.of("path/to/", "table", "path/to/table"),
                Arguments.of("path/to", "table", "path/to/table")
        );
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForBuildTablePath")
    public void buildYtTablePath(String path, String table, String expected) {
        assertEquals(YtUtil.buildYtTablePath(path, table), expected);
    }

    @Test
    public void inferSchemaDefaultString() {
        assertEquals(
                YtUtil.inferSchema(WithInferredSchema.class, true),
                WithInferredSchema.schema(true)
        );
    }

    @Test
    public void inferSchemaDefaultAny() {
        assertEquals(
                YtUtil.inferSchema(WithInferredSchema.class, false),
                WithInferredSchema.schema(false)
        );
    }

    private static class WithInferredSchema {
        private Object unannontatedFieldIgnored;

        @YtColumn
        private Object autoInferredDefault;

        @YtColumn(type = "int64")
        private long explicitTypeInt64 = 1;

        @YtColumn
        private Long autoInferredInt64;

        @YtColumn
        private Integer autoInferredInt32;

        @YtColumn
        private boolean autoInferredBoolean;

        @YtColumn
        private String autoInferredString;

        @YtColumn
        private Double autoInferredDouble;

        @YtColumn
        private float autoInferredFloat;

        @YtColumn(name = "someOverloadedName")
        private String overloadedName;

        static YTreeNode schema(boolean useStringByDefault) {
            return YTree.listBuilder()
                    .beginMap()
                    .key("name").value("autoInferredDefault")
                    .key("type").value(useStringByDefault ? "string" : "any")
                    .endMap()
                    .beginMap()
                    .key("name").value("explicitTypeInt64")
                    .key("type").value("int64")
                    .endMap()
                    .beginMap()
                    .key("name").value("autoInferredInt64")
                    .key("type").value("int64")
                    .endMap()
                    .beginMap()
                    .key("name").value("autoInferredInt32")
                    .key("type").value("int32")
                    .endMap()
                    .beginMap()
                    .key("name").value("autoInferredBoolean")
                    .key("type").value("boolean")
                    .endMap()
                    .beginMap()
                    .key("name").value("autoInferredString")
                    .key("type").value("string")
                    .endMap()
                    .beginMap()
                    .key("name").value("autoInferredDouble")
                    .key("type").value("double")
                    .endMap()
                    .beginMap()
                    .key("name").value("autoInferredFloat")
                    .key("type").value("double")
                    .endMap()
                    .beginMap()
                    .key("name").value("someOverloadedName")
                    .key("type").value("string")
                    .endMap()
                    .buildList();
        }
    }
}
