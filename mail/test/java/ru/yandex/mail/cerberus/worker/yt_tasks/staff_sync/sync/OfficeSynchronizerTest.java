package ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import lombok.SneakyThrows;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.micronaut.common.Pageable;
import ru.yandex.mail.cerberus.client.dto.Location;
import ru.yandex.mail.cerberus.core.location.LocationManager;
import ru.yandex.mail.micronaut.common.JsonMapper;
import ru.yandex.mail.cerberus.dao.location.LocationEntity;
import ru.yandex.mail.cerberus.dao.location.RoLocationRepository;
import ru.yandex.mail.cerberus.worker.api.TaskExecutionContext;
import ru.yandex.mail.cerberus.yt.data.YtOfficeInfo;
import ru.yandex.mail.cerberus.yt.staff.client.StaffClient;
import ru.yandex.mail.cerberus.yt.staff.client.StaffResult;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffOffice;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;
import javax.inject.Named;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.BC;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.TODAY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.TOMORROW;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.Constants.YESTERDAY;
import static ru.yandex.mail.cerberus.worker.yt_tasks.staff_sync.sync.OfficeSynchronizerTest.DB_NAME;
import static ru.yandex.mail.cerberus.yt.staff.StaffConstants.YT_OFFICE_TYPE;

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
@ExtendWith(MockitoExtension.class)
public class OfficeSynchronizerTest {
    static final String DB_NAME = "office_synchronizer_test_db";

    @Inject
    @Named("office")
    private Synchronizer synchronizer;

    @Inject
    private RoLocationRepository roLocationRepository;

    @Inject
    private LocationManager locationManager;

    @Inject
    private JsonMapper jsonMapper;

    private static final StaffClient staffClientMock = mock(StaffClient.class);

    @MockBean(StaffClient.class)
    public StaffClient mockStaffClient() {
        return staffClientMock;
    }

    @BeforeEach
    void reset() {
        Mockito.reset(staffClientMock);
    }

    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Moscow");

    private static Location<YtOfficeInfo> location(long id, String name, String code, String cityName, boolean isDeleted) {
        val info = new YtOfficeInfo(
            new StaffLocalizedString(name + "_ru", name + "_en"),
            code,
            new StaffLocalizedString(cityName + "_ru", cityName + "_en"),
            TIMEZONE,
            isDeleted
        );
        return new Location<>(new LocationId(id), YT_OFFICE_TYPE, name + "_en", Optional.of(info));
    }

    private static StaffOffice office(OffsetDateTime modifiedAt, long id, String name, String code, String cityName, boolean isDeleted) {
        return new StaffOffice(
            new Meta(modifiedAt),
            new LocationId(id),
            new StaffLocalizedString(name + "_ru", name + "_en"),
            code,
            new StaffOffice.City(new StaffLocalizedString(cityName + "_ru", cityName + "_en")),
            TIMEZONE,
            isDeleted
        );
    }

    private List<LocationEntity> getAllLocations() {
        val locations = roLocationRepository.findAll();
        locations.sort(Comparator.comparing(entity -> entity.getId().getValue()));
        return locations;
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that nothing changes if nothing to sync")
    void testEmptyChanges(@Mock TaskExecutionContext context) {
        when(staffClientMock.offices(anyInt(), any(), anyInt(), any(), any()))
            .thenReturn(completedFuture(StaffResult.empty()));

        val locationsBefore = getAllLocations();
        val result = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(result)
            .isNull();

        verify(staffClientMock, only())
            .offices(anyInt(), any(), anyInt(), any(), any());

        assertThat(getAllLocations())
            .containsExactlyElementsOf(locationsBefore);

        verify(context, never())
            .getExecutor();
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that offices synchronization correctly applies changes from staff")
    void testSynchronization(@Mock TaskExecutionContext context) {
        val existingLocations = List.of(
            location(100L, "office1", "code1", "Moscow", false),
            location(101L, "office2", "code2", "Moscow", true),
            location(102L, "office3", "code3", "Bryansk", false)
        );

        CompletableFuture.allOf(StreamEx.of(existingLocations)
            .map(locationManager::insert)
            .toArray(CompletableFuture[]::new)
        ).get();

        val newOffice = office(BC,99L, "office99", "code99", "NY", false);
        val updatedOffice = office(YESTERDAY,100L, "office11", "code11", "Moscow", false);
        val deletedOffice = office(TOMORROW,101L, "del", "", "Trash", true);
        val upToDateOffice = office(TODAY,102L, "office3", "code3", "Bryansk", false);
        val items = List.of(upToDateOffice, deletedOffice, updatedOffice, newOffice);

        when(staffClientMock.offices(anyInt(), any(), anyInt(), any(), any()))
            .thenAnswer(new StaffResultAnswer<>(items, jsonMapper));

        val maxModificationTime = synchronizer.synchronize(context, Optional.empty()).block();
        assertThat(maxModificationTime)
            .isAtSameInstantAs(TOMORROW);

        assertThat(roLocationRepository.count())
            .isEqualTo(4);

        val locations = locationManager.locations(Pageable.first(10), YtOfficeInfo.class).get().getElements();
        assertThat(locations)
            .containsExactlyInAnyOrder(
                location(99L, "office99", "code99", "NY", false),
                location(100L, "office11", "code11", "Moscow", false),
                location(101L, "del", "", "Trash", true),
                location(102L, "office3", "code3", "Bryansk", false)
            );

        verify(staffClientMock, times(3))
            .offices(anyInt(), any(), anyInt(), any(), any());
        verifyNoMoreInteractions(staffClientMock);

        verify(context, never())
            .getExecutor();
    }
}
