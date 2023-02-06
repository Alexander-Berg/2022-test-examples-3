package ru.yandex.market.tpl.courier.data.feature.datetime

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.extensions.successWith
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeMapperTest {

    private val mapper = DateTimeMapper()

    @Test
    fun `Нормально маппит ZonedDateTime туда-сюда`() {
        val date = ZonedDateTime.now()
        val string = mapper.mapToRfc3339(date)
        val fromString = mapper.mapFromRfc3339(string)

        fromString shouldBe successWith<ZonedDateTime, Throwable>(date)
    }

    @Test
    fun `Нормально парсит дату-время из строки Instant в формате RFC3339 в ZonedDateTime`() {
        val fromString = mapper.mapFromRfc3339("2021-02-01T11:37:12Z".requireNotEmpty())

        val expectedResult = ZonedDateTime.of(
            2021,
            2,
            1,
            11,
            37,
            12,
            0,
            ZoneId.of("Z")
        )
        fromString shouldBe successWith<ZonedDateTime, Throwable>(expectedResult)
    }

    @Test
    fun `Нормально мапит ZonedDateTime в строку в формате RFC3339`() {
        val dateTime = ZonedDateTime.of(
            2021,
            2,
            1,
            20,
            50,
            39,
            0,
            ZoneId.of("Europe/Moscow")
        )

        val asString = mapper.mapToRfc3339(dateTime).unwrap()

        asString shouldBe "2021-02-01T20:50:39+03:00[Europe/Moscow]"
    }
}