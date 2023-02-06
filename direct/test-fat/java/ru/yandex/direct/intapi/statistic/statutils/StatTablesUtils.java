package ru.yandex.direct.intapi.statistic.statutils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import one.util.streamex.StreamEx;
import org.jooq.Table;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.direct.ytcomponents.config.OverridableTableMappings;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static ru.yandex.direct.grid.schema.yt.Tables.CAESARORDERINFO_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.ORDERSTATDAY_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.ORDERSTATFRAUD_BS;
import static ru.yandex.direct.grid.schema.yt.Tables.TAXHISTORY_BS;
import static ru.yandex.direct.intapi.utils.TablesUtils.createTable;
import static ru.yandex.direct.ytwrapper.YtPathUtil.TEMPORARY_NODE_NAME;
import static ru.yandex.direct.ytwrapper.YtPathUtil.generatePath;
import static ru.yandex.inside.yt.kosher.tables.YTableEntryTypes.YSON;

public class StatTablesUtils {

    private final YtProvider ytProvider;
    private final OverridableTableMappings tableMappings;

    public StatTablesUtils(YtProvider ytProvider, OverridableTableMappings tableMappings) {
        this.ytProvider = ytProvider;
        this.tableMappings = tableMappings;
    }

    private YPath getPath(Table table) {
        return YPath.simple(tableMappings.getTableMappings().get(table));
    }

    /**
     * Задать в маппингах хранение таблицы во временной директории по пути {@code //tmp/prefix/tableName}
     */
    public void bindTableToTmp(Table table, String prefix) {
        String newPath = generatePath("//" + TEMPORARY_NODE_NAME, prefix, table.getName());
        tableMappings.addOverride(table, newPath);
    }

    /**
     * Создаёт и заполняет данными таблицу для хранения значений НДС
     */
    public void createTaxHistoryTable(YtCluster ytCluster, Collection<TaxHistoryYTRecord> records) {
        YtOperator ytOperator = ytProvider.getOperator(ytCluster);
        YPath tablePath = getPath(TAXHISTORY_BS);

        createTable(ytOperator, Optional.empty(), tablePath, TaxHistoryYTRecord.YT_COLUMNS);
        List<YTreeMapNode> ytRecords = StreamEx.of(records).map(TaxHistoryYTRecord::buildMapNode).toList();
        ytOperator.getYt().tables().insertRows(tablePath, true, false, true, YSON, Cf.wrap(ytRecords).iterator());
    }

    /**
     * Создаёт таблицу для хранения данных по заказам и заполняет её данными из records
     */
    public void createOrderInfoTable(YtCluster ytCluster, Collection<OrderInfoYTRecord> records) {
        YtOperator ytOperator = ytProvider.getOperator(ytCluster);
        YPath tablePath = getPath(CAESARORDERINFO_BS);

        createTable(ytOperator, Optional.empty(), tablePath, OrderInfoYTRecord.YT_COLUMNS);
        List<YTreeMapNode> ytRecords = StreamEx.of(records).map(OrderInfoYTRecord::buildMapNode).toList();
        ytOperator.getYt().tables().insertRows(tablePath, true, false, true, YSON, Cf.wrap(ytRecords).iterator());
    }

    /**
     * Создаёт таблицу для хранения статистики и заполняет её данными из records
     */
    public void createOrderStatDayTable(YtCluster ytCluster, Collection<OrderStatDayYTRecord> records) {
        YtOperator ytOperator = ytProvider.getOperator(ytCluster);
        YPath tablePath = getPath(ORDERSTATDAY_BS);

        createTable(ytOperator, Optional.empty(), tablePath, OrderStatDayYTRecord.YT_COLUMNS);
        List<YTreeMapNode> ytRecords = StreamEx.of(records).map(OrderStatDayYTRecord::buildMapNode).toList();
        ytOperator.getYt().tables().insertRows(tablePath, true, false, true, YSON, Cf.wrap(ytRecords).iterator());
    }

    /**
     * Создаёт и заполняет таблицу для хранения статистики по фрод-кликам
     */
    public void createOrderStatFraudTable(YtCluster ytCluster, Collection<OrderStatFraudYTRecord> records) {
        YtOperator ytOperator = ytProvider.getOperator(ytCluster);
        YPath tablePath = getPath(ORDERSTATFRAUD_BS);

        createTable(ytOperator, Optional.empty(), tablePath, OrderStatFraudYTRecord.YT_COLUMNS);
        List<YTreeMapNode> ytRecords = StreamEx.of(records).map(OrderStatFraudYTRecord::buildMapNode).toList();
        ytOperator.getYt().tables().insertRows(tablePath, true, false, true, YSON, Cf.wrap(ytRecords).iterator());
    }
}
