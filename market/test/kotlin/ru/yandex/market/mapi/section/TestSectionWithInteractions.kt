package ru.yandex.market.mapi.section

import com.fasterxml.jackson.annotation.JsonInclude
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.model.screen.AbstractSection

/**
 * @author Ilya Kislitsyn / ilyakis@ / 04.04.2022
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class TestSectionWithInteractions(
    val widget: AbstractSection,
    val analytics: Map<String, Any> = MapiContext.get().analytics,
    val toDebug: Boolean? = null,
    val toDivkit: Boolean? = null,
    val flattened: List<AbstractSection>? = null,
    val replacedSectionId: String? = null
)
