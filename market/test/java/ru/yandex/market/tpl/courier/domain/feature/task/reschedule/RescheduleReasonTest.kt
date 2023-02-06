package ru.yandex.market.tpl.courier.domain.feature.task.reschedule

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.data.parse
import ru.yandex.market.tpl.courier.data.toJson
import ru.yandex.market.tpl.courier.testApplication

@RunWith(RobolectricTestRunner::class)
class RescheduleReasonTest {

    @Test
    fun `Маппинг объекта RescheduleReason в json и обратно работает`() {
        val jsonMapper = testApplication.component.jsonMapper
        val initialReason = rescheduleReasonTestInstance()
        val asJson = jsonMapper.toJson(initialReason).orThrow()
        val mappedReason = jsonMapper.parse<RescheduleReason>(asJson).orThrow()

        initialReason shouldBe mappedReason
    }
}