package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.CopyOperation
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo

/**
 * Проверяет массовое копирование кампаний с конверсионными стратегиями. Нужен, чтобы проверить одновременное
 * копирование связок кампаний с проверкой прав на разные типы целей, мобильные и обычные. При копировании только
 * одной кампании это проверить нельзя.
 */
@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyMassCampaignsWithConversionStrategyTest : BaseCopyCampaignWithConversionStrategyTest() {

    /**
     * Поля в каждом списке:
     * 1 - isCopyConversionStrategies: Boolean - флаг копирования конверсионной стратегии
     * 2 - isDoNotCheckRightsToMetrikaGoals: Boolean - флаг копирования стратегии без проверки прав доступа на цели
     *                                                 в метрике
     *
     * Дальше идут списки полей для каждой из 5 кампаний со следующими типами стратегий
     * autobudget_avg_cpi (мобильные), autobudget_avg_cpa, autobudget, autobudget_crr, autobudget_roi
     * 3 - strategyGoalsIdsLists: List<List<Long>> - цели каждой стратегии
     * 4 - statisticsGoalsIdsLists: List<List<Long>> - статистика по целям каждой стратегии
     * 5 - shouldDropStrategyToDefaults: List<Boolean> - должна ли сброситься стратегия на дефолтную для каждой кампании
     * 6 - shouldDropGoalsStats: List<Boolean> - должна ли не копироваться статистика по целям для каждой кампании
     */
    private fun massCampaignsCopyBetweenClientsParams() = listOf(
        // Не копировать стратегии, проверять доступность целей
        listOf(
            false, false,
            listOf(zeroGoal, avGoal2C_2, avGoal1C_1, unAvGoal_Engaged, avGoal2C_1),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(true, true, true, true, true),
            listOf(true, true, true, true, true),
        ),
        // Не копировать стратегии, не проверять доступность целей
        listOf(
            false, true,
            listOf(engagedGoal, avGoal2C_2, avGoal2C_Engaged, unAvGoal_Engaged, avGoal1C_2),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(true, true, true, true, true),
            listOf(false, false, false, false, false),
        ),
        // Копировать стратегии, проверять доступность целей
        listOf(
            true, false,
            listOf(zeroGoal, unAvGoal_Engaged, avGoal2C_Engaged, avGoal2C_2, avGoal1C_2),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(true, false, false, false, false),
            listOf(true, false, false, false, false),
        ),
        listOf(
            true, false,
            listOf(listOf(allMobileGoals[0]), zeroGoal, unAvGoal3C_1, avGoal1C_1, engagedGoal),
            listOf(avGoal2C_2, unAvGoal1C_1, avGoal2C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(false, true, true, false, true),
            listOf(false, true, true, false, true),
        ),
        listOf(
            true, false,
            listOf(listOf(getRandomMobileGoal()), engagedGoal, zeroGoal, avGoal2C_1, avGoal2C_Engaged),
            listOf(avGoal1C_1, avGoal2C_1, unAvGoal2C_1, avGoal2C_2, semiAvGoal3C_2),
            listOf(false, true, true, true, false),
            listOf(false, true, true, true, false),
        ),
        listOf(
            true, false,
            listOf(listOf(getRandomMobileGoal()), semiAvGoal3C_2, avGoal1C_2, zeroGoal, unAvGoal_Engaged),
            listOf(unAvGoal2C_1, semiAvGoal1C_1, avGoal2C_2, unAvGoal3C_1, unAvGoal1C_1),
            listOf(false, true, false, true, true),
            listOf(false, true, false, true, true),
        ),
        listOf(
            true, false,
            listOf(listOf(unAvGoal2C_1[1]), avGoal2C_1, unAvGoal1C_1, semiAvGoal1C_2, zeroGoal),
            listOf(unAvGoal1C_1, avGoal2C_2, avGoal2C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(true, false, true, true, true),
            listOf(true, false, true, true, true),
        ),
        // Копировать стратегии, не проверять доступность целей
        listOf(
            true, true,
            listOf(listOf(unAvGoal1C_1[0]), zeroGoal, avGoal2C_Engaged, unAvGoal_Engaged, avGoal2C_2),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(false, true, false, true, false),
            listOf(false, false, false, false, false),
        ),
    )

    private fun massCampaignsCopySameClientParams() = listOf(
        // Не копировать стратегии, проверять доступность целей
        listOf(
            false, false,
            listOf(zeroGoal, avGoal2C_2, avGoal1C_1, unAvGoal_Engaged, avGoal2C_1),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(true, false, false, true, false),
            listOf(false, false, false, false, false),
        ),
        // Не копировать стратегии, не проверять доступность целей
        listOf(
            false, true,
            listOf(engagedGoal, avGoal2C_2, avGoal2C_Engaged, avGoal1C_2, unAvGoal_Engaged),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(true, false, false, false, true),
            listOf(false, false, false, false, false),
        ),
        // Копировать стратегии, проверять доступность целей
        listOf(
            true, false,
            listOf(zeroGoal, unAvGoal_Engaged, avGoal2C_Engaged, avGoal2C_2, avGoal1C_2),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(true, false, false, false, false),
            listOf(false, false, false, false, false),
        ),
        listOf(
            true, false,
            listOf(listOf(allMobileGoals[0]), zeroGoal, unAvGoal3C_1, avGoal1C_1, engagedGoal),
            listOf(avGoal2C_2, unAvGoal1C_1, avGoal2C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(false, true, false, false, true),
            listOf(false, false, false, false, false),
        ),
        listOf(
            true, false,
            listOf(listOf(getRandomMobileGoal()), engagedGoal, zeroGoal, avGoal2C_1, avGoal2C_Engaged),
            listOf(avGoal1C_1, avGoal2C_1, unAvGoal2C_1, avGoal2C_2, semiAvGoal3C_2),
            listOf(false, true, true, false, false),
            listOf(false, false, false, false, false),
        ),
        listOf(
            true, false,
            listOf(listOf(getRandomMobileGoal()), semiAvGoal3C_2, avGoal1C_2, zeroGoal, semiAvGoal3C_2),
            listOf(unAvGoal2C_1, semiAvGoal1C_1, avGoal2C_2, unAvGoal3C_1, unAvGoal1C_1),
            listOf(false, false, false, true, false),
            listOf(false, false, false, false, false),
        ),
        listOf(
            true, false,
            listOf(listOf(unAvGoal2C_1[1]), avGoal2C_1, unAvGoal1C_1, semiAvGoal1C_2, zeroGoal),
            listOf(unAvGoal1C_1, avGoal2C_2, avGoal2C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(false, false, false, false, true),
            listOf(false, false, false, false, false),
        ),
        // Копировать стратегии, не проверять доступность целей
        listOf(
            true, true,
            listOf(listOf(unAvGoal1C_1[0]), zeroGoal, avGoal2C_Engaged, unAvGoal_Engaged, avGoal2C_2),
            listOf(avGoal1C_1, avGoal2C_1, semiAvGoal1C_2, unAvGoal3C_1, semiAvGoal3C_2),
            listOf(false, true, false, true, false),
            listOf(false, false, false, false, false),
        ),
    )

    @Test
    @TestCaseName(
        "{method}(" +
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, " +
            "strategyGoalsIdsLists={2}, statisticsGoalsIdsLists={3}, " +
            "shouldDropStrategyToDefaults={4}, shouldDropGoalsStats={5})"
    )
    @Parameters(method = "massCampaignsCopyBetweenClientsParams")
    fun testCheckMassCampaignsCopyBetweenClients(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsIdsLists: List<List<Long>>,
        statisticsGoalsIdsLists: List<List<Long>>,
        shouldDropStrategyToDefaults: List<Boolean>,
        shouldDropGoalsStats: List<Boolean>,
    ) {
        val (originalStrategies: List<DbStrategy>, originalCampaignInfos: List<CampaignInfo<out CommonCampaign>>) =
            createStrategiesAndCampaigns(
                strategyGoalsIdsLists
            )

        copyMassCampaignsBetweenClientsAndCheck(
            originalStrategies,
            originalCampaignInfos,
            statisticsGoalsIdsLists,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefaults,
            shouldDropGoalsStats
        )
    }

    @Before
    fun before() {
        initCounters()
    }

    @Test
    @TestCaseName(
        "{method}(" +
            "flags.isCopyConversionStrategies={0}, flags.isDoNotCheckRightsToMetrikaGoals={1}, " +
            "strategyGoalsIdsLists={2}, statisticsGoalsIdsLists={3}, " +
            "shouldDropStrategyToDefaults={4}, shouldDropGoalsStats={5})"
    )
    @Parameters(method = "massCampaignsCopySameClientParams")
    fun testCheckMassCampaignsCopySameClient(
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        strategyGoalsIdsLists: List<List<Long>>,
        statisticsGoalsIdsLists: List<List<Long>>,
        shouldDropStrategyToDefaults: List<Boolean>,
        shouldDropGoalsStats: List<Boolean>,
    ) {
        val (originalStrategies: List<DbStrategy>, originalCampaignInfos: List<CampaignInfo<out CommonCampaign>>) =
            createStrategiesAndCampaigns(
                strategyGoalsIdsLists
            )

        copyMassCampaignsSameClientAndCheck(
            originalStrategies,
            originalCampaignInfos,
            statisticsGoalsIdsLists,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefaults,
            shouldDropGoalsStats
        )
    }

    /**
     * Создает стратегии и кампании. Порядок стратегий:
     * autobudget_avg_cpi (мобильные), autobudget_avg_cpa, autobudget, autobudget_crr, autobudget_roi,
     * Порядок кампаний:
     * MobileContentCampaign (мобильные), TextCampaign, TextCampaign, TextCampaign, TextCampaign
     */
    private fun createStrategiesAndCampaigns(
        goalIdsLists: List<List<Long>>
    ): Pair<List<DbStrategy>, List<CampaignInfo<out CommonCampaign>>> {
        //                                  goalId      meaningfulGoals   metrikaCountersIds
        val goalsAndCountersList: List<Triple<Long, List<MeaningfulGoal>, List<Long>>> =
            goalIdsLists.map { unpackFromGoalsIds(it) }

        val originalStrategies: List<DbStrategy> = listOf(
            createAutobudgetAvgCpiStrategy(goalsAndCountersList[0].first),
            createAutobudgetAvgCpaStrategy(goalsAndCountersList[1].first),
            createAutobudgetStrategy(goalsAndCountersList[2].first),
            createAutobudgetCrrStrategy(goalsAndCountersList[3].first),
            createAutobudgetRoiStrategy(goalsAndCountersList[4].first),
        )

        val originalCampaignInfos: List<CampaignInfo<out CommonCampaign>> = listOf(
            createMobileContentCampaign(originalStrategies[0]),
            createTextCampaign(originalStrategies[1], goalsAndCountersList[1].third, goalsAndCountersList[1].second),
            createTextCampaign(originalStrategies[2], goalsAndCountersList[2].third, goalsAndCountersList[2].second),
            createTextCampaign(originalStrategies[3], goalsAndCountersList[3].third, goalsAndCountersList[3].second),
            createTextCampaign(originalStrategies[4], goalsAndCountersList[4].third, goalsAndCountersList[4].second),
        )

        return Pair(originalStrategies, originalCampaignInfos)
    }

    private fun copyMassCampaignsSameClientAndCheck(
        originalStrategies: List<DbStrategy>,
        originalCampaignInfos: List<CampaignInfo<out CommonCampaign>>,
        statisticsGoalsIdsLists: List<List<Long>>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefaults: List<Boolean>,
        shouldDropGoalsStats: List<Boolean>,
    ) {
        val originalCampMetrikaGoals: List<CampMetrikaGoal> =
            saveCampMetrikaGoals(originalCampaignInfos, statisticsGoalsIdsLists)

        val operation = sameClientCampaignCopyOperation(
            originalCampaignInfos,
            flags = CopyCampaignFlags(
                isCopyConversionStrategies = isCopyConversionStrategies,
                isDoNotCheckRightsToMetrikaGoals = isDoNotCheckRightsToMetrikaGoals
            )
        )

        copyMassCampaignsAndCheck(
            client,
            originalStrategies,
            originalCampaignInfos,
            originalCampMetrikaGoals,
            operation,
            shouldDropStrategyToDefaults,
            shouldDropGoalsStats
        )
    }

    private fun copyMassCampaignsBetweenClientsAndCheck(
        originalStrategies: List<DbStrategy>,
        originalCampaignInfos: List<CampaignInfo<out CommonCampaign>>,
        statisticsGoalsIds: List<List<Long>>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefaults: List<Boolean>,
        shouldDropGoalsStats: List<Boolean>,
    ) {
        val originalCampMetrikaGoals: List<CampMetrikaGoal> =
            saveCampMetrikaGoals(originalCampaignInfos, statisticsGoalsIds)

        val operation = betweenClientsCampaignCopyOperation(
            originalCampaignInfos, targetClient,
            flags = CopyCampaignFlags(
                isCopyConversionStrategies = isCopyConversionStrategies,
                isDoNotCheckRightsToMetrikaGoals = isDoNotCheckRightsToMetrikaGoals
            )
        )

        copyMassCampaignsAndCheck(
            client,
            originalStrategies,
            originalCampaignInfos,
            originalCampMetrikaGoals,
            operation,
            shouldDropStrategyToDefaults,
            shouldDropGoalsStats
        )
    }

    private fun copyMassCampaignsAndCheck(
        targetClient: ClientInfo,
        originalStrategies: List<DbStrategy>,
        originalCampaignInfos: List<CampaignInfo<out CommonCampaign>>,
        allOriginalCampMetrikaGoals: List<CampMetrikaGoal>,
        operation: CopyOperation<BaseCampaign, Long>,
        shouldDropStrategyToDefaults: List<Boolean>,
        shouldDropGoalsStats: List<Boolean>,
    ) {
        val campaignIds = copyValidCampaigns(operation)
        val originalCampaignWithMeaningfulGoalsIds: List<Long> =
            originalCampaignInfos
                .filter { it.typedCampaign is CampaignWithMeaningfulGoalsWithRequiredFields }
                .map { it.campaignId }

        val originalCampaignsByIdMap: Map<Long, CampaignWithMeaningfulGoalsWithRequiredFields> =
            getCampaigns<CampaignWithMeaningfulGoalsWithRequiredFields>(originalCampaignWithMeaningfulGoalsIds)
                .associateBy { it.id }

        val originalCampMetrikaGoalsByCampaignIdMap: Map<Long, List<CampMetrikaGoal>> =
            allOriginalCampMetrikaGoals.groupBy { it.campaignId }

        val copiedCampaigns: List<CommonCampaign> = getCampaigns(campaignIds, targetClient.shard)
        val copiedCampMetrikaGoalsByCampaignIdMap: Map<Long, List<CampMetrikaGoal>> =
            testMetrikaCampaignRepository.getCampMetrikaGoalsByCampaignIds(
                targetClient.shard,
                targetClient.clientId,
                campaignIds
            ).groupBy { it.campaignId }

        softly {
            copiedCampaigns.forEachIndexed { index, copiedCampaign ->
                val originalCampaignId: Long = originalCampaignInfos[index].campaignId
                val originalCampMetrikaGoals: List<CampMetrikaGoal> =
                    originalCampMetrikaGoalsByCampaignIdMap.getOrDefault(originalCampaignId, listOf())

                val copiedCampMetrikaGoals: List<CampMetrikaGoal> =
                    copiedCampMetrikaGoalsByCampaignIdMap.getOrDefault(copiedCampaign.id, listOf())

                val originalStrategy: DbStrategy = originalStrategies[index]
                val shouldDropStrategyToDefault: Boolean = shouldDropStrategyToDefaults[index]
                val shouldDropGoalsStat: Boolean = shouldDropGoalsStats[index]

                if (copiedCampaign is MobileContentCampaign) {
                    checkCopiedCampaignWithMobileContent(
                        this,
                        originalStrategy,
                        originalCampMetrikaGoals,
                        copiedCampaign,
                        copiedCampMetrikaGoals,
                        shouldDropStrategyToDefault,
                        shouldDropGoalsStat
                    )
                } else if (copiedCampaign is CampaignWithMeaningfulGoalsWithRequiredFields) {
                    checkCopiedCampaignWithMeaningfulGoals(
                        this,
                        originalStrategy,
                        originalCampaignsByIdMap[originalCampaignId]!!,
                        originalCampMetrikaGoals,
                        copiedCampaign,
                        copiedCampMetrikaGoals,
                        shouldDropStrategyToDefault,
                        shouldDropGoalsStat
                    )
                }
            }
        }
    }

    private fun saveCampMetrikaGoals(
        originalCampaignInfos: List<CampaignInfo<out CommonCampaign>>,
        statisticsGoalsIds: List<List<Long>>
    ) = saveStatisticsGoals(
        client,
        originalCampaignInfos
            .mapIndexed { index, campaign -> campaign.campaignId to statisticsGoalsIds[index] }.toMap()
    )
}
