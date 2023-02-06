package ru.yandex.market.yql_test.utils;

import java.io.InputStream;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.ITable;

import ru.yandex.market.common.test.db.SingleFileCsvDataSet;

public class YqlDbUnitUtils {

    private YqlDbUnitUtils() {
    }

    public interface CheckedSupplier<T> {
        T get() throws DataSetException;
    }

    public interface CheckedRunnable {
        void run() throws DataSetException;
    }

    public static <T> T wrapToUnchecked(CheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (DataSetException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void wrapToUnchecked(CheckedRunnable runnable) {
        try {
            runnable.run();
        } catch (DataSetException e) {
            throw new IllegalStateException(e);
        }
    }

    public static SingleFileCsvDataSet parseCsv(String csv) {
        return parseCsv(IOUtils.toInputStream(csv));
    }

    public static SingleFileCsvDataSet parseCsv(InputStream inputStream) {
        try {
            return new SingleFileCsvDataSet(inputStream);
        } catch (DataSetException e) {
            throw new IllegalStateException("exception on parsing csv", e);
        }
    }

    public static SingleFileCsvDataSet readCsvFromResources(Class<?> testClass, String csvPath) {
        return parseCsv(testClass.getResourceAsStream(csvPath));
    }

    public static void addTable(DefaultDataSet dataSet, ITable table) {
        try {
            dataSet.addTable(table);
        } catch (DataSetException e) {
            throw new IllegalStateException("can't add table to DefaultDataSet", e);
        }
    }

    public static List<String> getTableNames(SingleFileCsvDataSet dataSet) {
        try {
            return ImmutableList.copyOf(dataSet.getTableNames());
        } catch (DataSetException e) {
            throw new IllegalStateException("exception on extracting table names from csv", e);
        }
    }
}
