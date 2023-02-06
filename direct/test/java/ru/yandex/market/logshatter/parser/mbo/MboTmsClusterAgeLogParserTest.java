package ru.yandex.market.logshatter.parser.mbo;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 09/03/16
 */
public class MboTmsClusterAgeLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new MboTmsClusterAgeLogParser());
        checker.check(
            "2016-03-05T05:00:00+0300\t997520\tDELETED_EXPIRED_HALFYEAR\t682\t14",
            null, "DELETED_EXPIRED_HALFYEAR", checker.getHost(), 997520, 682 * 24, 14
        );


    }
}