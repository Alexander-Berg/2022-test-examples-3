package ru.yandex.market.billing.util.yt;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.collections.CollectionUtils;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeIntegerNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class YtUtilTest {

    private YtUtilTest() {
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
    public static YTreeIntegerNode longNode(Object value) {
        return new YTreeIntegerNodeImpl(false, (Long) value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeNode nullableLongNode(@Nullable Object value) {
        return value == null ? nullNode() : longNode(value);
    }

    @Nonnull
    public static YTreeStringNodeImpl stringNode(Object value) {
        return new YTreeStringNodeImpl((String) value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeDoubleNodeImpl floatNode(float value) {
        return new YTreeDoubleNodeImpl(value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeBooleanNodeImpl booleanNode(Object value) {
        return new YTreeBooleanNodeImpl((Boolean) value, new OpenHashMap<>());
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

    public static Yt mockYt(List<YTreeMapNode> tableData) {
        final Yt mockedYt = mock(Yt.class);
        return mockYt(tableData, mockedYt);
    }

    public static Yt mockYt(List<YTreeMapNode> tableData, Yt mockedYt) {
        final YtTables tables = mock(YtTables.class);
        when(mockedYt.tables()).thenReturn(tables);
        if (CollectionUtils.isNotEmpty(tableData)) {
            doAnswer(invocation -> {
                Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
                tableData.forEach(consumer);
                return null;
            }).when(tables).read(any(), any(), any(Consumer.class));

            /* mock iterator call as well for {@link YtHelper#readTableAsStream} method */
            doAnswer(invocation -> {
                Function<Iterator<YTreeMapNode>, Object> callback = invocation.getArgument(2);
                return callback.apply(tableData.iterator());
            }).when(tables).read(any(), any(), any(Function.class));
        }
        return mockedYt;
    }

    /**
     * Метод для формирования ответа аналогичного YT ответу
     * @param data данные
     * @param schema схема YT таблицы
     */
    public static List<YTreeMapNode> toYtNodes(
            List<Map<String, Object>> data,
            Map<String, Function<Object, YTreeNode>> schema
    ) {
        return data.stream()
                .map(dataRow -> YtUtilTest.treeMapNode(
                        schema.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().apply(dataRow.get(entry.getKey()))
                                        )
                                )
                        )
                )
                .collect(Collectors.toList());
    }

}
