package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.promo.PromoLandingSnippetAssembler
import ru.yandex.market.mapi.section.common.promo.PromosScrollboxSection
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 28.01.2022
 */
class PromosScrollboxSectionTest : AbstractSectionTest() {
    private val assembler = PromoLandingSnippetAssembler()
    private val resolver = PromoLandingSnippetAssembler.RESOLVE_CMS

    @Test
    fun testAssembly() {
        assembler.testAssembly(
            fileMap = mapOf(resolver to "/section/common/promo/fapiResponse.json"),
            expected = "/section/common/promo/assembled.json",
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/promo/fapiResponse.json"),
            expected = "/section/common/promo/sectionResult.json"
        )
    }

    @Test
    fun testContentResult() {
        testContentResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to "/section/common/promo/fapiResponse.json"),
            expected = "/section/common/promo/contentResult.json"
        )
    }

    @Test
    fun testPromoEndDateFormatting() {
        assertEquals("До 18 января", assembler.formatPromoEndDate("2022-01-18"))
        assertEquals("До 18 февраля", assembler.formatPromoEndDate("2022-02-18"))
        assertEquals("До 18 марта", assembler.formatPromoEndDate("2022-03-18"))
        assertEquals("До 18 апреля", assembler.formatPromoEndDate("2022-04-18"))
        assertEquals("До 18 мая", assembler.formatPromoEndDate("2022-05-18"))
        assertEquals("До 18 июня", assembler.formatPromoEndDate("2022-06-18"))
        assertEquals("До 18 июля", assembler.formatPromoEndDate("2022-07-18"))
        assertEquals("До 18 августа", assembler.formatPromoEndDate("2022-08-18"))
        assertEquals("До 18 сентября", assembler.formatPromoEndDate("2022-09-18"))
        assertEquals("До 18 октября", assembler.formatPromoEndDate("2022-10-18"))
        assertEquals("До 18 ноября", assembler.formatPromoEndDate("2022-11-18"))
        assertEquals("До 18 декабря", assembler.formatPromoEndDate("2022-12-18"))
    }

    private fun buildWidget(): PromosScrollboxSection {
        return PromosScrollboxSection().apply {
            addDefParams()
            minCountToShow = 2

        }
    }
}