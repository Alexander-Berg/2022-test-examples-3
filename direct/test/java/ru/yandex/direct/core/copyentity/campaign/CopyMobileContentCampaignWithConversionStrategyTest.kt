package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentYtHelper
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyMobileContentCampaignWithConversionStrategyTest : BaseCopyCampaignWithConversionStrategyTest() {

    @Autowired
    lateinit var mobileContentYtHelper: MobileContentYtHelper
    /**
     * Поля в каждом списке
     * 1 - isCopyConversionStrategies: Boolean - флаг копирования конверсионной стратегии
     * 2 - isDoNotCheckRightsToMetrikaGoals: Boolean - флаг копирования стратегии без проверки прав доступа на цели
     *                                                 в метрике
     * 3 - goalId: Long? - мобильная цель стратегии
     * 4 - statisticsGoalsIds: List<Long> - имеющаяся статистика по целям кампании
     * 5 - shouldDropStrategyToDefault: Boolean - должна ли сброситься стратегия на дефолтную
     * 6 - shouldDropGoalsStat: Boolean - должна ли не копироваться статистика по целям для каждой кампании
     */
    private fun strategyGoalsInBetweenClientsCopying() = listOf(
        // Нулевая цель
        listOf(false, false, zeroGoal[0], mobAvGoalS_1, true, true),
        listOf(false, true, zeroGoal[0], mobAvGoalS_2, true, false),
        listOf(true, false, zeroGoal[0], mobUnAvGoalS_1, true, true),
        listOf(true, true, zeroGoal[0], mobSemiAvGoalM_1, true, false),
        // Единичная доступная цель
        listOf(false, false, allMobileGoals[0], mobAvGoalM_1, true, true),
        listOf(false, true, getRandomMobileGoal(), mobAvGoalM_2, true, false),
        listOf(true, false, null, mobSemiAvGoalM_1, false, false), // null с для cpi стратегии
                                                                   // воспринимается как goalId = 4L
        listOf(true, false, allMobileGoals[0], mobSemiAvGoalM_2, false, false),
        listOf(true, false, allMobileGoals[10], mobSemiAvGoalS_1, false, false),
        listOf(true, false, getRandomMobileGoal(), mobSemiAvGoalS_2, false, false),
        listOf(true, true, getRandomMobileGoal(), mobUnAvGoalM_1, false, false),
        // Единичная недоступная цель
        listOf(false, false, unAvGoal1C_1[0], mobAvGoalM_1, true, true),
        listOf(false, true, unAvGoal2C_1[1], mobSemiAvGoalS_2, true, false),
        listOf(true, false, unAvGoal3C_1[2], mobAvGoalM_2, true, true),
        listOf(true, true, unAvGoal1C_1[0], mobUnAvGoalL_1, false, false),
    )

    private fun strategyGoalsInSameClientsCopying() = listOf(
        // Нулевая цель
        listOf(false, false, zeroGoal[0], mobAvGoalS_1, true, false),
        listOf(false, true, zeroGoal[0], mobSemiAvGoalM_1, true, false),
        listOf(true, false, zeroGoal[0], mobAvGoalM_2, true, false),
        listOf(true, true, zeroGoal[0], mobSemiAvGoalM_2, true, false),
        // Единичная доступная цель
        listOf(false, false, rmpClientMobileGoals[0], mobAvGoalS_2, false, false),
        listOf(false, true, getRandomMobileGoal(), mobSemiAvGoalS_1, false, false),
        listOf(true, false, null, mobAvGoalM_1, false, false), // null с для cpi стратегии
                                                               // воспринимается как goalId = 4L
        listOf(true, false, rmpClientMobileGoals[0], mobAvGoalM_2, false, false),
        listOf(true, false, rmpClientMobileGoals[10], mobSemiAvGoalS_2, false, false),
        listOf(true, false, getRandomRmpClientMobileGoal(), mobSemiAvGoalM_2, false, false),
        listOf(true, true, getRandomRmpClientMobileGoal(), mobUnAvGoalS_1, false, false),
        // Единичная недоступная цель
        listOf(false, false, unAvGoal1C_1[0], mobAvGoalS_1, false, false),
        listOf(false, true, unAvGoal2C_1[1], mobSemiAvGoalS_2, false, false),
        listOf(true, false, unAvGoal3C_1[2], mobAvGoalM_2, false, false),
        listOf(true, true, unAvGoal1C_1[0], mobUnAvGoalM_1, false, false),
    )

    @Before
    fun before() {
        initCounters()
        Mockito.doReturn(listOf<MobileContent>())
            .`when`(mobileContentYtHelper)
            .getMobileContentFromYt(anyInt(), any(), anyCollection())
    }

    @After
    fun after() {
        Mockito.reset(mobileContentYtHelper)
    }

    @Test
    @TestCaseName(
        "{method}(" +
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsInBetweenClientsCopying")
    fun testCheckCopyAutobudgetAvgCpiConversionStrategyBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        goalId: Long?,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val strategy: DbStrategy = createAutobudgetAvgCpiStrategy(goalId)

        createMobileContentCampaignCopyBetweenClientsAndCheck(
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    @Test
    @TestCaseName(
        "{method}(" +
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsInSameClientsCopying")
    fun testCheckCopyAutobudgetAvgCpiConversionStrategySameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        goalId: Long?,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val strategy: DbStrategy = createAutobudgetAvgCpiStrategy(goalId)

        createMobileContentCampaignCopySameClientAndCheck(
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    @Test
    @TestCaseName(
        "{method}(" +
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsInBetweenClientsCopying")
    fun testCheckCopyAutobudgetAvgCpiConversionStrategyWithPayForConversionBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        goalId: Long?,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val strategy: DbStrategy = createAutobudgetAvgCpiStrategyWithPayForConversion(goalId)

        createMobileContentCampaignCopyBetweenClientsAndCheck(
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    @Test
    @TestCaseName(
        "{method}(" +
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsInSameClientsCopying")
    fun testCheckCopyAutobudgetAvgCpiConversionStrategyWithPayForConversionSameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        goalId: Long?,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val strategy: DbStrategy = createAutobudgetAvgCpiStrategyWithPayForConversion(goalId)

        createMobileContentCampaignCopySameClientAndCheck(
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }
}
