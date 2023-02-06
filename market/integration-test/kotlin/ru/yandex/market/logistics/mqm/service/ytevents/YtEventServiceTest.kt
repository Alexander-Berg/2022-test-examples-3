package ru.yandex.market.logistics.mqm.service.ytevents

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import java.time.Instant

internal class YtEventServiceTest: AbstractContextualTest(){

    @Autowired
    private lateinit var ytEventService: YtEventService

    @Test
    @DisplayName("Найти существующее время первичной приемки на цте")
    @DatabaseSetup("/service/ytevents/service/before/first_cte_intake_event.xml")
    fun findExistingFirstCteIntakeTime() {
        ytEventService.getFirstCteIntakeTime("123") shouldBe Instant.parse("2021-12-13T12:00:00.00Z")
    }

    @Test
    @DisplayName("Вернуть null если не найдено событие первичной приемки на цте")
    @DatabaseSetup("/service/ytevents/service/before/first_cte_intake_event.xml")
    fun firstCteIntakeTimeNullIfNoSuchEvent() {
        ytEventService.getFirstCteIntakeTime("124") shouldBe null
    }
}
