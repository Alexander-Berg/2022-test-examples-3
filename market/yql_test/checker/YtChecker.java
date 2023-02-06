package ru.yandex.market.yql_test.checker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.ColumnFilterTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.filter.IColumnFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.common.test.db.SingleFileCsvDataSet;
import ru.yandex.market.common.test.util.ITablePrettyGenerator;
import ru.yandex.market.yql_test.YqlTablePathConverter;
import ru.yandex.market.yql_test.cache.CachedYtData;
import ru.yandex.market.yql_test.cache.YqlCache;
import ru.yandex.market.yql_test.proxy.YqlCachingServletListener;
import ru.yandex.market.yql_test.utils.YqlDbUnitUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static ru.yandex.market.yql_test.checker.UncheckedDataSetFacade.uncheckedDataSetFacade;
import static ru.yandex.market.yql_test.utils.YqlDbUnitUtils.wrapToUnchecked;

/**
 * Несколько слов о кэшировании состояния таблиц в YT после выполнения теста.
 * На конечное состояние таблиц в YT после выполнения теста влияют:
 * - схемы исходных таблиц
 * - содержимое всех исходных таблиц
 * - содержимое каждого YQL-запроса (с учетом порядка запросов)
 * <p>
 * Если ни один из вышеперечисленных факторов не изменился, значит, можно считать,
 * что и состояние таблиц после выполнения теста не изменится. Таким образом,
 * ключ кэша должен учитывать все эти факторы.
 * <p>
 * Кроме того, на возможность проверки по кэшу влияет набор проверяемых таблиц,
 * и если он изменился, то заново перезабираем данные из YT. Эту логику можно сделать
 * удобнее: если количество проверяемых таблиц стало меньше, то брать ответ из кэша.
 */
public class YtChecker {

    private static final Logger logger = LoggerFactory.getLogger(YtChecker.class);

    private final YtCheckerSettings settings;
    private final YqlCache cache;

    private final SingleFileCsvDataSet expectedDataSet;
    private final List<String> expectedTableNames;

    private final YtCheckerDataLoader ytCheckerDataLoader;
    private final YqlRequestRegistry yqlRequestRegistry;

    public YtChecker(YtCheckerSettings settings,
                     YqlCache cache,
                     Yt yt,
                     YqlTablePathConverter yqlTablePathConverter) {
        this.settings = settings;
        this.cache = cache;

        this.expectedDataSet = YqlDbUnitUtils.parseCsv(settings.getExpectedCsvContent());
        this.expectedTableNames = YqlDbUnitUtils.getTableNames(expectedDataSet);

        this.ytCheckerDataLoader = new YtCheckerDataLoader(yt, yqlTablePathConverter);
        this.yqlRequestRegistry = new YqlRequestRegistry(yqlTablePathConverter);
    }

    public YqlCachingServletListener getListener() {
        return yqlRequestRegistry;
    }

    public boolean isCacheMatched() {
        CachedYtData cachedYtData = cache.getCachedYtData();
        return computeCurrentCacheKey().equals(cachedYtData.getCacheKey());
    }

    public void checkYtTables() {
        CachedYtData cachedYtData = cache.getCachedYtData();
        String currentCacheKey = computeCurrentCacheKey();

        IDataSet actualDataSet;
        if (currentCacheKey.equals(cachedYtData.getCacheKey())) {
            actualDataSet = cachedYtData.getDataSet();
        } else {
            actualDataSet = ytCheckerDataLoader.getActualDataFromYt(expectedTableNames);
            putActualDataSetToCache(currentCacheKey, actualDataSet);
        }

        assertDataMatches(actualDataSet);
    }

    private String computeCurrentCacheKey() {
        SortedMap<String, String> sortedSchemas = new TreeMap<>(settings.getSchemas());
        List<String> sortedTableNames = expectedTableNames.stream().sorted().collect(toList());
        return String.valueOf(Objects.hash(
                sortedSchemas,
                settings.getInitialCsvContent(),
                yqlRequestRegistry.requests,
                sortedTableNames));
    }

    private void putActualDataSetToCache(String cacheKey, IDataSet actualData) {
        CachedYtData newCache = CachedYtData.fromDataSet(cacheKey, actualData);
        cache.setCachedYtData(newCache);
    }

    private void assertDataMatches(IDataSet actualDataSet) {
        UncheckedDataSetFacade actualDataSetFacade = uncheckedDataSetFacade(actualDataSet);
        UncheckedDataSetFacade expectedDataSetFacade = uncheckedDataSetFacade(expectedDataSet);

        for (String tableName : expectedDataSetFacade.getTableNames()) {
            UncheckedTableFacade expectedTable = expectedDataSetFacade.getTable(tableName);
            UncheckedTableFacade actualTable = actualDataSetFacade.getTable(tableName);

            Set<String> actualColumnNames = Stream.of(actualTable.getTableMetaData().getColumns())
                    .map(Column::getColumnName)
                    .collect(Collectors.toCollection(HashSet::new));
            Set<String> expectedColumnNames = Stream.of(expectedTable.getTableMetaData().getColumns())
                    .map(Column::getColumnName)
                    .collect(Collectors.toCollection(HashSet::new));

            checkState(!Sets.intersection(actualColumnNames, expectedColumnNames).isEmpty(),
                    "All columns are ignored. Nothing to compare." +
                            " TABLE_NAME: " + tableName +
                            " EXPECTED_COLUMNS: " + expectedColumnNames +
                            " ACTUAL_COLUMNS: " + actualColumnNames);

            Set<String> ignoredActualColumns = Sets.difference(actualColumnNames, expectedColumnNames);
            DefaultColumnFilter ignoredColumns = new DefaultColumnFilter();
            ignoredActualColumns.forEach(ignoredColumns::excludeColumn);

            ColumnFilterTable filteredExpectedTable = columnFilterTable(expectedTable, ignoredColumns);
            ColumnFilterTable filteredActualTable = columnFilterTable(actualTable, ignoredColumns);
            try {
                Assertion.assertEquals(filteredExpectedTable, filteredActualTable);
            } catch (AssertionError ex) {
                logger.error(
                        "Actual data differs from expected data. Actual table ({}) content is: \n{}",
                        actualTable.getTableMetaData().getTableName(),
                        ITablePrettyGenerator.generate(actualTable)
                );
                throw ex;
            } catch (DatabaseUnitException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private ColumnFilterTable columnFilterTable(ITable table, IColumnFilter filter) {
        return wrapToUnchecked(() -> new ColumnFilterTable(table, filter));
    }

    private static class YqlRequestRegistry implements YqlCachingServletListener {

        private final YqlTablePathConverter yqlTablePathConverter;
        private final List<String> requests = new ArrayList<>();

        YqlRequestRegistry(YqlTablePathConverter yqlTablePathConverter) {
            this.yqlTablePathConverter = yqlTablePathConverter;
        }

        @Override
        public void yqlRequestSent(String request) {
            requests.add(yqlTablePathConverter.convertTestPathToNormal(request));
        }
    }
}
