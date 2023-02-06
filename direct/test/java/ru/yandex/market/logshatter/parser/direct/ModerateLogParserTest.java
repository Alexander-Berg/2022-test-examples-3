package ru.yandex.market.logshatter.parser.direct;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;

public class ModerateLogParserTest {

    @Test
    public void testParse1() throws Exception {
        String line = "2019-01-16\t11:16:40\tbid=6872179614,obj_type=sitelinks_set,data_type=status_sent,uuid=09A794CC-1967-11E9-BDC4-134D821420C3\t{\"new_status\":\"Sent\",\"type\":\"sitelinks_set\",\"bid\":\"6872179614\",\"statusSitelinksModerate\":\"Sending\"}";

        LogParserChecker checker = new LogParserChecker(new ModerateLogParser());
        checker.setFile("/tmp/test-responce-log");
        checker.setHost("ppcdiscord.yandex.ru");

        checker.check(
            line,
            new Date(1547626600000L),
            0L,
            6872179614L,
            0L,
            "status_sent",
            "09A794CC-1967-11E9-BDC4-134D821420C3",
            "bid=6872179614,obj_type=sitelinks_set,data_type=status_sent,uuid=09A794CC-1967-11E9-BDC4-134D821420C3",
            "file:ppcdiscord.yandex.ru/tmp/test-responce-log",
            "{\"new_status\":\"Sent\",\"type\":\"sitelinks_set\",\"bid\":\"6872179614\",\"statusSitelinksModerate\":\"Sending\"}"
        );
    }


    @Test
    public void testParse2() throws Exception {
        String line = "2019-01-18\t01:29:16\tpid=3651035532,obj_type=phrases,cid=40075726,data_type=put_records_response,uuid=4EF6B9AE-1AA7-11E9-8DFA-B65F861420C3\t{\"status\":\"Sent\",\"cid\":\"40075726\",\"id\":\"3651035532\",\"type\":\"phrases\"}";

        LogParserChecker checker = new LogParserChecker(new ModerateLogParser());
        checker.setFile("/tmp/test-responce-log");
        checker.setHost("ppcdiscord.yandex.ru");

        checker.check(
            line,
            new Date(1547764156000L),
            40075726L,
            0L,
            3651035532L,
            "put_records_response",
            "4EF6B9AE-1AA7-11E9-8DFA-B65F861420C3",
            "pid=3651035532,obj_type=phrases,cid=40075726,data_type=put_records_response,uuid=4EF6B9AE-1AA7-11E9-8DFA-B65F861420C3",
            "file:ppcdiscord.yandex.ru/tmp/test-responce-log",
            "{\"status\":\"Sent\",\"cid\":\"40075726\",\"id\":\"3651035532\",\"type\":\"phrases\"}"
        );
    }
}
