package ru.yandex.market.yql_test.checker;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.ITableMetaData;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.yandex.market.yql_test.utils.YqlDbUnitUtils.wrapToUnchecked;

public class UncheckedTableMetaDataFacade implements ITableMetaData {

    private final ITableMetaData tableMetaData;

    public UncheckedTableMetaDataFacade(ITableMetaData tableMetaData) {
        this.tableMetaData = tableMetaData;
    }

    public static UncheckedTableMetaDataFacade uncheckedTableMetaDataFacade(
            ITableMetaData tableMetaData) {
        checkNotNull(tableMetaData, "tableMetaData");
        return new UncheckedTableMetaDataFacade(tableMetaData);
    }

    @Override
    public String getTableName() {
        return tableMetaData.getTableName();
    }

    @Override
    public Column[] getColumns() {
        return wrapToUnchecked(tableMetaData::getColumns);
    }

    @Override
    public Column[] getPrimaryKeys() {
        return wrapToUnchecked(tableMetaData::getPrimaryKeys);
    }

    @Override
    public int getColumnIndex(String columnName) {
        return wrapToUnchecked(() -> tableMetaData.getColumnIndex(columnName));
    }
}
