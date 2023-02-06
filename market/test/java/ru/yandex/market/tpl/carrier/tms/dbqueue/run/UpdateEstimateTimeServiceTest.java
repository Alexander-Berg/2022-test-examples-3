package ru.yandex.market.tpl.carrier.tms.dbqueue.run;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.dbqueue.model.DriverQueueType;
import ru.yandex.market.tpl.carrier.core.domain.location.TestLocationService;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.LocationData;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.maps.client.Location;
import ru.yandex.market.tpl.common.maps.client.MapsClient;
import ru.yandex.market.tpl.common.maps.client.RouteSummary;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@TmsIntTest
public class UpdateEstimateTimeServiceTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final DbQueueTestUtil dbQueueTestUtil;

    private final TestLocationService testLocationService;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final UserShiftRepository userShiftRepository;

    private final MapsClient mapsClient;

    private UserShift userShift;
    private User user;

    @BeforeEach
    void setUp() {
        Run run = runGenerator.generate();
        user = testUserHelper.findOrCreateUser(1L);
        Transport transport = testUserHelper.findOrCreateTransport();

        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @Test
    void shouldUpdateEstimateTime() {
        var nextPoint = userShift.streamRoutePoints()
                .removeBy(RoutePoint::getStatus, RoutePointStatus.FINISHED)
                .findFirst().get();

        Mockito.when(mapsClient.getRouteSummary(List.of(
                Location.builder()
                        .longitude(BigDecimal.TEN)
                        .latitude(BigDecimal.TEN)
                        .build(),
                Location.builder()
                        .longitude(nextPoint.getLongitude())
                        .latitude(nextPoint.getLatitude())
                        .build()
        ))).thenReturn(Optional.of(RouteSummary.builder()
                .timeSeconds(600)
                .build()));

        testLocationService.saveLocation(user, LocationData.builder()
                .longitude(BigDecimal.TEN)
                .latitude(BigDecimal.TEN)
                .userShiftId(userShift.getId())
                .build());
        testLocationService.saveLocation(user, LocationData.builder()
                .longitude(BigDecimal.TEN)
                .latitude(BigDecimal.TEN)
                .userShiftId(userShift.getId())
                .build());

        dbQueueTestUtil.assertQueueHasSize(DriverQueueType.UPDATE_ESTIMATE_TIME, 1);
        dbQueueTestUtil.executeAllQueueItems(DriverQueueType.UPDATE_ESTIMATE_TIME);

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.getEstimatedTime()).isNotNull();
    }
}
