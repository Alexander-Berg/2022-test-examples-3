package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Collections.singletonList;

public class PpcCmdLogParserTest {
    private LogParserChecker checker = new LogParserChecker(new PpcCmdLogParser());
    private SimpleDateFormat dateFormat = new SimpleDateFormat(PpcCmdLogParser.DATE_PATTERN);

    @Test
    public void testParseWebApiPpcLogCmd() throws Exception {
        String inputLine =
            "2018-09-30 00:00:04 {\"cluid\":614864912,"
                + "\"logtime\":\"20180930000004\","
                + "\"reqid\":\"1409848943083994849\","
                + "\"trace_id\":1409848943083994849,"
                + "\"ip\":\"31.135.91.28\","
                + "\"host\":\"sas2-0030-sas-ppc-direct-java-web-16791.gencfg-c.yandex.net\","
                + "\"cmd\":\"grid.api\","
                + "\"uid\":614864912,"
                + "\"cids\":null,"
                + "\"pids\":null,"
                + "\"bids\":null,"
                + "\"http_status\":403,"
                + "\"runtime\":0.056288243,"
                + "\"cpu_user_time\":0.010188148,"
                + "\"response\":null,"
                + "\"role\":\"CLIENT\","
                + "\"yandexuid\":\"5233804201505326951\","
                + "\"service\":\"direct.java.webapi\","
                + "\"tvm_service_id\":null,"
                + "\"param\":{\"ulogin\":\"elama-16218961\"}}";

        Date date = dateFormat.parse("2018-09-30 00:00:04");
        Object[] row = new Object[]{
            new ArrayList<Long>(),
            new ArrayList<Long>(),
            new ArrayList<Long>(),
            "31.135.91.28",
            "grid.api",
            0.056288242F,
            "direct.java.webapi",
            "{\"ulogin\":\"elama-16218961\"}",
            "",
            403,
            new ArrayList<Long>(Arrays.asList(Long.valueOf(614864912L))),
            new ArrayList<UnsignedLong>(),
            UnsignedLong.valueOf(1409848943083994849L),
            1409848943083994849L,
            614864912L,
            "sas2-0030-sas-ppc-direct-java-web-16791.gencfg-c.yandex.net",
            0L,
            0F,
            0.010188148F,
            "5233804201505326951",
            "CLIENT",
            "",
            0,
        };

        checker.check(inputLine,
            singletonList(date),
            singletonList(row)
        );
    }

    @Test
    public void testParsePpcLogIntapi() throws Exception {
        String inputLine =
            "2018-09-29 00:00:03 {\"logtime\":\"20180929000003\","
                + "\"reqid\":\"3356961850432728774\","
                + "\"trace_id\":3356960149625685935,"
                + "\"ip\":\"2a02:6b8:c11:ca2:10d:6b3f:0:7d69\","
                + "\"host\":\"sas2-0301-sas-ppc-java-intapi-13904.gencfg-c.yandex.net\","
                + "\"cmd\":\"statistic.phrase.get\","
                + "\"uid\":null,"
                + "\"cids\":null,"
                + "\"pids\":null,"
                + "\"bids\":null,"
                + "\"http_status\":200,"
                + "\"runtime\":0.101776367,"
                + "\"cpu_user_time\":0.010256038,"
                + "\"response\":null,"
                + "\"client_id\":null,"
                + "\"service\":\"direct.java.intapi\","
                + "\"tvm_service_id\":2009921,"
                + "\"param\":{\"_json\":{\"interval_days\":28,\"selection_criteria\":[{\"ad_group_id\":\"3480346164\","
                + "\"campaign_id\":\"36147491\",\"phrase_ids\":[\"14323540928\",\"14323165204\",\"14323165203\","
                + "\"14323447666\"]},{\"ad_group_id\":\"3474312891\",\"campaign_id\":\"36147491\",\"phrase_ids\":"
                + "[\"14323625895\",\"14281001848\",\"14281001840\",\"14323625899\",\"14281001847\",\"14281001860\","
                + "\"14323625892\",\"14281001864\",\"14323625894\",\"14323625897\",\"14281001853\",\"14323625896\","
                + "\"14281001836\",\"14323625898\",\"14281001843\"]},{\"campaign_id\":\"36147491\",\"ad_group_id\":"
                + "\"3480510200\",\"phrase_ids\":[\"14324060267\",\"14324060271\",\"14324060270\",\"14324060273\","
                + "\"14324060275\",\"14324060277\",\"14324060279\",\"14324060272\",\"14324060274\",\"14324060269\","
                + "\"14324060278\",\"14324060276\",\"14324060268\",\"14324060266\"]},{\"ad_group_id\":\"3415119867\","
                + "\"campaign_id\":\"36147491\",\"phrase_ids\":[\"13852582793\",\"13852582803\",\"13852582796\","
                + "\"13852582798\",\"13852582802\",\"13852582804\",\"13852582795\",\"13852582805\",\"13852582800\","
                + "\"13852582799\",\"13852582797\",\"13852582792\",\"13852582801\",\"13852582806\"]}]}}}";

        Date date = dateFormat.parse("2018-09-29 00:00:03");
        Object[] row = new Object[]{
            new ArrayList<Long>(),
            new ArrayList<Long>(),
            new ArrayList<Long>(),
            "2a02:6b8:c11:ca2:10d:6b3f:0:7d69",
            "statistic.phrase.get",
            0.101776367F,
            "direct.java.intapi",
            "{\"_json\":{\"interval_days\":28,\"selection_criteria\":[{\"ad_group_id\":\"3480346164\","
                + "\"campaign_id\":\"36147491\",\"phrase_ids\":[\"14323540928\",\"14323165204\",\"14323165203\","
                + "\"14323447666\"]},{\"ad_group_id\":\"3474312891\",\"campaign_id\":\"36147491\",\"phrase_ids\":"
                + "[\"14323625895\",\"14281001848\",\"14281001840\",\"14323625899\",\"14281001847\",\"14281001860\","
                + "\"14323625892\",\"14281001864\",\"14323625894\",\"14323625897\",\"14281001853\",\"14323625896\","
                + "\"14281001836\",\"14323625898\",\"14281001843\"]},{\"campaign_id\":\"36147491\",\"ad_group_id\":"
                + "\"3480510200\",\"phrase_ids\":[\"14324060267\",\"14324060271\",\"14324060270\",\"14324060273\","
                + "\"14324060275\",\"14324060277\",\"14324060279\",\"14324060272\",\"14324060274\",\"14324060269\","
                + "\"14324060278\",\"14324060276\",\"14324060268\",\"14324060266\"]},{\"ad_group_id\":\"3415119867\","
                + "\"campaign_id\":\"36147491\",\"phrase_ids\":[\"13852582793\",\"13852582803\",\"13852582796\","
                + "\"13852582798\",\"13852582802\",\"13852582804\",\"13852582795\",\"13852582805\",\"13852582800\","
                + "\"13852582799\",\"13852582797\",\"13852582792\",\"13852582801\",\"13852582806\"]}]}}",
            "",
            200,
            new ArrayList<Long>(),
            new ArrayList<UnsignedLong>(),
            UnsignedLong.valueOf(3356960149625685935L),
            3356961850432728774L,
            0L,
            "sas2-0301-sas-ppc-java-intapi-13904.gencfg-c.yandex.net",
            0L,
            0F,
            0.010256038F,
            "",
            "",
            "",
            2009921,
        };

        checker.check(inputLine,
            singletonList(date),
            singletonList(row));
    }

    @Test
    public void testParsePpcCmdLog() throws Exception {
        String inputLine =
            "<134>1 2018-09-29T00:00:00+03:00 "
                + "iva1-5919-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net PPCLOG.ppclog_cmd.log 565961 - - "
                + "2018-09-29 00:00:00 "
                + "{\"param\":{\"csrf_token\":\"********\",\"period_num\":\"0\","
                + "\"currency\":\"USD\",\"json_minus_words\":\"[]\",\"unglue\":\"1\",\"period\":\"month\","
                + "\"fixate_stopwords\":\"1\",\"geo\":\"213\",\"advanced_forecast\":\"yes\","
                + "\"phrases\":\"\\\"!новости !бастрыкин\\\"\\n\\\"!курсы !маникюра "
                + "!астана\\\"\\n\\\"!сертификат !по !наращиванию !ногтей\\\"\\n\\\"!пензенский "
                + "!район\\\"\\n\\\"!меню !на !кафе\\\"\\n\\\"!министерство !труда !профессиональные "
                + "!стандарты\\\"\\n\\\"!курсы !маникюра !серпухов\\\"\\n\\\"!теория !маникюра\\\"\\n\\\""
                + "!дрыманов !александр !александрович !биография\\\"\\n\\\"!стажировка !в !следственном "
                + "!комитете\\\"\\n\\\"!юрий !чабуев\\\"\\n\\\"!стратегическое !прогнозирование\\\"\\n\\\"!приказы"
                + " !следственного !комитета\\\"\\n\\\"!баклажан !ресторан !меню\\\"\\n\\\"!отставка "
                + "!бастрыкина\\\"\\n\\\"!бастрыкин !александр\\\"\\n\\\"!школа !массажа !одесса\\\"\\n\\\"!о "
                + "!похоронном !деле\\\"\\n\\\"!социальная !политика !в !рф\\\"\\n\\\"!наполеон "
                + "!ресторан !москва\\\"\"},"
                + "\"reqid\":3356958733481140304,"
                + "\"http_status\":200,"
                + "\"cluid\":\"732151306\","
                + "\"yandexuid\":\"921937631538168380\","
                + "\"runtime\":0.513428926467896,"
                + "\"log_type\":\"cmd\","
                + "\"ip\":\"178.89.197.114\","
                + "\"cmd\":\"ajaxDataForNewBudgetForecast\","
                + "\"cid\":\"0\","
                + "\"host\":\"iva1-5919-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net\","
                + "\"pid\":0,"
                + "\"role\":\"client\","
                + "\"cpu_user_time\":0.129999999999995,"
                + "\"logtime\":\"20180929000000\","
                + "\"proc_id\":565961,"
                + "\"bid\":0,"
                + "\"tvm_service_id\":null,"
                + "\"uid\":\"732151306\"}";

        Date date = dateFormat.parse("2018-09-29 00:00:00");
        Object[] row = new Object[]{
            new ArrayList<Long>(Arrays.asList(Long.valueOf(0))),
            new ArrayList<Long>(Arrays.asList(Long.valueOf(0))),
            new ArrayList<Long>(Arrays.asList(Long.valueOf(0))),
            "178.89.197.114",
            "ajaxDataForNewBudgetForecast",
            0.513428926467896F,
            "",
            "{\"csrf_token\":\"********\",\"period_num\":\"0\","
                + "\"currency\":\"USD\",\"json_minus_words\":\"[]\",\"unglue\":\"1\",\"period\":\"month\","
                + "\"fixate_stopwords\":\"1\",\"geo\":\"213\",\"advanced_forecast\":\"yes\","
                + "\"phrases\":\"\\\"!новости !бастрыкин\\\"\\n\\\"!курсы !маникюра "
                + "!астана\\\"\\n\\\"!сертификат !по !наращиванию !ногтей\\\"\\n\\\"!пензенский "
                + "!район\\\"\\n\\\"!меню !на !кафе\\\"\\n\\\"!министерство !труда !профессиональные "
                + "!стандарты\\\"\\n\\\"!курсы !маникюра !серпухов\\\"\\n\\\"!теория !маникюра\\\"\\n\\\""
                + "!дрыманов !александр !александрович !биография\\\"\\n\\\"!стажировка !в !следственном "
                + "!комитете\\\"\\n\\\"!юрий !чабуев\\\"\\n\\\"!стратегическое !прогнозирование\\\"\\n\\\"!приказы"
                + " !следственного !комитета\\\"\\n\\\"!баклажан !ресторан !меню\\\"\\n\\\"!отставка "
                + "!бастрыкина\\\"\\n\\\"!бастрыкин !александр\\\"\\n\\\"!школа !массажа !одесса\\\"\\n\\\"!о "
                + "!похоронном !деле\\\"\\n\\\"!социальная !политика !в !рф\\\"\\n\\\"!наполеон "
                + "!ресторан !москва\\\"\"}",
            "",
            200,
            new ArrayList<Long>(Arrays.asList(Long.valueOf(732151306L))),
            new ArrayList<UnsignedLong>(),
            UnsignedLong.valueOf(3356958733481140304L),
            3356958733481140304L,
            732151306L,
            "iva1-5919-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net",
            565961L,
            0F,
            0.129999999999995F,
            "921937631538168380",
            "client",
            "",
            0,
        };

        checker.check(inputLine,
            singletonList(date),
            singletonList(row));
    }

    @Test
    public void testParsePpcCmdLogMultipleValuesCids() throws Exception {
        String inputLine = "<134>1 2018-09-29T15:02:24+03:00 "
            + "iva1-5919-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net PPCLOG.ppclog_cmd.log 782854 - - "
            + "2018-09-29 15:02:24 {\"role\":\"client\","
            + "\"bid\":0,"
            + "\"reqid\":3416490752514352712,"
            + "\"host\":\"iva1-5919-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net\","
            + "\"cpu_user_time\":0.0399999999999991,"
            + "\"runtime\":0.213147878646851,"
            + "\"ip\":\"94.159.31.238\","
            + "\"http_status\":200,"
            + "\"cluid\":\"391034260\","
            + "\"cmd\":\"addPdfReport\","
            + "\"uid\":\"584235259\","
            + "\"cid\":\"0\","
            + "\"logtime\":\"20180929150224\","
            + "\"yandexuid\":\"5229691181502257865\","
            + "\"log_type\":\"cmd\","
            + "\"proc_id\":782854,"
            + "\"tvm_service_id\":null,"
            + "\"param\":{\"add_categories\":\"0\",\"csrf_token\":\"********\",\"add_vendor\":\"0\","
            + "\"cids\":\"19607084, 19607113, 19607173, 19607220, 19607224, 19607628, 19607632, 19607638\","
            + "\"date_to\":\"2018-09-26\",\"date_from\":\"2018-09-21\",\"grouping\":\"day\","
            + "\"type\":\"pdf\"},\"pid\":0}";

        Date date = dateFormat.parse("2018-09-29 15:02:24");
        Object[] row = new Object[]{
            new ArrayList<Long>(Arrays.asList(Long.valueOf(0), Long.valueOf(19607084), Long.valueOf(19607113),
                Long.valueOf(19607173), Long.valueOf(19607220), Long.valueOf(19607224), Long.valueOf(19607628),
                Long.valueOf(19607632), Long.valueOf(19607638))),
            new ArrayList<Long>(Arrays.asList(Long.valueOf(0))),
            new ArrayList<Long>(Arrays.asList(Long.valueOf(0))),
            "94.159.31.238",
            "addPdfReport",
            0.213147878646851F,
            "",
            "{\"add_categories\":\"0\",\"csrf_token\":\"********\",\"add_vendor\":\"0\","
                + "\"cids\":\"19607084, 19607113, 19607173, 19607220, 19607224, 19607628, 19607632, 19607638\","
                + "\"date_to\":\"2018-09-26\",\"date_from\":\"2018-09-21\",\"grouping\":\"day\","
                + "\"type\":\"pdf\"}",
            "",
            200,
            new ArrayList<Long>(Arrays.asList(Long.valueOf(391034260L))),
            new ArrayList<Long>(),
            UnsignedLong.valueOf(3416490752514352712L),
            3416490752514352712L,
            584235259L,
            "iva1-5919-msk-iva-ppc-direct-f-ede-14646.gencfg-c.yandex.net",
            782854L,
            0F,
            0.0399999999999991F,
            "5229691181502257865",
            "client",
            "",
            0,
        };

        checker.check(inputLine,
            singletonList(date),
            singletonList(row));
    }
}