package ru.yandex.market.crm.platform.api.test.kv;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.platform.common.UidTypes;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.platform.yt.KvStorageClient;
import ru.yandex.market.crm.platform.yt.YtTable;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.tables.ColumnSchema;

/**
 * @author apershukov
 */
public class MappingTableSetter {

    private final KvStorageClient client;
    private final YtTable mappingTable;
    private final YtSchemaTestUtils schemaTestUtils;

    public MappingTableSetter(YtTables tables, KvStorageClient client, YtSchemaTestUtils schemaTestUtils) {
        this.client = client;
        this.mappingTable = tables.getIdsMappingTable();
        this.schemaTestUtils = schemaTestUtils;
    }

    public void addSberToYandex(String id, YandexIds yandexIds) {
        addRow(
                UidType.SBER_ID,
                id,
                YTree.mapBuilder()
                        .key("puids").value(
                                yandexIds.getPuids().stream()
                                        .map(String::valueOf)
                                        .collect(Collectors.toList())
                        )
                        .key("yandexuids").value(yandexIds.getYandexuids())
                        .key("uuids").value(yandexIds.getUuids())
                        .key("mm_device_ids").value(yandexIds.getDeviceIds())
                        .buildMap()
        );
    }

    public void addYandexToSber(UidType idType, String id, Set<String> sberIds) {
        addRow(
                idType,
                id,
                YTree.mapBuilder()
                        .key("sber_ids").value(sberIds)
                        .buildMap()
        );
    }

    private void addRow(UidType idType, String id, YTreeNode node) {
        ensureTablePrepared();

        ModifyRowsRequest request = new ModifyRowsRequest(mappingTable.getPath().toString(), mappingTable.getSchema())
                .addInsert(
                        ImmutableMap.of(
                                "id", id,
                                "id_type", UidTypes.value(idType),
                                "value", node
                        )
                );

        client.doInTx(tx -> tx.modifyRows(request)).join();
    }

    private void ensureTablePrepared() {
        if (schemaTestUtils.isPrepared(mappingTable.getPath())) {
            return;
        }

        Map<String, YTreeNode> attrs = YTree.builder()
                .beginMap()
                .key("dynamic").value(true)
                .key("schema")
                .beginAttributes()
                    .key("unique_keys").value(true)
                    .key("strict").value(true)
                .endAttributes()
                .value(
                        mappingTable.getSchema().getColumns().stream()
                                .map(ColumnSchema::toYTree)
                                .collect(Collectors.toList())
                )
                .endMap()
                .build()
                .asMap();

        schemaTestUtils.prepareDynamicTable(mappingTable.getPath(), attrs);
    }
}
