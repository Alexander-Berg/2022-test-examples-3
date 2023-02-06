package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.assertj.core.internal.Arrays;
import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class TraceLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new TraceLogParser());
    private SimpleDateFormat dateFormat = new SimpleDateFormat(TraceLogParser.DATE_PATTERN);

    @Test
    public void testParse() throws Exception {
        String jsonLine = "[3,"
            + "'2018-09-21 02:15:13.329128',"
            + "'sas2-1317-sas-ppc-direct-intapi-perl-32123.gencfg-c.yandex.net',"
            + "327206,"
            + "'direct.jsonrpc',"
            + "'Moderation_process_mod_result',"
            + "'tag1, tag2',"
            + "2617691324595755307,"
            + "0,"
            + "2617691324595755307,"
            + "2,"
            + "true,"
            + "12.9184730052948,"
            + "1,"
            + "{"
            + "'annotations':["
            + "['key','value']"
            + "],"
            + "'marks':["
            + "[5.231,'all data']"
            + "],"
            + "'times':{"
            + "'cs':0.09,'mem':4684,'ela':12.9184730052948,'cu':1.21"
            + "},"
            + "'services':["
            + "['advq','search',4453358886602725740,15.54416448,30.88449236],"
            + "['advq','search',4453358884853470174,15.54416813,30.88448878]"
            + "],"
            + "'profile':["
            + "['db:write','ppc:9',0.89480185508728,0,121,2],"
            + "['db:write','ppc:5',6.41379308700562,0,825,2],"
            + "['db:write','ppclog',0.0693674087524414,0,5,2],"
            + "['db:write','ppcdict',0.010206937789917,0,1,6],"
            + "['db:read','ppc:5',0.147401094436646,0,7,0],"
            + "['db:read','ppclog',0.0072479248046875,0,1,0],"
            + "['db:read','ppc:9',0.0457251071929932,0,5,0],"
            + "['db:read','ppcdict',0.0167331695556641,0,3,0],"
            + "['i18n:init_i18n','',0.000310182571411133,0,1,0],"
            + "['yandex_log:out','',0.150135278701782,0,760,0],"
            + "['db:ping','ppcdict',0.000512838363647461,0,1,0],"
            + "['db:ping','ppc:9',0.00655293464660645,0,1,0],"
            + "['db:ping','ppclog',0.00677108764648438,0,1,0],"
            + "['rest','',5.14891409873962,0,1,0]"
            + "]"
            + "}"
            + "]";

        Date date = dateFormat.parse("2018-09-21 05:15:13");

        List<String> profileFuncs =
            asList("db:write", "db:write", "db:write", "db:write", "db:read", "db:read", "db:read", "db:read",
                "i18n:init_i18n", "yandex_log:out", "db:ping", "db:ping", "db:ping", "rest");

        List<String> profileTags =
            asList("ppc:9", "ppc:5", "ppclog", "ppcdict", "ppc:5", "ppclog", "ppc:9", "ppcdict", "", "",
                "ppcdict", "ppc:9", "ppclog", "");

        List<Float> profileAllEla = asList(0.89480185508728f,
            6.41379308700562f,
            0.0693674087524414f,
            0.010206937789917f,
            0.147401094436646f,
            0.0072479248046875f,
            0.0457251071929932f,
            0.0167331695556641f,
            0.000310182571411133f,
            0.150135278701782f,
            0.000512838363647461f,
            0.00655293464660645f,
            0.00677108764648438f,
            5.14891409873962f);

        List<Float> profileChildsEla = asList(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        List<Integer> profileCalls = asList(121, 825, 5, 1, 7, 1, 5, 3, 1, 760, 1, 1, 1, 1);
        List<Integer> profileObjNums = asList(2, 2, 2, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        List<String> servicesServices = asList("advq", "advq");
        List<String> servicesMethods = asList("search", "search");
        List<Long> servicesSpanIds = asList(4453358886602725740L, 4453358884853470174L);
        List<Float> servicesRelClientSends = asList(15.54416448f, 15.54416813f);
        List<Float> servicesEla = asList(30.88449236f, 30.88448878f);

        Object[] traceLog = new Object[]{
            329128000, // log_time_nanos
            3, // format_id
            "sas2-1317-sas-ppc-direct-intapi-perl-32123.gencfg-c.yandex.net", // host
            "direct.jsonrpc", // service
            "Moderation_process_mod_result", // method
            "tag1, tag2", // tags
            2617691324595755307L, // trace_id
            0L, // parentId
            2617691324595755307L, // span_id
            2, // chunk_index
            1, // chunk_last
            12.9184730052948f, // span_time
            1, // samplerate
            12.9184730052948f, // ela
            1.21f, // cpu_user
            0.09f, // cpu_system
            4684.0f, // mem

            profileFuncs,
            profileTags,
            profileAllEla,
            profileChildsEla,
            profileCalls,
            profileObjNums,

            servicesServices,
            servicesMethods,
            servicesSpanIds,
            servicesRelClientSends,
            servicesEla,

            singletonList(5.231f), // marks.relative_time
            singletonList("all data"), // marks.message

            singletonList("key"), // annotations.key
            singletonList("value") // annotations.value
        };
        checker.check(jsonLine, date, traceLog);
    }
    @Test
    public void testParse2() throws Exception {
        String jsonLine = "[3,"
            + "'2018-09-21 02:15:13.329128',"
            + "'sas2-1317-sas-ppc-direct-intapi-perl-32123.gencfg-c.yandex.net',"
            + "327206,"
            + "'direct.jsonrpc',"
            + "'Moderation_process_mod_result',"
            + "'tag1, tag2',"
            + "2617691324595755307,"
            + "0,"
            + "2617691324595755307,"
            + "2,"
            + "true,"
            + "12.9184730052948,"
            + "1,"
            + "{"
            + "'marks':["
            + "[5.231,'all data']"
            + "],"
            + "'times':{"
            + "'cs':0.09,'mem':4684,'ela':12.9184730052948,'cu':1.21"
            + "},"
            + "'services':["
            + "['advq','search',4453358886602725740,15.54416448,30.88449236],"
            + "['advq','search',4453358884853470174,15.54416813,30.88448878]"
            + "],"
            + "'profile':["
            + "['db:write','ppc:9',0.89480185508728,0,121,2],"
            + "['db:write','ppc:5',6.41379308700562,0,825,2],"
            + "['db:write','ppclog',0.0693674087524414,0,5,2],"
            + "['db:write','ppcdict',0.010206937789917,0,1,6],"
            + "['db:read','ppc:5',0.147401094436646,0,7,0],"
            + "['db:read','ppclog',0.0072479248046875,0,1,0],"
            + "['db:read','ppc:9',0.0457251071929932,0,5,0],"
            + "['db:read','ppcdict',0.0167331695556641,0,3,0],"
            + "['i18n:init_i18n','',0.000310182571411133,0,1,0],"
            + "['yandex_log:out','',0.150135278701782,0,760,0],"
            + "['db:ping','ppcdict',0.000512838363647461,0,1,0],"
            + "['db:ping','ppc:9',0.00655293464660645,0,1,0],"
            + "['db:ping','ppclog',0.00677108764648438,0,1,0],"
            + "['rest','',5.14891409873962,0,1,0]"
            + "]"
            + "}"
            + "]";

        Date date = dateFormat.parse("2018-09-21 05:15:13");

        List<String> profileFuncs =
            asList("db:write", "db:write", "db:write", "db:write", "db:read", "db:read", "db:read", "db:read",
                "i18n:init_i18n", "yandex_log:out", "db:ping", "db:ping", "db:ping", "rest");

        List<String> profileTags =
            asList("ppc:9", "ppc:5", "ppclog", "ppcdict", "ppc:5", "ppclog", "ppc:9", "ppcdict", "", "",
                "ppcdict", "ppc:9", "ppclog", "");

        List<Float> profileAllEla = asList(0.89480185508728f,
            6.41379308700562f,
            0.0693674087524414f,
            0.010206937789917f,
            0.147401094436646f,
            0.0072479248046875f,
            0.0457251071929932f,
            0.0167331695556641f,
            0.000310182571411133f,
            0.150135278701782f,
            0.000512838363647461f,
            0.00655293464660645f,
            0.00677108764648438f,
            5.14891409873962f);

        List<Float> profileChildsEla = asList(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        List<Integer> profileCalls = asList(121, 825, 5, 1, 7, 1, 5, 3, 1, 760, 1, 1, 1, 1);
        List<Integer> profileObjNums = asList(2, 2, 2, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        List<String> servicesServices = asList("advq", "advq");
        List<String> servicesMethods = asList("search", "search");
        List<Long> servicesSpanIds = asList(4453358886602725740L, 4453358884853470174L);
        List<Float> servicesRelClientSends = asList(15.54416448f, 15.54416813f);
        List<Float> servicesEla = asList(30.88449236f, 30.88448878f);

        Object[] traceLog = new Object[]{
            329128000, // log_time_nanos
            3, // format_id
            "sas2-1317-sas-ppc-direct-intapi-perl-32123.gencfg-c.yandex.net", // host
            "direct.jsonrpc", // service
            "Moderation_process_mod_result", // method
            "tag1, tag2", // tags
            2617691324595755307L, // trace_id
            0L, // parentId
            2617691324595755307L, // span_id
            2, // chunk_index
            1, // chunk_last
            12.9184730052948f, // span_time
            1, // samplerate
            12.9184730052948f, // ela
            1.21f, // cpu_user
            0.09f, // cpu_system
            4684.0f, // mem

            profileFuncs,
            profileTags,
            profileAllEla,
            profileChildsEla,
            profileCalls,
            profileObjNums,

            servicesServices,
            servicesMethods,
            servicesSpanIds,
            servicesRelClientSends,
            servicesEla,

            singletonList(5.231f), // marks.relative_time
            singletonList("all data"), // marks.message

            Collections.emptyList(), // annotations.key
            Collections.emptyList() // annotations.value
        };
        checker.check(jsonLine, date, traceLog);
    }

}
