package ru.yandex.market.fulfillment.stockstorage.service.stocks;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;

public class StockServiceColumnsTest extends AbstractContextualTest {

    @Autowired
    private DataSource dataSource;

    /**
     * Проверяет были ли добавлены/удалены колонки в БД
     * Информация о колонках для сравнения с БД указывается вручную
     */
    @Test
    @CleanDatabase
    public void checkTablesFromFile() throws SQLException, DatabaseUnitException {

        // Датасет с таблицами для проверки
        IDataSet expected = new FlatXmlDataSetBuilder()
                .build(new StringReader(
                        extractFileContent("database/expected/large_tables_check/large_tables_structure.xml")
                ));

        DatabaseConnection connection = new DatabaseConnection(dataSource.getConnection());

        ITableIterator iterator = expected.iterator();
        while (iterator.next()) {
            ITableMetaData table = iterator.getTableMetaData();

            // Получение датасета из таблицы в БД
            IDataSet actual = connection.createDataSet(new String[] {table.getTableName()});
            ITableMetaData actualMetadata = actual.getTableMetaData(table.getTableName());
            List<String> actualColumns = Stream.of(actualMetadata.getColumns())
                    .map(Column::getColumnName)
                    .collect(Collectors.toList());

            List<String> expectedColumns = Stream.of(table.getColumns())
                    .map(Column::getColumnName)
                    .collect(Collectors.toList());

            Assertions.assertEquals(expectedColumns, actualColumns);
        }
    }



}
