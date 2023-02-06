package ru.yandex.market.yql_test.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.IDataSet;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.yql_test.checker.UncheckedDataSetFacade.uncheckedDataSetFacade;
import static ru.yandex.market.yql_test.checker.UncheckedTableFacade.uncheckedTableFacade;
import static ru.yandex.market.yql_test.checker.UncheckedTableMetaDataFacade.uncheckedTableMetaDataFacade;

public class DbUnitCsvWriter {

    public String toCsv(IDataSet iDataSet) {
        StringBuilder csvBuilder = new StringBuilder();

        UncheckedDataSetFacade dataSet = uncheckedDataSetFacade(iDataSet);

        Stream.of(dataSet.getTableNames()).forEach(tableName -> {
            csvBuilder.append(tableName).append("\n");

            UncheckedTableFacade table = uncheckedTableFacade(dataSet.getTable(tableName));
            UncheckedTableMetaDataFacade metaData = uncheckedTableMetaDataFacade(table.getTableMetaData());
            List<String> columnNames = Stream.of(metaData.getColumns()).map(Column::getColumnName).collect(toList());

            appendColumnNames(csvBuilder, columnNames);
            appendRowValues(csvBuilder, table, columnNames);

            csvBuilder.append("\n");
        });

        return csvBuilder.toString();
    }

    private void appendColumnNames(StringBuilder csvBuilder, List<String> columnNames) {
        csvBuilder.append(StringUtils.join(columnNames, ",")).append("\n");
    }

    private void appendRowValues(StringBuilder csvBuilder, UncheckedTableFacade table, List<String> columnNames) {
        for (int rowNum = 0; rowNum < table.getRowCount(); rowNum++) {
            List<Object> values = new ArrayList<>();
            for (String columnName : columnNames) {
                values.add(table.getValue(rowNum, columnName));
            }
            csvBuilder
                    .append("\"")
                    .append(StringUtils.join(values, "\",\""))
                    .append("\"")
                    .append("\n");
        }
    }
}
