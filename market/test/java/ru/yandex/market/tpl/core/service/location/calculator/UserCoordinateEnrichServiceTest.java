package ru.yandex.market.tpl.core.service.location.calculator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocationRepository;
import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;
import ru.yandex.market.tpl.core.service.location.calculator.util.RoutePointBuilderTestUtil;
import ru.yandex.market.tpl.core.service.location.calculator.util.UserLocationBuilderTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCoordinateEnrichServiceTest {

    public static final long USER_SHIFT_ID = 1L;
    public static final long USER_ID = 1L;
    @Mock
    private UserLocationRepository userLocationRepository;
    @Mock
    private UserShiftRepository userShiftRepository;
    @InjectMocks
    private UserCoordinateEnrichService userCoordinateEnrichService;

    @DisplayName("Пустые координаты и routPoint")
    @Test
    public void emptyUserShiftCoordinateTest() {
        when(userLocationRepository.findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID))
                .thenReturn(Stream.empty());
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(List.of());
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);

        List<CoordinateDto> enrichUserLocations = userCoordinateEnrichService.findEnrichUserLocations(USER_ID,
                USER_SHIFT_ID);

        assertNotNull(enrichUserLocations);
        assertEquals(enrichUserLocations.size(), 0);
        verify(userLocationRepository, atLeastOnce()).findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID);
        verify(userShiftRepository, atLeastOnce()).findByIdOrThrow(USER_SHIFT_ID);
    }

    @DisplayName("Валидное обогащение координат")
    @Test
    public void validEnrichTest() {
        int countUserLocation = 2;
        List<UserLocation> userLocations = UserLocationBuilderTestUtil.buildUserLocations(countUserLocation);
        int countRoutePoint = 2;
        List<RoutePoint> routePoints = RoutePointBuilderTestUtil.buildRoutePoints(countRoutePoint);
        when(userLocationRepository.findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID))
                .thenReturn(userLocations.stream());
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(routePoints);
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);

        List<CoordinateDto> enrichUserLocations = userCoordinateEnrichService.findEnrichUserLocations(USER_ID,
                USER_SHIFT_ID);

        assertNotNull(enrichUserLocations);
        assertEquals(enrichUserLocations.size(), countUserLocation + countRoutePoint);
        verify(userLocationRepository, atLeastOnce()).findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID);
        verify(userShiftRepository, atLeastOnce()).findByIdOrThrow(USER_SHIFT_ID);
    }

    @DisplayName("Обогощенние пустых координат точками посещенных routePoint")
    @Test
    public void enrichEmptyUserLocationTest() {
        int countRoutePoint = 2;
        List<RoutePoint> routePoints = RoutePointBuilderTestUtil.buildRoutePoints(countRoutePoint);
        when(userLocationRepository.findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID))
                .thenReturn(Stream.empty());
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(routePoints);
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);

        List<CoordinateDto> enrichUserLocations = userCoordinateEnrichService.findEnrichUserLocations(USER_ID,
                USER_SHIFT_ID);

        assertNotNull(enrichUserLocations);
        assertEquals(enrichUserLocations.size(), countRoutePoint);
        verify(userLocationRepository, atLeastOnce()).findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID);
        verify(userShiftRepository, atLeastOnce()).findByIdOrThrow(USER_SHIFT_ID);
    }

    @DisplayName("Часть координат и routePoint не валидные")
    @Test
    public void enrichWithHalfValidCoordinateAndRoutePointTest() {
        int countUserLocation = 2;
        List<UserLocation> userLocations = UserLocationBuilderTestUtil.buildUserLocations(countUserLocation);
        userLocations.add(UserLocationBuilderTestUtil.buildUserLocation(100, 100, null));
        int countRoutePoint = 2;
        List<RoutePoint> routePoints = RoutePointBuilderTestUtil.buildRoutePoints(countRoutePoint);
        routePoints.add(RoutePointBuilderTestUtil.buildRoutePoint(100, 100, null));
        routePoints.add(RoutePointBuilderTestUtil.buildRoutePoint(BigDecimal.ONE, null, null));
        when(userLocationRepository.findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID))
                .thenReturn(userLocations.stream());
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(routePoints);
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);

        List<CoordinateDto> enrichUserLocations = userCoordinateEnrichService.findEnrichUserLocations(USER_ID,
                USER_SHIFT_ID);

        assertNotNull(enrichUserLocations);
        assertEquals(enrichUserLocations.size(), countUserLocation + countRoutePoint);
        verify(userLocationRepository, atLeastOnce()).findByUserIdAndUserShiftIdOrderByIdDesc(USER_ID, USER_SHIFT_ID);
        verify(userShiftRepository, atLeastOnce()).findByIdOrThrow(USER_SHIFT_ID);
    }

}
