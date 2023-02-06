package ru.yandex.direct.ytwrapper.model;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.ytwrapper.model.YtTableRow.TABLE_INDEX_ATTR_NAME;
import static ru.yandex.direct.ytwrapper.model.YtTableRow.TI_FIELD;


public class YtTableIndexMapperTest {
    private static final YtField<Long> longField = new YtField<>("long", Long.class);
    private static final YtField<String> stringField = new YtField<>("string", String.class);

    @Mock
    private Yield<YTreeMapNode> yield;

    @Captor
    private ArgumentCaptor<YTreeMapNode> captor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMap() {
        YtMapper mapper = new YtTableIndexMapper();
        YTreeMapNode node = YTree.mapBuilder()
                .key(longField.getName()).value(123L)
                .key(stringField.getName()).value("Test")
                .buildMap();
        node.putAttribute(TABLE_INDEX_ATTR_NAME, YTree.integerNode(1));
        mapper.map(node, yield, null);

        verify(yield).yield(eq(0), captor.capture());

        YTreeMapNode result = captor.getValue();
        assertThat("Поле Long имеет верное значение", longField.extractValue(result), equalTo(123L));
        assertThat("Поле String имеет верное значение", stringField.extractValue(result), equalTo("Test"));
        assertThat("Поле Индекса имеет верное значение", TI_FIELD.extractValue(result), equalTo(1));
        assertFalse("Аттрибут индекса не задан", result.containsAttribute(TABLE_INDEX_ATTR_NAME));
    }
}
