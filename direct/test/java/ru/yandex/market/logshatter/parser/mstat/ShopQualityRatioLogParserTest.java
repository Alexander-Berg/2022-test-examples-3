package ru.yandex.market.logshatter.parser.mstat;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 */
public class ShopQualityRatioLogParserTest {
    private LogParserChecker checker = new LogParserChecker(new ShopQualityRatioLogParser());

    @Test
    public void parse() throws Exception {
        checker.check(
                "2017-04-30 16:34:10\t234\t4968\t1476\t4448\t956\t0.7763\t0.6986\t77\t69\t0\tfalse",
                new Date(1493559250000L),
                234L,
                4968,
                1476,
                4448,
                956,
                0.7763,
                0.6986,
                77,
                69,
                0L,
                0
        );
    }

}
