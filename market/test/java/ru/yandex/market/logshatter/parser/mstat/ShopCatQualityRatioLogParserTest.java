package ru.yandex.market.logshatter.parser.mstat;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 */
public class ShopCatQualityRatioLogParserTest {
    private LogParserChecker checker = new LogParserChecker(new ShopCatQualityRatioLogParser());

    @Test
    public void parse() throws Exception {
        checker.check(
            "2017-04-30 16:34:10\t234\t12802914\t34\t5\t31\t2\t8\t5\t0.9197\t0.8278\t91\t82\t1\t0",
            new Date(1493559250000L),
            234L,
            12802914L,
            34,
            5,
            31,
            2,
            8,
            5,
            0.9197,
            0.8278,
            91,
            82,
            1,
            0L
        );
    }

}
