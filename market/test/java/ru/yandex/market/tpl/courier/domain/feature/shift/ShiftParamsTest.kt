package ru.yandex.market.tpl.courier.domain.feature.shift

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.data.parse
import ru.yandex.market.tpl.courier.data.toJson
import ru.yandex.market.tpl.courier.testApplication

@RunWith(RobolectricTestRunner::class)
class ShiftParamsTest {

    @Test
    fun `Маппинг ShiftParams в json и обратно не падает и выдаёт неизменный объект`() {
        val jsonMapper = testApplication.component.jsonMapper
        val initialShiftParams = shiftParamsTestInstance()
        val asJson = jsonMapper.toJson(initialShiftParams).orThrow()
        val mappedShiftParams = jsonMapper.parse<ShiftParams>(asJson).orThrow()

        initialShiftParams shouldBe mappedShiftParams
    }
}