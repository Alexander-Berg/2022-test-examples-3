package ru.yandex.calendar.frontend.caldav;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import ru.yandex.calendar.frontend.caldav.impl.CaldavService;
import ru.yandex.calendar.frontend.caldav.impl.CaldavServiceImpl;

@Configuration
@Import({
        CaldavContextConfiguration.class,
})
public class CaldavContextTestConfiguration {
    @Bean
    @Primary
    public CaldavService caldavService() {
        return Mockito.mock(CaldavServiceImpl.class);
    }
}
