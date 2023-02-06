package ru.yandex.market.tsum.pipe.engine.runtime.calendar;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 05.04.2019
 */
@Configuration
public class WorkCalendarTestConfig {

    @Bean
    public WorkCalendarProvider workCalendarProvider() {
        return new TestWorkCalendarProvider();
    }

}
