package ru.yandex.direct.useractionlog.dict;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jooq.SQLDialect;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.direct.clickhouse.ClickHouseCluster;
import ru.yandex.direct.clickhouse.SqlBuilder;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.test.clickhouse.JunitRuleClickHouseCluster;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.useractionlog.db.ReadWriteDictTable;
import ru.yandex.direct.useractionlog.schema.dict.DictRecord;
import ru.yandex.direct.useractionlog.schema.dict.DictSchema;

@ParametersAreNonnullByDefault
@YaIgnore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class ClickHouseDictRepositoryTest {
    private static final String DATABASE_NAME = TestUtils.randomName("test_ch_", 16);
    private static final String TABLE_NAME = "test_dict";
    @ClassRule
    public static JunitRuleClickHouseCluster clickHouseClusterRule = new JunitRuleClickHouseCluster();
    private static DatabaseWrapper databaseWrapper;
    private static ClickHouseCluster clickHouseCluster;
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private MockedCurrentTimeClickHouseDictRepository clickHouseDictRepository;
    private MockedCurrentTimeClickHouseDictRepository otherSourceClickHouseDictRepository;
    private MockedCurrentTimeClickHouseDictRepository readOnlyClickHouseDictRepository;
    private ReadWriteDictTable readWriteDictTable;

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException, SQLException {
        clickHouseCluster = clickHouseClusterRule.singleServerCluster();
        try (Connection conn = new ClickHouseDataSource(clickHouseCluster.getClickHouseJdbcUrls().get("clickhouse"))
                .getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.executeQuery("CREATE DATABASE " + DATABASE_NAME);
            }
        }
        databaseWrapper = new DatabaseWrapper("clickhouse",
                new ClickHouseDataSource(clickHouseCluster.getClickHouseJdbcUrls(DATABASE_NAME).get("clickhouse")),
                SQLDialect.DEFAULT,
                EnvironmentType.DB_TESTING);
    }

    @AfterClass
    public static void tearDownClass() throws SQLException {
        try (Connection conn = new ClickHouseDataSource(clickHouseCluster.getClickHouseJdbcUrls().get("clickhouse"))
                .getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.executeQuery("DROP DATABASE IF EXISTS " + DATABASE_NAME);
            }
        }
    }

    @Before
    public void setUp() {
        databaseWrapper.query(jdbc ->
                jdbc.update("DROP TABLE IF EXISTS " + TABLE_NAME));
        databaseWrapper.getDslContext().connection(connection ->
                new DictSchema(DATABASE_NAME, TABLE_NAME).createTable(connection));
        readWriteDictTable = new ReadWriteDictTable(
                ignored -> databaseWrapper, ignored -> databaseWrapper, TABLE_NAME);
        clickHouseDictRepository =
                new MockedCurrentTimeClickHouseDictRepository("current-source", readWriteDictTable);
        otherSourceClickHouseDictRepository =
                new MockedCurrentTimeClickHouseDictRepository("other-source", readWriteDictTable);
        readOnlyClickHouseDictRepository = new MockedCurrentTimeClickHouseDictRepository(null, readWriteDictTable);
    }

    /**
     * Простая проверка, что новые словарные данные пишутся в соответствующую таблицу кликхауса.
     */
    @Test
    public void testAddData() {
        clickHouseDictRepository.addData(ImmutableMap.of(
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123), "camp name 123",
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456), "adgroup name 456"));

        List<DictRecord> result = readWriteDictTable.read.select(readWriteDictTable.read.sqlBuilder()
                .orderBy(DictSchema.TYPE.getName(), SqlBuilder.Order.ASC)
                .orderBy(DictSchema.ID.getName(), SqlBuilder.Order.ASC));

        softly.assertThat(result).describedAs("Categories as expected")
                .extracting(DictRecord::getCategory)
                .containsExactly(DictDataCategory.CAMPAIGN_NAME, DictDataCategory.ADGROUP_NAME);
        softly.assertThat(result).describedAs("Ids as expected")
                .extracting(DictRecord::getId)
                .containsExactly(123L, 456L);
        softly.assertThat(result).describedAs("Values as expected")
                .extracting(DictRecord::getValue)
                .containsExactly("camp name 123", "adgroup name 456");
    }

    /**
     * Проверка работоспособности чтения словарных данных из писалки, без краевых случаев.
     */
    @Test
    public void testGetData() {
        ImmutableMap<DictRequest, Object> data = ImmutableMap.of(
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123), "camp name 123",
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 124), "camp name 124");
        clickHouseDictRepository.addData(data);

        ImmutableMap<DictRequest, Object> otherSourceData = ImmutableMap.of(
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456), "adgroup name 456",
                new DictRequest(DictDataCategory.ADGROUP_NAME, 457), "does not matter");
        otherSourceClickHouseDictRepository.addData(otherSourceData);

        List<DictRequest> requests = ImmutableList.of(
                // Три каких-то значения
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L),
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 124L),
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456L),

                // Значение из несуществующей категории
                new DictRequest(DictDataCategory.AD_TITLE, 456L),

                // Значение для несуществующего идентификатора
                new DictRequest(DictDataCategory.ADGROUP_NAME, 100500L));

        Map<DictRequest, Object> result = readOnlyClickHouseDictRepository.getData(requests);

        softly.assertThat(result)
                .describedAs("All existing requested data was fetched")
                .isEqualTo(ImmutableMap.builder()
                        .putAll(Maps.filterKeys(data, requests::contains))
                        .putAll(Maps.filterKeys(otherSourceData, requests::contains))
                        .build());
    }

    /**
     * Если в таблице словарных данных есть записи с одинаковыми ключами, то следует брать запись с наибольшей
     * датой и временем.
     */
    @Test
    public void testGetDataRetrieveFreshest() {
        // Чтобы порядок вставки точно не влиял на результат, желанная запись будет в центре, хотя в реальности
        // так будет происходить очень редко, обычно время будет монотонно расти.
        for (int second : ImmutableList.of(1, 3, 2)) {
            clickHouseDictRepository.utcNowGetter = () -> LocalDateTime.of(2018, 1, 1, 10, 0, second);
            clickHouseDictRepository.addData(ImmutableMap.of(
                    new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123), "some name " + second));

            otherSourceClickHouseDictRepository.utcNowGetter = () -> LocalDateTime.of(2018, 1, 2, 10, 0, second);
            otherSourceClickHouseDictRepository.addData(ImmutableMap.of(
                    new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123), "some name " + second + " other source"));
        }

        softly.assertThat(clickHouseDictRepository.getData(
                ImmutableList.of(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L))))
                .describedAs("Fetched only latest value")
                .containsValue("some name 3");

        softly.assertThat(otherSourceClickHouseDictRepository.getData(
                ImmutableList.of(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L))))
                .describedAs("Fetched only latest value for other source")
                .containsValue("some name 3 other source");

        softly.assertThat(readOnlyClickHouseDictRepository.getData(
                ImmutableList.of(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L))))
                .describedAs("Fetched only latest value for both sources")
                .containsValue("some name 3 other source");
    }

    private static class MockedCurrentTimeClickHouseDictRepository extends ClickHouseDictRepository {
        Supplier<LocalDateTime> utcNowGetter = null;

        MockedCurrentTimeClickHouseDictRepository(@Nullable String source, ReadWriteDictTable dictTable) {
            super(source, dictTable);
        }

        @Override
        LocalDateTime getUtcNow() {
            return utcNowGetter == null ? super.getUtcNow() : utcNowGetter.get();
        }
    }
}
