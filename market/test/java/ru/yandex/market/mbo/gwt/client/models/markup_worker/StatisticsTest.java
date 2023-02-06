package ru.yandex.market.mbo.gwt.client.models.markup_worker;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * @author york
 * @since 15.11.2018
 */
public class StatisticsTest {

    @Test
    public void testStatsAppend() {
        int i = 1;
        Statistics statistics = new Statistics();
        statistics.setGeneratedEntitiesCount(i);
        statistics.setCannotRequestsCount(i << 1);
        statistics.setGeneratedTasksCount(i << 1);
        statistics.setResponseProcessedTasksCount(i << 1);
        statistics.setResponseProcessingFailedTasksCount(i << 1);
        statistics.setResponseReceivedTasksCount(i << 1);
        statistics.setRunningTasksCount(i << 1);
        statistics.setSentRequestsCount(i << 1);
        statistics.setCannotRequestsCount(i << 1);

        Statistics res = new Statistics();
        res.add(statistics);
        res.add(statistics);
        compare(res, statistics, Statistics::getGeneratedEntitiesCount);
        compare(res, statistics, Statistics::getCannotRequestsCount);
        compare(res, statistics, Statistics::getGeneratedTasksCount);
        compare(res, statistics, Statistics::getResponseProcessedTasksCount);
        compare(res, statistics, Statistics::getResponseProcessingFailedTasksCount);
        compare(res, statistics, Statistics::getResponseReceivedTasksCount);
        compare(res, statistics, Statistics::getRunningTasksCount);
        compare(res, statistics, Statistics::getSentRequestsCount);
        compare(res, statistics, Statistics::getCannotRequestsCount);

        checkDateField(Statistics::getLastGenerateTime, Statistics::setLastGenerateTime);
        checkDateField(Statistics::getLastSendToHitmanTime, Statistics::setLastSendToHitmanTime);
        checkDateField(Statistics::getLastSuccessfulProcessTime, Statistics::setLastSuccessfulProcessTime);
    }

    private void compare(Statistics res, Statistics statistics, Function<Statistics, Integer> getter) {
        assertEquals(getter.apply(statistics) * 2, (int) getter.apply(res));
    }

    private void checkDateField(Function<Statistics, Date> getter, BiConsumer<Statistics, Date> setter) {
        final Calendar cal = Calendar.getInstance();
        Date now = cal.getTime();
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();
        //check null + now
        Statistics statistics1 = new Statistics();
        Statistics statistics2 = new Statistics();
        setter.accept(statistics2, now);
        statistics1.add(statistics2);
        assertEquals(now, getter.apply(statistics1));

        //check now + null
        Statistics statistics3 = new Statistics();
        Statistics statistics4 = new Statistics();
        setter.accept(statistics3, now);
        statistics3.add(statistics4);
        assertEquals(now, getter.apply(statistics3));

        //check yesterday + now
        Statistics statistics5 = new Statistics();
        Statistics statistics6 = new Statistics();
        setter.accept(statistics5, yesterday);
        setter.accept(statistics6, now);
        statistics5.add(statistics6);
        assertEquals(now, getter.apply(statistics5));

        //check now + yesterday
        Statistics statistics7 = new Statistics();
        Statistics statistics8 = new Statistics();
        setter.accept(statistics7, now);
        setter.accept(statistics8, yesterday);
        statistics7.add(statistics8);
        assertEquals(now, getter.apply(statistics7));
    }
}
