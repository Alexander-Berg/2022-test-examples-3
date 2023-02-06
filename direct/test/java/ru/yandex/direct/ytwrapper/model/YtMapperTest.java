package ru.yandex.direct.ytwrapper.model;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class YtMapperTest {
    private static final YtField<Long> longField = new YtField<>("long", Long.class);
    private static final YtField<String> stringField = new YtField<>("string", String.class);

    @ParametersAreNonnullByDefault
    private static class TestMapper extends YtMapper {
        private YtTableRow tableRow;

        @Override
        public void map(YtTableRow tableRow) {
            this.tableRow = tableRow;
        }
    }

    @Test
    public void testMap() {
        TestMapper mapper = new TestMapper();
        YTreeMapNode node = YTree.mapBuilder()
                .key(longField.getName()).value(123L)
                .key(stringField.getName()).value("Test")
                .buildMap();
        node.putAttribute(YtTableRow.TABLE_INDEX_ATTR_NAME, YTree.integerNode(1));
        mapper.map(node, null, null);

        assertThat("Поле Long имеет верное значение", mapper.tableRow.valueOf(longField), equalTo(123L));
        assertThat("Поле String имеет верное значение", mapper.tableRow.valueOf(stringField), equalTo("Test"));
        assertThat("Индекс имеет верное значение", mapper.tableRow.getTableIndex(), equalTo(1));

        YTreeMapNode nodeTwo = YTree.mapBuilder()
                .key(longField.getName()).value(321L)
                .key(stringField.getName()).value("Another test")
                .buildMap();
        nodeTwo.putAttribute(YtTableRow.TABLE_INDEX_ATTR_NAME, YTree.integerNode(3));
        mapper.map(nodeTwo, null, null);

        assertThat("Поле Long имеет верное значение", mapper.tableRow.valueOf(longField), equalTo(321L));
        assertThat("Поле String имеет верное значение", mapper.tableRow.valueOf(stringField), equalTo("Another test"));
        assertThat("Индекс имеет верное значение", mapper.tableRow.getTableIndex(), equalTo(3));
    }
}
