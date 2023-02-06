package ru.yandex.market.logistics.management.controller.admin.deliveryInterval;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.deliveryInterval.DeliveryIntervalSnapshotNewDto;
import ru.yandex.market.logistics.management.domain.dto.front.deliveryInterval.YtDeliveryIntervalSnapshotDto;
import ru.yandex.market.logistics.management.exception.BadRequestException;
import ru.yandex.market.logistics.management.facade.PartnerDeliveryIntervalSnapshotFacade;
import ru.yandex.market.logistics.management.util.TestableClock;

@DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/manualCreation/prepare_database.xml")
public class PartnerDeliveryIntervalSnapshotFacadeTest extends AbstractContextualTest {

    private static final String SCHEDULE_PATH =
        "/data/controller/admin/deliveryIntervalSnapshots/manualCreation/schedules.csv";

    @Autowired
    private PartnerDeliveryIntervalSnapshotFacade partnerDeliveryIntervalSnapshotFacade;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        LocalDate today = LocalDate.of(2020, 12, 4);
        clock.setFixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    @Test
    void testDifferentPartnerIdsFail() {
        Exception exception = Assertions.assertThrows(BadRequestException.class, () ->
            partnerDeliveryIntervalSnapshotFacade.createSnapshot(
                getSnapshotDto(
                    SCHEDULE_PATH,
                    "/data/controller/admin/deliveryIntervalSnapshots/manualCreation" +
                        "/calendar_for_another_partner.csv"

                )));
        softly.assertThat(exception.getMessage())
            .isEqualTo("400 BAD_REQUEST \"Calendar partner id 1 not equals schedule partner id 2\"");
    }

    @Test
    void testDifferentLocationForCalendarFail() {
        Exception exception = Assertions.assertThrows(BadRequestException.class, () ->
            partnerDeliveryIntervalSnapshotFacade.createSnapshot(
                getSnapshotDto(
                    SCHEDULE_PATH,
                    "/data/controller/admin/deliveryIntervalSnapshots/manualCreation" +
                        "/calendar_with_different_location.csv"

                )));
        softly.assertThat(exception.getMessage())
            .isEqualTo("400 BAD_REQUEST \"There is no delivery interval for calendar with location id 4\"");
    }

    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/manualCreation/without_calendars.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testWithoutCalendars() throws IOException {
        partnerDeliveryIntervalSnapshotFacade.createSnapshot(
            getSnapshotDto(
                SCHEDULE_PATH,
                null
            ));
    }

    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/manualCreation/with_calendars.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testWithCalendars() throws IOException {
        partnerDeliveryIntervalSnapshotFacade.createSnapshot(
            getSnapshotDto(
                SCHEDULE_PATH,
                "/data/controller/admin/deliveryIntervalSnapshots/manualCreation/calendars.csv"
            ));
    }

    @DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/expired_snapshots.xml")
    @ExpectedDatabase(
        value = "/data/controller/admin/deliveryIntervalSnapshots/expired_snapshots.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testExtractingExpiredSnapshots() {
        List<YtDeliveryIntervalSnapshotDto> snapshotsForYt =
            partnerDeliveryIntervalSnapshotFacade.extractIntervalsForYtTransportation();

        List<Long> ids =
            snapshotsForYt.stream().map(YtDeliveryIntervalSnapshotDto::getId).collect(Collectors.toList());
        softly.assertThat(ids).containsAll(Set.of(2L, 3L));
    }

    private DeliveryIntervalSnapshotNewDto getSnapshotDto(String schedulePath, String calendarPath) throws IOException {
        byte[] scheduleBytes = FileUtils.readFileToByteArray(new ClassPathResource(schedulePath).getFile());
        byte[] calendarBytes = null;
        if (calendarPath != null) {
            calendarBytes = FileUtils.readFileToByteArray(new ClassPathResource(calendarPath).getFile());
        }

        return new DeliveryIntervalSnapshotNewDto(scheduleBytes, calendarBytes);
    }
}
