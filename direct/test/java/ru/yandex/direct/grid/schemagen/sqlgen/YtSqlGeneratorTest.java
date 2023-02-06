package ru.yandex.direct.grid.schemagen.sqlgen;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig;
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfigMockFactory;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class YtSqlGeneratorTest {
    @Mock
    private YtProvider ytProvider;

    @Mock
    private YtOperator ytOperator;

    private YtSqlGenerator ytSqlGenerator;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(ytOperator)
                .when(ytProvider).getOperator(any());

        DirectYtDynamicConfig dynamicConfig = DirectYtDynamicConfigMockFactory.createConfigMock();
        when(dynamicConfig.getClusters())
                .thenReturn(Collections.singletonList(YtCluster.HAHN));

        ytSqlGenerator = new YtSqlGenerator(ytProvider, dynamicConfig, null);
    }

    private YTreeMapNode mapNode(String name, String type) {
        return YTree.mapBuilder()
                .key("name").value(name)
                .key("type").value(type)
                .endMap()
                .build()
                .mapNode();
    }

    @Test
    public void testGetSchemas() {
        String tableOne = "//tmp/table_one";
        doReturn(Arrays.asList(mapNode("cid", "uint64"), mapNode("name", "string")))
                .when(ytOperator).getSchema(eq(new YtTable(tableOne)));
        String tableTwo = "//tmp/table_two";
        doReturn(Arrays.asList(mapNode("pid", "int64"), mapNode("active", "boolean")))
                .when(ytOperator).getSchema(eq(new YtTable(tableTwo)));

        Map<String, String> schemas =
                ytSqlGenerator.getSchemas("ut", Arrays.asList("//tmp/table_one", "//tmp/table_two"));

        assertThat(schemas)
                .containsOnly(
                        entry("table_one_ut", "CREATE TABLE table_one_ut (\n"
                                + "  cid bigint(20) unsigned,\n"
                                + "  name text\n"
                                + ");"),
                        entry("table_two_ut", "CREATE TABLE table_two_ut (\n"
                                + "  pid bigint(20),\n"
                                + "  active tinyint(1)\n"
                                + ");")
                );
    }
}
