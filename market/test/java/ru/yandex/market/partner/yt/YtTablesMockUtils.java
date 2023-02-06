package ru.yandex.market.partner.yt;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.common.YtErrorMapping;
import ru.yandex.inside.yt.kosher.impl.common.YtException;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeDoubleNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YtTablesMockUtils {
    public static final String HAHN = "hahn.yt.yandex.net";
    public static final String ARNOLD = "arnold.yt.yandex.net";

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static Yt mockYt(Class fromSource, String resourceName, String value) throws IOException {
        InputStream is = fromSource.getResourceAsStream(resourceName);
        Map mapJson = objectMapper.readValue(is, Map.class);
        List<Map<String, Object>> values = (List<Map<String, Object>>) mapJson.get(value);
        List<YTreeMapNode> nodes = values.stream().map(YtTablesMockUtils::createMap).collect(Collectors.toList());
        final Yt yt = mock(Yt.class);
        final YtTables tables = mock(YtTables.class);
        when(yt.tables()).thenReturn(tables);
        if (CollectionUtils.isNotEmpty(nodes)) {
            doAnswer(invocation -> {
                final Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
                nodes.forEach(consumer);
                return null;
            }).when(tables).read(any(), any(), any(Consumer.class));
        }
        return yt;
    }

    public static Yt mockYtThrowErr() {
        final Yt yt = mock(Yt.class);
        when(yt.tables()).thenThrow(YtException.class);
        return yt;
    }

    public static Yt mockYtTableNotFound() {
        final Yt yt = mock(Yt.class);
        when(yt.tables()).thenThrow(YtErrorMapping.ResolveError.class);
        return yt;
    }

    public static Yt mockEmtryResult() {
        final Yt yt = mock(Yt.class);
        final YtTables tables = mock(YtTables.class);
        when(yt.tables()).thenReturn(tables);
        return yt;
    }

    private static YTreeMapNode createMap(Map map) {
        return (YTreeMapNode) create(map);
    }

    public static YTreeNode create(Object object) {
        if (object == null) {
            return new YTreeEntityNodeImpl(null);
        }
        if (object instanceof Boolean) {
            return new YTreeBooleanNodeImpl((Boolean) object, null);
        }
        if (object instanceof Double) {
            return new YTreeDoubleNodeImpl((Double) object, null);
        }
        if (object instanceof Long || object instanceof Integer) {
            return new YTreeIntegerNodeImpl(true, ((Number) object).longValue(), null);
        }
        if (object instanceof String) {
            return new YTreeStringNodeImpl((String) object, null);
        }
        if (object instanceof List) {
            List list = (List) object;
            YTreeListNodeImpl node = new YTreeListNodeImpl(null);
            list.forEach(v -> node.add(create(v)));
            return node;
        }
        if (object instanceof Map) {
            Map map = (Map) object;
            YTreeMapNodeImpl res = new YTreeMapNodeImpl(null);
            map.forEach((key, value) -> res.put(key.toString(), create(value)));
            return res;
        }
        throw new RuntimeException(object.getClass().getSimpleName() + " not supported");
    }
}
