package ru.yandex.market.tpl.courier.presentation.feature.photo

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.market.tpl.courier.arch.fp.NonNegativeInt
import ru.yandex.market.tpl.courier.arch.fp.nonEmptyListOf
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.checkWriteIntoParcelAndReadBackWithoutErrors

@RunWith(RobolectricTestRunner::class)
class PhotoViewerArgsTest {

    @Test
    fun `Нормально парселизуется и распарселизуется`() {
        checkWriteIntoParcelAndReadBackWithoutErrors(
            PhotoViewerArgs(
                startImageIndex = NonNegativeInt.ZERO,
                imageUris = nonEmptyListOf("ABC".requireNotEmpty()),
            )
        )
    }
}