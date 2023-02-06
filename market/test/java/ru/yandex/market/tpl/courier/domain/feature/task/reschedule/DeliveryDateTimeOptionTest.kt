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
class DeliveryDateTimeOptionTest {

    @Test
    fun `Маппинг объекта DeliveryDateTimeOption в json и обратно работает`() {
        val jsonMapper = testApplication.component.jsonMapper
        val initialOption = deliveryDateTimeOptionTestInstance()
        val asJson = jsonMapper.toJson(initialOption).orThrow()
        val mappedOption = jsonMapper.parse<DeliveryDateTimeOption>(asJson).orThrow()

        initialOption shouldBe mappedOption
    }
}