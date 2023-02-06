package ru.yandex.direct.ytwrapper.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class YtReducerTest {
    private static final YtField<Long> longField = new YtField<>("long", Long.class);
    private static final YtField<String> stringField = new YtField<>("string", String.class);

    @ParametersAreNonnullByDefault
    private static class TestReducer extends YtReducer<Integer> {
        private Integer keyValue;
        private List<YtTableRow> rows;

        @Override
        public Integer key(YtTableRow row) {
            keyValue = row.getTableIndex();
            return row.getTableIndex();
        }

        @Override
        public void reduce(Integer i, Iterator<YtTableRow> entries) {
            rows = new ArrayList<>();
            while (entries.hasNext()) {
                rows.add(entries.next());
            }
        }
    }

    @Test
    public void testReducer() {
        YTreeMapNode node = YTree.mapBuilder()
                .key(longField.getName()).value(123L)
                .key(stringField.getName()).value("Test")
                .key(YtTableRow.TI_FIELD.getName()).value(1)
                .buildMap();
        node.putAttribute(YtTableRow.TABLE_INDEX_ATTR_NAME, YTree.integerNode(0));

        TestReducer reducer = new TestReducer();
        reducer.reduce(Collections.singletonList(node).iterator(), null, null, null);

        assertThat("Ключ получен корректно", reducer.keyValue, equalTo(1));
        assertThat("Получено ожидаемое количество значений", reducer.rows.size(), equalTo(1));
        assertThat("Поле Long имеет верное значение", reducer.rows.get(0).valueOf(longField), equalTo(123L));
        assertThat("Поле String имеет верное значение", reducer.rows.get(0).valueOf(stringField), equalTo("Test"));
    }
}
