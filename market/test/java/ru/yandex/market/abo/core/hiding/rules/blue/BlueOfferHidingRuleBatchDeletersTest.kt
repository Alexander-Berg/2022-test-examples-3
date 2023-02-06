package ru.yandex.market.abo.core.hiding.rules.blue

import java.time.LocalDateTime
import java.util.function.Function
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.util.FakeUsers

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.07.2022
 */
class BlueOfferHidingRuleBatchDeletersTest @Autowired constructor(
    private val bySskuHidingBatchDeleter: BlueOfferHidingRuleBatchDeleter,
    private val byMskuHidingBatchDeleter: BlueOfferHidingRuleBatchDeleter,
    private val bySupplierMskuHidingBatchDeleter: BlueOfferHidingRuleBatchDeleter,
    private val byModelHidingBatchDeleter: BlueOfferHidingRuleBatchDeleter,
    private val blueOfferHidingRuleRepo: BlueOfferHidingRuleRepo
) : EmptyTest() {

    @Test
    fun `delete by ssku test`() {
        baseDeletionTest(
            deletableRuleKey = "ssku",
            anotherRuleKey = "assku",
            { buildSskuRule(it) },
            { it.shopSku },
            bySskuHidingBatchDeleter
        )
    }

    @Test
    fun `delete by msku test`() {
        baseDeletionTest(
            deletableRuleKey = 1111L,
            anotherRuleKey = 1112L,
            { buildMskuRule(it as Long) },
            { it.marketSku },
            byMskuHidingBatchDeleter
        )
    }

    @Test
    fun `delete by supplier msku test`() {
        baseDeletionTest(
            deletableRuleKey = 1111L,
            anotherRuleKey = 1112L,
            { buildSupplierMskuRule(it as Long) },
            { it.marketSku },
            bySupplierMskuHidingBatchDeleter
        )
    }

    @Test
    fun `delete by model test`() {
        baseDeletionTest(
            deletableRuleKey = 11111L,
            anotherRuleKey = 11112L,
            { buildModelRule(it as Long) },
            { it.modelId },
            byModelHidingBatchDeleter
        )
    }

    private fun <T> baseDeletionTest(
        deletableRuleKey: T, anotherRuleKey: T,
        buildRuleFunction: Function<T, BlueOfferHidingRule>,
        ruleKeyExtractor: Function<BlueOfferHidingRule, T>,
        blueOfferHidingRuleBatchDeleter: BlueOfferHidingRuleBatchDeleter
    ) {
        val deletableRule = buildRuleFunction.apply(deletableRuleKey)
        blueOfferHidingRuleRepo.saveAll(listOf(deletableRule, buildRuleFunction.apply(anotherRuleKey)))
        flushAndClear()

        blueOfferHidingRuleBatchDeleter.deleteHidingRules(
            listOf(deletableRule), FakeUsers.BLUE_OFFER_HIDING_PROCESSOR.id, LocalDateTime.now()
        )
        val notDeletedRules = blueOfferHidingRuleRepo.findAllByDeletedFalse()
        assertEquals(1, notDeletedRules.size)
        assertEquals(anotherRuleKey, ruleKeyExtractor.apply(notDeletedRules[0]))
    }
}
