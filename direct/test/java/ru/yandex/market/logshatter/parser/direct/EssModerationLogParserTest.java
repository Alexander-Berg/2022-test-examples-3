package ru.yandex.market.logshatter.parser.direct;

import com.google.gson.JsonObject;
import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.direct.logformat.EssModerationFormat;

public class EssModerationLogParserTest {
    private EssModerationLogParser parser = new EssModerationLogParser();
    private LogParserChecker checker = new LogParserChecker(parser);

    @Test
    public void testParse_AllFields() throws Exception {
        EssModerationFormat essModerationFormat = getFullModerationFormat();

        String line = "{'log_time':'" + essModerationFormat.getLogTime() + "'," +
            "'cid':" + essModerationFormat.getCid() + "," +
            "'pid':" + essModerationFormat.getPid() + "," +
            "'bid':" + essModerationFormat.getBid() + "," +
            "'span_id':" + essModerationFormat.getSpanId() + "," +
            "'action':'" + essModerationFormat.getAction() + "'," +
            "'source':'" + essModerationFormat.getSource() + "'," +
            "'success':" + essModerationFormat.isSuccess() + "," +
            "'data':" + essModerationFormat.getData().toString() + "}";

        checker.check(line,
            parser.dateTimeFormat.parse(essModerationFormat.getLogTime()),
            essModerationFormat.getCid(),
            essModerationFormat.getPid(),
            essModerationFormat.getBid(),
            essModerationFormat.getSpanId(),
            essModerationFormat.getAction(),
            essModerationFormat.getSource(),
            essModerationFormat.isSuccess(),
            essModerationFormat.getData());
    }

    @Test
    public void testParse_OptionalFields() throws Exception {
        EssModerationFormat essModerationFormat = getFullModerationFormat();
        essModerationFormat.setAction(null);
        essModerationFormat.setSource(null);

        String line = "{'log_time':'" + essModerationFormat.getLogTime() + "'," +
            "'cid':" + essModerationFormat.getCid() + "," +
            "'pid':" + essModerationFormat.getPid() + "," +
            "'bid':" + essModerationFormat.getBid() + "," +
            "'span_id':" + essModerationFormat.getSpanId() + "," +
            "'action':" + essModerationFormat.getAction() + "," +
            "'source':" + essModerationFormat.getSource() + "," +
            "'success':" + essModerationFormat.isSuccess() + "," +
            "'data':" + essModerationFormat.getData().toString() + "}";

        checker.check(line,
            parser.dateTimeFormat.parse(essModerationFormat.getLogTime()),
            essModerationFormat.getCid(),
            essModerationFormat.getPid(),
            essModerationFormat.getBid(),
            essModerationFormat.getSpanId(),
            "",
            "",
            essModerationFormat.isSuccess(),
            essModerationFormat.getData());
    }

    private EssModerationFormat getFullModerationFormat() {
        EssModerationFormat essModerationFormat = new EssModerationFormat();
        essModerationFormat.setLogTime("2019-08-26 12:13:14");
        essModerationFormat.setCid(1234L);
        essModerationFormat.setPid(567L);
        essModerationFormat.setBid(88745L);
        essModerationFormat.setSpanId(11223344L);
        essModerationFormat.setAction("RESPONSE");
        essModerationFormat.setSource("ERROR");
        essModerationFormat.setSuccess(true);

        JsonObject data = new JsonObject();
        data.addProperty("field1", "value1");
        data.addProperty("field2", 5);
        essModerationFormat.setData(data);
        return essModerationFormat;
    }
}
