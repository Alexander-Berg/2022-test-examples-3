package ru.yandex.market.tpl.carrier.tms.executor.location;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.db.QueryCountAssertions;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.location.UserLocationRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;
import ru.yandex.mj.generated.client.taxi_driver_trackstory.api.TaxiDriverTrackstoryApiClient;
import ru.yandex.mj.generated.client.taxi_driver_trackstory.model.DriverPosition;
import ru.yandex.mj.generated.client.taxi_driver_trackstory.model.GpsPosition;
import ru.yandex.mj.generated.client.taxi_driver_trackstory.model.PositionType;
import ru.yandex.mj.generated.client.taxi_driver_trackstory.model.PositionsResponse;

@TmsIntTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class PollLocationExecutorTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final PollLocationExecutor executor;
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final UserLocationRepository userLocationRepository;
    private final TaxiDriverTrackstoryApiClient taxiDriverTrackstoryApiClient;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;
    private UserShift userShift;
    private Instant now;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.IS_ADJUSTED_TAXI_LOCATION, false);

        user = testUserHelper.findOrCreateUser(UserUtil.TAXI_ID, UserUtil.UID);
        var transport = testUserHelper.findOrCreateTransport("Газелька", Company.DEFAULT_COMPANY_NAME);
        var run = runGenerator.generate();
        userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());

        now = Instant.now();
        long epochSecond = now.toEpochMilli() / 1000;
        now = Instant.ofEpochSecond(epochSecond);

        ExecuteCall<PositionsResponse, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule())
                .thenReturn(CompletableFuture.completedFuture(new PositionsResponse().addResultsItem(new DriverPosition()
                        .driverId(UserUtil.TAXI_ID)
                        .type(PositionType.RAW)
                        .position(new GpsPosition().lat(BigDecimal.ONE)
                                .lon(BigDecimal.ONE)
                                .direction(BigDecimal.ZERO)
                                .speed(BigDecimal.TEN)
                                .timestamp(epochSecond)))));

        Mockito.when(taxiDriverTrackstoryApiClient.positionsPost(Mockito.any()))
                .thenReturn(call);
    }


    @SneakyThrows
    @Test
    @Transactional
    void test() {
        executor.doRealJob(null);

        var location = userLocationRepository.findLastLocation(user.getId(), userShift.getId()).orElseThrow();

        Assertions.assertEquals(user.getId(), location.getUser().getId());
        Assertions.assertEquals(BigDecimal.ONE, location.getGeoPoint().getLatitude().stripTrailingZeros());
        Assertions.assertEquals(BigDecimal.ONE, location.getGeoPoint().getLongitude().stripTrailingZeros());
        Assertions.assertEquals(userShift.getId(), location.getUserShiftId());
        Assertions.assertEquals(now, location.getExtractedAt());
        Assertions.assertEquals(BigDecimal.ZERO, location.getBearingDegrees());
        Assertions.assertEquals(BigDecimal.TEN, location.getSpeedMs());

        executor.doRealJob(null);
        executor.doRealJob(null);
        executor.doRealJob(null);

        var count = userLocationRepository.findByUserIdAndUserShiftIdOrderByIdDesc(user.getId(),
                        userShift.getId()).size();
        Assertions.assertEquals(1, count);
    }

    @Test
    void shouldNotDoTooMuchRequests() {
        var user2 = testUserHelper.findOrCreateUser(UserUtil.ANOTHER_TAXI_ID, UserUtil.ANOTHER_UID,
                UserUtil.ANOTHER_PHONE);

        var transport = testUserHelper.findOrCreateTransport("Урал", Company.DEFAULT_COMPANY_NAME);

        var run2 = runGenerator.generate();
        var userShift2 = runHelper.assignUserAndTransport(run2, user2, transport);
        testUserHelper.openShift(user2, userShift2.getId());

        long epochSecond = now.toEpochMilli() / 1000;

        ExecuteCall<PositionsResponse, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule())
                .thenReturn(CompletableFuture.completedFuture(new PositionsResponse()
                        .addResultsItem(new DriverPosition()
                                .driverId(UserUtil.TAXI_ID)
                                .type(PositionType.RAW)
                                .position(new GpsPosition().lat(BigDecimal.ONE)
                                        .lon(BigDecimal.ONE)
                                        .direction(BigDecimal.ZERO)
                                        .speed(BigDecimal.TEN)
                                        .timestamp(epochSecond)))
                        .addResultsItem(new DriverPosition()
                                .driverId(UserUtil.ANOTHER_TAXI_ID)
                                .type(PositionType.RAW)
                                .position(new GpsPosition().lat(BigDecimal.ONE)
                                        .lon(BigDecimal.ONE)
                                        .direction(BigDecimal.ZERO)
                                        .speed(BigDecimal.TEN)
                                        .timestamp(epochSecond)))
                        ));

        Mockito.when(taxiDriverTrackstoryApiClient.positionsPost(Mockito.any()))
                .thenReturn(call);

        // 3 на получение батча
        // 5 * 2 на каждую вставку
        // 1 * 2 на каждую проверку прошлого обновления ожидаемого времени прибытия
        // 1 * 2 инсерта в queue_task
        // 1 * 2 обновления последнего времени обработки estimated_time
        // 1 * 2 инсерта в queue_log
        // select value from configuration where key = ? = 5
        QueryCountAssertions.assertQueryCountTotalEqual(26, () -> {
            executor.doRealJob(null);
            return null;
        });


    }
}
