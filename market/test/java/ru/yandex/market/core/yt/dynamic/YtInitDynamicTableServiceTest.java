package ru.yandex.market.core.yt.dynamic;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeBooleanNode;
import ru.yandex.market.mbi.yt.YtCluster;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

/**
 * Тесты для {@link YtInitDynamicTableService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class YtInitDynamicTableServiceTest {

    private static final String YT_FOLDER_PATH = "//tmp/mbi";
    private static final String YT_TABLE_PATH = YT_FOLDER_PATH + "/table_name";

    private YtInitDynamicTableService ytInitDynamicTableService;

    private Yt mockedYt;
    private Cypress mockedCypress;
    private YtTables mockedYtTables;

    @BeforeEach
    void init() {
        mockedYt = Mockito.mock(Yt.class);
        YtCluster ytCluster = new YtCluster("arnold", mockedYt);

        mockedCypress = Mockito.mock(Cypress.class);
        Mockito.doReturn(mockedCypress).when(mockedYt).cypress();

        mockedYtTables = Mockito.mock(YtTables.class);
        Mockito.doReturn(mockedYtTables).when(mockedYt).tables();

        ytInitDynamicTableService = new YtInitDynamicTableService(ytCluster,
                EmptyMap.INSTANCE, EmptyMap.INSTANCE, List.of(), YT_FOLDER_PATH, YT_TABLE_PATH);
    }

    @Test
    @DisplayName("Проверка инициализации аттрибутов таблицы")
    void testAttrInitialisations() {
        Mockito.doReturn(YTree.stringNode("mounted"))
                .when(mockedCypress)
                .get(eq(YPath.simple(YT_TABLE_PATH + "/@tablet_state")));

        ytInitDynamicTableService.initialize();

        ArgumentCaptor<YPath> attrPathCaptor = ArgumentCaptor.forClass(YPath.class);
        ArgumentCaptor<YTreeBooleanNode> attrValueCaptor = ArgumentCaptor.forClass(YTreeBooleanNode.class);

        Mockito.verify(mockedCypress, times(1)).set(attrPathCaptor.capture(), attrValueCaptor.capture());

        YPath actualAttrPath = attrPathCaptor.getValue();
        String expectedAttrPath = YT_TABLE_PATH + "/@replicated_table_options/enable_replicated_table_tracker";
        Assertions.assertEquals(expectedAttrPath, actualAttrPath.toString());

        YTreeBooleanNode actualAttrValue = attrValueCaptor.getValue();
        Assertions.assertTrue(actualAttrValue.getValue());
    }
}
