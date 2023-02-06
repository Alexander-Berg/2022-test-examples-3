package ru.yandex.market.mapi.core.model.section

import ru.yandex.market.mapi.core.model.screen.AbstractSnippet

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2022
 */
class EngineTestSnippet(
    val testField: String,
    val testData: Int,
    val testResolverParam: Int? = null
) : AbstractSnippet()
