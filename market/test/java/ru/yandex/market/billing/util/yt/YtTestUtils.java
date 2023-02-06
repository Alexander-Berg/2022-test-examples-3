package ru.yandex.market.billing.util.yt;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Утильный класс для тестирования взаимодействия с YT таблицами.
 */
@SuppressWarnings("checkstyle:all")
public class YtTestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        }
        return yt;
    }

    public static List<YTreeMapNode> readYtData(InputStream resource, Function<JsonNode, YTreeMapNode> converter) {
        JsonNode jsonRoot;
        try {
            jsonRoot = objectMapper.readTree(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return getRows(jsonRoot, converter);
    }

    private static List<YTreeMapNode> getRows(JsonNode tableContentsRoot, Function<JsonNode, YTreeMapNode> rowConverter) {
        List<YTreeMapNode> dataset = new ArrayList<>();
        Iterator<JsonNode> elements = tableContentsRoot.elements();

        while (elements.hasNext()) {
            JsonNode jsonNode = elements.next();
            dataset.add(rowConverter.apply(jsonNode));
        }

        return dataset;
    }

    public static Map<String, List<YTreeMapNode>> readMultipleYtTables(
            InputStream resource,
            Function<JsonNode, YTreeMapNode> rowConverter
    ) {
        JsonNode jsonRoot;
        try {
            jsonRoot = objectMapper.readTree(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, List<YTreeMapNode>> resultTables = new HashMap<>();
        Iterator<JsonNode> elements = jsonRoot.elements();

        while (elements.hasNext()) {
            JsonNode tableNode = elements.next();
            resultTables.put(
                    tableNode.get("table").asText(),
                    getRows(tableNode.get("rows"), rowConverter)
            );
        }

        return resultTables;
    }

    @Nonnull
    public static YTreeMapNode treeMapNode(final Map<String, YTreeNode> attributes) {
        final YTreeMapNodeImpl entries = new YTreeMapNodeImpl(null);
        attributes.forEach(entries::put);
        return entries;
    }

    @Nonnull
    public static YTreeIntegerNodeImpl intNode(long value) {
        return new YTreeIntegerNodeImpl(false, value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeStringNodeImpl stringNode(String value) {
        return new YTreeStringNodeImpl(value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeDoubleNodeImpl doubleNode(double value) {
        return new YTreeDoubleNodeImpl(value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeBooleanNodeImpl booleanNode(boolean value) {
        return new YTreeBooleanNodeImpl(value, new OpenHashMap<>());
    }

    @Nonnull
    public static YTreeEntityNodeImpl nullNode() {
        return new YTreeEntityNodeImpl(null);
    }
}
