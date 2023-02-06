package ru.yandex.calendar.logic.stat;

import org.joda.time.LocalDate;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author Daniel Brylev
 */
public class YandexMailStatsParserTest {
    @Test
    public void parseTodoDailyStats() {
        String input =
                "[{" +
                    "\"label\" : \"todo_stats\"," +
                    "\"data\" : [" +
                        "[1331424000000,37]," +
                        "[1331510400000,55]," +
                        "[1331769600000,128]" +
                    "]" +
                "}]";
        ListF<YandexMailTodoDailyStat> stats =
                YandexMailStatsParser.parseTodoDailyStats(InputStreamSourceUtils.bytes(input.getBytes()));

        Assert.sizeIs(3, stats);
        Assert.unique(stats.map(YandexMailTodoDailyStat.getDateF()));

        Assert.equals(new LocalDate(2012, 3, 11), stats.first().getDate());
        Assert.equals(128, stats.last().getUsersWithShowTodoSettingOn());
    }
}
