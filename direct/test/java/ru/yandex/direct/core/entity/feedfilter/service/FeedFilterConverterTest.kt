package ru.yandex.direct.core.entity.feedfilter.service

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.feedfilter.converter.FeedFilterConditionValue
import ru.yandex.direct.core.entity.feedfilter.converter.FeedFilterConverters
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterCondition
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilterTab
import ru.yandex.direct.core.entity.performancefilter.container.DecimalRange
import ru.yandex.direct.core.entity.performancefilter.container.Exists
import ru.yandex.direct.core.entity.performancefilter.model.Operator

@RunWith(JUnitParamsRunner::class)
class FeedFilterConverterTest {

    @Test
    @Parameters(method = "serialize_params, serializeThenDeserialize_params")
    @TestCaseName("Serialize: {0}")
    fun serialize(
        testCaseName: String,
        feedFilterConverter: FeedFilterConverter, feedFilter: FeedFilter, result: String
    ) {
        println(feedFilterConverter.serializeFeedFilter(feedFilter))
        assertEquals(
            "$feedFilter serialized should be $result",
            result, feedFilterConverter.serializeFeedFilter(feedFilter)
        )
    }

    @Test
    @Parameters(method = "serializeThenDeserialize_params")
    @TestCaseName("Serialize then deserialize: {0}")
    fun serializeThenDeserialize(
        testCaseName: String,
        feedFilterConverter: FeedFilterConverter, feedFilter: FeedFilter, result: String
    ) {
        assertEquals(feedFilter,
            feedFilter
                .let { feedFilterConverter.serializeFeedFilter(it) }
                .let { feedFilterConverter.deserializeFeedFilter(it) }
        )
    }

    private class Condition<V>(
        val fieldName: String,
        val operator: Operator,
        private val value: V,
        val serializedType: String,
        val serializedValue: String
    ) {
        fun toTextCondition() = FeedFilterCondition<V>(fieldName, operator, serializedValue).withParsedValue(value)
    }

    private data class DbConditionsCase(val testDataName: String, val filterConditions: List<Condition<*>>) {
        fun toTestData(): List<Any> {
            val serializedConditions = filterConditions.joinToString(prefix = "[", separator = ",", postfix = "]") {
                """{"fieldName":"${it.fieldName}","operator":"${it.operator}","value":{"${it.serializedType}":${it.serializedValue}}}"""
            }
            return listOf(
                testDataName,
                dbConverter,
                FeedFilter().apply {
                    tab = FeedFilterTab.TREE
                    conditions = filterConditions.map { it.toTextCondition() }
                },
                """{"fromTab":"tree","conditions":$serializedConditions}"""
            )
        }
    }

    companion object {
        val dbConverter: FeedFilterSerializer = FeedFilterConverters.DB_CONVERTER

        @JvmStatic
        fun serializeThenDeserialize_params(): List<List<Any>> =
            listOf(
                DbConditionsCase(
                    "All operators and value formats",
                    listOf(
                        Condition("field1", Operator.EQUALS, true, FeedFilterConditionValue.BOOLEAN_TYPE, "true"),
                        Condition(
                            "field2", Operator.RANGE,
                            listOf(DecimalRange("11.03-19.47"), DecimalRange("20-40")),
                            FeedFilterConditionValue.DECIMAL_RANGE_LIST_TYPE, """["11.03-19.47","20-40"]"""
                        ),
                        Condition(
                            "field3", Operator.CONTAINS, listOf("one", "two", "three"),
                            FeedFilterConditionValue.STRING_LIST_TYPE, """["one","two","three"]"""
                        ),
                        Condition(
                            "field4", Operator.NOT_CONTAINS, listOf("one", "two", "three"),
                            FeedFilterConditionValue.STRING_LIST_TYPE, """["one","two","three"]"""
                        ),
                        Condition(
                            "field5",
                            Operator.LESS,
                            listOf(175.02),
                            FeedFilterConditionValue.DOUBLE_LIST_TYPE,
                            "[175.02]"
                        ),
                        Condition(
                            "field6",
                            Operator.GREATER,
                            listOf(0.03),
                            FeedFilterConditionValue.DOUBLE_LIST_TYPE,
                            "[0.03]"
                        ),
                        Condition(
                            "field7",
                            Operator.EXISTS,
                            Exists(true),
                            FeedFilterConditionValue.EXISTS_TYPE,
                            "1"
                        ),
                        Condition(
                            "field8",
                            Operator.EXISTS,
                            Exists(false),
                            FeedFilterConditionValue.EXISTS_TYPE,
                            "0"
                        ),
                        Condition(
                            "field9",
                            Operator.EQUALS,
                            listOf<Int>(),
                            FeedFilterConditionValue.EMPTY_LIST_TYPE,
                            "[]"
                        ),
                        Condition(
                            "field10",
                            Operator.EQUALS,
                            listOf(1L, 2L, 91285315234L),
                            FeedFilterConditionValue.LONG_LIST_TYPE,
                            "[1,2,91285315234]"
                        )
                    )
                )
            ).map { it.toTestData() }

        @JvmStatic
        fun serialize_params(): List<List<Any>> =
            listOf(
                DbConditionsCase(
                    "Double precision and rounding",
                    listOf(
                        Condition(
                            "field1", Operator.EQUALS, listOf(1.0, 1.01, 2.001, 2.99, 2.999),
                            FeedFilterConditionValue.DOUBLE_LIST_TYPE, "[1,1.01,2,2.99,2.99]"
                        ),
                    )

                )

            ).map { it.toTestData() }
    }

}
