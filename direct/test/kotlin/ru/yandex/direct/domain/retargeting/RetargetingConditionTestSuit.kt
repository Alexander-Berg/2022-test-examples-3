package ru.yandex.direct.domain.retargeting

import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.equality.FieldsEqualityCheckConfig
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import ru.yandex.direct.domain.client.ClientID
import kotlin.random.Random

fun retargetingConditionTestSuit(repository: RetargetingConditionRepository) = funSpec {
    test("add should store retargeting condition with new id") {
        val clientId = ClientID(Random.nextLong())

        val expected =
            MetrikaRetargetingCondition(
                id = RetargetingConditionID(0), clientId = clientId, rules = listOf(
                    MetrikaRule(
                        RuleType.ANY, goals = listOf(
                            MetrikaGoal(MetrikaGoal.ID.random()),
                            MetrikaGoal(MetrikaGoal.ID.random())
                        )
                    )
                )
            )

        val id = repository.create(expected).shouldNotBeNull()

        val condition = repository.findById(id).shouldNotBeNull()

        condition.shouldBeEqualToComparingFields(
            expected,
            FieldsEqualityCheckConfig(propertiesToExclude = listOf(MetrikaRetargetingCondition::id))
        )
        condition.id shouldBe id
    }

    test("findForClient should return all retargetings for client") {
        val clientId = ClientID(Random.nextLong())

        val expectedIDs = listOf(
            MetrikaRetargetingCondition(id = RetargetingConditionID(0), clientId = clientId, rules = emptyList()),
            InterestsRetargetingCondition(
                id = RetargetingConditionID(0),
                clientId = clientId,
                rule = InterestsRule(RuleType.ANY, InterestsGoal(InterestsGoal.ID.random()))
            ),
        ).map { repository.create(it) }

        val conditions = repository.findForClient(clientId).map { it.id }

        conditions shouldContainAll expectedIDs
    }

    test("findById should return null for unknown retargeting conditions") {
        repository.findById(RetargetingConditionID(Random.nextLong())) shouldBe null
    }
}
