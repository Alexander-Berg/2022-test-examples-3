package ru.yandex.direct.useractionlog.dict;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jooq.SQLDialect;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.test.clickhouse.JunitRuleClickHouseCluster;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.useractionlog.db.ShardReplicaChooser;

/**
 * DIRECT-75522
 * Тест заглушки для бесшовного обновления синхронизатора и web.
 */
@Deprecated
@ParametersAreNonnullByDefault
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class ClickHouseJournalDictRepositoryTest {
    @ClassRule
    public static JunitRuleClickHouseCluster junitRuleClickHouseCluster = new JunitRuleClickHouseCluster();
    private static ClickHouseCluster clickHouseCluster;
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private MemoryDictRepository memoryDictRepository;
    private DatabaseWrapper databaseWrapper;
    private ShardReplicaChooser shardReplicaChooser;

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException {
        clickHouseCluster = junitRuleClickHouseCluster.singleServerCluster();
    }

    @Before
    public void setUp() {
        memoryDictRepository = new MemoryDictRepository();
        memoryDictRepository.addData(DictDataCategory.CAMPAIGN_NAME, 123L, "camp 123");
        memoryDictRepository.addData(DictDataCategory.ADGROUP_NAME, 456L, "adgroup 456");

        String dbName = TestUtils.randomName("test_", 12);
        String jdbcUrl = clickHouseCluster.getClickHouseJdbcUrls().values().iterator().next();
        databaseWrapper = new DatabaseWrapper(dbName,
                new ClickHouseDataSource(jdbcUrl),
                SQLDialect.DEFAULT,
                EnvironmentType.DB_TESTING);
        shardReplicaChooser = new ShardReplicaChooser() {
            @Override
            public DatabaseWrapper getForReading(String tableName) {
                return databaseWrapper;
            }

            @Override
            public DatabaseWrapper getForWriting(String tableName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<DatabaseWrapper> findEachShardLeader(String tableName, Duration timeout) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    public void withoutJournalDict() {
        TestUtils.assumeThat(softAssertions ->
                softAssertions.assertThat(databaseWrapper.query("show tables", new SingleColumnRowMapper<>()))
                        .describedAs("Expected empty test database")
                        .isEmpty());

        ClickHouseJournalDictRepository target = new ClickHouseJournalDictRepository(
                memoryDictRepository, shardReplicaChooser);
        ImmutableList<DictRequest> dictRequests = ImmutableList.of(
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L),
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456L),
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 100500L),
                new DictRequest(DictDataCategory.ADGROUP_NAME, 100500L),
                new DictRequest(DictDataCategory.AD_TITLE, 123L));
        Map<DictRequest, Object> expectedResponses = ImmutableMap.of(
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L), "camp 123",
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456L), "adgroup 456");
        softly.assertThat(target.getData(dictRequests)).isEqualTo(expectedResponses);
    }

    @Test
    public void withJournalDict() {
        databaseWrapper.query(jdbc -> jdbc.update(
                "CREATE TABLE journal_dict ("
                        + " type Int32,"
                        + " shard String,"
                        + " id Int64,"
                        + " value String,"
                        + " last_updated DateTime,"
                        + " event_date_time DateTime,"
                        + " server_uuid String,"
                        + " server_event_id Int64)"
                        + " ENGINE = Memory()"));
        String uuid = "00000000-0000-0000-0000-000000000000";
        databaseWrapper.query(jdbc -> jdbc.batchUpdate("INSERT INTO journal_dict "
                        + "(type, shard, id, value, last_updated, event_date_time, server_uuid, server_event_id)"
                        + " VALUES "
                        + "(?, ?, ?, ?, ?, ?, ?, ?)",
                ImmutableList.of(
                        // Этот код должен прожить недолго, пусть здесь побудет магическое число
                        // DictDataCategory.CAMPAIGN_NAME = 2
                        new Object[]{2, "ppc:1", 123L, "camp 123 new 1", "2018-06-05 22:59:00",
                                "2018-06-05 22:59:00", uuid, 100},
                        new Object[]{2, "ppc:1", 123L, "camp 123 new 2", "2018-06-05 22:59:01",
                                "2018-06-05 22:59:01", uuid, 101})));

        ClickHouseJournalDictRepository target = new ClickHouseJournalDictRepository(
                memoryDictRepository, shardReplicaChooser);
        ImmutableList<DictRequest> dictRequests = ImmutableList.of(
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L),
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456L),
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 100500L),
                new DictRequest(DictDataCategory.ADGROUP_NAME, 100500L),
                new DictRequest(DictDataCategory.AD_TITLE, 123L));
        Map<DictRequest, Object> expectedResponses = ImmutableMap.of(
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L), "camp 123 new 2",
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456L), "adgroup 456");
        softly.assertThat(target.getData(dictRequests)).isEqualTo(expectedResponses);
    }
}
