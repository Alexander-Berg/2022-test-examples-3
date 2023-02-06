package ru.yandex.market.tpl.tms.service.usershift.distance;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.service.location.calculator.CoordinateStartShiftCleanerService;
import ru.yandex.market.tpl.core.service.location.calculator.UserCoordinateEnrichService;
import ru.yandex.market.tpl.core.service.location.calculator.UserLocationDistanceCalculator;
import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;
import ru.yandex.market.tpl.core.service.location.calculator.util.CoordinateDtoBuilderTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalcUserShiftDistanceServiceTest {

    public static final Long USER_SHIFT_ID = 1L;
    public static final Long USER_ID = 1L;
    @Mock
    private UserLocationDistanceCalculator userLocationDistanceCalculator;
    @Mock
    private UserCoordinateEnrichService userRoutePointEnrichService;
    @Mock
    private CoordinateStartShiftCleanerService startShiftCleanerService;
    @InjectMocks
    private CalcUserShiftDistanceService calcUserShiftDistanceService;


    @DisplayName("Расчёт нулевого пробега")
    @Test
    public void evaluatedDistanceZeroTest() {
        List<CoordinateDto> coordinates = Collections.emptyList();
        when(userRoutePointEnrichService.findEnrichUserLocations(USER_ID, USER_SHIFT_ID))
                .thenReturn(coordinates);
        when(userLocationDistanceCalculator.evaluate(anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(startShiftCleanerService.cleanStartShift(anyList(), eq(USER_SHIFT_ID)))
                .thenReturn(coordinates);

        BigDecimal calc = calcUserShiftDistanceService.calc(USER_ID, USER_SHIFT_ID);

        assertEquals(BigDecimal.ZERO, calc);
        verify(userRoutePointEnrichService, atLeastOnce()).findEnrichUserLocations(USER_ID, USER_SHIFT_ID);
        verify(userLocationDistanceCalculator, atLeastOnce()).evaluate(anyList());
        verify(startShiftCleanerService, atLeastOnce()).cleanStartShift(anyList(), eq(USER_SHIFT_ID));
    }

    @DisplayName("Расчет ненулевого пробега")
    @Test
    public void evaluatedNotEmptyDistanceTest() {
        BigDecimal distance = BigDecimal.valueOf(10);
        List<CoordinateDto> coordinates = CoordinateDtoBuilderTestUtil.buildCoordinateDtos(2);
        when(userRoutePointEnrichService.findEnrichUserLocations(USER_ID, USER_SHIFT_ID))
                .thenReturn(coordinates);
        when(userLocationDistanceCalculator.evaluate(anyList()))
                .thenReturn(distance);
        when(startShiftCleanerService.cleanStartShift(anyList(), eq(USER_SHIFT_ID)))
                .thenReturn(coordinates);

        BigDecimal calculationDistance = calcUserShiftDistanceService.calc(USER_ID, USER_SHIFT_ID);

        assertEquals(calculationDistance, distance);
        verify(userRoutePointEnrichService, atLeastOnce()).findEnrichUserLocations(USER_ID, USER_SHIFT_ID);
        verify(userLocationDistanceCalculator, atLeastOnce()).evaluate(anyList());
        verify(startShiftCleanerService, atLeastOnce()).cleanStartShift(anyList(), eq(USER_SHIFT_ID));
    }

    @DisplayName("Расчет пробега с учётом очищенных точек")
    @Test
    public void evaluatedNotEmptyDistanceAndClearRoutePointTest() {
        BigDecimal distance = BigDecimal.valueOf(10);
        List<CoordinateDto> richCoordinates = CoordinateDtoBuilderTestUtil.buildCoordinateDtos(10);
        List<CoordinateDto> clearCoordinates = CoordinateDtoBuilderTestUtil.buildCoordinateDtos(2);
        when(userRoutePointEnrichService.findEnrichUserLocations(USER_ID, USER_SHIFT_ID))
                .thenReturn(richCoordinates);
        when(startShiftCleanerService.cleanStartShift(anyList(), eq(USER_SHIFT_ID)))
                .thenReturn(clearCoordinates);
        ArgumentCaptor<List<CoordinateDto>> coordinatesCaptor =
                ArgumentCaptor.forClass(List.class);
        when(userLocationDistanceCalculator.evaluate(coordinatesCaptor.capture()))
                .thenReturn(distance);

        BigDecimal calculationDistance = calcUserShiftDistanceService.calc(USER_ID, USER_SHIFT_ID);

        assertEquals(calculationDistance, distance);
        verify(userRoutePointEnrichService, atLeastOnce()).findEnrichUserLocations(USER_ID, USER_SHIFT_ID);
        verify(startShiftCleanerService, atLeastOnce()).cleanStartShift(anyList(), eq(USER_SHIFT_ID));
        verify(userLocationDistanceCalculator, atLeastOnce()).evaluate(anyList());
        assertEquals(clearCoordinates, coordinatesCaptor.getValue());
    }

}
