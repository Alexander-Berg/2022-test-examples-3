package ru.yandex.direct.oneshot.oneshots.retargetingconditions

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.retargeting.model.Rule
import ru.yandex.direct.core.entity.retargeting.model.RuleType
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.CAMPAIGN_GOALS_SHORTCUT_NAME
import ru.yandex.direct.core.testing.data.TestFullGoals
import ru.yandex.direct.core.testing.data.TestRetargetingConditions
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.multitype.entity.LimitOffset
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.OneshotTestsUtils.Companion.hasDefect
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound

@OneshotTest
@RunWith(SpringRunner::class)
class RenameShortcutsOneshotTest {
    companion object {
        private const val OLD_SHORTCUT_NAME = "Old name"
        private const val ANOTHER_SHORTCUT_NAME = "Another shortcut name"
        private const val INVALID_SHORTCUT_NAME = "Invalid new name"
        private const val VALID_SHORTCUT_NAME = CAMPAIGN_GOALS_SHORTCUT_NAME
    }

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var retargetingConditionRepository: RetargetingConditionRepository

    @Autowired
    private lateinit var renameShortcutsOneshot: RenameShortcutsOneshot

    private var shard: Int = 0
    private lateinit var clientInfo: ClientInfo
    private lateinit var goal: Goal

    @Before
    fun before() {
        val userInfo = steps.userSteps().createDefaultUser()
        shard = userInfo.shard
        clientInfo = userInfo.clientInfo!!
        goal = TestFullGoals.defaultGoalByType(GoalType.GOAL)
    }

    @Test
    fun validate_ValidInputData_success() {
        val inputData = InputData(oldName = OLD_SHORTCUT_NAME, newName = VALID_SHORTCUT_NAME)
        val vr = renameShortcutsOneshot.validate(inputData)
        assertThat(vr.hasAnyErrors()).`as`("hasAnyErrors").isFalse
    }

    @Test
    fun validate_InvalidInputData_failure() {
        val inputData = InputData(oldName = OLD_SHORTCUT_NAME, newName = INVALID_SHORTCUT_NAME)
        val vr = renameShortcutsOneshot.validate(inputData)
        assertThat(vr).`as`("newNameError")
            .hasDefect("newName", objectNotFound())
    }

    private fun createRetargetingCondition(name: String): Long {
        val rule = Rule()
            .withGoals(listOf(goal))
            .withType(RuleType.OR)
        val shortcutRetargetingCondition = TestRetargetingConditions.defaultRetCondition(clientInfo.clientId)
            .withType(ConditionType.shortcuts)
            .withName(name)
            .withRules(listOf(rule))
            .withAvailable(true) as RetargetingCondition

        val shortcutRetargetingConditionInfo = steps.retConditionSteps()
            .createRetCondition(shortcutRetargetingCondition, clientInfo)
        return shortcutRetargetingConditionInfo.retConditionId
    }

    private fun getRetargetingConditionNamesCount(): Map<String, Int> {
        val conditions = retargetingConditionRepository.getFromRetargetingConditionsTable(
            shard, clientInfo.clientId, LimitOffset.maxLimited()
        )
        return conditions.groupBy { it.name }.mapValues { it.value.size }
    }

    @Test
    fun execute_success() {
        createRetargetingCondition(OLD_SHORTCUT_NAME)
        createRetargetingCondition(OLD_SHORTCUT_NAME)
        createRetargetingCondition(ANOTHER_SHORTCUT_NAME)

        var namesCount = getRetargetingConditionNamesCount()
        assertThat(namesCount).hasSize(2)
        assertThat(namesCount[OLD_SHORTCUT_NAME]).isEqualTo(2)
        assertThat(namesCount[ANOTHER_SHORTCUT_NAME]).isEqualTo(1)

        val inputData = InputData(oldName = OLD_SHORTCUT_NAME, newName = VALID_SHORTCUT_NAME)
        renameShortcutsOneshot.execute(inputData = inputData, prevState = null, shard = shard)

        namesCount = getRetargetingConditionNamesCount()
        assertThat(namesCount).hasSize(2)
        assertThat(namesCount[VALID_SHORTCUT_NAME]).isEqualTo(2)
        assertThat(namesCount[ANOTHER_SHORTCUT_NAME]).isEqualTo(1)
    }
}
