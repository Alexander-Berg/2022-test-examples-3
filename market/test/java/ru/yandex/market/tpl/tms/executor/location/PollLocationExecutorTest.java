package ru.yandex.market.tpl.tms.executor.location;


import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.common.taxi.driver.trackstory.client.api.DefaultTaxiDriverTrackStoryTrackApi;
import ru.yandex.market.tpl.common.taxi.driver.trackstory.client.model.GetTrackResponse;
import ru.yandex.market.tpl.common.taxi.driver.trackstory.client.model.GpsPosition;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocationRepository;
import ru.yandex.market.tpl.core.service.location.LocationService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class PollLocationExecutorTest extends TplTmsAbstractTest {

    private final PollLocationExecutor executor;
    private final UserLocationRepository locationRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final TestUserHelper userHelper;
    private final DefaultTaxiDriverTrackStoryTrackApi taxiDriverTrackStoryTrackApi;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final OrderGenerateService orderGenerateService;
    private final LocationService locationService;

    private static final String YA_PRO_ID = "TEST";

    @BeforeEach
    void setup() {
        ClockUtil.initFixed(clock, LocalDateTime.ofInstant(Instant.now(), TimeZoneUtil.DEFAULT_OFFSET));
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.IS_TAXI_LOCATION_ENABLED))
                .thenReturn(true);
        when(configurationProviderAdapter.getValueAsLong(ConfigurationProperties.TRACK_REQUEST_BEFORE_INTERVAL_IN_MINUTES))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void clear() {
        Mockito.reset(taxiDriverTrackStoryTrackApi);
    }

    @Test
    void locationTestWhenAllLocationShouldBeSync()  throws Exception {
        User proUser = userHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .yaProId(YA_PRO_ID)
                .workdate(LocalDate.now(clock))
                .build());
        UserShift userShift = createUserShift(proUser);

        List<FakePosition> fakePositions = List.of(
                new FakePosition(BigDecimal.ONE, BigDecimal.ZERO,
                        Instant.now().minusSeconds(2L).truncatedTo(ChronoUnit.SECONDS)),
                new FakePosition(BigDecimal.ZERO, BigDecimal.ONE,
                        Instant.now().minusSeconds(1L).truncatedTo(ChronoUnit.SECONDS))
        );

        when(taxiDriverTrackStoryTrackApi.getTrackPost(any())).thenReturn(constructFakeResponse(fakePositions));

        executor.doRealJob(null);

        List<LocationDto> locations = findLocations(proUser, userShift);

        assertThat(locations).containsAll(constructLocationFromPositions(userShift, fakePositions));
    }

    @Test
    void locationTestWhenOneLocationIsDuplicate()  throws Exception {
        User proUser = userHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .yaProId(YA_PRO_ID)
                .workdate(LocalDate.now(clock))
                .build());
        UserShift userShift = createUserShift(proUser);
        locationRepository.deleteAllById(
            locationRepository.findUserLocationsForLastHour(proUser.getId())
                    .stream()
                    .map(UserLocation::getId)
                    .collect(Collectors.toList())
        );

        FakePosition duplicatePosition =  new FakePosition(BigDecimal.ONE, BigDecimal.ZERO,
                Instant.now().minusSeconds(2L).truncatedTo(ChronoUnit.SECONDS));

        locationRepository.saveAll(
                constructLocationFromPositions(userShift, List.of(duplicatePosition))
                        .stream()
                        .map(l ->  locationService.constructUserLocation(proUser, l))
                        .collect(Collectors.toList())
        );

        List<FakePosition> fakePositions = List.of(
                new FakePosition(BigDecimal.ONE, BigDecimal.ZERO,
                        Instant.now().minusSeconds(2L).truncatedTo(ChronoUnit.SECONDS)),
                new FakePosition(BigDecimal.ZERO, BigDecimal.ONE,
                        Instant.now().minusSeconds(1L).truncatedTo(ChronoUnit.SECONDS))
        );

        when(taxiDriverTrackStoryTrackApi.getTrackPost(any())).thenReturn(constructFakeResponse(fakePositions));

        executor.doRealJob(null);

        List<LocationDto> locations = findLocations(proUser, userShift);
        assertThat(locations).hasSize(2);
        assertThat(locations).containsExactlyInAnyOrderElementsOf(constructLocationFromPositions(userShift, fakePositions));
    }


    @Nullable
    private List<LocationDto> findLocations(User proUser, UserShift userShift) {
        return transactionTemplate.execute(a -> locationRepository.findByUserIdAndUserShiftIdOrderByIdDesc(
                        proUser.getId(), userShift.getId())
                .map(ul -> new LocationDto(
                        ul.getLongitude(), ul.getLatitude(), null,
                        ul.getUserShiftId(), ul.getPostedAt()))
                .collect(Collectors.toList()));
    }

    @NotNull
    private UserShift createUserShift(User proUser) {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .build());

        UserShift userShift = userHelper.createEmptyShift(proUser, LocalDate.now(clock));
        User user = userShift.getUser();
        var task = userHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        userHelper.openShift(user, userShift.getId());
        userHelper.finishPickupAtStartOfTheDay(userShift);
        return userShift;
    }



    private GetTrackResponse constructFakeResponse(List<FakePosition> positions) {
        GetTrackResponse getTrackResponse = new GetTrackResponse();
        getTrackResponse.track(positions
                        .stream()
                        .map(p -> new GpsPosition().lat(p.lat).lon(p.lon).timestamp(p.timestamp.getEpochSecond()))
                        .collect(Collectors.toList())
        );
        return getTrackResponse;
    }

    private List<LocationDto> constructLocationFromPositions(UserShift userShift, List<FakePosition> positions) {
        return positions
                .stream()
                .map(p -> new LocationDto(p.getLon(), p.getLat(), null, userShift.getId(),  p.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Data
    @Builder
    private static class FakePosition {
        private BigDecimal lat;
        private BigDecimal lon;
        private Instant timestamp;
    }
}
