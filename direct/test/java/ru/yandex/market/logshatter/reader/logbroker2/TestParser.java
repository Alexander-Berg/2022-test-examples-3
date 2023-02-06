package ru.yandex.market.logshatter.reader.logbroker2;

import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;

import java.util.Date;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.12.2018
 */
public class TestParser implements LogParser {
    private static final TableDescription TABLE_DESCRIPTION = TableDescription.createDefault(
        new Column("data", ColumnType.Int64)
    );

    @Override
    public TableDescription getTableDescription() {
        return TABLE_DESCRIPTION;
    }

    @Override
    public void parse(String line, ParserContext context) {
        context.write(new Date(), Integer.parseInt(line));
    }
}
