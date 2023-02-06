package ru.yandex.market.mbo.cms.core.migration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import junit.framework.TestCase
import org.junit.Test
import ru.yandex.market.mbo.cms.core.models.MarketEntity

class LinkParamGlFilterEntitiesProcessorTest : TestCase() {

    companion object {
        val transitions: Map<MarketEntity, Map<String, String>> = mapOf(
            MarketEntity.PARAMETER to mapOf("15944280" to "p1", "15944287" to "p2"),
            MarketEntity.OPTION to mapOf("15944287" to "o1", "123456" to "o2", "15944280" to "o3")
        )
    }

    @Test
    fun testProcessSimple() {
        val p = LinkParamGlFilterEntitiesProcessor(transitions)
        val r = p.process("15944280:15944287", mutableMapOf())
        r shouldNotBe null
        r.changed shouldBe true
        r.result shouldBe "p1:o1"
    }

    @Test
    fun testProcessBoolean() {
        val p = LinkParamGlFilterEntitiesProcessor(transitions)
        val r = p.process("15944280:1", mutableMapOf())
        r shouldNotBe null
        r.changed shouldBe true
        r.result shouldBe "p1:1"
    }

    @Test
    fun testProcessMultipleOptions() {
        val p = LinkParamGlFilterEntitiesProcessor(transitions)
        val r = p.process("15944280:15944287,123456", mutableMapOf())
        r shouldNotBe null
        r.changed shouldBe true
        r.result shouldBe "p1:o1,o2"
    }

    @Test
    fun testProcessMultipleOptionsSome() {
        val p = LinkParamGlFilterEntitiesProcessor(transitions)
        val r = p.process("15944280:11111111,15944287,2222222,123456", mutableMapOf())
        r shouldNotBe null
        r.changed shouldBe true
        r.result shouldBe "p1:11111111,o1,2222222,o2"
    }

    @Test
    fun testProcessBadInput() {
        val p = LinkParamGlFilterEntitiesProcessor(transitions)
        val r = p.process("15944280:11111111,", mutableMapOf())
        r shouldNotBe null
        r.changed shouldBe true
        r.result shouldBe "p1:11111111,"
    }

    @Test
    fun testProcessBadInput2() {
        val p = LinkParamGlFilterEntitiesProcessor(transitions)
        val r = p.process("15944280:15944287:,", mutableMapOf())
        r shouldNotBe null
        r.changed shouldBe true
        r.result shouldBe "p1:15944287:,"
    }

}
