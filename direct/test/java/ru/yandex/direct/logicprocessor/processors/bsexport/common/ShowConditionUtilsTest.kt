package ru.yandex.direct.logicprocessor.processors.bsexport.common

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.expression2.TargetingExpressionAtom
import ru.yandex.direct.logicprocessor.processors.bsexport.common.ShowConditionUtils.calcContextID
import ru.yandex.direct.logicprocessor.processors.bsexport.common.ShowConditionUtils.targetingExpressionSort
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration

@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class ShowConditionUtilsTest {

    private fun toAtom(keyword: Int, operation: Int, value: String): TargetingExpressionAtom {
        return TargetingExpressionAtom.newBuilder()
            .setKeyword(keyword)
            .setOperation(operation)
            .setValue(value)
            .build()
    }

    @Test
    fun `sort complex condition`() {
        val protoIn = TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(2, 4, "2"))
                    .addOr(toAtom(2, 5, "1"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(1, 1, "555"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(11, 1, "0"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(1, 1, "99"))
                    .addOr(toAtom(1, 1, "333"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(1, 2, "44"))
            ).build()
        val expectedProto = TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(1, 1, "555"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(1, 2, "44"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(11, 1, "0"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(1, 1, "333"))
                    .addOr(toAtom(1, 1, "99"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(2, 4, "2"))
                    .addOr(toAtom(2, 5, "1"))
            ).build()
        val protoOut = targetingExpressionSort(protoIn)
        Assertions.assertThat(protoOut).isEqualTo(expectedProto)
    }

    @Test
    fun `calc zero contextID`() {
        val protoIn = TargetingExpression.newBuilder().build()
        val contextID = calcContextID(protoIn)
        Assertions.assertThat(contextID).isEqualTo(0L)
    }

    @Test
    fun `calc contextID`() {
        val protoIn = TargetingExpression.newBuilder()
            .addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(1, 4, "542"))
            ).addAnd(
                TargetingExpression.Disjunction.newBuilder()
                    .addOr(toAtom(17, 4, "11309"))
            ).build()
        val contextID = calcContextID(protoIn)
        Assertions.assertThat(contextID).isEqualTo(4847473603232723294L)
    }
}
