package ru.yandex.market.logshatter.parser.mstat;

import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 */
public class DefaultCategoryAverageQualityRatioLogParserTest {
    private LogParserChecker checker = new LogParserChecker(new DefaultCategoryAverageQualityRatioLogParser());

    @Test
    public void parse() throws Exception {
        checker.check(
            "2017-04-30 16:34:10\t12802914\t0.8966\t0.8531\t80\t85",
            new Date(1493559250000L),
            12802914L,
            0.8966,
            0.8531,
            80,
            85
        );
    }

}
