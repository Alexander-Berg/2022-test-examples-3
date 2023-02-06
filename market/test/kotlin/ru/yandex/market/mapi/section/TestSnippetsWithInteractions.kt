package ru.yandex.market.mapi.section

import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.model.screen.AbstractSnippet

/**
 * @author Ilya Kislitsyn / ilyakis@ / 04.04.2022
 */
class TestSnippetsWithInteractions(
    val snippets: List<AbstractSnippet>,
    val analytics: Map<String, Any> = MapiContext.get().analytics
)