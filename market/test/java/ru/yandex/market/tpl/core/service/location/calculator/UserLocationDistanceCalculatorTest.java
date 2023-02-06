package ru.yandex.market.tpl.core.service.location.calculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;
import ru.yandex.market.tpl.core.service.location.calculator.util.CoordinateDtoBuilderTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserLocationDistanceCalculatorTest {

    @Mock
    private UserLocationSpoofingService spoofingService;
    @InjectMocks
    private UserLocationDistanceCalculator calculator;

    @DisplayName("Расчёт пробега на пустом треке")
    @Test
    public void emptyListCalculatorEvaluateTest() {
        List<CoordinateDto> emptyList = Collections.emptyList();
        when(spoofingService.removeSpoofingCoordinate(anyList()))
                .thenReturn(emptyList);

        BigDecimal distance = calculator.evaluate(emptyList);

        assertEquals(distance, BigDecimal.ZERO);
        verify(spoofingService, never()).removeSpoofingCoordinate(anyList());
    }

    @DisplayName("Расчёт пробега в листе, размер которого меньше или равен 2")
    @Test
    public void tinyListCalculatorEvaluateTest() {
        List<CoordinateDto> tinyList = List.of(
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(0, 0, Instant.now()),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(45, 45, Instant.now())
        );
        when(spoofingService.removeSpoofingCoordinate(anyList()))
                .thenReturn(tinyList);

        BigDecimal distance = calculator.evaluate(tinyList);

        assertEquals(distance, BigDecimal.ZERO);
        verify(spoofingService, never()).removeSpoofingCoordinate(anyList());
    }

    @DisplayName("Расчёт пробега в валидном листе")
    @Test
    public void validListCalculatorEvaluateTest() {
        List<CoordinateDto> tinyList = List.of(
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(0, 0, Instant.now()),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(45, 45, Instant.now()),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(90, 90, Instant.now())
        );
        when(spoofingService.removeSpoofingCoordinate(anyList()))
                .thenReturn(tinyList);

        BigDecimal distance = calculator.evaluate(tinyList);

        assertTrue(distance.intValue() > 0);
        verify(spoofingService, atLeastOnce()).removeSpoofingCoordinate(anyList());
    }
}
