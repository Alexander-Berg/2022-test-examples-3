package ru.yandex.market.mapi.core

import com.fasterxml.jackson.databind.JsonNode
import ru.yandex.market.mapi.core.contract.ResolverClientResponse
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import kotlin.reflect.KClass

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.02.2022
 */
class ResolverClientResponseMock(
    private val fileName: String,
    private val name: String = "some_mock",
    private val debugInfo: String? = null
) : ResolverClientResponse {

    override fun getName(): String {
        return name
    }

    override fun <T : Any> parse(type: KClass<T>): T {
        return JsonHelper.parse(fileName.asResource(), type)
    }

    override fun parseTree(): JsonNode {
        return JsonHelper.parseTree(fileName.asResource())
    }

    override fun getDebugInfo() = debugInfo
}
