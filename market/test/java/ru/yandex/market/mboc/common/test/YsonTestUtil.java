package ru.yandex.market.mboc.common.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;

/**
 * @author yuramalinov
 * @created 26.06.18
 */
public class YsonTestUtil {
    private YsonTestUtil() {
    }

    public static YTreeListNode readJsonAsYson(Object test, String resourceName) {
        List<Map<String, Object>> values;
        try {
            values = new ObjectMapper().readValue(test.getClass().getClassLoader().getResourceAsStream(resourceName),
                new TypeReference<List<Map<String, Object>>>() {
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        YTreeBuilder builder = YTree.listBuilder();
        values.forEach(map -> {
            YTreeBuilder mapBuilder = YTree.mapBuilder();
            map.forEach((key, value) -> {
                if (value == null) {
                    mapBuilder.key(key).value(YTree.builder().entity().build());
                } else {
                    mapBuilder.key(key).value(value);
                }
            });
            builder.value(mapBuilder.buildMap());
        });

        return builder.buildList();
    }
}
