package ru.yandex.direct.web.entity.uac.repository

import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.test.utils.randomNegativeLong
import ru.yandex.direct.test.utils.randomPositiveLong


/**
 * Проверяем, что репозитории корректно работают с большими uint64 id
 */
object UacIdsProvider {
    @JvmStatic
    fun provideIds(): Array<Array<String>> {
        return arrayOf(
            arrayOf("positive id", randomPositiveLong().toIdString()),
            arrayOf("negative id", randomNegativeLong().toIdString()),
        )
    }
}

