package ru.yandex.market.yt.util.table;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.impl.common.http.HttpUtils;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.yt.util.table.constants.CompressionCodec;
import ru.yandex.market.yt.util.table.constants.ErasureCodec;
import ru.yandex.market.yt.util.table.constants.Medium;
import ru.yandex.market.yt.util.table.constants.OptimizeFor;
import ru.yandex.market.yt.util.table.model.YtColumnSchema;
import ru.yandex.market.yt.util.table.model.YtTableAttributes;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.market.yt.util.table.model.YtTableSchema;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author gilmulla
 */
@SuppressWarnings("magicnumber")
public class YtTableServiceTest {

    private static final String BASE_PATH = "//home/market/development/mbo/integration_test/yt-utils";
    private static final String HTTP_PROXY = "hahn.yt.yandex.net";
    private static final String TOKEN = "SECRET";

    private YtTableService tableService;
    private Yt yt;
    private YPath tablePath;
    private YtTableModel tableModel;

    @Before
    public void prepare() throws InterruptedException {
        yt = YtUtils.http(
            YtConfiguration.builder()
                .withApiHost(HTTP_PROXY)
                .withToken(TOKEN)
                .build()
        );

        YPath basePath = YPath.simple(BASE_PATH);
        if (!yt.cypress().exists(basePath)) {
            yt.cypress().create(basePath, CypressNodeType.MAP, true);
        }

        // schedule next day remove
        Instant now = Instant.now().minusSeconds(1);
        yt.cypress().set(basePath.attribute("expiration_time"), HttpUtils.YT_INSTANT_FORMATTER.format(now.plus(Duration.ofDays(1))));

        tablePath = basePath.child("testtable_" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
        tableModel = new YtTableModel();
        tableModel.setPath(tablePath.toString());
        tableModel.setShardColumn("model_hash");
        tableModel.setTabletCount(4);
        tableService = new YtTableService();
        tableService.setYt(yt);
        clean();
    }

    @After
    public void clean() throws InterruptedException {
        if (tablePath != null && yt.cypress().exists(tablePath)) {
            yt.tables().unmount(tablePath);
            yt.cypress().remove(tablePath);
        }
    }

    @Test
    public void testYtTableCreation() throws Exception {
        YtTableAttributes attributes = createAttributes();
        tableModel.setAttributes(attributes);

        YtTableSchema schema = createSchema();
        tableModel.setSchema(schema);

        YtTable table = tableService.getTable(tableModel);

        Assert.assertEquals(table.getPath(), tableModel.getPath());

        Assert.assertTrue(yt.cypress().exists(tablePath));
        YTreeNode tableNode = yt.cypress().get(YPath.simple(table.getPath() + "/@"));
        Assert.assertNotNull(tableNode);
        Assert.assertEquals("brotli_8",
            tableNode.asMap().getOptional("compression_codec").get().stringValue());
        Assert.assertEquals("lrc_12_2_2",
            tableNode.asMap().getOptional("erasure_codec").get().stringValue());
        Assert.assertEquals("lookup",
            tableNode.asMap().getOptional("optimize_for").get().stringValue());
    }

    @Test
    public void testYtSsdTable() throws Exception {
        YtTableAttributes attributes = createAttributes();
        attributes.setPrimaryMedium(Medium.SSD_BLOBS);
        tableModel.setAttributes(attributes);

        YtTableSchema schema = createSchema();
        tableModel.setSchema(schema);

        YtTable table = tableService.getTable(tableModel);

        Assert.assertEquals(table.getPath(), tableModel.getPath());

        Assert.assertTrue(yt.cypress().exists(tablePath));
        YTreeNode tableNode = yt.cypress().get(YPath.simple(table.getPath()), Cf.set("primary_medium"));
        Assert.assertNotNull(tableNode);

        String primaryMedium = tableNode.getAttributeOrThrow("primary_medium").stringValue();
        Assert.assertEquals("ssd_blobs", primaryMedium);
    }

    @Test
    public void testMountTable() throws Exception {
        tableModel.setAttributes(createAttributes());
        tableModel.setSchema(createSchema());

        YtTable table = tableService.getTable(tableModel);
        Assert.assertEquals(table.getPath(), tableModel.getPath());
        Assert.assertTrue(yt.cypress().exists(tablePath));

        String state = tableService.getTableState(table);
        Assert.assertEquals("mounted", state);

        tableService.unmountTable(table);
        state = tableService.getTableState(table);
        Assert.assertEquals("unmounted", state);

        tableService.mountTable(table);
        state = tableService.getTableState(table);
        Assert.assertEquals("mounted", state);
    }

    private YtTableAttributes createAttributes() {
        YtTableAttributes attributes = new YtTableAttributes();
        attributes.setCompressionCodec(CompressionCodec.BROTLI_8);
        attributes.setErasureCodec(ErasureCodec.LRC_12_2_2);
        attributes.setOptimizeFor(OptimizeFor.LOOKUP);
        return attributes;
    }

    private YtTableSchema createSchema() {
        YtTableSchema schema = new YtTableSchema();

        YtColumnSchema modelHash = YtColumnSchema.create("model_hash", YtColumnSchema.Type.UINT64);
        modelHash.setSorted(true);
        modelHash.setExpression("farm_hash(category_id, model_id)");

        YtColumnSchema categoryId = YtColumnSchema.create("category_id", YtColumnSchema.Type.UINT64);
        categoryId.setSorted(true);

        YtColumnSchema modelId = YtColumnSchema.create("model_id", YtColumnSchema.Type.UINT64);
        modelId.setSorted(true);

        YtColumnSchema type = YtColumnSchema.create("type", YtColumnSchema.Type.STRING);
        type.setSorted(false);

        YtColumnSchema deleted = YtColumnSchema.create("deleted", YtColumnSchema.Type.BOOLEAN);
        deleted.setSorted(false);

        schema.setColumns(Arrays.asList(modelHash, categoryId, modelId, type, deleted));
        return schema;
    }
}
