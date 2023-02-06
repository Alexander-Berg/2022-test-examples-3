package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.adv.direct.expression.multipler.type.MultiplierTypeEnum
import ru.yandex.adv.direct.expression.operations.OperationEnum
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.expression2.TargetingExpressionAtom
import ru.yandex.adv.direct.multipliers.Multiplier

inline fun buildMultiplier(init: Multiplier.Builder.() -> Unit): Multiplier =
    Multiplier.newBuilder().apply(init).build()

inline fun Multiplier.Builder.buildCondition(init: TargetingExpression.Builder.() -> Unit) =
    conditionBuilder.init()

inline fun TargetingExpression.Builder.buildAnd(init: TargetingExpression.Disjunction.Builder.() -> Unit) =
    addAndBuilder().init()

inline fun TargetingExpression.Disjunction.Builder.buildOr(init: TargetingExpressionAtom.Builder.() -> Unit) =
    addOrBuilder().init()

class MultiplierUtilsTest {

    @Test
    fun `multiple atoms`() {
        val original = buildMultiplierAtom {
            multiplier = 2
            buildCondition {
                buildAnd {
                    buildOr {
                        keyword = KeywordEnum.BrowserNameAndVersion
                        operation = OperationEnum.Equal
                        value = "IE9"
                    }
                    buildOr {
                        keyword = KeywordEnum.BrowserNameAndVersion
                        operation = OperationEnum.Equal
                        value = "IE8"
                    }
                }
            }
        }

        val multiplierType = MultiplierTypeEnum.DeviceType
        val expected = buildMultiplier {
            value = 2
            type = multiplierType
            buildCondition {
                buildAnd {
                    buildOr {
                        keyword = KeywordEnum.BrowserNameAndVersion.number
                        operation = OperationEnum.Equal.number
                        value = "IE9"
                    }
                    buildOr {
                        keyword = KeywordEnum.BrowserNameAndVersion.number
                        operation = OperationEnum.Equal.number
                        value = "IE8"
                    }
                }
            }
        }

        assertThat(original.toExpression2Format(multiplierType)).isEqualTo(expected)
    }

    @Test
    fun `multiplier without optional fields`() {
        val original = buildMultiplierAtom {
            multiplier = 5
        }
        val multiplierType = MultiplierTypeEnum.DeviceType
        val expected = buildMultiplier {
            value = 5
            type = multiplierType
        }
        assertThat(original.toExpression2Format(multiplierType)).isEqualTo(expected)
    }
}
