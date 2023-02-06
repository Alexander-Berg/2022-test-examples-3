package ru.yandex.market.logshatter.parser.ir;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 09/03/16
 */
public class ScAgeParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new ScAgeParser());
        checker.check(
            "05/Mar/2016:02:38:24 +0300\toffersAgeDays\t2\t1\t13479931",
            new Date(1457134704000L), "offersAge", checker.getHost(), 13479931, 2 * 24, 1
        );

        checker.check(
            "05/Mar/2016:02:38:24 +0300\toffersAgeHours\t2\t1\t13479931",
            new Date(1457134704000L), "offersAge", checker.getHost(), 13479931, 2, 1
        );
    }
}