package ru.yandex.market.yql_test.checker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.DataType;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.yql_test.YqlTablePathConverter;
import ru.yandex.market.yql_test.utils.YqlDbUnitUtils;

import static com.google.common.base.Preconditions.checkState;
import static ru.yandex.market.yql_test.checker.UncheckedDefaultTableFacade.uncheckedDefaultTableFacade;

public class YtCheckerDataLoader {

    private final Yt yt;
    private final YqlTablePathConverter yqlTablePathConverter;

    public YtCheckerDataLoader(Yt yt, YqlTablePathConverter yqlTablePathConverter) {
        this.yt = yt;
        this.yqlTablePathConverter = yqlTablePathConverter;
    }

    public IDataSet getActualDataFromYt(Collection<String> expectedTablePaths) {
        DefaultDataSet dataSet = new DefaultDataSet();

        for (String tablePath : expectedTablePaths) {
            YPath testTablePath = yqlTablePathConverter.toTestPath(tablePath);

            List<JsonNode> jsonNodeList = new ArrayList<>();
            yt.tables().read(testTablePath, YTableEntryTypes.JACKSON_UTF8,
                    (Consumer<JsonNode>) jsonNodeList::add);

            checkState(!jsonNodeList.isEmpty(),
                    "expected table `%s` in YT is empty, can't construct model for comparing data", testTablePath);

            UncheckedDefaultTableFacade table = null;
            for (int row = 0; row < jsonNodeList.size(); row++) {
                ObjectNode jsonRow = (ObjectNode) jsonNodeList.get(row);
                if (table == null) {
                    table = uncheckedDefaultTableFacade(new DefaultTable(tablePath, extractColumns(jsonRow)));
                }
                table.addRow();

                final UncheckedDefaultTableFacade fTable = table;
                final int fRow = row;

                jsonRow.fieldNames().forEachRemaining(
                        fieldName -> fTable.setValue(fRow, fieldName, jsonRow.get(fieldName).asText()));
            }

            YqlDbUnitUtils.addTable(dataSet, table.getOriginalTable());
        }

        return dataSet;
    }

    private Column[] extractColumns(ObjectNode jsonRow) {
        List<Column> columns = new ArrayList<>();
        jsonRow.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            Column column = new Column(name, DataType.UNKNOWN);
            columns.add(column);
        });
        return columns.toArray(new Column[0]);
    }
}
