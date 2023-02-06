package ru.yandex.market.mbo.core.join.yt;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mbo.yt.utils.YtTablesConstants;
import ru.yandex.market.yt.util.table.model.YtColumnSchema;
import ru.yandex.market.yt.util.table.model.YtTableSchema;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

@RunWith(MockitoJUnitRunner.class)
public class YtJoinServiceTest {

    private static final YPath YT_DIR = YPath.simple("//home/mbo/temp");
    private static final YPath TABLE_1 = YT_DIR.child("table1");
    private static final YPath TABLE_2 = YT_DIR.child("table2");
    private static final YPath RESULT = YT_DIR.child("result");
    private static final String JSON_PATH = "/ru/yandex/market/mbo/join/yt/";
    private static final String TABLE1_JSON = JSON_PATH + "table1.json";
    private static final String TABLE2_JSON = JSON_PATH + "table2.json";
    private static final String RESULT_JSON = JSON_PATH + "result.json";

    private static final String SUPER_POOL = "super_pool";

    private YtJoinService ytJoinService;
    private TestYt testYt;

    private List<YTreeMapNode> resultDataSet;

    @Before
    public void setUp() throws IOException {
        testYt = new TestYt();
        ytJoinService = new YtJoinService(testYt);
        ReflectionTestUtils.setField(ytJoinService, "pool", SUPER_POOL);
        prepareTables();
    }

    @Test
    public void testJoinWithInnerTransaction() {
        ytJoinService.joinTable(Optional.empty(),
            RESULT,
            YT_DIR,
            table2Attrs(),
            "id",
            new TestReducer(),
            TABLE_1, TABLE_2);
        Assert.assertEquals(resultDataSet.size(), testYt.tables().readToList(RESULT, YTableEntryTypes.YSON).size());
    }

    @Test
    public void testJoinWithOuterTransaction() {
        GUID transactionGuid = testYt.transactions().start();
        ytJoinService.joinTable(Optional.of(transactionGuid),
            RESULT,
            YT_DIR,
            table2Attrs(),
            "id",
            new TestReducer(),
            TABLE_1, TABLE_2);
        testYt.transactions().commit(transactionGuid);
        Assert.assertEquals(resultDataSet.size(), testYt.tables().readToList(RESULT, YTableEntryTypes.YSON).size());
    }

    private void prepareTables() throws IOException {
        createTables();
        List<YTreeMapNode> table1DataSet = loadDataSet(TABLE1_JSON);
        List<YTreeMapNode> table2DataSet = loadDataSet(TABLE2_JSON);
        resultDataSet = loadDataSet(RESULT_JSON);
        writeDataSet(TABLE_1, table1DataSet);
        writeDataSet(TABLE_2, table2DataSet);
        writeDataSet(RESULT, resultDataSet);
    }

    private void writeDataSet(YPath table, List<YTreeMapNode> dataSet) {
        testYt.tables().write(table,
            YTableEntryTypes.YSON,
            Cf.wrap(dataSet).iterator());
    }

    private List<YTreeMapNode> loadDataSet(String path) throws IOException {
        InputStream is = new ClassPathResource(path)
            .getInputStream();

        ObjectMapper om = new ObjectMapper();
        JavaType map = om.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        JavaType javaType = om.getTypeFactory().constructCollectionType(List.class, map);
        List<Map<String, String>> loadedData = om.readValue(is, javaType);
        return convert(loadedData);
    }

    private List<YTreeMapNode> convert(List<Map<String, String>> data) {
        return data.stream()
            .map(this::convert)
            .collect(Collectors.toList());
    }

    private YTreeMapNode convert(Map<String, String> data) {
        YTreeBuilder builder = YTree.mapBuilder();
        data.forEach((key, value) -> builder.key(key).value(value));
        return builder.buildMap();
    }

    private void createTables() {
        testYt.cypress().create(createRequest(TABLE_1, table1Attrs()));
        testYt.cypress().create(createRequest(TABLE_2, table2Attrs()));
        testYt.cypress().create(createRequest(RESULT, table2Attrs()));

    }

    private CreateNode createRequest(YPath path, MapF<String, YTreeNode> tableAttrs) {
        CreateNode request = new CreateNode(path, CypressNodeType.TABLE, tableAttrs);
        request.setRecursive(true);
        return request;
    }

    private MapF<String, YTreeNode> table1Attrs() {
        return Cf.map(
            YtTablesConstants.OPTIMIZE_FOR, YTree.stringNode("scan"),
            YtTablesConstants.SCHEMA, YtTableSchema.create(
                YtColumnSchema.create("id", YtColumnSchema.Type.STRING)
            ).toYTreeNode()
        );
    }

    private MapF<String, YTreeNode> table2Attrs() {
        return Cf.map(
            YtTablesConstants.OPTIMIZE_FOR, YTree.stringNode("scan"),
            YtTablesConstants.SCHEMA, YtTableSchema.create(
                YtColumnSchema.create("id", YtColumnSchema.Type.STRING),
                YtColumnSchema.create("super_column", YtColumnSchema.Type.STRING)
            ).toYTreeNode()
        );
    }
}
