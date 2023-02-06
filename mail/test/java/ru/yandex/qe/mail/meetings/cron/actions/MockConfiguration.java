package ru.yandex.qe.mail.meetings.cron.actions;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.qe.mail.meetings.blamer.DeclinedEventsDao;
import ru.yandex.qe.mail.meetings.booking.BookingResultMessageBuilder;
import ru.yandex.qe.mail.meetings.booking.FuzzyBookingService;
import ru.yandex.qe.mail.meetings.config.NotificationConfiguration;
import ru.yandex.qe.mail.meetings.mocks.CommonMockConfiguration;
import ru.yandex.qe.mail.meetings.mocks.MockCalendarConfiguration;
import ru.yandex.qe.mail.meetings.rooms.ResourceDao;
import ru.yandex.qe.mail.meetings.services.abc.AbcClient;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarUpdate;
import ru.yandex.qe.mail.meetings.services.gaps.GapApi;
import ru.yandex.qe.mail.meetings.storage.CalendarStorage;
import ru.yandex.qe.mail.meetings.storage.s3.YTStorage;
import ru.yandex.qe.mail.meetings.synchronizer.MeetingSynchronizer;
import ru.yandex.qe.mail.meetings.synchronizer.SyncResultMessageBuilder;
import ru.yandex.qe.mail.meetings.synchronizer.impl.AbcServices;
import ru.yandex.qe.mail.meetings.synchronizer.impl.StaffGroups;
import ru.yandex.qe.mail.meetings.ws.mock.CalendarUpdateMock;
import ru.yandex.qe.yt.cypress.http.builders.HttpCypressBuilderFactoryBean;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Galyamichev
 */

@Profile("test")
@Configuration
@Import({NotificationConfiguration.class, MockCalendarConfiguration.class, CommonMockConfiguration.class})
@ComponentScan(basePackages = {"ru.yandex.qe.mail.meetings.cron", "ru.yandex.qe.mail.meetings.ws", "ru.yandex.qe.mail.meetings.utils"})
public class MockConfiguration {
    @Bean
    public CalendarUpdate calendarUpdate() {
        return spy(new CalendarUpdateMock());
    }

    @Bean
    public DeclinedEventsDao declinedEventsDao() {
        return mock(DeclinedEventsDao.class);
    }

    @Bean
    public HttpCypressBuilderFactoryBean httpCypressBuilderFactoryBean() {
        return mock(HttpCypressBuilderFactoryBean.class);
    }

    @Bean
    public FuzzyBookingService fuzzyBookingService() {
        return mock(FuzzyBookingService.class);
    }

    @Bean
    public MetricRegistry metricRegistry() {
        MetricRegistry registry = mock(MetricRegistry.class);
        when(registry.counter(anyString())).thenReturn(new Counter());
        return registry;
    }

    @Bean
    public YTStorage ytStorage() {
        return mock(YTStorage.class);
    }

    @Bean
    public ResourceDao resourceDao() {
        return mock(ResourceDao.class);
    }

    @Bean
    public GapApi gapApi() {
        return mock(GapApi.class);
    }

    @Bean
    public CalendarStorage calendarStorage() {
        return mock(CalendarStorage.class);
    }

    @Bean
    public BookingResultMessageBuilder bookingResultMessageBuilder() {
        return mock(BookingResultMessageBuilder.class);
    }

    @Bean
    public MeetingSynchronizer meetingSynchronizer() {
        return mock(MeetingSynchronizer.class);
    }

    @Bean
    public AbcClient abcClient() {
        return mock(AbcClient.class);
    }

    @Bean
    public AbcServices abcServices() {
        return mock(AbcServices.class);
    }

    @Bean
    public StaffGroups staffGroups() {
        return mock(StaffGroups.class);
    }

    @Bean
    public SyncResultMessageBuilder syncResultMessageBuilder() {
        return mock(SyncResultMessageBuilder.class);
    }
}
