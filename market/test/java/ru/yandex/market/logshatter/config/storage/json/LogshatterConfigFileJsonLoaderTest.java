package ru.yandex.market.logshatter.config.storage.json;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.TimestampFormat;
import ru.yandex.market.health.configs.logshatter.mongo.AutoParserColumnEntity;
import ru.yandex.market.health.configs.logshatter.mongo.AutoParserConditionEntity;
import ru.yandex.market.health.configs.logshatter.mongo.AutoParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceDirectoryEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceLogBrokerEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceTrackerEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourcesEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JavaParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JsonParserColumnEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JsonParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigVersionEntity;
import ru.yandex.market.health.configs.logshatter.mongo.ParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.TableEngineEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class LogshatterConfigFileJsonLoaderTest {
    @Test
    public void sourcesLogbrokerImplicit() {
        check("sourcesLogbrokerImplicit", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("sourcesLogbrokerImplicit", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void sourcesLogbrokerExplicit() {
        check("sourcesLogbrokerExplicit", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("sourcesLogbrokerExplicit", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(
                    Arrays.asList("topic--one", "topic/two"),
                    "hostGlob1",
                    "pathGlob1",
                    null
                ),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void sourcesTracker() {
        check("sourcesTracker", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("sourcesTracker", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                null,
                Arrays.asList(
                    new DataSourceTrackerEntity("QUEUEONE"),
                    new DataSourceTrackerEntity("QUEUETWO")
                ),
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void sourcesDirectory() {
        check("sourcesDirectory", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("sourcesDirectory", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                null,
                null,
                new DataSourceDirectoryEntity(Collections.singletonList("/some/directory/"), "hostGlob1", "pathGlob1")
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void tableWithDatabase() {
        check("tableWithDatabase", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("tableWithDatabase", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                "database1",
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void metadataTable() {
        check("metadataTable", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("metadataTable", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,

            new TableEntity(
                null,
                "table2"
            )
        ));
    }

    @Test
    public void metadataTableWithDatabase() {
        check("metadataTableWithDatabase", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("metadataTableWithDatabase", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,

            new TableEntity(
                "database1",
                "table2"
            )
        ));
    }

    @Test
    public void dataRotationDays() {
        check("dataRotationDays", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("dataRotationDays", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            123,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void params() {
        check("params", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("params", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                new JavaParserEntity("parserClass1"),
                null,
                null,
                ImmutableMap.of(
                    "param1", "value1",
                    "param2", "value2"
                )
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void jsonParser() {
        check("jsonParser", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("jsonParser", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                null,
                new JsonParserEntity(
                    "dateFormat1",
                    "dateField1",
                    "dateJsonPath1",
                    "timestampField1",
                    "timestampJsonPath1",
                    false,
                    Arrays.asList(
                        new JsonParserColumnEntity(
                            "column1",
                            "type1",
                            null,
                            null,
                            null,
                            null,
                            null
                        ),
                        new JsonParserColumnEntity(
                            "column2",
                            "type2",
                            "field2",
                            "jsonPath2",
                            "defaultValue2",
                            "defaultExpr2",
                            "codec2"
                        )
                    ),
                    new TableEngineEntity(
                        "partitionBy1",
                        Collections.singletonList("orderBy1"),
                        "sampleBy1"
                    )
                ),
                null,
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
    }

    @Test
    public void autoParser() {
        check("autoParser", new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("autoParser", -1L),
            VersionedConfigSource.CODE,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(null, "hostGlob1", "pathGlob1", null),
                null,
                null
            ),
            new ParserEntity(
                null,
                null,
                new AutoParserEntity(
                    "dateFormat1",
                    Collections.singletonMap("column2", "dateFormat2"),
                    TimestampFormat.UNIXTIME,
                    Arrays.asList("column1", "column2"),
                    "separator1",
                    Arrays.asList("column3", "column4"),
                    Collections.singletonMap("column1", "pattern1"),
                    true,
                    Collections.singletonList(new AutoParserConditionEntity(
                        "condition1",
                        Collections.singletonMap("column1", "value1"),
                        Collections.singletonMap("column2", "value2")
                    )),
                    Collections.singletonMap("column3", Collections.singletonList("requirement3")),
                    Collections.singletonMap("column4", "value4"),
                    false,
                    false,
                    false,
                    Collections.singletonMap("column5", "column6"),
                    Collections.singletonList(new AutoParserColumnEntity(
                        "column7",
                        "type1",
                        "default1",
                        "defaultExpr1",
                        "codec1"
                    )),
                    new TableEngineEntity(
                        "partitionBy1",
                        Collections.singletonList("orderBy1"),
                        "sampleBy1"
                    )
                ),
                null
            ),
            null,
            new TableEntity(
                null,
                "table1"
            ),
            null,
            "cluster1",
            null,
            null,
            null,
            null,
            null
        ));
    }

    private static void check(String dir, LogshatterConfigVersionEntity expected) {
        String configDir = ClassLoader.getSystemResource("configs/" + dir).getPath();
        assertThat(new LogshatterConfigFileJsonLoader(configDir).load())
            .hasSize(1)
            .first().isEqualToComparingFieldByFieldRecursively(expected);
    }
}
