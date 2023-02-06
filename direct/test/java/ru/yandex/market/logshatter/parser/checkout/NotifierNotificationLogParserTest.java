package ru.yandex.market.logshatter.parser.checkout;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

public class NotifierNotificationLogParserTest {

    @Test
    public void parse() throws Exception {
        NotifierNotificationLogParser parser = new NotifierNotificationLogParser();
        LogParserChecker logParserChecker = new LogParserChecker(parser);

        String kvString = "{\"how\":\"are you\",\"hello\":\"world\"}";
        KeyValueParserUtil.KeysAndValues keysAndValues = KeyValueParserUtil.parseKeysAndValues(kvString);

        String line = "12/Apr/2019:14:11:25 +0300\t42\ttestType\t831\t1555067485378/813dcc2d69d55d81f73896ef93f99d43\t[SMS]\t"+kvString;
        logParserChecker.check(line,
            1555067485,
            logParserChecker.getHost(),
            42,
            "testType",
            831,
            "1555067485378/813dcc2d69d55d81f73896ef93f99d43",
            "[SMS]",
            keysAndValues.getKeys(),
            keysAndValues.getValues()
        );
    }

    @Test
    public void parseEmptyKV() throws Exception {
        NotifierNotificationLogParser parser = new NotifierNotificationLogParser();
        LogParserChecker logParserChecker = new LogParserChecker(parser);

        KeyValueParserUtil.KeysAndValues keysAndValues = KeyValueParserUtil.parseKeysAndValues(null);

        String line = "12/Apr/2019:14:11:25 +0300\t42\ttestType\t831\t1555067485378/813dcc2d69d55d81f73896ef93f99d43\t[SMS]";
        logParserChecker.check(line,
            1555067485,
            logParserChecker.getHost(),
            42,
            "testType",
            831,
            "1555067485378/813dcc2d69d55d81f73896ef93f99d43",
            "[SMS]",
            keysAndValues.getKeys(),
            keysAndValues.getValues()
        );
    }

    @Test
    public void parseEmptyLine() throws Exception {
        NotifierNotificationLogParser parser = new NotifierNotificationLogParser();
        LogParserChecker logParserChecker = new LogParserChecker(parser);

        String line = "12/Apr/2019:14:11:25 +0300";
        logParserChecker.checkEmpty(line);
    }

}
