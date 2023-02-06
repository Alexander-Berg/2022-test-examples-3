package ru.yandex.market.logshatter.onetime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 03/03/15
 */
public class GenerationStatsLogParser implements LogParser {
    private static final TableDescription TABLE_DESCRIPTION = TableDescription.createDefault(
        new Column("generation_id", ColumnType.String),
        new Column("total_offers", ColumnType.UInt64),
        new Column("nocateg_offers", ColumnType.UInt64)
    );

    private String generationId;
    private Long totalOffers;
    private Long noCategoryOffers;

    //20150303_0242
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");

    @Override
    public void parse(String line, ParserContext context) throws Exception {
        String[] splits = line.split("\t");

        if (generationId == null || !generationId.equals(splits[0])) {
            flush();
            generationId = splits[0];
        }

        switch (splits[1]) {
            case "total-offers":
                totalOffers = Long.parseLong(splits[2]);
                break;
            case "nocateg-offers":
                noCategoryOffers = Long.parseLong(splits[2]);
                break;

            default:
                // no-op
        }

        if (totalOffers != null && noCategoryOffers != null) {
            Date date = dateFormat.parse(generationId);
            context.write(date, generationId, totalOffers, noCategoryOffers);
            flush();
        }
    }

    private void flush() {
        generationId = null;
        totalOffers = null;
        noCategoryOffers = null;
    }

    @Override
    public TableDescription getTableDescription() {
        return TABLE_DESCRIPTION;
    }
}
