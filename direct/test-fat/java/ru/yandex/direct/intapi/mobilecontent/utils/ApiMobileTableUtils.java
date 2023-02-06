package ru.yandex.direct.intapi.mobilecontent.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import one.util.streamex.StreamEx;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.direct.common.mobilecontent.MobileContentYtTablesConfig;
import ru.yandex.direct.intapi.utils.TablesUtils;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static ru.yandex.inside.yt.kosher.tables.YTableEntryTypes.YSON;

public class ApiMobileTableUtils {
    private final YtProvider ytProvider;
    private final MobileContentYtTablesConfig mobileContentYtTables;

    public ApiMobileTableUtils(YtProvider ytProvider, MobileContentYtTablesConfig mobileContentYtTables) {
        this.ytProvider = ytProvider;
        this.mobileContentYtTables = mobileContentYtTables;
    }

    public void createMobileContentTable(String storeName, YtCluster ytCluster,
                                         Collection<ApiMobileContentYTRecord> records) {
        var ytOperator = ytProvider.getOperator(ytCluster);
        var tablePath = mobileContentYtTables.getShopInfo(storeName)
                .orElseThrow(() -> new IllegalStateException("Incorrect store name: " + storeName))
                .getTable();
        var tableYPath = YPath.simple(tablePath);

        TablesUtils.createTable(ytOperator, Optional.empty(), tableYPath, ApiMobileContentYTRecord.YT_COLUMNS);
        List<YTreeMapNode> ytRecords = StreamEx.of(records).map(ApiMobileContentYTRecord::buildMapNode).toList();
        ytOperator.getYt().tables().insertRows(tableYPath, true, false, true, YSON, Cf.wrap(ytRecords).iterator());
    }
}
