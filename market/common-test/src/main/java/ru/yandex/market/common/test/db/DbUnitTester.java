package ru.yandex.market.common.test.db;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;

import static java.util.Collections.singletonList;

/**
 * @author jkt on 19.04.17.
 */
public class DbUnitTester {

    private final DataSetLoader dataSetLoader;

    private final DataSetProcessor dataSetProcessor;


    public DbUnitTester(Class<?> testClass, DataSource dataSource, String schema) {
        dataSetLoader = new DataSetLoader(testClass);
        dataSetProcessor =
                new DataSetProcessor(dataSource, schema, testClass.getAnnotation(DbUnitDataBaseConfig.class));
    }

    public void insertDataSet(String dataSetName) {
        if (dataSetName == null) {
            return;
        }

        DataSetType dataSetType = DataSetType.parseFromDataSetName(dataSetName);
        insertDataSet(dataSetType, dataSetName);
    }

    public void insertDataSet(DataSetType type, String dataSetName) {

        List<IDataSet> dataSets = dataSetLoader.loadDataSets(type, singletonList(dataSetName));

        try {
            dataSetProcessor.insertDataSet(new CompositeDataSet(dataSets.toArray(new IDataSet[0])));
        } catch (DataSetException e) {
            throw new RuntimeException(String.format("Error inserting dataset %s", dataSetName), e);
        }
    }

    public void assertDataSet(String dataSetName) {
        if (dataSetName == null) {
            return;
        }

        DataSetType dataSetType = DataSetType.parseFromDataSetName(dataSetName);
        assertDataSet(dataSetType, dataSetName);
    }

    public void assertDataSet(DataSetType type, String dataSetName) {
        List<IDataSet> dataSets = dataSetLoader.loadDataSets(type, singletonList(dataSetName));

        try {
            dataSetProcessor.assertDataSet(new CompositeDataSet(dataSets.toArray(new IDataSet[0])));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error asserting dataset %s", dataSetName), e);
        }
    }

    public void truncateAllTables(String... ignored) throws SQLException {
        dataSetProcessor.truncateAllTables(Stream.of(ignored).collect(Collectors.toSet()));
    }

    public void truncateAllTables(Collection<String> ignored) throws SQLException {
        dataSetProcessor.truncateAllTables(new HashSet<>(ignored));
    }

    public void restartAllSequences(String... ignored) throws SQLException {
        dataSetProcessor.restartAllSequences(Stream.of(ignored).collect(Collectors.toSet()));
    }

    public void restartAllSequences(Collection<String> ignored) throws SQLException {
        dataSetProcessor.restartAllSequences(new HashSet<>(ignored));
    }

    public void cleanUpDb() throws SQLException {
        truncateAllTables();
        restartAllSequences();
    }

    public void cleanUpDb(Collection<String> ignoredTables, Collection<String> ignoredSequences) throws SQLException {
        truncateAllTables(ignoredTables);
        restartAllSequences(ignoredSequences);
    }
}
