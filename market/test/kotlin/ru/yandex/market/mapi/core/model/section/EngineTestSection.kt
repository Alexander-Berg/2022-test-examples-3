package ru.yandex.market.mapi.core.model.section

import ru.yandex.market.mapi.core.model.screen.AbstractSection
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * @author Ilya Kislitsyn / ilyakis@ / 04.02.2022
 */
class EngineTestSection : AbstractSection() {
    var testField: String? = null

    override fun isSupported(type: KClass<*>): Boolean {
        return type.isSubclassOf(EngineTestSnippet::class)
    }
}