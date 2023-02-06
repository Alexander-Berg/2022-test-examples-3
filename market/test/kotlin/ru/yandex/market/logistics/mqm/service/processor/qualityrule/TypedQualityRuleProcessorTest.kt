package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.enums.QualityRuleProcessorType
import ru.yandex.market.logistics.mqm.entity.rules.payloads.QualityRulePayload
import ru.yandex.market.logistics.mqm.entity.rules.payloads.StartrekPayload

class TypedQualityRuleProcessorTest : AbstractTest() {
    private open class SuperPayload : QualityRulePayload()
    private open class SubSuperPayload : SuperPayload()

    @Test
    fun canProcessShouldReturnTrueWhenTypeMatches() {
        val processor: QualityRuleProcessor =
            object : TypedQualityRuleProcessor<QualityRulePayload>(QualityRuleProcessorType.STARTREK) {}
        val canProcess: Boolean = processor.canProcess(
            QualityRule().apply { ruleProcessor = QualityRuleProcessorType.STARTREK }
        )
        Assertions.assertThat(canProcess).isTrue
    }

    @Test
    fun canProcessShouldReturnFalseWhenTypeMismatches() {
        val processor: QualityRuleProcessor =
            object : TypedQualityRuleProcessor<QualityRulePayload>(QualityRuleProcessorType.STARTREK) {}
        val canProcess: Boolean = processor.canProcess(QualityRule())
        Assertions.assertThat(canProcess).isFalse
    }

    @Test
    fun canProcessShouldReturnTrueWhenGenericMatches() {
        val processor: QualityRuleProcessor =
            object : TypedQualityRuleProcessor<SubSuperPayload>(QualityRuleProcessorType.STARTREK) {}
        val canProcess: Boolean = processor.canProcess(
            QualityRule().apply {
                ruleProcessor = QualityRuleProcessorType.STARTREK
                rule = SubSuperPayload()
            }
        )
        Assertions.assertThat(canProcess).isTrue
    }

    @Test
    fun canProcessShouldReturnTrueWhenRuleIsNull() {
        val processor: QualityRuleProcessor =
            object : TypedQualityRuleProcessor<SubSuperPayload>(QualityRuleProcessorType.STARTREK) {}
        val canProcess: Boolean = processor.canProcess(
            QualityRule().apply {
                ruleProcessor = QualityRuleProcessorType.STARTREK
            }
        )
        Assertions.assertThat(canProcess).isTrue
    }

    @Test
    fun canProcessShouldReturnTrueWhenGenericMatchesBySupertype() {
        val processor: QualityRuleProcessor =
            object : TypedQualityRuleProcessor<SuperPayload>(QualityRuleProcessorType.STARTREK) {}
        val canProcess: Boolean = processor.canProcess(
            QualityRule().apply {
                ruleProcessor = QualityRuleProcessorType.STARTREK
                rule = SubSuperPayload()
            }
        )
        Assertions.assertThat(canProcess).isTrue
    }

    @Test
    fun canProcessShouldReturnFalseWhenGenericMismatches() {
        val processor: QualityRuleProcessor =
            object : TypedQualityRuleProcessor<StartrekPayload>(QualityRuleProcessorType.STARTREK) {}
        val canProcess: Boolean = processor.canProcess(
            QualityRule().apply {
                ruleProcessor = QualityRuleProcessorType.STARTREK
                rule = SubSuperPayload()
            }
        )
        Assertions.assertThat(canProcess).isFalse
    }
}
