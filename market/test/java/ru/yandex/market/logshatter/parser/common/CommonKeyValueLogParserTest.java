package ru.yandex.market.logshatter.parser.common;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.health.KeyValueLog;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.HealthEnvironment;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 09/12/15
 */
public class CommonKeyValueLogParserTest {

    private final LogParserChecker checker = new LogParserChecker(new CommonKeyValueLogParser());


    @Test
    public void testParse() throws Exception {

        Date date = new Date(1322907330000L);
        String key = "key";
        String subkey = "subkey";
        double value = 42.21;

        String wSubkey = KeyValueLog.format(date, key, subkey, value);
        String woSubkey = KeyValueLog.format(date, key, value);

        checker.check(
            wSubkey,
            date, "access.log", checker.getHost(), key, subkey, value, HealthEnvironment.UNKNOWN
        );
        checker.check(
            woSubkey,
            date, "access.log", checker.getHost(), key, "", value, HealthEnvironment.UNKNOWN
        );
    }
}
