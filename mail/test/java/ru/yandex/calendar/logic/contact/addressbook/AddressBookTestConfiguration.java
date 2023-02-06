package ru.yandex.calendar.logic.contact.addressbook;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.yandex.calendar.boot.CalendarContextConfiguration;
import ru.yandex.calendar.test.generic.CalendarTestInitContextConfiguration;
import ru.yandex.calendar.test.generic.CalendarTestJdbcContextConfiguration;
import ru.yandex.calendar.test.generic.TvmClientTestConfiguration;

@Configuration
@Import({
        CalendarTestInitContextConfiguration.class,
        CalendarTestJdbcContextConfiguration.class,
        CalendarContextConfiguration.class,
        TvmClientTestConfiguration.class
})
public class AddressBookTestConfiguration {
}
