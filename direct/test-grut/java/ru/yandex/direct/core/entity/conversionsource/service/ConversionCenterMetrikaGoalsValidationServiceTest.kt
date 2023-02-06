package ru.yandex.direct.core.entity.conversionsource.service

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.conversionsource.model.MetrikaGoalSelection
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.PathHelper
import java.util.Collections.singletonList

class ConversionCenterMetrikaGoalsValidationServiceTest {
    private lateinit var validationService: ConversionCenterMetrikaGoalsValidationService

    private val counterId1 = 100000000L
    private val notExistingCounterId = 200000000L

    private val goalId1 = 1000000000
    private val goalId2 = 1000000001

    private lateinit var goalsInfo: Map<Int, List<CounterGoal>>

    @Before
    fun setUp() {
        validationService = ConversionCenterMetrikaGoalsValidationService()

        goalsInfo = mapOf(
            counterId1.toInt() to singletonList(CounterGoal().withId(goalId1))
        )
    }

    @Test
    fun validate_success() {
        val metrikaGoalSelections: List<MetrikaGoalSelection> = listOf(
            MetrikaGoalSelection(counterId1, goalId1.toLong(), null, true)
        )
        val vr = validationService.validate(metrikaGoalSelections, goalsInfo)
        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validateCounterExistence_withValidationError() {
        val metrikaGoalSelections: List<MetrikaGoalSelection> = listOf(
            MetrikaGoalSelection(notExistingCounterId, goalId1.toLong(), null, true)
        )
        val vr = validationService.validate(metrikaGoalSelections, goalsInfo)
        assertThat(vr.flattenErrors()).`is`(
            Conditions.matchedBy(
                contains(
                    validationError(
                        PathHelper.path(
                            PathHelper.index(0),
                            PathHelper.field("metrikaCounterId")
                        ), objectNotFound()
                    )
                )
            )
        )
    }

    @Test
    fun validateGoalMatchCounter_withValidationError() {
        val metrikaGoalSelections: List<MetrikaGoalSelection> = listOf(
            MetrikaGoalSelection(counterId1, goalId2.toLong(), null, true)
        )
        val vr = validationService.validate(metrikaGoalSelections, goalsInfo)
        assertThat(vr.flattenErrors()).`is`(
            Conditions.matchedBy(
                contains(
                    validationError(
                        PathHelper.path(
                            PathHelper.index(0),
                            PathHelper.field("goalId")
                        ), invalidValue()
                    )
                )
            )
        )
    }
}
