package ru.yandex.market.crm.platform.test.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.services.facts.impl.FactSchemaService;
import ru.yandex.market.crm.platform.yt.YtTables;
import ru.yandex.market.crm.platform.yt.YtUtils;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.request.ColumnFilter;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;
import ru.yandex.yt.ytclient.proxy.request.GetNode;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

/**
 * @author apershukov
 */
@Component
public class YtSchemaTestUtils {

    private static final String TABLET_STATE = "tablet_state";

    private final YtClient ytClient;
    private final YtTables ytTables;
    private final FactSchemaService factSchemaService;

    private final Map<YPath, Boolean> createdTables = new ConcurrentHashMap<>();

    public YtSchemaTestUtils(YtClient ytClient, YtTables ytTables, FactSchemaService factSchemaService) {
        this.ytClient = ytClient;
        this.ytTables = ytTables;
        this.factSchemaService = factSchemaService;
    }

    public void prepareDynamicTable(YPath path, Map<String, YTreeNode> attrs) {
        createTable(path, attrs);
        mountTable(path);
    }

    public void prepareFactTable(FactConfig factConfig) {
        YPath factTablePath = ytTables.getFactTable(factConfig.getId());
        if (isPrepared(factTablePath)) {
            return;
        }

        prepareDynamicTable(
                factTablePath,
                factSchemaService.getFactTableAttrs(factConfig)
        );
    }

    public void prepareUserTables() {
        prepareDynamicTable(ytTables.getUsers(), "/yt/schemas/users.yson");
        prepareDynamicTable(ytTables.getUserIds(), "/yt/schemas/user_ids.yson");
    }

    public void prepareTriggersHistoryTable() {
        prepareDynamicTable(ytTables.getTriggersHistory(), "/yt/schemas/triggers_history.yson");
    }

    public void removeCreated() {
        createdTables.keySet().forEach(path -> ytClient.removeNode(path.toString()).join());
        createdTables.clear();
    }

    public boolean isPrepared(YPath path) {
        return createdTables.containsKey(path);
    }

    private void prepareDynamicTable(YPath path, String schemaPath) {
        prepareDynamicTable(
                path,
                YTree.mapBuilder()
                        .key("dynamic").value(true)
                        .key("schema").value(YtUtils.loadSchema(schemaPath).toYTree())
                        .buildMap().asMap()
        );
    }

    private void createTable(YPath path, Map<String, YTreeNode> attrs) {
        ytClient.createNode(
                new CreateNode(path.toString(), ObjectType.Table)
                        .setForce(true)
                        .setRecursive(true)
                        .setAttributes(attrs)
        ).join();

        createdTables.put(path, Boolean.TRUE);
    }

    private void mountTable(YPath path) {
        ytClient.mountTable(path.toString()).join();
        waitMountedState(path);
    }
    private void waitMountedState(YPath path) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 60_000) {
            YTreeNode table = ytClient.getNode(
                    new GetNode(path.toString())
                            .setAttributes(ColumnFilter.of(TABLET_STATE))
            ).join();

            if ("mounted".equals(table.getAttributeOrThrow(TABLET_STATE).stringValue())) {
                return;
            }

            ThreadUtils.sleep(500);
        }

        throw new IllegalStateException("Tablets of '" + path.toString() + "' is not in mounted state");
    }
}
