package ru.yandex.direct.ytwrapper.model;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class YtTableRowTest {
    private static final YtField<Long> longField = new YtField<>("long", Long.class);
    private static final YtField<String> stringField = new YtField<>("string", String.class);
    private static final YtField<String> anotherField = new YtField<>("stringTwo", String.class);

    private YtTableRow testRow;

    @Before
    public void before() {
        testRow = new YtTableRow(Arrays.asList(longField, stringField));
    }

    @Test
    public void testGetDataEmpty() {
        YTreeMapNode data = testRow.getData();
        assertThat("Данные в пустом ряду создаются", data, notNullValue());
        assertTrue("Данные в пустом ряду пустые", data.asMap().isEmpty());
    }

    @Test
    public void testSetDataTiField() {
        YTreeMapNode data = YTree.mapBuilder()
                .key(longField.getName()).value(1)
                .key(stringField.getName()).value("2")
                .key(YtTableRow.TI_FIELD.getName()).value(5)
                .buildMap();
        // Мы ожидаем что поле TI_FIELD всегда имеет приоритет над TABLE_INDEX_ATTR_NAME и полагаемся на это в другом коде
        data.putAttribute(YtTableRow.TABLE_INDEX_ATTR_NAME, YTree.integerNode(0));
        testRow.setData(data);

        assertThat("Поле Long имеет верное значение", testRow.valueOf(longField), equalTo(1L));
        assertThat("Поле String имеет верное значение", testRow.valueOf(stringField), equalTo("2"));
        assertThat("Индекс имеет верное значение", testRow.getTableIndex(), equalTo(5));
    }

    @Test
    public void testSetDataTiAttr() {
        YTreeMapNode data = YTree.mapBuilder()
                .key(longField.getName()).value(1)
                .key(stringField.getName()).value("2")
                .buildMap();
        data.putAttribute(YtTableRow.TABLE_INDEX_ATTR_NAME, YTree.integerNode(6));
        testRow.setData(data);

        assertThat("Поле Long имеет верное значение", testRow.valueOf(longField), equalTo(1L));
        assertThat("Поле String имеет верное значение", testRow.valueOf(stringField), equalTo("2"));
        assertThat("Индекс имеет верное значение", testRow.getTableIndex(), equalTo(6));
    }

    @Test
    public void testSetDataNoTi() {
        YTreeMapNode data = YTree.mapBuilder()
                .key(longField.getName()).value(1)
                .key(stringField.getName()).value("2")
                .buildMap();
        testRow.setData(data);
        assertThat("Нет индекса - значит нет", testRow.getTableIndex(), nullValue());
    }

    @Test
    public void testSetValue() {
        testRow.setValue(longField, 3L);

        assertThat("Значение установлено верно", testRow.valueOf(longField), equalTo(3L));
    }

    @Test
    public void testValueOfDefault() {
        assertThat("Значение по-умолчанию при отсутствии данных верно", testRow.valueOf(longField, 3L), equalTo(3L));

        testRow.setValue(stringField, "Test");
        assertThat("Значение по-умолчанию при отсутствии значения", testRow.valueOf(longField, 3L), equalTo(3L));
    }

    @Test
    public void testGetStrippedDataEmpty() {
        testRow.setValue(longField, 3L);
        testRow.setValue(stringField, "Text");
        YTreeMapNode data = testRow.getStrippedData(Collections.emptyList());

        assertThat("Получена корректная структура данных",
                data,
                equalTo(
                        YTree.mapBuilder()
                                .key(longField.getName()).value(3L)
                                .key(stringField.getName()).value("Text")
                                .buildMap()));
    }

    @Test
    public void testGetStrippedDataEmptyWithIndex() {
        testRow.setValue(longField, 3L);
        testRow.setValue(stringField, "Text");
        testRow.setTableIndex(1);
        YTreeMapNode data = testRow.getStrippedData(Collections.emptyList());

        assertThat("Получена корректная структура данных",
                data,
                equalTo(
                        YTree.mapBuilder()
                                .key(longField.getName()).value(3L)
                                .key(stringField.getName()).value("Text")
                                .key(YtTableRow.TI_FIELD.getName()).value(1)
                                .buildMap()));
    }

    @Test
    public void testGetStrippedDataListWithIndex() {
        testRow.setValue(longField, 3L);
        testRow.setValue(stringField, "Text");
        testRow.setTableIndex(1);
        YTreeMapNode data = testRow.getStrippedData(Arrays.asList(longField, anotherField));

        assertThat("Получена корректная структура данных",
                data,
                equalTo(
                        YTree.mapBuilder()
                                .key(longField.getName()).value(3L)
                                .key(YtTableRow.TI_FIELD.getName()).value(1)
                                .buildMap()));
    }
}
