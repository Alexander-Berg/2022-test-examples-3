package ru.yandex.direct.infrastructure.mysql.tables

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class RetargetingConditionsTest : FunSpec({
    test("ConditionJSON parsing works") {
        val condition: ConditionJSON =
            Json.decodeFromString("""[{"goals":[{"goal_id":1872841,"time":7}],"type":"or"}]""")

        val expected = listOf(
            ConditionJSONRule(
                listOf(
                    ConditionJSONGoal(1872841, 7)
                ),
                ConditionJSONRuleType.OR
            )
        )

        condition shouldBe expected
    }
})
