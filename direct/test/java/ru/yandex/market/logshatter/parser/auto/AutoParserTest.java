package ru.yandex.market.logshatter.parser.auto;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.market.clickhouse.ddl.engine.MergeTree;
import ru.yandex.market.logshatter.config.ConfigValidationException;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.config.ParserConfig;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.ParserException;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.logshatter.parser.mbo.MboGwtLogParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author amaslak
 * @date 21.12.2015
 */
public class AutoParserTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final JsonParser JSON_PARSER = new JsonParser();
    private SimpleDateFormat dateFormat = new SimpleDateFormat(MboGwtLogParser.DATE_PATTERN);

    private ConfigurationService configurationService = new ConfigurationService();

    @Test
    public void testIgnoreNoMatches() throws Exception {
        ParserConfig parserConfig = readParserConfig("/configs/regexp.json");
        assertTrue(parserConfig.isIgnoreNoMatches());
        LogParserChecker checker = getChecker(parserConfig);

        String line = "INFO   [2016-02-01 14:56:10 +0300] 25984/14021423 ta.cpp(195): root->[mark: Ford]";
        checker.checkEmpty(line);

        line = "PROFILE [2016-02-01 14:56:10 +0300] Post 123";
        checker.check(line, dateFormat.parse("2016-02-01 14:56:10,000"), checker.getHost(), 0, "Post", 123);
    }

    @Test
    public void testCustomTimestampFormat() throws Exception {
        LogParserChecker checker = getChecker("/configs/customTimestampFormat.json");

        String line = "unixtime_microsec_utc=1487564291.103815";

        checker.check(line,
            1487564291,
            UnsignedLong.valueOf(1487564291103815L)
        );
    }

    @Test
    public void testCustomTimestampFormatMillis() throws Exception {
        LogParserChecker checker = getChecker("/configs/customTimestampFormat.json");

        String line = "unixtime_microsec_utc=1487564291.103";

        checker.check(line,
            1487564291,
            UnsignedLong.valueOf(1487564291103000L)
        );
    }

    @Test
    public void testCustomTimestampFormatExtraDigits() throws Exception {
        LogParserChecker checker = getChecker("/configs/customTimestampFormat.json");

        String line = "unixtime_microsec_utc=1487564291.103815666";

        checker.check(line,
            1487564291,
            UnsignedLong.valueOf(1487564291103815L)
        );
    }

    @Test
    public void testDefaultTimestampFormat() throws Exception {
        LogParserChecker checker = getChecker("/configs/defaultTimestampFormat.json");

        String line = "unixtime_utc=1487564291";

        checker.check(line, 1487564291);
    }


    @Test
    public void testUnixtimeTskvMatch() throws Exception {
        LogParserChecker checker = getChecker("/configs/unixtimeTskvMatchConfig.json");

        checker.check(
            "tskv\tunixtime=1509173551\ttimestamp=2017-10-28 09:52:31\ttimezone=+0300\t" +
                "tskv_format=access-log-morda-ext\t" +
                "method=GET\thost=ya.ru\trequest=/\trequestid=1509173551.03359.20945.28593\taccept_encoding=gzip\t" +
                "exp_config_version=6110\tgeo_h=1\tgeo_h_region=111897\tgeo_prec=2\tgeo_region=111897\t" +
                "hostname=s2.wfront.yandex.net\thttps=0\tlocation=https://ya.ru/\tm_content=yaru\tm_language=ru\t" +
                "m_test_content=touch\tm_zone=ru\tpid=28593\tprotocol=HTTP/1.1\tredirect_type=force_https.yaru.ru\t" +
                "size=0\tstatus=302\ttemplate=y\t" +
                "timing=total=0.009\tuser_agent=Dalvik/2.1.0 (Linux; U; Android Android os V6.0; A88W Build/LRX21M)\t" +
                "vhost=m.ya.ru\twait=0.664\twait_avg=0.619\tyandexuid=4242\tyuid_days=0",
            1509173551, "ya.ru", "GET", "1509173551.03359.20945.28593", "4242");
    }

    @Test
    public void testHidesTskvMatch() throws Exception {
        LogParserChecker checker = getChecker("/configs/hideTskvMatch.json");

        checker.check("tskv\tdate=2018-02-08T11:42:45.949Z\tadf=aa\trequest_id=request_id\tmessage=request",
            Date.from(Instant.ofEpochMilli(1518090165949L)), "request_id", "request");

        checker.check("tskv\tdate=2018-02-08T11:42:45.949Z\tadf=aa\trequest_id=request_id\trequest=a\tmessage=request",
            Date.from(Instant.ofEpochMilli(1518090165949L)), "request_id", "a");
    }

    @Test
    public void testLine() throws Exception {
        LogParserChecker checker = getChecker("/configs/tskvConfig.json");

        String line = "date=21/12/2015:13:39:54" +
            "\tmethod=POST" +
            "\tsome_other_filed=value" +     // will be ignored as unspecified column
            "\tclient_ip=213.180.204.3" +
            "\tmethod=POST" +                // will be ignored as duplicate
            "\tresp_code=200" +
            "\tresptime_ms=21" +
            "\tuid=42";
        checker.check(line,
            dateFormat.parse("2015-12-21 13:39:54,000"),
            "hostname.test",
            "POST",
            "213.180.204.3",
            200,
            21,
            UnsignedLong.valueOf(42L)
        );
    }

    @Test(expected = ParserException.class)
    public void testTsvEmptyWithoutDefaults() throws Exception {
        LogParserChecker checker = getChecker("/configs/columnConfig.json");

        String line = "21/12/2015:13:39:54" +
            "\t" +
            "\t" +
            "\t" +
            "\t" +
            "\t";
        checker.check(line,
            dateFormat.parse("2015-12-21 13:39:54,000"),
            "hostname.test",
            "",
            "",
            200,
            21,
            1L
        );
    }

    @Test
    public void testColumnConfigLineTrailingEmpty() throws Exception {
        LogParserChecker checker = getChecker("/configs/columnConfig.json");

        String line = "21/12/2015:13:39:54" +
            "\tPOST" +
            "\t213.180.204.3" +
            "\t200" +
            "\t21" +
            "\t";
        checker.check(line,
            dateFormat.parse("2015-12-21 13:39:54,000"),
            "hostname.test",
            "POST",
            "213.180.204.3",
            200,
            21,
            1L
        );
    }

    @Test
    public void testRejectedLine() throws Exception {
        LogParserChecker checker = getChecker("/configs/tskvConfig.json");

        thrown.expect(ru.yandex.market.logshatter.parser.ParserException.class);
        String line = "n-date_12/21/2015:13:39:54 +03:00\tmethod=POST\tsome_other_filed=value" +
            "\tclient_ip=213.180.204.3\tmethod=POST\thttp_code=200\tresptime_ms=21\tuid=42";
        checker.checkEmpty(line);
    }

    @Test
    public void testDefaultOnEmpty() throws Exception {
        LogParserChecker checker = getChecker("/configs/testDefaultOnEmpty.json");

        String line = "tskv\tdate=2017-12-05T10:36:57.000+03\thost=blacksmith01h.market.yandex.net\tmethod=\tclient_ip=";
        checker.check(line,
            dateFormat.parse("2017-12-05 10:36:57,000"),
            "blacksmith01h.market.yandex.net",
            "DEFAULT_VALUE",
            "DEFAULT_VALUE",
            200,
            0
        );
    }

    @Test
    public void defaultEngine() throws Exception {
        assertEquals(
            TableDescription.DEFAULT_ENGINE,
            new AutoParser(readParserConfig("/configs/engineDefault.json"))
                .getTableDescription().getEngine()
        );
    }

    @Test
    public void engineWithDefaultFields() throws Exception {
        assertEquals(
            TableDescription.DEFAULT_ENGINE,
            new AutoParser(readParserConfig("/configs/engineDefaultFields.json"))
                .getTableDescription().getEngine()
        );
    }

    @Test
    public void customEngine() throws Exception {
        assertEquals(
            new MergeTree(
                "partitionBy",
                Arrays.asList("sampleBy", "orderBy1", "orderBy2"),
                "sampleBy",
                TableDescription.DEFAULT_ENGINE.getIndexGranularity()
            ),
            new AutoParser(readParserConfig("/configs/engineCustom.json"))
                .getTableDescription().getEngine()
        );
    }

    private LogParserChecker getChecker(String configFilePath) throws IOException, ConfigValidationException {
        ParserConfig parserConfig = readParserConfig(configFilePath);
        return getChecker(parserConfig);
    }

    private LogParserChecker getChecker(ParserConfig parserConfig) {
        return new LogParserChecker(new AutoParser(parserConfig));
    }

    private ParserConfig readParserConfig(String configFilePath) throws IOException, ConfigValidationException {
        JsonObject configObject;
        String configData;
        try (InputStreamReader confReader = new InputStreamReader(getClass().getResourceAsStream(configFilePath))) {
            configData = IOUtils.toString(confReader);
        }

        configurationService.validateJsonSchema(configData);
        configObject = JSON_PARSER.parse(configData).getAsJsonObject();

        return ConfigurationService.getParserConfig(configObject);
    }
}
