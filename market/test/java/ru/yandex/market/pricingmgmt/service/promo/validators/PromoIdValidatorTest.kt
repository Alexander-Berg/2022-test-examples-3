package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions
import ru.yandex.market.pricingmgmt.TestUtils.notNull
import ru.yandex.market.pricingmgmt.exception.BadRequestException
import ru.yandex.market.pricingmgmt.model.promo.PromoSearchRequest
import ru.yandex.market.pricingmgmt.model.promo.PromoSearchResultItem
import ru.yandex.market.pricingmgmt.model.promo.PromosSearchResult
import ru.yandex.market.pricingmgmt.service.promo.PromoSearchService

internal class PromoIdValidatorTest {
    private val promoSearchService: PromoSearchService = mock(PromoSearchService::class.java)
    private val promoIdValidator: PromoIdValidator = PromoIdValidator(promoSearchService)

    @Test
    fun validate_ciface_ok() {
        Mockito.`when`(promoSearchService.findPromos(notNull())).thenReturn(
            PromosSearchResult(
                totalCount = 1,
                promos = listOf(
                    PromoSearchResultItem(
                        promoId = "cf_123456"
                    )
                )
            )
        )

        assertDoesNotThrow { promoIdValidator.validate("cf_123456") }
        verify(promoSearchService).findPromos(buildPromoSearchRequest("cf_123456"))
        verifyNoMoreInteractions(promoSearchService)
    }

    @Test
    fun validate_ciface_throw() {
        Mockito.`when`(promoSearchService.findPromos(notNull())).thenReturn(
            PromosSearchResult(
                totalCount = 0,
                promos = emptyList()
            )
        )

        val e = assertThrows<BadRequestException> { promoIdValidator.validate("cf_123456") }
        assertEquals("PromoId cf_123456 not found", e.message)
        verify(promoSearchService).findPromos(buildPromoSearchRequest("cf_123456"))
        verifyNoMoreInteractions(promoSearchService)
    }

    @Test
    fun validate_anaplan_ok() {
        assertDoesNotThrow { promoIdValidator.validate("#123456") }
        verifyZeroInteractions(promoSearchService)
    }

    @Test
    fun validate_unknown_throw() {
        val e = assertThrows<BadRequestException> { promoIdValidator.validate("unknown") }
        assertEquals("Unknown source of promoId: unknown", e.message)
        verifyZeroInteractions(promoSearchService)
    }

    @Test
    fun validate_empty_throw() {
        val e = assertThrows<BadRequestException> { promoIdValidator.validate("") }
        assertEquals("Unknown source of promoId: ", e.message)
        verifyZeroInteractions(promoSearchService)
    }

    private fun buildPromoSearchRequest(promoId: String): PromoSearchRequest {
        return PromoSearchRequest(
            promoId = listOf(
                promoId
            )
        )
    }
}
