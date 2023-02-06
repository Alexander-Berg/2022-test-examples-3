package ru.yandex.market.crm.campaign.test.utils;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.services.pluggabletable.PluggableTableService;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

/**
 * @author apershukov
 */
@Component
public class PluggableTablesTestHelper implements StatefulHelper {

    public static YTreeMapNode pluggedTableRow(String id) {
        return pluggedTableRow(id, "100500");
    }

    public static YTreeMapNode pluggedTableRow(String id, String savedMoney) {
        return YTree.mapBuilder()
                .key("id").value(id)
                .key("saved_money").value(savedMoney)
                .buildMap();
    }

    private static final YPath PLUGGABLE_TABLES_PATH = YPath.cypressRoot().child("plugged_tables");

    private final PluggableTableService pluggableTableService;
    private final YtClient ytClient;

    private volatile boolean tableCreated;

    public PluggableTablesTestHelper(PluggableTableService pluggableTableService, YtClient ytClient) {
        this.pluggableTableService = pluggableTableService;
        this.ytClient = ytClient;
    }

    public PluggableTable preparePluggableTable() {
        return preparePluggableTable(UidType.PUID);
    }

    public PluggableTable preparePluggableTable(UidType idType, YTreeMapNode... rows) {
        TableSchema schema = new TableSchema.Builder()
            .setUniqueKeys(false)
            .addValue("id", ColumnValueType.STRING)
            .addValue("saved_money", ColumnValueType.STRING)
            .build();

        return preparePluggableTable(idType, schema, rows);
    }

    public PluggableTable preparePluggableTable(UidType idType, TableSchema schema, YTreeMapNode... rows) {
        YPath path = PLUGGABLE_TABLES_PATH.child(UUID.randomUUID().toString());

        ytClient.createTableWithAttrs(path, Cf.map("schema", schema.toYTree()));
        tableCreated = true;

        if (rows.length > 0) {
            ytClient.write(path, Arrays.asList(rows));
        }

        PluggableTable pluggableTable = pluggableTableService.preloadSchema(path.toString())
                .setName("Plugged Table")
                .setUidColumn("id")
                .setUidType(idType);

        return pluggableTableService.save(pluggableTable);
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
        if (tableCreated) {
            ytClient.remove(PLUGGABLE_TABLES_PATH);
        }
    }
}
