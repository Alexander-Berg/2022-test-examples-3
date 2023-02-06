package ru.yandex.market.partner.mvc.controller.agency;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.agency.program.quarter.Quarter;
import ru.yandex.market.core.util.DateTimes;

/**
 * тест на перевод Instant в RewardQuarter.
 * Проверяется первый и последний день каждого месяца.
 */
public class ArpInstantQuarterConverterTest {

    static Stream<Arguments> rewardQuarterProvider() {
        return Stream.of(
                Arguments.of(Quarter.of(2019, 4), 2020, 1, 1),
                Arguments.of(Quarter.of(2019, 4), 2020, 1, 31),
                Arguments.of(Quarter.of(2019, 4), 2020, 2, 1),
                Arguments.of(Quarter.of(2019, 4), 2020, 2, 28),
                Arguments.of(Quarter.of(2020, 1), 2020, 3, 1),
                Arguments.of(Quarter.of(2020, 1), 2020, 3, 31),
                Arguments.of(Quarter.of(2020, 1), 2020, 4, 1),
                Arguments.of(Quarter.of(2020, 1), 2020, 4, 30),
                Arguments.of(Quarter.of(2020, 1), 2020, 5, 1),
                Arguments.of(Quarter.of(2020, 1), 2020, 5, 31),
                Arguments.of(Quarter.of(2020, 2), 2020, 6, 1),
                Arguments.of(Quarter.of(2020, 2), 2020, 6, 30),
                Arguments.of(Quarter.of(2020, 2), 2020, 7, 1),
                Arguments.of(Quarter.of(2020, 2), 2020, 8, 1),
                Arguments.of(Quarter.of(2020, 2), 2020, 8, 31),
                Arguments.of(Quarter.of(2020, 3), 2020, 9, 1),
                Arguments.of(Quarter.of(2020, 3), 2020, 9, 30),
                Arguments.of(Quarter.of(2020, 3), 2020, 10, 1),
                Arguments.of(Quarter.of(2020, 3), 2020, 10, 31),
                Arguments.of(Quarter.of(2020, 3), 2020, 11, 1),
                Arguments.of(Quarter.of(2020, 3), 2020, 11, 30),
                Arguments.of(Quarter.of(2020, 4), 2020, 12, 1),
                Arguments.of(Quarter.of(2020, 4), 2020, 12, 31),
                Arguments.of(Quarter.of(2020, 4), 2021, 1, 1),
                Arguments.of(Quarter.of(2020, 4), 2021, 1, 31),
                Arguments.of(Quarter.of(2020, 4), 2021, 2, 1),
                Arguments.of(Quarter.of(2020, 4), 2021, 2, 28)
        );
    }

    static Stream<Arguments> testGetEndDateProvider() {
        return Stream.of(
                Arguments.of(Quarter.of(2019, 1), 2019, 5, 31),
                Arguments.of(Quarter.of(2019, 2), 2019, 8, 31),
                Arguments.of(Quarter.of(2019, 3), 2019, 11, 30),
                Arguments.of(Quarter.of(2019, 4), 2020, 2, 29),
                Arguments.of(Quarter.of(2020, 4), 2021, 2, 28)
        );
    }

    @ParameterizedTest
    @MethodSource("rewardQuarterProvider")
    void testRewardQuarter(Quarter q, int year, int month, int day) {
        Assertions.assertEquals(q, Quarter.rewardQuarter(DateTimes.toInstant(year, month, day)));
    }

    @DisplayName("Получение последнего дня квартала")
    @ParameterizedTest
    @MethodSource("testGetEndDateProvider")
    void testGetEndDate(Quarter q, int year, int month, int day) {
        final Instant expected = DateTimes.toInstant(year, month, day);
        Assertions.assertEquals(expected, q.getEndDate());
    }
}
