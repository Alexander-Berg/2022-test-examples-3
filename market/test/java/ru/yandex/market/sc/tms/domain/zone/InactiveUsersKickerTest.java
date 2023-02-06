package ru.yandex.market.sc.tms.domain.zone;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.operation_log.repository.OperationLogRepository;
import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.repository.OrderScanLogEntryRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.zone.ZoneQueryService;
import ru.yandex.market.sc.core.domain.zone.model.ZoneStatisticDto;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.setupMockClock;
import static ru.yandex.market.sc.core.test.TestFactory.setupMockClockToSystemTime;
import static ru.yandex.market.sc.tms.domain.zone.InactiveUsersKicker.USER_KICKER_FROM_ZONE_JOB;

@EmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class InactiveUsersKickerTest {

    private final InactiveUsersKicker inactiveUsersKicker;
    private final ZoneQueryService zoneQueryService;
    private final OperationLogRepository operationLogRepository;
    private final OrderScanLogEntryRepository orderScanLogEntryRepository;
    private final ScanService scanService;
    private final TestFactory testFactory;
    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    Process process;

    @BeforeEach
    void init() {
        setupMockClockToSystemTime(clock);

        sortingCenter = testFactory.storedSortingCenter();
        process = testFactory.storedCheckInAndLeaveOperation();

        testFactory.setConfiguration(ConfigurationProperties.INACTIVE_USERS_KICKER_JOB_ENABLED, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SC_OPERATION_MODE_WITH_ZONE_ENABLED, true);
    }

    @Test
    @SneakyThrows
    void excludeInactiveUsersFromZoneOperationLogActive() {
        var zone = testFactory.storedZone(sortingCenter, "1", List.of(process));
        var ws = testFactory.storedWorkstation(sortingCenter, "1.1", zone.getId(), process);

        testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 125L));
        testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 129L));
        testFactory.checkInZone(ws, testFactory.storedUser(sortingCenter, 132L));
        testFactory.checkInZone(ws, testFactory.storedUser(sortingCenter, 135L));

        assertThat(zoneQueryService.getZones(sortingCenter))
                .extracting("name", "statistic")
                .contains(tuple(zone.getName(), new ZoneStatisticDto(2, 4, 4, 1)));

        setupMockClock(clock, clock.instant().plus(1L, ChronoUnit.HOURS));
        inactiveUsersKicker.excludeInactiveUsersFromZone();

        assertThat(zoneQueryService.getZones(sortingCenter))
                .extracting("name", "statistic")
                .contains(tuple(zone.getName(), new ZoneStatisticDto(0, 0, 0, 1)));
        assertThat(operationLogRepository.findAll())
                .filteredOn(operationLog -> Objects.equals(operationLog.getSuffix(), USER_KICKER_FROM_ZONE_JOB))
                .hasSize(4);
    }

    @Test
    @SneakyThrows
    void excludeInactiveUsersFromZoneAnalyseOrderScanLogActive() {
        testFactory.createForToday(order(sortingCenter, "o1").places("o1-1").build()).get();
        var zone = testFactory.storedZone(sortingCenter, "1", List.of(process));
        var ws = testFactory.storedWorkstation(sortingCenter, "1.1", zone.getId(), process);

        testFactory.checkInZone(zone, testFactory.storedUser(sortingCenter, 125L));
        testFactory.checkInZone(ws, testFactory.storedUser(sortingCenter, 135L));
        setupMockClock(clock, clock.instant().plus(30, ChronoUnit.MINUTES));
        scanService.acceptOrder(
                new AcceptOrderRequestDto("o1", "o1-1"),
                new ScContext(testFactory.storedUser(sortingCenter, 135L), ws)
        );

        setupMockClock(clock, clock.instant().plus(30, ChronoUnit.MINUTES));
        inactiveUsersKicker.excludeInactiveUsersFromZone();

        assertThat(zoneQueryService.getZones(sortingCenter))
                .extracting("name", "statistic")
                .contains(tuple(zone.getName(), new ZoneStatisticDto(0, 1, 1, 1)));
        assertThat(operationLogRepository.findAll())
                .filteredOn(operationLog -> Objects.equals(operationLog.getSuffix(), USER_KICKER_FROM_ZONE_JOB))
                .hasSize(1);
        assertThat(orderScanLogEntryRepository.findAll()).hasSize(1);
    }
}
