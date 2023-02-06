package ru.yandex.market.tpl.core.service.location.calculator;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;
import ru.yandex.market.tpl.core.service.location.calculator.util.CoordinateDtoBuilderTestUtil;
import ru.yandex.market.tpl.core.service.location.calculator.util.RoutePointBuilderTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoordinateStartShiftCleanerServiceTest {

    public static final long USER_SHIFT_ID = 1L;
    @InjectMocks
    private CoordinateStartShiftCleanerService startShiftCleanerService;
    @Mock
    private UserShiftRepository userShiftRepository;


    @DisplayName("Пустой список координат и нет роут поинтов")
    @Test
    public void emptyListWithoutRoutePoint() {
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(List.of());
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);

        List<CoordinateDto> coordinates = startShiftCleanerService.cleanStartShift(Collections.emptyList(),
                USER_SHIFT_ID);

        assertTrue(coordinates.isEmpty());
    }

    @DisplayName("Непустой список координат и нет роут поинтов")
    @Test
    public void coordinateListWithoutRoutePoint() {
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(List.of());
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);
        List<CoordinateDto> coordinateList = CoordinateDtoBuilderTestUtil.buildCoordinateDtos(5);

        List<CoordinateDto> coordinates = startShiftCleanerService.cleanStartShift(coordinateList,
                USER_SHIFT_ID);

        assertTrue(coordinates.isEmpty());
    }

    @DisplayName("Успешная фильтрация точек, когда не остаётся точек после фильтрации")
    @Test
    public void cleanStartShiftingSuccess() {
        Instant now = Instant.now();
        List<RoutePoint> routePoints = List.of(
                RoutePointBuilderTestUtil.buildRoutePoint(0, 0, now),
                RoutePointBuilderTestUtil.buildRoutePoint(1, 1, now.plusSeconds(100))
        );
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(routePoints);
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);
        List<CoordinateDto> coordinateList = List.of(
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(0, 0, now.minusSeconds(100))
        );

        List<CoordinateDto> coordinates = startShiftCleanerService.cleanStartShift(coordinateList,
                USER_SHIFT_ID);

        assertTrue(coordinates.isEmpty());
    }

    @DisplayName("Успешная фильтрация точек, когда остаются точки после фильтрации")
    @Test
    public void cleanStartShiftingSuccessNotEmptyResult() {
        Instant now = Instant.now();
        List<RoutePoint> routePoints = List.of(
                RoutePointBuilderTestUtil.buildRoutePoint(2, 2, now),
                RoutePointBuilderTestUtil.buildRoutePoint(1, 1, now.minusSeconds(100)), // первая точка
                RoutePointBuilderTestUtil.buildRoutePoint(3, 3, now.plusSeconds(100))
        );
        UserShift mockUserShift = new UserShift();
        mockUserShift.setRoutePoints(routePoints);
        when(userShiftRepository.findByIdOrThrow(USER_SHIFT_ID))
                .thenReturn(mockUserShift);
        List<CoordinateDto> coordinateList = List.of(
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(1, 1, now.minusSeconds(50)),    // +
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(-1, -1, now.minusSeconds(200)), // -
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(2, 2, now.plusSeconds(200)),    // +
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(3, 3, now.plusSeconds(250))     // +
        );

        List<CoordinateDto> coordinates = startShiftCleanerService.cleanStartShift(coordinateList,
                USER_SHIFT_ID);

        assertFalse(coordinates.isEmpty());
        assertEquals(coordinates.size(), 3);
    }

}
