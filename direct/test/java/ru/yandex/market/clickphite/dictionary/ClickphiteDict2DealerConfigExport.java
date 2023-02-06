package ru.yandex.market.clickphite.dictionary;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickphite.dictionary.dicts.OrdersAggrDictionary;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 16/06/2018
 */
@Ignore
public class ClickphiteDict2DealerConfigExport {

    @Test
    public void runExport() {
        JsonObject dealerConfig = export(
            new OrdersAggrDictionary(),
            "mbi.analyst_orders",
            "offer_ware_md5"
        );
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(dealerConfig));
    }

    private static JsonObject export(MergeTreeDictionary dict, String destTable, String shardingKey) {
        YtSource ytSource = dict.getClass().getAnnotation(YtSource.class);

        Preconditions.checkArgument(dict.getCalculatedColumns().isEmpty());

        JsonObject dealerConfig = new JsonObject();
        dealerConfig.addProperty("ytPath", ytSource.value());
        dealerConfig.addProperty("ytCluster", "hahn");
        dealerConfig.addProperty("clickHouseTmCluster", "${dealer.market-clickhouse.tm-cluster}");
        dealerConfig.addProperty("clickHouseUser", "${dealer.market-clickhouse.user}");
        dealerConfig.addProperty("clickHousePassword", "${dealer.market-clickhouse.password}");
        dealerConfig.addProperty("clickHouseTable", "mbi.clicks_by_shop");
        dealerConfig.addProperty("partitionBy", "toYYYYMM(" + dict.getDateColumn().getName() + ")");
        dealerConfig.addProperty("shardingKey", shardingKey);

        JsonArray orderBy = new JsonArray();
        for (Column column : dict.getPrimaryKey()) {
            orderBy.add(new JsonPrimitive(column.getName()));
        }

        dealerConfig.add("orderBy", orderBy);

        JsonObject columnsObject = new JsonObject();
        for (Column column : dict.getColumns()) {
            columnsObject.addProperty(column.getName(), column.getType().toClickhouseDDL());
        }
        dealerConfig.add("columns", columnsObject);
        return dealerConfig;
    }
}
