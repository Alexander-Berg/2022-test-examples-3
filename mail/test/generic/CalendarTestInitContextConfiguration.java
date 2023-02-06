package ru.yandex.calendar.test.generic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.calendar.boot.CalendarAppName;
import ru.yandex.calendar.boot.CalendarInitContextConfiguration;
import ru.yandex.misc.spring.context.EnvironmentTypeTestsContextConfiguration;
import ru.yandex.oauth.OAuth;

/**
 * @author dbrylev
 */
@Configuration
@Import({
        CalendarInitContextConfiguration.class,
        EnvironmentTypeTestsContextConfiguration.class,
})
public class CalendarTestInitContextConfiguration {

    @Bean
    public CalendarAppName calendarAppName() {
        return CalendarAppName.TEST;
    }

    @Bean
    public OAuth getOAuth() {
        return new OAuth();
    }
}
