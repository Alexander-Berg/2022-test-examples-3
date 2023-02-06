package ru.yandex.market.calendar;

import java.time.LocalDate;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 05.02.2020
 */
public class YandexCalendarServiceTest {
    /**
     * ssh -f -N -L 8080:calendar-api.tools.yandex.net:80 blacksmith01h.market.yandex.net
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void testCalendar() throws Exception {
        List<YandexCalendarDay> xmlDays = getDaysFromXmlApi();
        List<YandexCalendarDay> jsonDays = getDaysFromJsonApi();
        assertArrayEquals(xmlDays.toArray(), jsonDays.toArray());
    }

    private List<YandexCalendarDay> getDaysFromXmlApi() throws Exception {
        YandexCalendarService xmlCalendarService = new YandexCalendarService();
        xmlCalendarService.setClientName("market-clickphite");
        xmlCalendarService.setApiUrl("https://api.calendar.yandex-team.ru/");
        xmlCalendarService.afterPropertiesSet();

        return xmlCalendarService.getDays(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-12-31"));
    }

    private List<YandexCalendarDay> getDaysFromJsonApi() throws Exception {
        YandexCalendarService jsonCalendarService = new YandexCalendarService();
        jsonCalendarService.setClientName("market-clickphite");
        jsonCalendarService.setApiUrl("http://localhost:8080/");
        jsonCalendarService.setJsonApi(true);
        jsonCalendarService.afterPropertiesSet();

        return jsonCalendarService.getDays(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-12-31"));
    }

}
