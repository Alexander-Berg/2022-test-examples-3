package ru.yandex.market.tpl.core.service.location.calculator;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;
import ru.yandex.market.tpl.core.service.location.calculator.util.CoordinateDtoBuilderTestUtil;

import static java.lang.Math.round;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordinateDistanceCalculatorTest {

    @DisplayName("Расчёт пробега на пустом треке")
    @Test
    public void evaluateDistanceByEmptyCoordinateList() {
        ArrayList<CoordinateDto> coordinates = new ArrayList<>();

        double distance = CoordinateDistanceCalculator.calculate(coordinates);

        assertEquals(distance, 0);
    }

    @DisplayName("Расчёт между 2 точками Москвой и Питером")
    @Test
    public void evaluateDistanceBetweenMoscowAndSaintPetersburgTest() {
        CoordinateDto moscowCoordinate = CoordinateDtoBuilderTestUtil.buildCoordinateDto(55.753215, 37.622504);
        CoordinateDto saintPetersburgCoordinate = CoordinateDtoBuilderTestUtil.buildCoordinateDto(59.938951, 30.315635);
        List<CoordinateDto> coordinates = List.of(moscowCoordinate, saintPetersburgCoordinate);

        double distance = CoordinateDistanceCalculator.calculate(coordinates);

        assertEquals(round(distance), 634590);
    }

    @DisplayName("Расчёт пробега на малой дистанции по Москве")
    @Test
    public void evaluateLocaleLittleDistanceTest() {
        List<CoordinateDto> coordinates = CoordinateDtoBuilderTestUtil.buildMoscowTrack();

        double distance = CoordinateDistanceCalculator.calculate(coordinates);

        assertEquals(round(distance), 9981);
    }

    @DisplayName("Расчёт пробега по приблизительному маршруту СЦ Воронеж")
    @Test
    public void evaluateDistanceOnScVoronezhTest() {
        List<CoordinateDto> coordinates = CoordinateDtoBuilderTestUtil.buildVoronezhScTrack();

        double distance = CoordinateDistanceCalculator.calculate(coordinates);

        assertEquals(round(distance), 54016);
    }
}
