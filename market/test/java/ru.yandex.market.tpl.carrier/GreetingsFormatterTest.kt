package ru.yandex.market.tpl.carrier

import org.junit.Test
import ru.yandex.market.tpl.carrier.presentation.welcome.Greetings
import ru.yandex.market.tpl.carrier.presentation.welcome.calculateGreetings
import java.time.LocalDateTime
import java.time.Month

class GreetingsFormatterTest {

    @Test
    fun `Good day for 14_32 April 2021`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            14,
            32
        )
        assert(calculateGreetings(date) == Greetings.GoodDay)
    }

    @Test
    fun `Good day for 14_32 January 2021`() {
        val date = LocalDateTime.of(
            2021,
            Month.JANUARY,
            12,
            14,
            32
        )
        assert(calculateGreetings(date) == Greetings.GoodDay)
    }

    @Test
    fun `Good day for 14_32 January 2010`() {
        val date = LocalDateTime.of(
            2019,
            Month.JANUARY,
            12,
            14,
            32
        )
        assert(calculateGreetings(date) == Greetings.GoodDay)
    }

    @Test
    fun `Good day for 14_32 January 2050`() {
        val date = LocalDateTime.of(
            2050,
            Month.JANUARY,
            12,
            14,
            32
        )
        assert(calculateGreetings(date) == Greetings.GoodDay)
    }

    @Test
    fun `Good day for 12_00`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            12,
            0
        )
        assert(calculateGreetings(date) == Greetings.GoodDay)
    }

    @Test
    fun `Good morning for 11_59`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            11,
            59
        )
        assert(calculateGreetings(date) == Greetings.GoodMorning)
    }

    @Test
    fun `Good morning for 5_00`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            5,
            0
        )
        assert(calculateGreetings(date) == Greetings.GoodMorning)
    }

    @Test
    fun `Good evening for 18_00`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            18,
            0
        )
        assert(calculateGreetings(date) == Greetings.GoodEvening)
    }

    @Test
    fun `Good evening for 21_59`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            21,
            59
        )
        assert(calculateGreetings(date) == Greetings.GoodEvening)
    }

    @Test
    fun `Good night for 22_00`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            21,
            59
        )
        assert(calculateGreetings(date) == Greetings.GoodEvening)
    }

    @Test
    fun `Good night for 4_59`() {
        val date = LocalDateTime.of(
            2021,
            Month.APRIL,
            12,
            4,
            59
        )
        assert(calculateGreetings(date) == Greetings.GoodNight)
    }
}