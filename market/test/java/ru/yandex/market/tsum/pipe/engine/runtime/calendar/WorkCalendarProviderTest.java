package ru.yandex.market.tsum.pipe.engine.runtime.calendar;

import java.time.LocalDate;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.calendar.CalendarClient;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;
import ru.yandex.misc.test.Assert;

/**
 * Test connection with yandex calendar API.
 *
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 05.04.2019
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TsumDebugRuntimeConfig.class})
public class WorkCalendarProviderTest {

    @Autowired
    private CalendarClient calendarClient;

    @Test
    public void testWorkCalendarProvider() {
        WorkCalendarProvider workCalendarProvider = new WorkCalendarProviderImpl(calendarClient);

        Assert.notNull(workCalendarProvider.getTypeOfDay(LocalDate.now()));
    }

}
