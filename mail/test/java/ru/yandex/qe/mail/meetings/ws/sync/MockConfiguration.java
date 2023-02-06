package ru.yandex.qe.mail.meetings.ws.sync;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.opentable.db.postgres.embedded.FlywayPreparer;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.qe.mail.meetings.booking.RoomService;
import ru.yandex.qe.mail.meetings.config.NotificationConfiguration;
import ru.yandex.qe.mail.meetings.mocks.CommonMockConfiguration;
import ru.yandex.qe.mail.meetings.mocks.MockCalendarConfiguration;
import ru.yandex.qe.mail.meetings.services.abc.AbcClient;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarUpdate;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Resource;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.synchronizer.dao.SyncDao;
import ru.yandex.qe.mail.meetings.synchronizer.impl.AbcServices;
import ru.yandex.qe.mail.meetings.synchronizer.impl.StaffGroups;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.ABC_SERVICE;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.STAFF_GROUP;

@Profile("test")
@Configuration
@Import({NotificationConfiguration.class, CommonMockConfiguration.class, MockCalendarConfiguration.class})
@ComponentScan(basePackages = {"ru.yandex.qe.mail.meetings.synchronizer", "ru.yandex.qe.mail.meetings.utils", "ru.yandex.qe.mail.meetings.ws.sync"})
public class MockConfiguration {

    @Inject
    private StaffClient staffClient;

    @Bean
    public SyncDao syncDao(EmbeddedPostgres ps) throws SQLException {
        FlywayPreparer flywayPreparer = FlywayPreparer.forClasspathLocation("db/migration");
        flywayPreparer.prepare(ps.getPostgresDatabase());
        return new SyncDao(ps.getPostgresDatabase());
    }

    @Bean
    public AbcClient abcClient() {
        return mock(AbcClient.class);
    }

    @Bean
    public AbcServices abcServices() {
        var abcServices = mock(AbcServices.class);
        when(abcServices.membersById(ABC_SERVICE.id())).thenReturn(List.of(staffClient.getByLogin("abc-login")));
        when(abcServices.byId(ABC_SERVICE.id())).thenReturn(Optional.of(ABC_SERVICE));
        return abcServices;
    }

    @Bean
    public StaffGroups staffGroups() {
        var staffGroups = mock(StaffGroups.class);
        when(staffGroups.membersById(STAFF_GROUP.id())).thenReturn(List.of(staffClient.getByLogin("staff-login")));
        when(staffGroups.byId(STAFF_GROUP.id())).thenReturn(Optional.of(STAFF_GROUP));
        return staffGroups;
    }

    @Bean
    public CalendarUpdate calendarUpdate() {
        return mock(CalendarUpdate.class);
    }

    @Bean
    public RoomService roomService() {
        var roomService = mock(RoomService.class);
        when(roomService.byMail(anyString())).thenAnswer((Answer<Optional<Resource.Info>>) invocation -> {
            var mail = (String)invocation.getArguments()[0];
            return mail.contains("conf") ? Optional.of(new Resource.Info()) : Optional.empty();
        });
        return roomService;
    }
}
