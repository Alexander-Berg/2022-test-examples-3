package ru.yandex.direct.mysql2grut.enummappers

import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.RuleType
import ru.yandex.grut.auxiliary.proto.RetargetingRule
import ru.yandex.grut.auxiliary.proto.RetargetingRule.TRetargetingRule.EOperator

class RetargetingConditionEnumMappersTest : EnumMappersTestBase() {

    @Test
    fun checkRetCondOperator() {
        testBase(
            RuleType.values(),
            RetargetingConditionEnumMappers::toGrut,
            EOperator.RT_UNKNOWN
        )
    }

    @Test
    fun checkGoalType() {
        testBase(
            GoalType.values(),
            RetargetingConditionEnumMappers::toGrut,
            RetargetingRule.TRetargetingRule.EGoalType.GT_UNKNOWN
        )
    }
}
