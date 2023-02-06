package ru.yandex.market.common.test.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.dbunit.Assertion;
import org.dbunit.IOperationListener;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.ColumnFilterTable;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.LowerCaseTableMetaData;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.common.test.db.ddl.DDLDatabaseTester;
import ru.yandex.market.common.test.db.ddl.DDLHelper;
import ru.yandex.market.common.test.db.ddl.DDLHelperRegistry;
import ru.yandex.market.common.test.util.ITablePrettyGenerator;

/**
 * @author jkt on 13.04.17.
 */
class DataSetProcessor {
    private static final Logger log = LoggerFactory.getLogger(DataSetProcessor.class);

    private final DataSource dataSource;
    private final DDLDatabaseTester databaseTester;

    DataSetProcessor(DataSource dataSource, String schema, DbUnitDataBaseConfig config) {
        this.dataSource = dataSource;
        try (Connection c = dataSource.getConnection()) {
            DDLHelper ddl = DDLHelperRegistry.getHelper(c);
            this.databaseTester = ddl.makeTester(dataSource, schema, config);
            this.databaseTester.setOperationListener(new DatasourceInjectOperationListener(dataSource));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void applyDataSetOnSetUp(IDataSet dataSet) {
        try {
            databaseTester.setDataSet(dataSet);
            databaseTester.onSetup();
        } catch (Exception e) {
            throw new RuntimeException("Error applying annotated DbUnit data set to db on set up: " + e, e);
        }
    }

    void insertDataSet(IDataSet dataSet) {
        applyDataSetOnSetUp(dataSet);
    }

    void assertDataSet(IDataSet expectedDataSet) throws Exception {
        IDatabaseConnection connection = databaseTester.getConnection();
        IOperationListener operationListener = databaseTester.getOperationListener();
        if (operationListener != null) {
            operationListener.connectionRetrieved(connection);
        }
        try {
            IDataSet actualDataSet = connection.createDataSet();

            // Сравниваем только таблицы, присутствующие в after-xml/csv
            for (String tableName : expectedDataSet.getTableNames()) {
                ITable expectedTable = expectedDataSet.getTable(tableName);
                ITable actualTable = actualDataSet.getTable(tableName);
                if (!expectedDataSet.isCaseSensitiveTableNames() && !actualDataSet.isCaseSensitiveTableNames()) {
                    // имена колонок могут быть в разном регистре в expected и actual, поэтому оборачиваем
                    expectedTable = new LowerCaseTable(expectedTable);
                    actualTable = new LowerCaseTable(actualTable);
                }

                // Сравниваем только столбцы, присутствующие в after-xml/csv
                Set<String> actualColumnNames = Stream.of(actualTable.getTableMetaData().getColumns())
                        .map(Column::getColumnName)
                        .collect(Collectors.toCollection(HashSet::new));
                Set<String> expectedColumnNames = Stream.of(expectedTable.getTableMetaData().getColumns())
                        .map(Column::getColumnName)
                        .collect(Collectors.toCollection(LinkedHashSet::new)); // для предсказуемой сортировки колонок

                // Оставляем в множестве только те, которые надо игнорировать
                int actualColumnCount = actualColumnNames.size();
                actualColumnNames.removeAll(expectedColumnNames);
                if (actualColumnNames.size() == actualColumnCount) {
                    throw new IllegalStateException(
                            "All columns are ignored. Nothing to compare." +
                                    " TABLE_NAME: " + tableName +
                                    " EXPECTED_COLUMNS: " + expectedColumnNames +
                                    " ACTUAL_COLUMNS: " + actualColumnNames
                    );
                }

                // Используем общий фильтр для таблиц
                DefaultColumnFilter ignoredColumns = new DefaultColumnFilter();
                for (String columnName : actualColumnNames) {
                    ignoredColumns.excludeColumn(columnName);
                }
                String[] expectedColumns = expectedColumnNames.toArray(new String[0]);
                expectedTable = new SortedTable(new ColumnFilterTable(expectedTable, ignoredColumns), expectedColumns);
                actualTable = new SortedTable(new ColumnFilterTable(actualTable, ignoredColumns), expectedColumns);
                try {
                    Assertion.assertEquals(expectedTable, actualTable);
                } catch (AssertionError ex) {
                    log.error(
                            "Actual data diverged from expected data. Actual {} table content is: \n{}",
                            actualTable.getTableMetaData().getTableName(),
                            ITablePrettyGenerator.generate(actualTable)
                    );
                    throw ex;
                }
            }
        } finally {
            connection.close();
        }
    }

    void truncateAllTables(Set<String> ignore) throws SQLException {
        databaseTester.getDDL().truncateAllTables(dataSource, ignore);
    }

    void restartAllSequences(Set<String> ignore) throws SQLException {
        databaseTester.getDDL().restartAllSequences(dataSource, ignore);
    }

    void refreshAllMatViews(Set<String> ignore) throws SQLException {
        databaseTester.getDDL().refreshAllMatViews(dataSource, ignore);
    }

    private static final class LowerCaseTable implements ITable {
        private final ITable decoratee;

        LowerCaseTable(ITable decoratee) {
            this.decoratee = decoratee;
        }

        @Override
        public ITableMetaData getTableMetaData() {
            try {
                return new LowerCaseTableMetaData(decoratee.getTableMetaData());
            } catch (DataSetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int getRowCount() {
            return decoratee.getRowCount();
        }

        @Override
        public Object getValue(int row, String column) throws DataSetException {
            return decoratee.getValue(row, column);
        }
    }
}
