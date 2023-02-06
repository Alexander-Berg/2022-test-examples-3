package ru.yandex.market.health;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.ClickphiteMasterTimeTicker;
import ru.yandex.market.clickphite.whitelist.SplitWhitelistService;
import ru.yandex.market.health.configs.clickphite.ClickphiteConfigDao;
import ru.yandex.market.health.configs.clickphite.MetricPeriod;
import ru.yandex.market.health.configs.clickphite.SplitWhitelistDao;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupEntity;
import ru.yandex.market.health.configs.clickphite.mongo.ClickphiteConfigGroupVersionEntity;
import ru.yandex.market.health.configs.clickphite.mongo.GraphiteMetricsAndSolomonSensorsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistAutoUpdateEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistEntity;
import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistSettingsEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceReportEntity;
import ru.yandex.market.health.configs.clickphite.mongo.StatfaceSplitOrFieldEntity;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.statface.StatfaceField;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClickphiteSplitWhitelistTests extends IntegrationTestsBase {

    private static final int DAYS_COUNT = 5;
    private static final int NUMBER_THRESHOLD = 3;
    private static final SplitWhitelistAutoUpdateEntity SPLIT_WHITELIST_AUTO_UPDATE_ENTITY =
        new SplitWhitelistAutoUpdateEntity(
            DAYS_COUNT,
            NUMBER_THRESHOLD
        );
    private static final SplitWhitelistSettingsEntity SPLIT_WHITELIST_SETTINGS_ENTITY =
        new SplitWhitelistSettingsEntity(
            Collections.emptyList(),
            SPLIT_WHITELIST_AUTO_UPDATE_ENTITY,
            false
        );
    private static final String EXPECTED_VALUE_1 = "winner1";
    private static final String EXPECTED_VALUE_2 = "winner2";
    private static final String EXPECTED_VALUE_3 = "winner3";
    private static final String TABLE_NAME = "some_table";
    private static final String DB_NAME = "market";
    private static final TableEntity TABLE_ENTITY = new TableEntity(DB_NAME, TABLE_NAME);
    private static final String TABLE_NAME_WITH_DB = DB_NAME + "." + TABLE_NAME;
    private static final String COLUMN_NAME = "some_column";
    private static final SplitWhitelistEntity.Id EXPECTED_WHITELIST_ID = new SplitWhitelistEntity.Id(
        TABLE_ENTITY, COLUMN_NAME, SPLIT_WHITELIST_AUTO_UPDATE_ENTITY
    );

    private SplitWhitelistDao splitWhitelistDao;
    private SplitWhitelistService splitWhitelistService;
    private ZonedDateTime now;
    private ClickhouseTemplate clickhouseTemplate;

    @After
    public void teardown() {
        dropDatabases(clickhouseTemplate);
    }

    @Test
    public void graphiteSolomon() throws Exception {
        testSingleCallSingleValue(createClickphiteConfigEntityWithGraphiteSolomon());
    }

    @Test
    public void statface() throws Exception {
        testSingleCallSingleValue(createClickphiteConfigEntityWithStatface());
    }

    @Test
    public void append() throws Exception {
        setup(createClickphiteConfigEntityWithGraphiteSolomon());
        splitWhitelistService.call();
        runClickHouseSql(generateSql(now, EXPECTED_VALUE_2, NUMBER_THRESHOLD + 1));

        splitWhitelistService.call();

        checkMongo(new HashSet<>(Arrays.asList(EXPECTED_VALUE_1, EXPECTED_VALUE_2)));
    }

    @Test
    public void limitAutoWhitelistSize() throws Exception {
        setup(createClickphiteConfigEntityWithGraphiteSolomon());
        runClickHouseSql(generateSql(now, EXPECTED_VALUE_2, NUMBER_THRESHOLD + 1));
        splitWhitelistService.call();
        runClickHouseSql(generateSql(now, EXPECTED_VALUE_3, NUMBER_THRESHOLD + 3));
        runClickHouseSql(generateSql(now, "some_excess_value", NUMBER_THRESHOLD + 2));

        splitWhitelistService.call();

        checkMongo(new HashSet<>(Arrays.asList(EXPECTED_VALUE_1, EXPECTED_VALUE_2, EXPECTED_VALUE_3)));
    }

    private void testSingleCallSingleValue(ClickphiteConfigEntity config) throws Exception {
        setup(config);

        splitWhitelistService.call();

        checkMongo(Collections.singleton(EXPECTED_VALUE_1));
    }

    private void checkMongo(Set<String> expectedValues) {
        final SplitWhitelistEntity whitelist = splitWhitelistDao.get(EXPECTED_WHITELIST_ID);
        Assert.assertEquals(EXPECTED_WHITELIST_ID, whitelist.getId());
        checkWhitelistContainsExpectedValue(expectedValues, whitelist);
    }

    private void checkWhitelistContainsExpectedValue(Set<String> expectedValues,
                                                     SplitWhitelistEntity actualWhitelist) {
        Assert.assertNotNull(actualWhitelist);
        Assert.assertNotNull(actualWhitelist.getWhitelist());
        final Set<String> actualValues = actualWhitelist.getWhitelist().stream()
            .map(SplitWhitelistEntity.Element::getValue)
            .collect(Collectors.toSet());
        Assert.assertEquals(expectedValues, actualValues);
    }

    private void setup(ClickphiteConfigEntity config) throws Exception {
        now = Instant.now().atZone(ZoneId.of("UTC"));
        AnnotationConfigApplicationContext logshatterContext = new AnnotationConfigApplicationContext();
        initLogshatterContextAndTablesMinConfig(logshatterContext);
        clickhouseTemplate = logshatterContext.getBean(ClickhouseTemplate.class);
        // засыпаем, чтобы успела создаться необходимая база с таблицами в ClickHouse
        Thread.sleep(3000);
        fillDataIntoTestTable();
        AnnotationConfigApplicationContext clickphiteContext = createClickphiteContext();
        createClickphiteConfig(clickphiteContext, config);
        clickphiteContext.getBean(ClickphiteMasterTimeTicker.class).setLeader(true);
        splitWhitelistService = clickphiteContext.getBean(SplitWhitelistService.class);
        splitWhitelistDao = clickphiteContext.getBean(SplitWhitelistDao.class);
        clickphiteContext.getBean(MongoTemplate.class).remove(new Query(), SplitWhitelistEntity.class);
    }

    private void fillDataIntoTestTable() {
        final List<String> testData = generateTestData();
        runClickHouseSql(testData);
    }

    private void runClickHouseSql(List<String> queries) {
        for (String sql : queries) {
            clickhouseTemplate.update(sql);
        }
    }

    private List<String> generateTestData() {
        List<String> result = new ArrayList<>();
        result.addAll(generateSql(now.minus(DAYS_COUNT + 1, ChronoUnit.DAYS), "very old values", NUMBER_THRESHOLD + 1));
        result.addAll(generateSql(now, "too little values", NUMBER_THRESHOLD - 1));
        result.addAll(generateSql(now, EXPECTED_VALUE_1, NUMBER_THRESHOLD + 1));
        return result;
    }

    private List<String> generateSql(ZonedDateTime date, String value, int count) {
        final String dateString = DateTimeFormatter.ISO_LOCAL_DATE.format(date);
        String query = String.format("insert into %s(date, %s) values ('%s', '%s')", TABLE_NAME_WITH_DB, COLUMN_NAME,
            dateString, value);
        return Collections.nCopies(count, query);
    }

    private void createClickphiteConfig(AnnotationConfigApplicationContext context, ClickphiteConfigEntity config) {
        final ClickphiteConfigDao clickphiteConfigDao = context.getBean(ClickphiteConfigDao.class);
        final String configId = "some_config";
        clickphiteConfigDao.createConfig(new ClickphiteConfigGroupEntity(
            configId,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ));
        final VersionedConfigEntity.VersionEntity.Id configVersion = clickphiteConfigDao.createValidVersion(
            new ClickphiteConfigGroupVersionEntity(
                new VersionedConfigEntity.VersionEntity.Id(configId, -1L),
                null,
                null,
                "some_owner",
                null,
                Collections.singletonList(config)
            ), null);
        clickphiteConfigDao.activateVersion(configVersion, null);
    }

    private ClickphiteConfigEntity createClickphiteConfigEntityWithGraphiteSolomon() {
        return new ClickphiteConfigEntity(
            TABLE_ENTITY,
            null,
            Collections.singletonList(MetricPeriod.ONE_MIN),
            null,
            null,
            null,
            new GraphiteMetricsAndSolomonSensorsEntity(
                Collections.singletonList(
                    new SplitEntity(
                        "some_name",
                        COLUMN_NAME,
                        SPLIT_WHITELIST_SETTINGS_ENTITY
                    )
                ),
                "some_metric_expression",
                null,
                null,
                null,
                null,
                null
            ),
            null
        );
    }

    private ClickphiteConfigEntity createClickphiteConfigEntityWithStatface() {
        return new ClickphiteConfigEntity(
            TABLE_ENTITY,
            null,
            Collections.singletonList(MetricPeriod.ONE_MIN),
            null,
            null,
            null,
            null,
            new StatfaceReportEntity(
                "some_title",
                "some_report",
                Collections.singletonList(
                    new StatfaceSplitOrFieldEntity(
                        "some_name",
                        COLUMN_NAME,
                        "some_title",
                        false,
                        StatfaceField.ViewType.Float,
                        0,
                        SPLIT_WHITELIST_SETTINGS_ENTITY
                    )
                ),
                Collections.emptyList(),
                Collections.emptyList()
            )
        );
    }

}
