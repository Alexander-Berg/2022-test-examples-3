package ru.yandex.market.logshatter.config;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickhouse.ddl.engine.DistributedEngine;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.EntityConverter;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.TimestampFormat;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.config.LogSource;
import ru.yandex.market.health.configs.logshatter.config.ParserConfig;
import ru.yandex.market.health.configs.logshatter.config.TableDescriptionUtils;
import ru.yandex.market.health.configs.logshatter.config_history.LogshatterVersionHistoryDao;
import ru.yandex.market.health.configs.logshatter.json.JsonParser;
import ru.yandex.market.health.configs.logshatter.json.JsonParserConfig;
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
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.logshatter.parser.auto.AutoParser;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityToConfigTest {
    private static final String CLICK_HOUSE_ZOOKEEPER_TABLE_PREFIX = "/clickhouse/tables/{shard}";
    private static final String CLICKHOUSE_CLUSTER = "test_cluster";
    private static final Integer DEFAULT_ROTATION_DAYS = -1;

    private ConfigurationService configurationService;
    private EntityConverter entityConverter;
    private LogshatterConfigVersionEntity versionEntity;

    @Before
    public void setUp() {
        configurationService = new ConfigurationService();
        configurationService.setClickHouseZookeeperTablePrefix(CLICK_HOUSE_ZOOKEEPER_TABLE_PREFIX);

        entityConverter = new EntityConverter(
            "",
            "",
            CLICK_HOUSE_ZOOKEEPER_TABLE_PREFIX,
            DEFAULT_ROTATION_DAYS,
            false,
            "_lr"
        );

        configurationService.setEntityConverter(entityConverter);

        Map<String, String> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");

        versionEntity = new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("configId", 1L),
            VersionedConfigSource.UI,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(
                    Arrays.asList("topic1", "topic2"),
                    "**host**",
                    "**/myLog.log",
                    Collections.singletonList("**/tmp/*/myLog.log")
                ),
                Collections.emptyList(),
                null
            ),
            new ParserEntity(
                new JavaParserEntity(TestParser.class.getName()),
                null,
                null,
                params
            ),
            null,
            new TableEntity("database", "table"),
            30,
            "mdbsefw4434sasaffe",
            null,
            null,
            null,
            null,
            new TableEntity("database2", "table2")
        );
    }

    @Test
    public void logbrokerSourceParsing() throws ConfigurationException, ConfigValidationException {
        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getUri).collect(Collectors.toSet()))
            .isEqualTo(
                versionEntity.getDataSource().getLogBroker().getTopics().stream()
                    .map(t -> LogSource.LOGBROKER_SCHEMA + LogSource.SCHEMA_DELIMITER + t)
                    .collect(Collectors.toSet())
            );

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getSchema).collect(Collectors.toSet()))
            .isEqualTo(Collections.singleton(LogSource.LOGBROKER_SCHEMA));

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getPath).collect(Collectors.toSet()))
            .isEqualTo(new HashSet<>(versionEntity.getDataSource().getLogBroker().getTopics()));

        assertThat(logShatterConfig.getLogHosts()).isEqualTo(
            versionEntity.getDataSource().getLogBroker().getHostGlob()
        );
        assertThat(logShatterConfig.getLogPath()).isEqualTo(
            versionEntity.getDataSource().getLogBroker().getPathGlob()
        );
        assertThat(logShatterConfig.getIgnoreLogPaths()).isEqualTo(
            versionEntity.getDataSource().getLogBroker().getIgnorePathsGlob()
        );
    }

    @Test
    public void trackerSourceParsing() throws ConfigurationException, ConfigValidationException {
        versionEntity = new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("configId", 1L),
            VersionedConfigSource.UI,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                null,
                Arrays.asList(
                    new DataSourceTrackerEntity("MARKETINFRA"),
                    new DataSourceTrackerEntity("MARKETINFRATEST")
                ),
                null
            ),
            new ParserEntity(
                new JavaParserEntity(TestParser.class.getName()),
                null,
                null,
                null
            ),
            null,
            new TableEntity("database", "table"),
            30,
            null,
            null,
            null,
            null,
            null,
            null
        );

        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getUri).collect(Collectors.toSet()))
            .isEqualTo(
                versionEntity.getDataSource().getTracker().stream()
                    .map(DataSourceTrackerEntity::getQueue)
                    .map(q -> LogSource.STARTREK_SCHEMA + LogSource.SCHEMA_DELIMITER + q)
                    .collect(Collectors.toSet())
            );

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getSchema).collect(Collectors.toSet()))
            .isEqualTo(Collections.singleton(LogSource.STARTREK_SCHEMA));

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getPath).collect(Collectors.toSet()))
            .isEqualTo(
                versionEntity.getDataSource().getTracker().stream()
                    .map(DataSourceTrackerEntity::getQueue)
                    .collect(Collectors.toSet())
            );

        assertThat(logShatterConfig.getLogHosts()).isEqualTo("*");
        assertThat(logShatterConfig.getLogPath()).isEqualTo("*");
    }

    @Test
    public void fileSourceParsing() throws ConfigurationException, ConfigValidationException {
        versionEntity = new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("configId", 1L),
            VersionedConfigSource.UI,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                null,
                Collections.emptyList(),
                new DataSourceDirectoryEntity(Arrays.asList("/some/path/1", "/some/path/2"), "*", "*")
            ),
            new ParserEntity(
                new JavaParserEntity(TestParser.class.getName()),
                null,
                null,
                null
            ),
            null,
            new TableEntity("database", "table"),
            30,
            null,
            null,
            null,
            null,
            null,
            null
        );

        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getUri).collect(Collectors.toSet()))
            .isEqualTo(
                versionEntity.getDataSource().getFile().getDirectoryPaths().stream()
                    .map(p -> LogSource.FILE_SCHEMA + LogSource.SCHEMA_DELIMITER + p)
                    .collect(Collectors.toSet())
            );

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getSchema).collect(Collectors.toSet()))
            .isEqualTo(Collections.singleton(LogSource.FILE_SCHEMA));

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getPath).collect(Collectors.toSet()))
            .isEqualTo(
                new HashSet<>(versionEntity.getDataSource().getFile().getDirectoryPaths())
            );

        assertThat(logShatterConfig.getLogHosts()).isEqualTo(versionEntity.getDataSource().getFile().getHostGlob());
        assertThat(logShatterConfig.getLogPath()).isEqualTo(versionEntity.getDataSource().getFile().getPathGlob());
    }

    @Test
    public void javaParserParsing() throws ConfigurationException, ConfigValidationException {

        LogShatterConfig logShatterConfig = configurationService.entityToConfig(versionEntity, null, null);

        assertThat(logShatterConfig.getParserProvider().getName())
            .isEqualTo(TestParser.class.getName());

        assertThat(logShatterConfig.getParserClassName()).isEqualTo(TestParser.class.getName());

        assertThat(logShatterConfig.getParserProvider().getParserConfig()).isNull();
        assertThat(logShatterConfig.getParserProvider().getJsonParserConfig()).isNull();

        assertThat(logShatterConfig.getDistributedTable()).isNull();

        assertThat(logShatterConfig.getDataTable().getTableName())
            .isEqualTo(versionEntity.getTable().getTable());
        assertThat(logShatterConfig.getDataTable().getDatabaseName())
            .isEqualTo(versionEntity.getTable().getDatabase());

        Set<Column> columns = new HashSet<>(TestParser.COLUMNS);
        columns.add(TableDescription.DATE_COLUMN);
        columns.add(TableDescription.TIMESTAMP_COLUMN);
        assertThat(new HashSet<>(logShatterConfig.getDataTable().getColumns()))
            .isEqualTo(columns);

        assertThat(logShatterConfig.getDataTable().getEngine()).isEqualTo(TableDescriptionUtils.DEFAULT_ENGINE);
    }

    @Test
    public void autoParserParsing() throws ConfigurationException, ConfigValidationException {
        Map<String, String> dateFormats = new HashMap<>();
        dateFormats.put("dateKey1", "HH:mm:ss");
        dateFormats.put("dateKey2", "HH:mm:sss");

        Map<String, String> patterns = new HashMap<>();
        patterns.put("patternKey1", "pattern1");
        patterns.put("patternKey2", "pattern2");

        Map<String, String> thenValues = new HashMap<>();
        thenValues.put("valueKey1", "value1");
        thenValues.put("valueKey2", "value2");

        Map<String, String> elseValues = new HashMap<>();
        elseValues.put("valueKey1", "value3");
        elseValues.put("valueKey2", "value4");

        Map<String, List<String>> requirement = new HashMap<>();
        requirement.put("requirement1", Collections.singletonList("requirementValue1"));
        requirement.put("requirement2", Collections.singletonList("requirementValue2"));

        Map<String, String> tskvMatch = new HashMap<>();
        tskvMatch.put("tskvMatchKey1", "tskvMatchValue1");
        tskvMatch.put("tskvMatchKey2", "tskvMatchValue2");

        Map<String, String> aliases = new HashMap<>();
        patterns.put("aliasesKey1", "alias1");
        patterns.put("aliasesKey2", "alias2");

        versionEntity = new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("configId", 1L),
            VersionedConfigSource.UI,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(
                    Arrays.asList("topic1", "topic2"),
                    "**host**",
                    "**/myLog.log",
                    null
                ),
                Collections.emptyList(),
                null
            ),
            new ParserEntity(
                null,
                null,
                new AutoParserEntity(
                    "HH:mm",
                    dateFormats,
                    TimestampFormat.UNIXTIME,
                    Arrays.asList("field1", "field2"),
                    "/",
                    Arrays.asList("field3", "field4"),
                    patterns,
                    true,
                    Collections.singletonList(
                        new AutoParserConditionEntity(
                            "someCondition",
                            thenValues,
                            elseValues
                        )
                    ),
                    requirement,
                    tskvMatch,
                    false,
                    true,
                    false,
                    aliases,
                    Collections.singletonList(
                        new AutoParserColumnEntity(
                            "name",
                            ColumnType.Int64.name(),
                            "666",
                            "devaultExpression",
                            "codec"
                        )
                    ),
                    new TableEngineEntity(
                        "field1",
                        Collections.singletonList("field2"),
                        "field2"
                    )
                ),
                null
            ),
            null,
            new TableEntity("database", "table"),
            30,
            null,
            null,
            null,
            null,
            null,
            null
        );

        LogShatterConfig logShatterConfig = configurationService.entityToConfig(versionEntity, null, null);

        assertThat(logShatterConfig.getParserProvider().getName()).isEqualTo(AutoParser.class.getSimpleName());

        ParserConfig parserConfig = logShatterConfig.getParserProvider().getParserConfig();
        AutoParserEntity parserEntity = versionEntity.getParser().getAuto();

        Set<Column> columnsFromEntity = parserEntity.getColumns().stream()
            .map(c -> new Column(c.getName(), ColumnType.Int64, c.getDefaultExpr(), c.getDefaultValue(), c.getCodec()))
            .collect(Collectors.toSet());

        assertThat(parserConfig).isNotNull();
        assertThat(logShatterConfig.getParserClassName()).isEqualTo(AutoParser.class.getSimpleName());
        assertThat(logShatterConfig.getParserProvider().getJsonParserConfig()).isNull();

        assertThat(parserConfig.getDateFormat()).isEqualTo(parserEntity.getDateFormat());
        assertThat(parserConfig.getDateFormats()).isEqualTo(parserEntity.getDateFormats());
        assertThat(parserConfig.getTimestampFormat()).isEqualTo(parserEntity.getTimestampFormat());
        assertThat(parserConfig.getDateFields()).isEqualTo(parserEntity.getDateFields());
        assertThat(parserConfig.getSeparator()).isEqualTo(parserEntity.getSeparator());
        assertThat(parserConfig.getFields()).isEqualTo(parserEntity.getFields());
        assertThat(parserConfig.getPatterns()).isEqualTo(parserEntity.getPatterns());
        assertThat(parserConfig.isIgnoreNoMatches()).isEqualTo(parserEntity.getIgnoreNoMatches());
        assertThat(new HashSet<>(parserConfig.getConditions()))
            .isEqualTo(
                parserEntity.getConditions().stream()
                    .map(ParserConfig.Condition::new)
                    .collect(Collectors.toSet())
            );
        assertThat(parserConfig.getRequiredMap().getBackingMap()).isEqualTo(parserEntity.getRequirement());
        assertThat(parserConfig.getTskvMatch()).isEqualTo(parserEntity.getTskvMatch());
        assertThat(parserConfig.isUseDefaultOnEmpty()).isEqualTo(parserEntity.getUseDefaultOnEmpty());
        assertThat(parserConfig.getAliases()).isEqualTo(parserEntity.getAliases());

        Set<Column> columns = new HashSet<>(columnsFromEntity);
        columns.add(TableDescription.DATE_COLUMN);
        columns.add(TableDescription.TIMESTAMP_COLUMN);

        assertThat(new HashSet<>(parserConfig.getTableDescription().getColumns())).isEqualTo(columns);
        assertThat(parserConfig.getTableDescription().getEngine()).isEqualTo(
            TableDescriptionUtils.parseEngine(parserEntity.getTableEngine())
        );

        assertThat(logShatterConfig.getDistributedTable()).isNull();

        assertThat(new HashSet<>(logShatterConfig.getDataTable().getColumns())).isEqualTo(columns);
        assertThat(logShatterConfig.getDataTable().getEngine()).isEqualTo(
            TableDescriptionUtils.parseEngine(parserEntity.getTableEngine())
        );
    }

    @Test
    public void jsonParserParsing() throws ConfigurationException, ConfigValidationException {
        versionEntity = new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("configId", 1L),
            VersionedConfigSource.UI,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                new DataSourceLogBrokerEntity(
                    Arrays.asList("topic1", "topic2"),
                    "**host**",
                    "**/myLog.log",
                    null
                ),
                Collections.emptyList(),
                null
            ),
            new ParserEntity(
                null,
                new JsonParserEntity(
                    "HH:mm",
                    "field1",
                    "path1",
                    "field2",
                    "path2.path22",
                    false,
                    Collections.singletonList(
                        new JsonParserColumnEntity(
                            "name1",
                            ColumnType.String.name(),
                            "field3",
                            "path3",
                            "value1",
                            "defaultExpr1",
                            "codec1"
                        )
                    ),
                    new TableEngineEntity(
                        "field1",
                        Collections.singletonList("field2"),
                        "field2"
                    )
                ),
                null,
                null
            ),
            null,
            new TableEntity("database", "table"),
            30,
            null,
            null,
            null,
            null,
            null,
            null
        );

        LogShatterConfig logShatterConfig = configurationService.entityToConfig(versionEntity, null, null);

        JsonParserConfig parserConfig = logShatterConfig.getParserProvider().getJsonParserConfig();
        JsonParserEntity parserEntity = versionEntity.getParser().getJson();

        assertThat(logShatterConfig.getParserProvider().getName()).isEqualTo(JsonParser.class.getSimpleName());
        assertThat(logShatterConfig.getParserProvider().getParserConfig()).isNull();
        assertThat(logShatterConfig.getParserClassName()).isEqualTo(JsonParser.class.getSimpleName());
        assertThat(parserConfig).isNotNull();

        assertThat(parserConfig.getDateFormat()).isEqualTo(new SimpleDateFormat(parserEntity.getDateFormat()));
        assertThat(parserConfig.getDateField()).isEqualTo(parserEntity.getDateField());
        assertThat(parserConfig.getDateJsonPath()).isEqualTo(Collections.singletonList(parserEntity.getDateJsonPath()));
        assertThat(parserConfig.getTimestampFiled()).isEqualTo(parserEntity.getTimestampField());
        assertThat(parserConfig.getTimestampJsonPath())
            .isEqualTo(Arrays.asList(parserEntity.getTimestampJsonPath().split("\\.")));

        Set<Column> columnsFromEntity = parserEntity.getColumns().stream()
            .map(c -> new Column(c.getName(), ColumnType.String, c.getDefaultExpr(), c.getDefaultValue(), c.getCodec()))
            .collect(Collectors.toSet());

        Set<Column> columns = new HashSet<>(columnsFromEntity);
        columns.add(TableDescription.DATE_COLUMN);
        columns.add(TableDescription.TIMESTAMP_COLUMN);

        assertThat(new HashSet<>(parserConfig.getColumns()))
            .isEqualTo(parserEntity.getColumns().stream()
                .map(c -> new JsonParserConfig.Column(c.getName(), ColumnType.String, c.getField(), c.getJsonPath(),
                    c.getDefaultValue(), c.getDefaultExpr(), c.getCodec()))
                .collect(Collectors.toSet())
            );
        assertThat(parserConfig.getTableDescription().getEngine())
            .isEqualTo(TableDescriptionUtils.parseEngine(parserEntity.getTableEngine()));
        assertThat(new HashSet<>(parserConfig.getTableDescription().getColumns()))
            .isEqualTo(columns);

        assertThat(logShatterConfig.getDistributedTable()).isNull();

        assertThat(new HashSet<>(logShatterConfig.getDataTable().getColumns()))
            .isEqualTo(columns);

        assertThat(logShatterConfig.getDataTable().getEngine()).isEqualTo(
            TableDescriptionUtils.parseEngine(parserEntity.getTableEngine()));
    }

    @Test
    public void distributedTableDefinitionParsing() throws ConfigurationException, ConfigValidationException {
        entityConverter = new EntityConverter(
            "",
            "",
            CLICK_HOUSE_ZOOKEEPER_TABLE_PREFIX,
            -1,
            false,
            "_lr"
        );
        configurationService.setEntityConverter(entityConverter);
        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        Set<Column> columns = new HashSet<>(TestParser.COLUMNS);
        columns.add(TableDescription.DATE_COLUMN);
        columns.add(TableDescription.TIMESTAMP_COLUMN);

        assertThat(logShatterConfig.getDataTable().getTableName())
            .isEqualTo(versionEntity.getTable().getTable() + entityConverter.getLocalReplicatedTablePostfix());
        assertThat(logShatterConfig.getDataTable().getDatabaseName())
            .isEqualTo(versionEntity.getTable().getDatabase());
        assertThat(new HashSet<>(logShatterConfig.getDataTable().getColumns()))
            .isEqualTo(columns);
        assertThat(logShatterConfig.getDataTable().getEngine())
            .isEqualTo(TableDescriptionUtils.DEFAULT_ENGINE.replicated(
                versionEntity.getTable().getDatabase(),
                versionEntity.getTable().getTable() + entityConverter.getLocalReplicatedTablePostfix(),
                CLICK_HOUSE_ZOOKEEPER_TABLE_PREFIX
            ));

        assertThat(logShatterConfig.getDistributedTable().getTableName())
            .isEqualTo(versionEntity.getTable().getTable());
        assertThat(logShatterConfig.getDistributedTable().getDatabaseName())
            .isEqualTo(versionEntity.getTable().getDatabase());
        assertThat(new HashSet<>(logShatterConfig.getDistributedTable().getColumns()))
            .isEqualTo(columns);
        assertThat(logShatterConfig.getDistributedTable().getEngine())
            .isEqualTo(new DistributedEngine(
                CLICKHOUSE_CLUSTER,
                versionEntity.getTable().getDatabase(),
                versionEntity.getTable().getTable() + entityConverter.getLocalReplicatedTablePostfix(),
                "rand()"
            ));
    }

    @Test
    public void allFieldsParsing() throws ConfigurationException, ConfigValidationException {
        LogShatterConfig logShatterConfig = configurationService.entityToConfig(versionEntity, null, null);

        assertThat(logShatterConfig.getConfigId()).isEqualTo(versionEntity.getId().getConfigId());

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getUri).collect(Collectors.toSet()))
            .isEqualTo(
                versionEntity.getDataSource().getLogBroker().getTopics().stream()
                    .map(t -> LogSource.LOGBROKER_SCHEMA + LogSource.SCHEMA_DELIMITER + t)
                    .collect(Collectors.toSet())
            );

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getSchema).collect(Collectors.toSet()))
            .isEqualTo(Collections.singleton(LogSource.LOGBROKER_SCHEMA));

        assertThat(logShatterConfig.getSources().stream().map(LogSource::getPath).collect(Collectors.toSet()))
            .isEqualTo(
                new HashSet<>(versionEntity.getDataSource().getLogBroker().getTopics())
            );

        assertThat(logShatterConfig.getLogHosts()).isEqualTo(
            versionEntity.getDataSource().getLogBroker().getHostGlob()
        );
        assertThat(logShatterConfig.getLogPath()).isEqualTo(
            versionEntity.getDataSource().getLogBroker().getPathGlob()
        );

        assertThat(logShatterConfig.getParserProvider().getName())
            .isEqualTo(TestParser.class.getName());

        assertThat(logShatterConfig.getParserProvider().getParserConfig()).isNull();
        assertThat(logShatterConfig.getParserProvider().getJsonParserConfig()).isNull();

        assertThat(logShatterConfig.getDistributedTable()).isNull();

        assertThat(logShatterConfig.getDataTable().getTableName())
            .isEqualTo(versionEntity.getTable().getTable());
        assertThat(logShatterConfig.getDataTable().getDatabaseName())
            .isEqualTo(versionEntity.getTable().getDatabase());

        Set<Column> columns = new HashSet<>(TestParser.COLUMNS);
        columns.add(TableDescription.DATE_COLUMN);
        columns.add(TableDescription.TIMESTAMP_COLUMN);
        assertThat(new HashSet<>(logShatterConfig.getDataTable().getColumns()))
            .isEqualTo(columns);

        assertThat(logShatterConfig.getDataTable().getEngine()).isEqualTo(TableDescriptionUtils.DEFAULT_ENGINE);

        assertThat(logShatterConfig.getParams()).isEqualTo(versionEntity.getParser().getParams());

        assertThat(logShatterConfig.getShortConfigName()).isEqualTo(versionEntity.getId().getConfigId());

        assertThat(logShatterConfig.getDataRotationDays()).isEqualTo(versionEntity.getDataRotationDays());

        assertThat(logShatterConfig.getMetadataTable().getTable())
            .isEqualTo(versionEntity.getMetadataTable().getTable());
        assertThat(logShatterConfig.getMetadataTable().getDatabase())
            .isEqualTo(versionEntity.getMetadataTable().getDatabase());
    }

    public static class TestParser implements LogParser {
        static final List<Column> COLUMNS = Arrays.asList(
            new Column("host", ColumnType.String),
            new Column("test1", ColumnType.String),
            new Column("test2", ColumnType.String)
        );

        @Override
        public TableDescription getTableDescription() {
            return TableDescription.createDefault(COLUMNS);
        }

        @Override
        public void parse(String line, ParserContext context) throws Exception {

        }
    }

    @Test
    public void clickHouseClusterIdParsing() throws ConfigurationException, ConfigValidationException {
        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        assertThat(logShatterConfig.getClickHouseClusterId()).isEqualTo(versionEntity.getClickHouseClusterId());
    }

    @Test
    public void rotationDaysParsing() throws ConfigurationException, ConfigValidationException {
        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        assertThat(logShatterConfig.getDataRotationDays()).isEqualTo(versionEntity.getDataRotationDays());
    }

    @Test
    public void defaultRotationDaysParsing() throws ConfigurationException, ConfigValidationException {
        versionEntity = new LogshatterConfigVersionEntity(
            new VersionedConfigEntity.VersionEntity.Id("configId", 1L),
            VersionedConfigSource.UI,
            VersionStatus.PUBLIC,
            new DataSourcesEntity(
                null,
                Collections.emptyList(),
                new DataSourceDirectoryEntity(Arrays.asList("/some/path/1", "/some/path/2"), "*", "*")
            ),
            new ParserEntity(
                new JavaParserEntity(TestParser.class.getName()),
                null,
                null,
                null
            ),
            null,
            new TableEntity("database", "table"),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        assertThat(logShatterConfig.getDataRotationDays()).isEqualTo(DEFAULT_ROTATION_DAYS);
    }

    @Test
    public void ignoreRotationDaysParsing() throws ConfigurationException, ConfigValidationException {
        entityConverter = new EntityConverter(
            "",
            "",
            CLICK_HOUSE_ZOOKEEPER_TABLE_PREFIX,
            -1,
            true,
            "_lr"
        );
        configurationService.setEntityConverter(entityConverter);

        LogShatterConfig logShatterConfig =
            configurationService.entityToConfig(versionEntity, null, CLICKHOUSE_CLUSTER);

        assertThat(logShatterConfig.getDataRotationDays()).isEqualTo(DEFAULT_ROTATION_DAYS);
    }

    @Ignore
    @Test
    public void integrationTest() throws ConfigValidationException, ConfigurationException {
        MongoClientOptions.Builder options = MongoClientOptions.builder()
            .writeConcern(WriteConcern.MAJORITY)
            .readPreference(ReadPreference.primary())
            .connectTimeout(5000)
            .socketTimeout(60000)
            .sslEnabled(false);

        MongoTemplate mongoTemplate = new MongoTemplate(new SimpleMongoDbFactory(
            new MongoClient(new MongoClientURI("mongodb://localhost", options)), "health")
        );

        LogshatterConfigDao logshatterConfigDao = new LogshatterConfigDao(
            mongoTemplate, new LogshatterVersionHistoryDao(mongoTemplate),
            new LocalValidatorFactoryBean(),
            null, null, null, null
        );

        configurationService.setConfigDao(logshatterConfigDao);
        configurationService.setDefaultClickHouseDatabase("market");
        configurationService.setDefaultSource("logbroker://market-health-testing--other," +
            "logbroker://market-health-dev--other");

        List<LogShatterConfig> logShatterConfigs = configurationService.readAndValidateConfigurationFromMongo();
    }
}
