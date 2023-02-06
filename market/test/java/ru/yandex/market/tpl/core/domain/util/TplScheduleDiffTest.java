package ru.yandex.market.tpl.core.domain.util;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.pickup.LocalTimeInterval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TplScheduleDiffTest {

    @Test
    void expectDifferences_whenCorrect() {
        //given
        var from = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(10,0), LocalTime.of(15, 0)),
                DayOfWeek.WEDNESDAY, new LocalTimeInterval(LocalTime.of(11,0), LocalTime.of(18, 0))
        );

        var to = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(11,0), LocalTime.of(15, 0))
        );


        //then
        assertTrue(TplScheduleDiff.of(from, to).isNotEmpty());
        assertThat(TplScheduleDiff.of(from, to).toString()).isNotBlank();
    }


    @Test
    void expectDifferences_whenNulls() {
        //given
        var from = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(10,0), null),
                DayOfWeek.WEDNESDAY, new LocalTimeInterval(null, LocalTime.of(18, 0))
        );

        var to = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(11,0), null)
        );


        //then
        assertTrue(TplScheduleDiff.of(from, to).isNotEmpty());
        assertThat(TplScheduleDiff.of(from, to).toString()).isNotBlank();
    }

    @Test
    void expectDifferences_whenIntervalNulls() {
        //given
        var from = new HashMap<DayOfWeek, LocalTimeInterval>()
        {{
                put(DayOfWeek.MONDAY, null);
                put(DayOfWeek.WEDNESDAY, new LocalTimeInterval(null, LocalTime.of(18, 0)));
        }};

        var to = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(11,0), null)
        );


        //then
        assertTrue(TplScheduleDiff.of(from, to).isNotEmpty());
        assertThat(TplScheduleDiff.of(from, to).toString()).isNotBlank();
    }

    @Test
    void expectNotDifferences_whenNulls() {
        //given
        var from = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(10,0), null)

        );

        var to = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(10,0), null)
        );


        //then
        assertFalse(TplScheduleDiff.of(from, to).isNotEmpty());
        assertThat(TplScheduleDiff.of(from, to).toString()).isBlank();
    }

    @Test
    void expectNotDifferences_whenCorrect() {
        //given
        var from = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(10,0), LocalTime.of(16,0))

        );

        var to = Map.of(
                DayOfWeek.MONDAY, new LocalTimeInterval(LocalTime.of(10,0), LocalTime.of(16,0))
        );


        //then
        assertFalse(TplScheduleDiff.of(from, to).isNotEmpty());
        assertThat(TplScheduleDiff.of(from, to).toString()).isBlank();
    }
}
