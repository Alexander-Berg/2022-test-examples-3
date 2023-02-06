package ru.yandex.market.yql_test.checker;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;

import ru.yandex.market.yql_test.utils.YqlDbUnitUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.yandex.market.yql_test.checker.UncheckedTableFacade.uncheckedTableFacade;
import static ru.yandex.market.yql_test.checker.UncheckedTableMetaDataFacade.uncheckedTableMetaDataFacade;
import static ru.yandex.market.yql_test.utils.YqlDbUnitUtils.wrapToUnchecked;

public class UncheckedDataSetFacade implements IDataSet {

    private final IDataSet dataSet;

    public UncheckedDataSetFacade(IDataSet dataSet) {
        this.dataSet = dataSet;
    }

    public static UncheckedDataSetFacade uncheckedDataSetFacade(IDataSet dataSet) {
        checkNotNull(dataSet, "dataSet");
        return new UncheckedDataSetFacade(dataSet);
    }

    @Override
    public String[] getTableNames() {
        return wrapToUnchecked(dataSet::getTableNames);
    }

    @Override
    public UncheckedTableMetaDataFacade getTableMetaData(String tableName) {
        return wrapToUnchecked(() -> uncheckedTableMetaDataFacade(dataSet.getTableMetaData(tableName)));
    }

    @Override
    public UncheckedTableFacade getTable(String tableName) {
        return wrapToUnchecked(() -> uncheckedTableFacade(dataSet.getTable(tableName)));
    }

    @Override
    public UncheckedTableFacade[] getTables() {
        YqlDbUnitUtils.CheckedSupplier<UncheckedTableFacade[]> supplier = () -> {
            ITable[] tables = dataSet.getTables();
            for (int i = 0; i < tables.length; i++) {
                tables[i] = uncheckedTableFacade(tables[i]);
            }
            return (UncheckedTableFacade[]) tables;
        };
        return wrapToUnchecked(supplier);
    }

    @Override
    public ITableIterator iterator() {
        return wrapToUnchecked(dataSet::iterator);
    }

    @Override
    public ITableIterator reverseIterator() {
        return wrapToUnchecked(dataSet::reverseIterator);
    }

    @Override
    public boolean isCaseSensitiveTableNames() {
        return dataSet.isCaseSensitiveTableNames();
    }
}
