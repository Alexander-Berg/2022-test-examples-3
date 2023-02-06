package ru.yandex.market.abo.core.hiding.rules.blue

import java.util.function.Function
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 25.11.2021
 */
class BlueOfferHidingRuleBatchUpdatersTest @Autowired constructor(
    private val bySskuHidingBatchUpdater: PgBatchUpdater<BlueOfferHidingRule>,
    private val byMskuHidingBatchUpdater: PgBatchUpdater<BlueOfferHidingRule>,
    private val bySupplierMskuHidingBatchUpdater: PgBatchUpdater<BlueOfferHidingRule>,
    private val byModelHidingBatchUpdater: PgBatchUpdater<BlueOfferHidingRule>,
    private val blueOfferHidingRuleRepo: BlueOfferHidingRuleRepo
) : EmptyTest() {

    @Test
    fun `update by ssku test`() {
        baseUpdateTest(ruleKey = "ssku", anotherRuleKey = "assku", { buildSskuRule(it) }, bySskuHidingBatchUpdater)
    }

    @Test
    fun `update by msku test`() {
        baseUpdateTest(ruleKey = 1111L, anotherRuleKey = 1112L, { buildMskuRule(it) }, byMskuHidingBatchUpdater)
    }

    @Test
    fun `update by supplier msku test`() {
        baseUpdateTest(ruleKey = 1111L, anotherRuleKey = 1112L, { buildSupplierMskuRule(it) }, bySupplierMskuHidingBatchUpdater)
    }

    @Test
    fun `update by model test`() {
        baseUpdateTest(ruleKey = 11111L, anotherRuleKey = 11112L, { buildModelRule(it) }, byModelHidingBatchUpdater)
    }

    private fun <T> baseUpdateTest(
        ruleKey: T, anotherRuleKey: T,
        buildRuleFunction: Function<T, BlueOfferHidingRule>,
        blueOfferHidingRuleBatchUpdater: PgBatchUpdater<BlueOfferHidingRule>
    ) {
        blueOfferHidingRuleRepo.saveAll(listOf(buildRuleFunction.apply(ruleKey)))
        flushAndClear()

        blueOfferHidingRuleBatchUpdater.insertWithoutUpdate(listOf(buildRuleFunction.apply(ruleKey)))
        assertEquals(1, blueOfferHidingRuleRepo.count())

        blueOfferHidingRuleBatchUpdater.insertWithoutUpdate(listOf(buildRuleFunction.apply(anotherRuleKey)))
        assertEquals(2, blueOfferHidingRuleRepo.count())
    }
}
