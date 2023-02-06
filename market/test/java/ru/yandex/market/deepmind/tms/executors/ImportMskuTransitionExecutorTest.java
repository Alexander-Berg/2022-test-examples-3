package ru.yandex.market.deepmind.tms.executors;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.stubs.YtTablesStub;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.deepmind.common.utils.YqlOverPgUtils;
import ru.yandex.market.mboc.common.config.YtAutoCluster;

import static ru.yandex.market.deepmind.tms.executors.ImportMskuTransitionExecutor.TransitionKey;

public class ImportMskuTransitionExecutorTest extends DeepmindBaseDbTestClass {

    @Resource(name = TestYqlOverPgDatasourceConfig.YQL_OVER_PG_TEMPLATE)
    private JdbcTemplate yqlJdbcTemplate;
    @Resource
    private JdbcTemplate jdbcTemplate;

    private ImportMskuTransitionExecutor executor;

    @Before
    public void setUp() {
        YqlOverPgUtils.setTransformYqlToSql(true);

        yqlJdbcTemplate.execute("" +
            "CREATE TABLE temp_yt_msku_transition(" +
            "   old_entity_id bigint, " +
            "   new_entity_id bigint, " +
            "   entity_type text" +
            ")"
        );

        YtAutoCluster ytAutoClusterMock = Mockito.mock(YtAutoCluster.class);
        Yt ytMock = Mockito.mock(Yt.class);
        Mockito.when(ytAutoClusterMock.get()).thenReturn(ytMock);
        var ytTablesStub = Mockito.mock(YtTablesStub.class);
        Mockito.when(ytMock.tables()).thenReturn(ytTablesStub);
        Mockito.doAnswer(invocation -> {
            var consumer = (Consumer<YTreeMapNode>) invocation.getArgument(2);
            String yql = "" +
                "select " +
                "    old_entity_id, " +
                "    new_entity_id, " +
                "    entity_type " +
                "from `temp_yt_msku_transition`";

            yqlJdbcTemplate.query(yql, rs -> {
                var node = YTree.mapBuilder()
                    .key("old_entity_id").value(rs.getLong("old_entity_id"))
                    .key("new_entity_id").value(rs.getInt("new_entity_id"))
                    .key("entity_type").value(rs.getString("entity_type"))
                    .buildMap();
                consumer.accept(node);
            });
            return null;
        }).when(ytTablesStub).read(Mockito.any(), Mockito.any(), Mockito.any(Consumer.class));

        executor = new ImportMskuTransitionExecutor(ytAutoClusterMock, jdbcTemplate,
            YPath.simple("//temp_yt_msku_transition"));
        executor.setBatchSize(2);
    }

    @After
    public void tearDown() {
        YqlOverPgUtils.setTransformYqlToSql(false);
    }

    @Test
    public void testImport() {
        insertToYt(111, 11);
        insertToYt(221, 22);
        insertToYt(222, 22);

        executor.execute();

        Assertions.assertThat(selectFromPG()).containsExactly(
            new TransitionKey(111, 11),
            new TransitionKey(221, 22),
            new TransitionKey(222, 22)
        );
    }

    private void insertToYt(long oldId, long newId) {
        yqlJdbcTemplate.update("INSERT INTO temp_yt_msku_transition VALUES (?, ?, ?)", oldId, newId, "SKU");
    }

    private List<TransitionKey> selectFromPG() {
        return jdbcTemplate.query("SELECT old_msku_id, new_msku_id FROM msku.msku_transition",
            (row, i) -> new TransitionKey(row.getLong("old_msku_id"), row.getLong("new_msku_id")));
    }

}
