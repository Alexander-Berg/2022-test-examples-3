package ru.yandex.market.mapi.core.util.ast

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * @author Ilya Kislitsyn / ilyakis@ / 27.06.2022
 */
class AstTest {
    @Test
    fun testExpressionParsing() {
        // valid cases
        assertExpression(
            "(android && v >= '4.05') || (iphone && v >= '4.4.9')",
            "(android && (v >= '4.05')) || (iphone && (v >= '4.4.9'))",
        )

        assertExpression(
            "(x && a > b)",
            "x && (a > b)",
        )

        // to solve this correctly, parser should know how to distinct logical and non-logical operations
        assertExpression(
            "(a > b && x)",
            "(a > b) && x",
        )

        assertExpression(
            "android || iphone",
            "android || iphone",
        )

        assertExpression(
            "android || iphone || ipad",
            "android || (iphone || ipad)",
        )

        // spaces
        assertExpression(
            "android && v>='4.02'",
            "android && (v >= '4.02')",
        )

        // complex variables
        assertExpression(
            "aNdroID && v1_2 >= '4.02'",
            "aNdroID && (v1_2 >= '4.02')",
        )

        // true/false
        assertExpression(
            "true || false",
            "true || false",
        )
    }

    @Test
    fun testExpressionErrorHandling() {
        // invalid cases for parsers
        assertExpression(
            "(android && v >= '4.05') || (iphone && )",
            "Unexpected character: )",
        )


        assertExpression(
            "   ",
            "empty expression",
        )


        assertExpression(
            " (  ",
            "Unexpected end of expression",
        )

        assertExpression(
            "   ( android && v >= '4.05') || (iphone && ",
            "Unexpected end of expression",
        )

        assertExpression(
            "(android ",
            "Invalid bracket sequence, position = 9",
        )

        assertExpression(
            "((a >()) && b)",
            "Unexpected character: )",
        )

        assertExpression(
            "((a > '1.23) && b)",
            "Unclosed quote, position = 6",
        )

        assertExpression(
            "(a && (b > '1.23))",
            "Unclosed quote, position = 11",
        )

        assertExpression(
            "((123 > '1.23') && b)",
            "Unexpected character: 1",
        )

        assertExpression(
            "v>",
            "invalid expression: none right expression after operator '>'",
        )

        assertExpression(
            "x > y > z",
            "attempt to use '>' operator in invalid way",
        )

        // makes no sense
        assertExpression(
            "android && v >= '4.02' == '1231'",
            "attempt to use '==' operator in invalid way",
        )
    }

    @Test
    fun testRunExpression() {
        val context = TestConditionContext()

        assertRun("god_exists || humanity == 'bright'", context, "false")
        assertRun("humanity", context, "is lost")
        assertRun("true || false", context, "true")
        assertRun("true && false", context, "false")
        assertRun("empty_universe == ant", context, "Unknown variable ant")
        assertRun("empty_universe == humanity", context, "expected both to be text")
        assertRun("empty_universe && humanity", context, "logical operation can't be applied to non-bool value")
    }

    private fun assertExpression(source: String, expected: String) {
        val result = Ast.parseExpression(source)
        if (result.error != null) {
            assertEquals(expected, result.error)
            return
        }

        assertEquals(expected, result.result?.serialize())
    }

    private fun assertRun(source: String, context: TestConditionContext, expected: String) {
        val result = Ast.parseExpression(source)
        if (result.error != null) {
            assertEquals(expected, result.error)
            return
        }

        val runResult = Ast.runExpression(result.result!!, context)
        if (runResult.error != null) {
            assertEquals(expected, runResult.error)
            return
        }

        val scalar = runResult.result
        when (scalar) {
            is ScalarBool -> assertEquals(expected, scalar.value.toString())
            is ScalarText -> assertEquals(expected, scalar.value)
            null -> fail("impossible")
        }
    }

    private fun Expression.serialize(): String {
        // useful only in tests
        return when (this) {
            is ScalarBool -> value.toString()
            is ScalarText -> "'$value'"
            is ExpressionVariable -> name
            is ExpressionBinOperator -> "${left.serializeChain()} $operator ${right.serializeChain()}"
        }
    }

    private fun Expression.serializeChain(): String {
        return if (this is ExpressionBinOperator) {
            "(${serialize()})"
        } else {
            serialize()
        }
    }

    class TestConditionContext : ExpressionContext {
        override val variables: Map<String, Scalar> = mapOf(
            "god_exists" to ScalarBool(false),
            "empty_universe" to ScalarBool(true),
            "humanity" to ScalarText("is lost"),
        )

        override fun handleBinOperator(operator: String, left: Scalar, right: Scalar): RunResult {
            return when (operator) {
                "==" -> checkEqual(left, right)

                Ast.OPERATOR_LOGIC_AND -> binaryLogical(left, right) { a, b -> a && b }
                Ast.OPERATOR_LOGIC_OR -> binaryLogical(left, right) { a, b -> a || b }

                else -> RunResult(error = "unsupported operator $operator")
            }
        }

        private fun checkEqual(left: Scalar, right: Scalar): RunResult {
            if (left !is ScalarText || right !is ScalarText) {
                return RunResult(error = "expected both to be text")
            }

            // invalid versions would match to '0' version
            val result = left.value.equals(right.value, ignoreCase = true)
            return RunResult(ScalarBool(result))
        }
    }
}