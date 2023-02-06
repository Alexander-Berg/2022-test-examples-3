package ru.yandex.market.mapi.engine.section

import com.fasterxml.jackson.annotation.JsonIgnore
import ru.yandex.market.mapi.core.model.screen.AbstractSnippet

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.03.2022
 */
class InteractionsTestSnippet(
    val type: String,
    val field: String
) : AbstractSnippet() {

    @JsonIgnore
    var internal: Internal? = null

    class Internal(
        val custom: String
    )

    override fun buildSnippetId(): String {
        return "type-$type"
    }
}
