package ru.yandex.market.clickhouse.dealer.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.support.ResourcePropertySource;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.engine.MergeTree;
import ru.yandex.market.clickhouse.dealer.clickhouse.SingletonClickHousePartitionExtractor;
import ru.yandex.market.clickhouse.dealer.clickhouse.ToYyyyMmClickHousePartitionExtractor;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 19/01/2018
 */
public class DealerConfigParserTest {

    private DealerGlobalConfig emptyConfig = DealerGlobalConfig.newBuilder().build();

    @Test
    public void parseConfig() throws Exception {
        DealerConfigParser parser = dealerConfigParser("/config/properties-example.properties", emptyConfig);
        DealerConfig actualConfig = parser.parseConfig(readJsonObject("/config/config-example.json"), null);

        DealerConfig expectedConfig = DealerConfig.newBuilder()
            .withGlobalConfig(emptyConfig)
            .withYtCluster("hahn")
            .withYtPath("//home/market/production/mstat/dictionaries/market_offers_ch")
            .withClickHouseTmCluster("market-clickhouse-testing", "user42", "secret")
            .withTableName("db.table")
            .withColumns(
                new Column("field1", ColumnType.Int32, "-1"),
                new Column("field2", ColumnType.String, null),
                new Column("date", ColumnType.Date, null)
            )
            .withPartitionBy("toYYYYMM(date)")
            .withOrderBy("field1", "field2")
            .withShardingKeys("field2")
            .withYtPartitionNameColumn("date")
            .withPartitionExtractor(ToYyyyMmClickHousePartitionExtractor.INSTANCE)
            .withRotationPeriodDays(60)
            .build();

        Assert.assertEquals(expectedConfig, actualConfig);
    }

    @Test
    public void parseDbaasConfig() throws Exception {
        DealerConfigParser parser = dealerConfigParser("/config/properties-example.properties", emptyConfig);
        DealerConfig actualConfig = parser.parseConfig(readJsonObject("/config/dbaas-config-example.json"), null);


        DealerConfig expectedConfig = DealerConfig.newBuilder()
            .withGlobalConfig(emptyConfig)
            .withYtCluster("hahn")
            .withYtPath("//home/market/production/mstat/dictionaries/market_offers_ch")
            .withClickHouseDbaasCluster("a9b53a6c-9c3d-41a7-870c-f85941ea2471", "user42", "secret", "abc-adasas")
            .withTableName("db.table2")
            .withColumns(new Column("field1", ColumnType.Int32))
            .withOrderBy("field1")
            .withShardingKeys("field1")
            .withPartitionBy("tuple()")
            .withPartitionExtractor(SingletonClickHousePartitionExtractor.INSTANCE)
            .withRotationPeriodDays(0)
            .build();

        Assert.assertEquals(expectedConfig, actualConfig);
    }

    @Test
    public void testTempDatabase() throws Exception {
        DealerConfigParser parser = dealerConfigParser(
            "/config/properties-example.properties",
            DealerGlobalConfig.newBuilder()
                .withTempDatabase("tmp")
                .build()
        );
        DealerConfig actualConfig = parser.parseConfig(readJsonObject("/config/dbaas-config-example.json"), null);

        Assert.assertEquals(actualConfig.getTempDataTable().getDatabase(), "tmp");
        Assert.assertEquals(actualConfig.getTempDistributedTable().getDatabase(), "tmp");
        Assert.assertEquals(actualConfig.getDataTable().getDatabase(), "db");
        Assert.assertEquals(actualConfig.getDistributedTable().getDatabase(), "db");
    }

    @Test
    public void testMergeTree() throws Exception {
        DealerConfigParser parser = dealerConfigParser("/config/properties-example.properties", emptyConfig);
        DealerConfig actualConfig = parser.parseConfig(readJsonObject("/config/config-example2.json"), null);

        MergeTree expectedMergeTree = new MergeTree(
            "toYYYYMM(date)",
            Arrays.asList("field1", "intHash32(field2)"),
            "intHash32(field2)"
        );
        Assert.assertEquals(expectedMergeTree, actualConfig.getMergeTree());
    }

    @Test(expected = IllegalStateException.class)
    public void testNoPartitionByInColumns() throws Exception {
        DealerConfigParser parser = dealerConfigParser("/config/properties-example.properties", emptyConfig);
        parser.parseConfig(readJsonObject("/config/no-sample-by-in-columns.json"), null);
    }

    private DealerConfigParser dealerConfigParser(String propertiesPath, DealerGlobalConfig globalConfig) throws Exception {
        ResourcePropertySource propertySource = new ResourcePropertySource(propertiesPath);
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(propertySource);
        PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        return new DealerConfigParser(propertyResolver::resolveRequiredPlaceholders, globalConfig);
    }

    private JsonObject readJsonObject(String path) throws Exception {
        try (Reader confReader = new InputStreamReader(getClass().getResourceAsStream(path))) {
            return new Gson().fromJson(confReader, JsonObject.class);
        }
    }
}