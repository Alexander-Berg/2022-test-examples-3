package ru.yandex.market.pers.shopinfo.schedule;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.pers.shopinfo.db.mapper.ScheduleRowMapper;

/**
 * @author fbokovikov
 */
public class ScheduleRowMapperTest {

    public static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(0, "00:00"),
                Arguments.of(510, "08:30"),
                Arguments.of(600, "10:00"),
                Arguments.of(1080, "18:00"),
                Arguments.of(1410, "23:30")
        );
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testMinutesConversion(int minutes, String expectedHHMMview) {
        Assertions.assertEquals(
                expectedHHMMview,
                ScheduleRowMapper.convertToHHMMFormat(minutes)
        );
    }
}
