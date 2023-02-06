package ru.yandex.market.logistics.lom.jobs.executor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.service.redis.util.RedisKeys;
import ru.yandex.market.logistics.lom.service.yt.dto.YtLogisticsPoint;
import ru.yandex.market.logistics.lom.service.yt.dto.YtMigrationModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtScheduleDayById;
import ru.yandex.market.logistics.lom.service.yt.util.YtMigrationUtils;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.ROWS_COUNT;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.buildMapForTable;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.expectedEntityMap;
import static ru.yandex.market.logistics.lom.utils.jobs.executor.RedisFromYtMigrationTestUtils.expectedPath;

@DisplayName("Миграция данных yt -> redis")
@ParametersAreNonnullByDefault
@DatabaseSetup("/jobs/executor/redis-migration/internal_variables_setup.xml")
class RedisFromYtMigrationExecutorTest extends AbstractRedisTest {

    private static final String TABLES_KEYS_PATTERN = "yt-*:*";
    private static final String YT_ACTUAL_VERSION = "version3";
    private static final String REDIS_ACTUAL_VERSION = "version2";
    private static final String REDIS_PREVIOUS_VERSION = "version1";

    @Autowired
    private RedisFromYtMigrationExecutor migrationExecutor;

    @Autowired
    private LmsYtProperties lmsYtProperties;

    @Autowired
    private Yt hahnYt;

    @Autowired
    private Yt arnoldYt;

    @Autowired
    private Cypress cypress;

    @Autowired
    private YtTables ytTables;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        when(hahnYt.cypress()).thenReturn(cypress);
        when(hahnYt.tables()).thenReturn(ytTables);
        when(arnoldYt.cypress()).thenReturn(cypress);
        when(arnoldYt.tables()).thenReturn(ytTables);

        doReturn(REDIS_ACTUAL_VERSION).when(migrationJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        doReturn(REDIS_PREVIOUS_VERSION).when(migrationJedis).get(RedisKeys.REDIS_PREVIOUS_YT_VERSION_KEY);

        var availableYtTables = YtMigrationUtils.getAvailableTablesForMigration(
            lmsYtProperties,
            YT_ACTUAL_VERSION
        ).entrySet();

        for (var entry : availableYtTables) {
            mockYtReading(entry.getKey());
            mockRedisReading(RedisKeys.getHashTableFromYtName(entry.getValue(), YT_ACTUAL_VERSION));
        }
        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, YT_ACTUAL_VERSION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        verify(migrationJedis).keys(TABLES_KEYS_PATTERN);
        super.tearDown();
        verifyNoMoreInteractions(cypress, hahnYt, arnoldYt, ytTables);
    }

    @Test
    @DisplayName("Успешная миграция, удалены таблицы с неиспользуемыми версиями")
    void invalidVersionsDeleted() {
        doReturn(Set.of(
            "2021-11-08_09:01:22:logisticsPoint",
            "2021-11-08_09:15:26:logisticsPointsAgg",
            REDIS_ACTUAL_VERSION + ":scheduleDaysByScheduleId"
        ))
            .when(migrationJedis).keys(TABLES_KEYS_PATTERN);

        migrateSuccessWithDefaultProperties();

        verify(migrationJedis).keys(TABLES_KEYS_PATTERN);
        verify(migrationJedis).del("2021-11-08_09:01:22:logisticsPoint");
        verify(migrationJedis).del("2021-11-08_09:15:26:logisticsPointsAgg");
        verify(migrationJedis, never())
            .del(REDIS_ACTUAL_VERSION + ":scheduleDaysByScheduleId");
    }

    @Test
    @DisplayName("Успешная миграция")
    void migrateSuccessWithDefaultProperties() {
        migrationExecutor.doJob(null);

        verifyYtOperations(hahnYt, true);
        verifyRedisWriting(REDIS_PREVIOUS_VERSION, true);
        verifyRedisVersions(REDIS_ACTUAL_VERSION);
    }

    @Test
    @DisplayName("Миграция из арнольда")
    @DatabaseSetup(
        value = "/jobs/executor/redis-migration/switching_to_arnold_enabled.xml",
        type = DatabaseOperation.INSERT
    )
    void migrateFromArnold() {
        migrationExecutor.doJob(null);

        verifyYtOperations(arnoldYt, true);
        verifyRedisWriting(REDIS_PREVIOUS_VERSION, true);
        verifyRedisVersions(REDIS_ACTUAL_VERSION);
    }

    @Test
    @DisplayName("В redis не проставлены actual и previous версии")
    void versionsNotSetInRedis() {
        doReturn(null).when(migrationJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        doReturn(null).when(migrationJedis).get(RedisKeys.REDIS_PREVIOUS_YT_VERSION_KEY);

        migrationExecutor.doJob(null);

        verifyYtOperations(hahnYt, true);
        verifyRedisWriting(RedisKeys.REDIS_DEFAULT_VERSION, true);
        verifyRedisVersions(RedisKeys.REDIS_DEFAULT_VERSION);
    }

    @Test
    @DisplayName("В yt не найдены версии графа LMS")
    void ytVersionNotFound() {
        when(ytTables.selectRows(
            eq(String.format("version FROM [%s] ORDER BY created_at DESC LIMIT 1", lmsYtProperties.getVersionPath())),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(Optional.empty()),
            eq(true),
            eq(YTableEntryTypes.YSON),
            any(Function.class)
        ))
            .thenThrow(new IllegalStateException("Actual LMS version not found in YT"));

        softly.assertThatThrownBy(() -> migrationExecutor.doJob(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Actual LMS version not found in YT");

        verify(hahnYt).tables();
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        verify(migrationJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(migrationJedis).get(RedisKeys.REDIS_PREVIOUS_YT_VERSION_KEY);
    }

    @Test
    @DisplayName("Версии графа в YT и Redis совпали")
    void versionsAreEqual() {
        doReturn(YT_ACTUAL_VERSION).when(migrationJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);

        migrationExecutor.doJob(null);
        verify(hahnYt).tables();
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);

        verify(migrationJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(migrationJedis).get(RedisKeys.REDIS_PREVIOUS_YT_VERSION_KEY);
    }

    @Test
    @DisplayName("Количества рядов в YT и Redis не совпали")
    void rowCountsNotEqual() {
        String redisTableName = RedisKeys.getHashTableFromYtName(YtLogisticsPoint.class, YT_ACTUAL_VERSION);
        doReturn(ROWS_COUNT - 1).when(migrationJedis).hlen(redisTableName);
        softly.assertThatThrownBy(() -> migrationExecutor.doJob(null))
            .isInstanceOf(RuntimeException.class)
            .hasMessage(String.format(
                "Version %s not uploaded to Redis. "
                    + "Row counts not equal in YT and Redis for table %s. "
                    + "YT: %s, Redis: %s",
                YT_ACTUAL_VERSION,
                redisTableName,
                ROWS_COUNT,
                ROWS_COUNT - 1
            ));
        verifyYtOperations(hahnYt, true);
        verifyRedisWriting(YT_ACTUAL_VERSION, true);
        verify(migrationJedis).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(migrationJedis).get(RedisKeys.REDIS_PREVIOUS_YT_VERSION_KEY);
    }

    @Test
    @DisplayName("Отключено копирование таблицы с расписаниями")
    @DatabaseSetup("/jobs/executor/redis-migration/copy_schedule_day_by_id_disabled.xml")
    void copyScheduleDayByIdDisabled() {
        migrationExecutor.doJob(null);

        verifyYtOperations(hahnYt, false);
        verifyRedisWriting(REDIS_PREVIOUS_VERSION, false);
        verifyRedisVersions(REDIS_ACTUAL_VERSION);
    }

    @Nonnull
    private Set<String> getAvailableTables() {
        Set<String> availableYtTables = YtMigrationUtils.getAvailableTablesForMigration(
            lmsYtProperties,
            YT_ACTUAL_VERSION
        ).keySet();
        softly.assertThat(new Reflections("ru.yandex.market.logistics.lom").getSubTypesOf(YtMigrationModel.class))
            .hasSize(availableYtTables.size());
        return availableYtTables;
    }

    private void verifyYtOperations(Yt yt, boolean copyScheduleDayByIdEnabled) {
        Set<String> availableYtTables = getAvailableTables().stream()
            .filter(table ->
                copyScheduleDayByIdEnabled ||
                    !table.equals(lmsYtProperties.getDynamicScheduleDayByIdPath(YT_ACTUAL_VERSION))
            )
            .collect(Collectors.toSet());
        verify(yt, times(availableYtTables.size() + 1)).tables();
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        verifyRowCountGetting(yt, availableYtTables);
        verifyRowsReading(availableYtTables);
    }

    private void verifyRowCountGetting(Yt yt, Set<String> availableYtTables) {
        for (String tablePath : availableYtTables) {
            verify(cypress).get(YPath.simple(tablePath), List.of("row_count"));
        }
        verify(yt, times(availableYtTables.size())).cypress();
    }

    private void verifyRowsReading(Set<String> availableYtTables) {
        for (String tablePath : availableYtTables) {
            verify(ytTables).read(refEq(expectedPath(tablePath)), eq(YTableEntryTypes.YSON));
        }
    }

    private void verifyRedisWriting(@Nullable String versionToDelete, boolean copyScheduleDayByIdEnabled) {
        var tableNameClassModelMap = YtMigrationUtils.getAvailableTablesForMigration(
            lmsYtProperties,
            YT_ACTUAL_VERSION
        );

        for (var tableClass : tableNameClassModelMap.entrySet()) {
            if (versionToDelete != null) {
                verify(migrationJedis).del(RedisKeys.getHashTableFromYtName(tableClass.getValue(), versionToDelete));
            }

            if (!copyScheduleDayByIdEnabled && tableClass.getValue().equals(YtScheduleDayById.class)) {
                continue;
            }

            String hashTableName = RedisKeys.getHashTableFromYtName(tableClass.getValue(), YT_ACTUAL_VERSION);

            verify(migrationJedis).hmset(
                hashTableName,
                expectedEntityMap(lmsYtProperties, YT_ACTUAL_VERSION, tableClass.getKey(), 0, 2)
            );
            verify(migrationJedis).hlen(hashTableName);
        }
    }

    private void verifyRedisVersions(String previousVersion) {
        verify(migrationJedis, times(2)).get(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY);
        verify(migrationJedis, times(2)).get(RedisKeys.REDIS_PREVIOUS_YT_VERSION_KEY);
        verify(migrationJedis).set(RedisKeys.REDIS_ACTUAL_YT_VERSION_KEY, YT_ACTUAL_VERSION);
        verify(migrationJedis).set(RedisKeys.REDIS_PREVIOUS_YT_VERSION_KEY, previousVersion);
    }

    private void mockYtReading(String tableName) {
        when(cypress.get(refEq(YPath.simple(tableName)), refEq(List.of("row_count"))))
            .thenReturn(YtUtils.rowCountNode(ROWS_COUNT));

        mockReadingForTableWithPaths(tableName, expectedPath(tableName));
    }

    private void mockRedisReading(String tableName) {
        when(migrationJedis.hlen(tableName)).thenReturn(ROWS_COUNT);
    }

    private void mockReadingForTableWithPaths(String tableName, YPath... yPaths) {
        for (YPath path : yPaths) {
            when(ytTables.read(
                eq(path),
                eq(YTableEntryTypes.YSON)
            ))
                .then(
                    invocation -> {
                        if (YtUtils.getLowerRowIndex(invocation.getArgument(0)) >= ROWS_COUNT) {
                            return null;
                        }

                        Set<YTreeMapNode> pointNodes = new HashSet<>();
                        long lowerRowIndex = YtUtils.getLowerRowIndex(invocation.getArgument(0));
                        long upperRowIndex = Math.min(
                            YtUtils.getUpperRowIndex(invocation.getArgument(0)),
                            ROWS_COUNT
                        );

                        for (long i = lowerRowIndex; i < upperRowIndex; i++) {
                            pointNodes.add(YtUtils.buildMapNode(buildMapForTable(
                                lmsYtProperties,
                                YT_ACTUAL_VERSION,
                                tableName,
                                i
                            )));
                        }
                        return YtUtils.getIterator(pointNodes);
                    }
                );
        }
    }
}
