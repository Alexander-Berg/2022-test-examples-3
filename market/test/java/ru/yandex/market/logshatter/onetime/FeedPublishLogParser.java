package ru.yandex.market.logshatter.onetime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 03/03/15
 */
public class FeedPublishLogParser implements LogParser {
    private static final TableDescription TABLE_DESCRIPTION = TableDescription.createDefault(
        new Column("generationId", ColumnType.String),
        new Column("feedId", ColumnType.UInt32),
        new Column("offersCount", ColumnType.UInt64),
        new Column("start", ColumnType.DateTime),
        new Column("download", ColumnType.DateTime),
        new Column("publish", ColumnType.DateTime),
        new Column("status", ColumnType.String)
    );
    //20150303_0242
    private final DateFormat generationIdFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
    //2015-02-26 18:08:24
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    private static final Map<String, Date> SESSION_ID_TO_PUBLISH_DATE = new HashMap<>();

    //TODO АААааааа..... в  проде так конечно нельзя
    static {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            BufferedReader reader = new BufferedReader(new FileReader("/Users/andreevdm/tmp/kpi/data/GENERATIONS"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splits = line.split("\t");
                SESSION_ID_TO_PUBLISH_DATE.put(splits[0], dateFormat.parse(splits[1]));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String generationId;
    private Date publish;

    private Integer feedId;
    private Date start;
    private Date download;
    private Long offersCount;
    private String status;
//
//    @Override
//    public void init(ParserContext context) {
//        Path file = context.getFile();
//        generationId = file.getName(file.getNameCount() - 1).toString().replace("feed_log_", "");
//        publish = sessionIdToPublishDate.get(generationId);
//    }

    /*
    394429  download-date  2015-01-03 19:07:12 (optional)
    394429  indexed-status  ok
    394429  offers-count  5621
    394429  start-date  2015-01-03 19:07:11
     */
    @Override
    public void parse(String line, ParserContext context) throws Exception {
        if (publish == null) {
            return;
        }

        String[] splits = line.split("\t");

        Integer newFeedId = Integer.valueOf(splits[0]);
        if (feedId == null || !feedId.equals(newFeedId)) {
            flush(context);
            feedId = newFeedId;
        }
        String value = splits[2];
        switch (splits[1]) {
            case "start-date":
                start = dateFormat.parse(value);
                break;
            case "download-date":
                download = dateFormat.parse(value);
                break;
            case "offers-count":
                offersCount = Long.parseLong(value);
                break;
            case "indexed-status":
                status = value;
                break;

            default:
                // no-op
        }

        if (feedId != null && start != null && offersCount != null && status != null) {
            flush(context);
        }
    }

    private void flush(ParserContext context) throws Exception {
        if (feedId != null && start != null && offersCount != null && status != null) {
            Date date = generationIdFormat.parse(generationId);
            context.write(date, generationId, feedId, offersCount, start, download, publish, status);
        }

        feedId = null;
        download = new Date(0);
        start = null;
        offersCount = null;
        status = null;
    }

    @Override
    public TableDescription getTableDescription() {
        return TABLE_DESCRIPTION;
    }
}
