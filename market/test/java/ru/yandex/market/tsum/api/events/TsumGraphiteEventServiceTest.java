package ru.yandex.market.tsum.api.events;

import org.junit.Test;
import ru.yandex.misc.test.Assert;

import java.util.concurrent.TimeUnit;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 05/12/2016
 */
public class TsumGraphiteEventServiceTest {
    @Test
    public void parseTimeSeconds() throws Exception {
        checkParseSeconds("noW", 0);
        checkParseSeconds("-3y", TimeUnit.DAYS.toSeconds(3 * 366));
        checkParseSeconds("-10mon", TimeUnit.DAYS.toSeconds(10 * 31));
        checkParseSeconds("-14d", TimeUnit.DAYS.toSeconds(14));
        checkParseSeconds("-4W", TimeUnit.DAYS.toSeconds(4 * 7));
        checkParseSeconds("-6h", TimeUnit.HOURS.toSeconds(6));
        checkParseSeconds("-15mIn", TimeUnit.MINUTES.toSeconds(15));
    }

    private static void checkParseSeconds(String time, long expectedSeconds) {
        Assert.assertEquals(expectedSeconds, TsumGraphiteEventService.parseTimeSeconds(time, 0) * -1);
    }

}