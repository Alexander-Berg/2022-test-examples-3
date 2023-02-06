package ru.yandex.market.mapi.section

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.product.ResolvePrimeResponse
import ru.yandex.market.mapi.core.ResolverClientResponseMock
import ru.yandex.market.mapi.core.model.screen.ResourceResolver
import ru.yandex.market.mapi.section.common.product.ProductScrollboxAssembler
import ru.yandex.market.mapi.section.common.product.ProductsScrollboxSection
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2022
 */
class GeneralWidgetTest : AbstractSectionTest() {
    // any assembler
    val assembler = ProductScrollboxAssembler()

    @Test
    fun testError() {
        val response = ResolverClientResponseMock("/common/fapiErrorResponse.json", ResolvePrimeResponse.RESOLVER)
        val result = assembler.convert(
            rawResponseList = listOf(response),
            section = ProductsScrollboxSection(),
            resolvers = listOf(ResourceResolver.simple(ResolvePrimeResponse.RESOLVER))
        )

        assertEquals(0, result.content.size)
        assertEquals(listOf("Fapi error. HandlerOfPrimeError - Some message"), result.errors?.map { it.message })
    }
}
