package ru.yandex.market.mbo.cardrender.app.service.reduce;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.DATA;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.DELETED_DATE;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.EXPORT_TS;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.MODEL_ID;

/**
 * @author apluhin
 * @created 6/4/21
 */
public class ModelReducerTest {

    private ModelReducer reducer;
    private Instant now = Instant.now();

    @Before
    public void setUp() throws Exception {
        reducer = new ModelReducer(now.toEpochMilli());
    }

    @Test
    public void importNewExportModel() {
        YTreeMapNode exportModel = buildExportModel(now.toEpochMilli());

        StubYield stubYield = new StubYield();
        reducer.reduce(1L, Cf.wrap(List.of(exportModel)).iterator(), stubYield, null);

        assertEquals(stubYield.nodes.size(), 1);
    }

    @Test
    public void intersectionModelCheckByExportTs() {
        YTreeMapNode exportModel = buildExportModel(now.toEpochMilli());
        Instant lbModelExportTs = now.plusSeconds(60);
        YTreeMapNode tableModel = buildTableModel(lbModelExportTs.toEpochMilli());

        StubYield stubYield = new StubYield();
        reducer.reduce(1L, Cf.wrap(List.of(exportModel, tableModel)).iterator(), stubYield, null);

        assertEquals(1, stubYield.nodes.size());
        assertEquals(lbModelExportTs.toEpochMilli(), stubYield.nodes.get(0).get("$value:export_ts").get().longValue());
    }

    @Test
    public void testSkipBySessionTime() {
        Instant lbModelExportTs = now;
        YTreeMapNode tableModel = buildTableModel(lbModelExportTs.toEpochMilli());

        StubYield stubYield = new StubYield();
        new ModelReducer(now.plusSeconds(60).toEpochMilli())
                .reduce(1L, Cf.wrap(List.of(tableModel)).iterator(), stubYield, null);

        assertEquals(0, stubYield.nodes.size());
    }

    @Test
    public void testImportNewModelFromLb() {
        Instant lbModelExportTs = now;
        YTreeMapNode tableModel = buildTableModel(lbModelExportTs.toEpochMilli());

        StubYield stubYield = new StubYield();
        new ModelReducer(now.minusSeconds(60).toEpochMilli())
                .reduce(1L, Cf.wrap(List.of(tableModel)).iterator(), stubYield, null);

        assertEquals(1, stubYield.nodes.size());
    }

    @Test
    public void testIgnoreDeletedModel() {
        YTreeMapNode exportModel = buildExportModel(now.toEpochMilli());
        Instant lbModelExportTs = now.plusSeconds(60);
        YTreeMapNode tableModel = buildTableModel(lbModelExportTs.toEpochMilli());
        tableModel.put(DELETED_DATE, YTree.stringNode(Instant.now().toString()));

        StubYield stubYield = new StubYield();
        reducer.reduce(1L, Cf.wrap(List.of(exportModel, tableModel)).iterator(), stubYield, null);

        assertEquals(0, stubYield.nodes.size());
    }

    @Test
    public void name() {
        System.out.println(new AtomicReference<Long>().get());
    }

    private YTreeMapNode buildTableModel(Long exportTs) {
        return YTree.mapBuilder()
                .key(MODEL_ID).value(1)
                .key(EXPORT_TS).value(exportTs)
                .key(DATA).value(ModelStorage.Model.newBuilder().setExportTs(exportTs).build().toByteArray())
                .buildMap();
    }

    private YTreeMapNode buildExportModel(Long exportTs) {
        return YTree.mapBuilder()
                .key(MODEL_ID).value(1)
                .key(DATA).value(ModelStorage.Model.newBuilder().setExportTs(exportTs).build().toByteArray())
                .buildMap();
    }

    private class StubYield implements Yield<YTreeMapNode> {

        private List<YTreeMapNode> nodes = new ArrayList<>();

        @Override
        public void yield(int index, YTreeMapNode value) {
            nodes.add(value);
        }

        @Override
        public void close() throws IOException {

        }

        public List<YTreeMapNode> addedNodes() {
            return nodes;
        }
    }

}
