package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.antifraud;

import com.google.common.cache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.LogRecord;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.fieldmarkers.HasLinkId;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.entities.fieldmarkers.HasStateAndFilter;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.MetricsService;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Created by oroboros on 21.01.16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Calendar.class, System.class, AbstractDayGapFilter.class})
public class DayGapTest {

    private Calendar calendar;
    private Long time = -1L;
    private MetricsService metricsService = mock(MetricsService.class);

    @Before
    public void mockSystemTime() {
        PowerMockito.mockStatic(Calendar.class);
        PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);

        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenAnswer(invocationOnMock -> time.longValue());
    }

    @Test
    public void mustInvalidateCache() {
        // today
        calendar = new Calendar.Builder().setDate(2000, 1, 1).build();
        time = calendar.getTimeInMillis();
        PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);
        MyFilter filter = new MyFilter(metricsService);
        Cache<String, Long> cacheToday = filter.getCache();

        // tomorrow
        calendar.add(Calendar.DATE, 1);
        time = calendar.getTimeInMillis();
        filter.tryMarkAsFraud(new MyLogRecord("linkid1", new Date(time)));
        Cache<String, Long> cacheTomorrow = filter.getCache();

        assertThat(cacheToday != cacheTomorrow, is(true));
    }

    @Test
    public void mustNotFilterOutOfDayGapClicks() {
        calendar = new Calendar.Builder().setDate(2000, 1, 1).build();
        time = calendar.getTimeInMillis();
        PowerMockito.when(Calendar.getInstance()).thenReturn(calendar);
        MyFilter filter = new MyFilter(metricsService);

        // today
        boolean resultToday1 = filter.tryMarkAsFraud(new MyLogRecord("linkid1", new Date(time)));
        boolean resultToday2 = filter.tryMarkAsFraud(new MyLogRecord("linkid1", new Date(time + 1)));
        // yesterday
        boolean resultYesterday = filter.tryMarkAsFraud(new MyLogRecord("linkid1", new Date(time - 1)));
        // future
        boolean resultFuture = filter.tryMarkAsFraud(new MyLogRecord("linkid1", new Date(time + 86400 * 1000 * 3)));

        assertThat(resultToday1, is(false));
        assertThat(resultToday2, is(true));
        assertThat(resultYesterday, is(false));
        assertThat(resultFuture, is(false));
    }

    static class MyFilter extends AbstractDayGapFilter<MyLogRecord, Long> {

        public MyFilter(MetricsService metricsService) {
            super(metricsService, 1, 100, 10_000);
        }

        @Override
        public boolean isFraud(MyLogRecord record) {
            String linkId = record.getLink_id();
            if(getCache().getIfPresent(linkId) == null) {
                getCache().put(linkId, 0L);
                return false;
            }
            else {
                // all subsequent are fraud
                return true;
            }
        }
    }


    static class MyLogRecord implements HasLinkId, HasStateAndFilter, LogRecord {
        int filter = 0;
        int state = 1;
        final Date eventtime;
        String linkId;

        public MyLogRecord(String linkId, Date eventtime) {
            this.linkId = linkId;
            this.eventtime = eventtime;
        }

        @Override
        public Integer getFilter() {
            return filter;
        }

        @Override
        public void setFilter(Integer filter) {
            this.filter = filter;
        }

        @Override
        public Integer getState() {
            return state;
        }

        @Override
        public void setState(Integer state) {
            this.state = state;
        }

        @Override
        public Date getEventtime() {
            return eventtime;
        }

        @Override
        public String getLink_id() {
            return linkId;
        }

        @Override
        public void setLink_id(String link_id) {
            this.linkId = link_id;
        }
    }
}
