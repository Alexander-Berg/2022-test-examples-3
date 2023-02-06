package ru.yandex.direct.grid.processing.service.validation

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.grid.model.GdOrderByParams
import ru.yandex.direct.grid.model.GdStatRequirements
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField
import ru.yandex.direct.grid.processing.model.common.GdOrderingItemWithParams
import ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions.invalidGoalIdsFromOrderBy
import ru.yandex.direct.test.utils.randomPositiveLong

@RunWith(JUnitParamsRunner::class)
class CheckOrderByForGoalsHasGoalIdsInRequestTest {

    fun parametrizedTestData(): List<List<Any?>> = listOf(
            listOf("empty orderBy", emptyList<GdOrderingItemWithParams<Any>>(), GdStatRequirements(), null),
            listOf("empty orderBy with goalIds in statRequirements", emptyList<GdOrderingItemWithParams<Any>>(),
                    GdStatRequirements().withGoalIds(setOf(randomPositiveLong())), null),
            listOf("orderBy without goalId", listOf(GdAdOrderBy().withField(GdAdOrderByField.STATUS),
                    GdAdOrderBy().withField(GdAdOrderByField.HREF).withParams(GdOrderByParams())),
                    GdStatRequirements(), null),

            listOf("goalId in orderBy", listOf(GdAdOrderBy().withParams(GdOrderByParams().withGoalId(123))),
                    GdStatRequirements(), setOf(123L)),
            listOf("goalId in orderBy and another goalId in statRequirements",
                    listOf(GdAdOrderBy().withParams(GdOrderByParams().withGoalId(123))),
                    GdStatRequirements().withGoalIds(setOf(444L)), setOf(123L)),

            listOf("goalId in orderBy and in statRequirements", listOf(GdAdOrderBy().withParams(GdOrderByParams().withGoalId(125))),
                    GdStatRequirements().withGoalIds(setOf(125L)), null),
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("checkGetBannerWithCloseCounterVarIds: {0}")
    fun checkOrderByForGoalsHasGoalIdsInRequest(@Suppress("UNUSED_PARAMETER") description: String,
                                                orderByList: List<GdOrderingItemWithParams<Any>>,
                                                gdStatRequirements: GdStatRequirements,
                                                invalidGoalIds: Set<Long>?) {
        val defect = GridValidationService
                .checkOrderByForGoalsHasGoalIdsInRequest(orderByList, gdStatRequirements)
        val expectedDefect = if (invalidGoalIds == null) null else invalidGoalIdsFromOrderBy(invalidGoalIds)

        assertThat(defect)
                .isEqualTo(expectedDefect)
    }

}
