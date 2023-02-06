package ru.yandex.direct.core.testing.db

import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyName
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import java.time.Duration

class TestPpcPropertiesSupport(dslContextProvider: DslContextProvider) : PpcPropertiesSupport(dslContextProvider) {

    override fun <T : Any?> get(name: PpcPropertyName<T>, expiredDuration: Duration): PpcProperty<T> {
        // Игнорируем duration - кэширование мешает в юнит-тестах
        return super.get(name)
    }
}
