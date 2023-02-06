package ru.yandex.market.tpl.courier.presentation.feature.task.delivery.passcode

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.market.tpl.courier.arch.fp.PositiveInt
import ru.yandex.market.tpl.courier.checkWriteIntoParcelAndReadBackWithoutErrors
import ru.yandex.market.tpl.courier.domain.feature.task.multiOrderIdTestInstance

@RunWith(RobolectricTestRunner::class)
class PassCodeArgsTest {

    @Test
    fun `Нормально парселизуется и распарселизуется`() {
        checkWriteIntoParcelAndReadBackWithoutErrors(
            PassCodeArgs(
                passCodeLength = PositiveInt.createOrThrow(5),
                multiOrderId = multiOrderIdTestInstance(),
            )
        )
    }
}