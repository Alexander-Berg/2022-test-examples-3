package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithConversionStrategyTest : BaseCopyCampaignWithConversionStrategyTest() {

    /**
     * Поля в каждом списке:
     * 1 - isCopyConversionStrategies: Boolean - флаг копирования конверсионной стратегии
     * 2 - isDoNotCheckRightsToMetrikaGoals: Boolean - флаг копирования стратегии без проверки прав доступа на цели
     *                                                 в метрике
     * 3 - strategyGoalsId: List<Long> - список целей стратегии. Если их больше одной, главная цель стратегии будет
     *                                   MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID (13), а переданный список целей будет
     *                                   установлен в поле meaningfulGoals
     * 4 - statisticsGoalsIds: List<Long> - имеющаяся статистика по целям кампании
     * 5 - shouldDropStrategyToDefault: Boolean - должна ли сброситься стратегия на дефолтную
     * 6 - shouldDropGoalsStat: Boolean - должна ли не копироваться статистика по целям для каждой кампании
     */
    private fun strategyGoalsInBetweenClientsCopying() = listOf(
        // Нулевая цель
        listOf(false, false, zeroGoal, avGoal1C_1, true, true),
        listOf(false, true, zeroGoal, avGoal1C_1, true, false),
        listOf(true, false, zeroGoal, avGoal1C_1, true, true),
        listOf(true, true, zeroGoal, avGoal1C_1, true, false),
        // Единичная доступная цель
        listOf(false, false, avGoal1C_1, avGoal1C_1, true, true),
        listOf(false, true, avGoal1C_2, avGoal2C_2, true, false),
        listOf(true, false, avGoal1C_2, avGoal1C_1, false, false),
        listOf(true, true, avGoal1C_1, unAvGoal2C_1, false, false),
        // Массовая доступная цель
        listOf(false, false, avGoal2C_1, avGoal1C_1, true, true),
        listOf(false, true, avGoal2C_2, avGoal2C_1, true, false),
        listOf(true, false, avGoal2C_2, avGoal2C_2, false, false),
        listOf(true, false, avGoal2C_Engaged, avGoal2C_2, false, false),
        listOf(true, true, avGoal2C_1, unAvGoal3C_1, false, false),
        // Единичная недоступная цель
        listOf(false, false, unAvGoal1C_1, avGoal1C_1, true, true),
        listOf(false, true, unAvGoal1C_1, avGoal2C_1, true, false),
        listOf(true, false, engagedGoal, avGoal1C_1, true, true),
        listOf(true, false, unAvGoal1C_1, avGoal2C_2, true, true),
        listOf(true, true, unAvGoal1C_1, unAvGoal3C_1, false, false),
        // Массовая недоступная цель
        listOf(false, false, unAvGoal2C_1, avGoal1C_1, true, true),
        listOf(false, true, unAvGoal3C_1, avGoal2C_1, true, false),
        listOf(true, false, semiAvGoal1C_1, avGoal2C_2, true, true),
        listOf(true, false, semiAvGoal1C_2, avGoal2C_2, true, true),
        listOf(true, false, semiAvGoal3C_1, avGoal2C_2, true, true),
        listOf(true, false, semiAvGoal3C_2, avGoal2C_2, true, true),
        listOf(true, false, semiAvGoal3C_Engaged, avGoal2C_2, true, true),
        listOf(true, true, unAvGoal2C_1, unAvGoal3C_1, false, false),
    )

    private fun strategyGoalsWithWeekEngagedCheckInBetweenClientsCopying() =
        strategyGoalsInBetweenClientsCopying() + listOf(
            listOf(true, false, unAvGoal_Engaged, avGoal1C_1, false, false)
        )

    private fun strategyGoalsWithStrongEngagedCheckInBetweenClientsCopying() =
        strategyGoalsInBetweenClientsCopying() + listOf(
            listOf(true, false, unAvGoal_Engaged, avGoal1C_1, true, true)
        )

    private fun strategyGoalsInSameClientsCopying() = listOf(
        // Нулевая цель
        listOf(false, false, zeroGoal, avGoal1C_1, true, false),
        listOf(false, true, zeroGoal, avGoal1C_1, true, false),
        listOf(true, false, zeroGoal, avGoal1C_1, true, false),
        listOf(true, true, zeroGoal, avGoal1C_1, true, false),
        // Единичная доступная цель
        listOf(false, false, avGoal1C_1, avGoal1C_1, false, false),
        listOf(false, true, avGoal1C_2, avGoal2C_2, false, false),
        listOf(true, false, avGoal1C_2, avGoal1C_1, false, false),
        listOf(true, true, avGoal1C_1, unAvGoal2C_1, false, false),
        // Массовая доступная цель
        listOf(false, false, semiAvGoal3C_1, avGoal1C_1, false, false),
        listOf(false, true, avGoal2C_1, avGoal2C_1, false, false),
        listOf(true, false, avGoal2C_2, avGoal2C_2, false, false),
        listOf(true, false, avGoal2C_Engaged, avGoal2C_2, false, false),
        listOf(true, true, semiAvGoal3C_2, unAvGoal3C_1, false, false),
        // Единичная недоступная цель
        listOf(false, false, unAvGoal1C_1, avGoal1C_1, false, false),
        listOf(false, true, unAvGoal1C_1, avGoal2C_1, false, false),
        listOf(true, false, engagedGoal, avGoal1C_1, true, false),
        listOf(true, false, unAvGoal1C_1, avGoal2C_2, false, false),
        listOf(true, true, unAvGoal1C_1, unAvGoal3C_1, false, false),
        // Массовая недоступная цель
        listOf(false, false, unAvGoal2C_1, avGoal1C_1, false, false),
        listOf(false, true, unAvGoal3C_1, avGoal2C_1, false, false),
        listOf(true, false, unAvGoal3C_1, avGoal2C_2, false, false),
        listOf(true, true, unAvGoal2C_1, unAvGoal3C_1, false, false),
    )

    private fun strategyGoalsWithWeekEngagedCheckInSameClientsCopying() =
        strategyGoalsInSameClientsCopying() + listOf(
            listOf(true, false, unAvGoal_Engaged, avGoal1C_1, false, false)
        )

    private fun strategyGoalsWithStrongEngagedCheckInSameClientsCopying() =
        strategyGoalsInSameClientsCopying() + listOf(
            listOf(true, false, unAvGoal_Engaged, avGoal1C_1, true, false)
        )

    private fun strategyOnlySingleGoalsInBetweenClientsCopying() = listOf(
        // Единичная доступная цель
        listOf(false, false, avGoal1C_1, avGoal1C_1, true, true),
        listOf(false, true, avGoal1C_2, avGoal2C_2, true, false),
        listOf(true, false, avGoal1C_2, avGoal1C_1, false, false),
        listOf(true, true, avGoal1C_1, unAvGoal2C_1, false, false),
        // Единичная недоступная цель
        listOf(false, false, unAvGoal1C_1, avGoal1C_1, true, true),
        listOf(false, true, unAvGoal1C_1, avGoal2C_1, true, false),
        listOf(true, false, engagedGoal, avGoal1C_1, true, true),
        listOf(true, false, unAvGoal1C_1, avGoal2C_2, true, true),
        listOf(true, true, unAvGoal1C_1, unAvGoal3C_1, false, false),
    )

    private fun strategyOnlySingleGoalsInSameClientsCopying() = listOf(
        // Единичная доступная цель
        listOf(false, false, avGoal1C_1, avGoal1C_1, false, false),
        listOf(false, true, avGoal1C_2, avGoal2C_2, false, false),
        listOf(true, false, avGoal1C_2, avGoal1C_1, false, false),
        listOf(true, true, avGoal1C_1, unAvGoal2C_1, false, false),
        // Единичная недоступная цель
        listOf(false, false, unAvGoal1C_1, avGoal1C_1, false, false),
        listOf(false, true, unAvGoal1C_1, avGoal2C_1, false, false),
        listOf(true, false, engagedGoal, avGoal1C_1, true, false),
        listOf(true, false, unAvGoal1C_1, avGoal2C_2, false, false),
        listOf(true, true, unAvGoal1C_1, unAvGoal3C_1, false, false),
    )

    @Before
    fun before() {
        initCounters()
    }

    @Test
    @TestCaseName(
        "{method}(" +
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithStrongEngagedCheckInBetweenClientsCopying")
    fun testCheckCopyAutobudgetStrategyBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetStrategy(goalId)

        createTextCampaignCopyBetweenClientsAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithStrongEngagedCheckInSameClientsCopying")
    fun testCheckCopyAutobudgetStrategySameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetStrategy(goalId)

        createTextCampaignCopySameClientAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithWeekEngagedCheckInBetweenClientsCopying")
    fun testCheckCopyAutobudgetAvgCpaStrategyBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetAvgCpaStrategy(goalId)

        createTextCampaignCopyBetweenClientsAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithWeekEngagedCheckInSameClientsCopying")
    fun testCheckCopyAutobudgetAvgCpaStrategySameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetAvgCpaStrategy(goalId)

        createTextCampaignCopySameClientAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyOnlySingleGoalsInBetweenClientsCopying")
    fun testCheckCopyAutobudgetAvgCpaStrategyWithPayPerConversionBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetAvgCpaStrategyWithPayForConversion(goalId)

        createTextCampaignCopyBetweenClientsAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyOnlySingleGoalsInSameClientsCopying")
    fun testCheckCopyAutobudgetAvgCpaStrategyWithPayPerConversionSameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetAvgCpaStrategyWithPayForConversion(goalId)

        createTextCampaignCopySameClientAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithStrongEngagedCheckInBetweenClientsCopying")
    fun testCheckCopyAutobudgetCrrStrategyBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetCrrStrategy(goalId)

        createTextCampaignCopyBetweenClientsAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithStrongEngagedCheckInSameClientsCopying")
    fun testCheckCopyAutobudgetCrrConversionStrategySameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetCrrStrategy(goalId)

        createTextCampaignCopySameClientAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyOnlySingleGoalsInBetweenClientsCopying")
    fun testCheckCopyAutobudgetCrrStrategyWithPayPerConversionBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetCrrStrategyWithPayForConversion(goalId)

        createTextCampaignCopyBetweenClientsAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyOnlySingleGoalsInSameClientsCopying")
    fun testCheckCopyAutobudgetCrrStrategyWithPayPerConversionSameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetCrrStrategyWithPayForConversion(goalId)

        createTextCampaignCopySameClientAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithStrongEngagedCheckInBetweenClientsCopying")
    fun testCheckCopyAutobudgetRoiStrategyBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetRoiStrategy(goalId)

        createTextCampaignCopyBetweenClientsAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
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
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, strategyGoalsId={2}, " +
            "statisticsGoalsIds={3}, shouldDropStrategyToDefault={4}, shouldDropGoalsStat={5})"
    )
    @Parameters(method = "strategyGoalsWithStrongEngagedCheckInSameClientsCopying")
    fun testCheckCopyAutobudgetRoiConversionStrategySameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsId: List<Long>,
        statisticsGoalsIds: List<Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val (goalId: Long, meaningfulGoals: List<MeaningfulGoal>, countersIds: List<Long>) = unpackFromGoalsIds(
            strategyGoalsId
        )
        val strategy: DbStrategy = createAutobudgetRoiStrategy(goalId)

        createTextCampaignCopySameClientAndCheck(
            strategy,
            countersIds,
            meaningfulGoals,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }
}
