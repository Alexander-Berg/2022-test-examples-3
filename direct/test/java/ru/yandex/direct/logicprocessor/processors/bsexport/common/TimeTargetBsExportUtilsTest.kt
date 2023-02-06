package ru.yandex.direct.logicprocessor.processors.bsexport.common

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.adv.direct.expression.operations.OperationEnum
import ru.yandex.adv.direct.expression2.TargetingExpression
import ru.yandex.adv.direct.expression2.TargetingExpressionAtom
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings

class TimeTargetBsExportUtilsTest {
    @ParameterizedTest
    @MethodSource("targetTimeCases")
    fun `target time condition`(timetable: String?, keyword: KeywordEnum?, operation: OperationEnum?, values: List<String>?) {
        val expectedResource = TargetingExpression.newBuilder()
        if (keyword != null && operation != null && values != null) {
            expectedResource.addAnd(
                TargetingExpression.Disjunction.newBuilder().addAllOr(
                    values.map {
                        TargetingExpressionAtom.newBuilder()
                            .setKeyword(keyword.number)
                            .setOperation(operation.number)
                            .setValue(it).build()
                    }
                )
            )
        }

        val builder = TargetingExpression.newBuilder()
        val timeTarget = CampaignMappings.timeTargetFromDb(timetable)

        TimeTargetBsExportUtils.addTargetTimeCondition(timeTarget, builder)

        assertThat(builder.build()).isEqualTo(expectedResource.build())
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun targetTimeCases() = listOf(
            Arguments.of("", null, null, null),
            Arguments.of(null, null, null, null),
            Arguments.of("1A", KeywordEnum.Timetable, OperationEnum.TimeNotLike,
                listOf("234567BCDEFGHIJKLMNOPQRSTUVWX")),
            Arguments.of("2BbCbD3BCD6A7BC9", KeywordEnum.TimetableSimple, OperationEnum.TimeNotLike,
                listOf("123456ADEFGHIJKLMNOPQRSTUVWX", "123457BCDEFGHIJKLMNOPQRSTUVWX", "14567AEFGHIJKLMNOPQRSTUVWX")),
            Arguments.of("2BCD3BCD4BCD", KeywordEnum.Timetable, OperationEnum.TimeNotLike,
                listOf("1567AEFGHIJKLMNOPQRSTUVWX")),
            Arguments.of("2BCD3BD4BCD", KeywordEnum.Timetable, OperationEnum.TimeNotLike,
                listOf("124567ACEFGHIJKLMNOPQRSTUVWX", "13567AEFGHIJKLMNOPQRSTUVWX")),
            Arguments.of("2BCD3BCD4BCD8", KeywordEnum.Timetable, OperationEnum.TimeLike,
                listOf("234BCD")),
            Arguments.of("2BCD3BCD4BCD8BCD", KeywordEnum.Timetable, OperationEnum.TimeLike,
                listOf("2348BCD")),
            Arguments.of("2BCD3BCD4BCD8ABCD", KeywordEnum.Timetable, OperationEnum.TimeLike,
                listOf("234BCD", "8ABCD")),
            Arguments.of("1BCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX",
                KeywordEnum.Timetable, OperationEnum.TimeNotLike, listOf("167", "234567A")),
            Arguments.of("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX",
                KeywordEnum.Timetable, OperationEnum.TimeNotLike, listOf("67")),
            Arguments.of("1Ab", KeywordEnum.Timetable, OperationEnum.TimeNotLike,
                listOf("234567BCDEFGHIJKLMNOPQRSTUVWX")),
            Arguments.of("2BbCbDb3BcCcDc4BdCdDd", KeywordEnum.Timetable, OperationEnum.TimeNotLike,
                listOf("1567AEFGHIJKLMNOPQRSTUVWX")),
            Arguments.of("2BbCD3BCD4BCD8BCD", KeywordEnum.Timetable, OperationEnum.TimeLike,
                listOf("2348BCD")),
            Arguments.of("2BCD3BCD4BCD8BjCjDj", KeywordEnum.Timetable, OperationEnum.TimeLike,
                listOf("2348BCD")),
            Arguments.of("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEF"+
                    "GHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX9",
                null, null, null),
        )
    }
}
