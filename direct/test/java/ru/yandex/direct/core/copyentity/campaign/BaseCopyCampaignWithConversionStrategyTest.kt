package ru.yandex.direct.core.copyentity.campaign

import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.assertj.core.util.BigDecimalComparator
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.assertListAreEqualsIgnoringOrder
import ru.yandex.direct.core.copyentity.CopyOperation
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.BY_ALL_GOALS_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCampaignRepository
import ru.yandex.direct.core.entity.metrika.service.MobileGoalsPermissionService
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoalId
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalRole
import ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.repository.TestMetrikaCampaignRepository
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.metrika.client.MetrikaClient
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.rbac.RbacService
import ru.yandex.direct.utils.CommonUtils
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Random
import java.util.function.BiPredicate

abstract class BaseCopyCampaignWithConversionStrategyTest() : BaseCopyCampaignTest() {
    @Autowired
    protected lateinit var metrikaClient: MetrikaClient

    @Autowired
    protected lateinit var metrikaCampaignRepository: MetrikaCampaignRepository

    @Autowired
    protected lateinit var testMetrikaCampaignRepository: TestMetrikaCampaignRepository

    @Autowired
    private lateinit var mobileGoalsPermissionService: MobileGoalsPermissionService

    @Autowired
    private lateinit var rbacService: RbacService

    private val rnd: Random = Random(0xFEDCBA987654321L)

    private val CAMPAIGN_STRATEGY_COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("strategyData.lastBidderRestartTime")
        .build()

    private val CAMP_METRIKA_GOAL_COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("statDate", "campaignId", "id.campaignId")
        .build()

    private val MEANINGFUL_GOAL_COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder().build()

    private val availableCounterBothClients1 = MetrikaCountersWithGoals(1000, listOf(101, 102, 103))
    private val availableCounterBothClients2 = MetrikaCountersWithGoals(1200, listOf(201, 202, 203, 224, 235))
    private val availableCounterSourceClient = MetrikaCountersWithGoals(1300, listOf(301, 302, 338))
    private val unAvailableCounter = MetrikaCountersWithGoals(4400, listOf(401, 433))

    private lateinit var goalIdToCounterIdMap: Map<Long, Int>

    protected val zeroGoal: List<Long> = listOf(BY_ALL_GOALS_GOAL_ID)
    protected val engagedGoal: List<Long> = listOf(ENGAGED_SESSION_GOAL_ID)
    protected val avGoal1C_1: List<Long> = listOf(102)
    protected val avGoal1C_2: List<Long> = listOf(224)
    protected val avGoal2C_1: List<Long> = listOf(101, 102, 103, 201, 202, 203, 224, 235)
    protected val avGoal2C_2: List<Long> = listOf(101, 203)
    protected val avGoal2C_Engaged: List<Long> = listOf(102, 224, ENGAGED_SESSION_GOAL_ID)
    protected val semiAvGoal1C_1: List<Long> = listOf(301)
    protected val semiAvGoal1C_2: List<Long> = listOf(301, 338)
    protected val semiAvGoal3C_1: List<Long> = listOf(101, 203, 302)
    protected val semiAvGoal3C_2: List<Long> = listOf(101, 102, 203, 235, 302, 338)
    protected val semiAvGoal3C_Engaged: List<Long> = listOf(101, 103, 201, 235, 302, 301, 338, ENGAGED_SESSION_GOAL_ID)

    // Специальный набор целей, чтобы проверить недоступность единственной ключевой цели
    // ENGAGED_SESSION_GOAL_ID в некоторых типах стратегий
    protected val unAvGoal_Engaged: List<Long> = listOf(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, ENGAGED_SESSION_GOAL_ID)
    protected val unAvGoal1C_1: List<Long> = listOf(401)
    protected val unAvGoal2C_1: List<Long> = listOf(101, 103, 433)
    protected val unAvGoal3C_1: List<Long> = listOf(101, 102, 103, 201, 202, 203, 224, 235, 301, 302, 338, 401, 433)

    // Актуальный список allMobileGoals и clientMobileGoals можно получить через printCurrentMobileGoals()
    // Все возможные цели
    protected val allMobileGoals: List<Long> = listOf(
        3, 4, 5, 6, 7,
        38402972, 38403008, 38403053, 38403071, 38403080, 38403095, 38403104,
        38403131, 38403173, 38403191, 38403197, 38403206, 38403215, 38403230,
        38403338, 38403494, 38403530, 38403545, 38403581
    )

    // Цели доступные для клиента с фичей FeatureName.RMP_STAT_CPA_ENABLED
    protected val rmpClientMobileGoals: List<Long> = listOf(
        4,
        38402972, 38403008, 38403053, 38403071, 38403080, 38403095, 38403104,
        38403131, 38403173, 38403191, 38403197, 38403206, 38403215, 38403230,
        38403338, 38403494, 38403530, 38403545, 38403581
    )

    protected val mobAvGoalS_1: List<Long> = listOf(rmpClientMobileGoals[0])
    protected val mobAvGoalS_2: List<Long> = listOf(rmpClientMobileGoals[10])
    protected val mobAvGoalM_1: List<Long> = rmpClientMobileGoals.subList(0, 3)
    protected val mobAvGoalM_2: List<Long> = rmpClientMobileGoals.subList(5, 9)
    protected val mobSemiAvGoalS_1: List<Long> = listOf(allMobileGoals[1])
    protected val mobSemiAvGoalS_2: List<Long> = listOf(allMobileGoals[4])
    protected val mobSemiAvGoalM_1: List<Long> = allMobileGoals.subList(2, 6)
    protected val mobSemiAvGoalM_2: List<Long> = allMobileGoals.subList(5, 9)
    protected val mobUnAvGoalS_1: List<Long> = listOf(433)
    protected val mobUnAvGoalM_1: List<Long> = listOf(202, 203, 401)
    protected val mobUnAvGoalL_1: List<Long> = listOf(101, 102, 103, 201, 202, 203, 224, 235, 301, 338, 401, 433)

    private fun addMetricaCounter(
        metrikaClient: MetrikaClientStub,
        uid: Long,
        counters: List<MetrikaCountersWithGoals>
    ) {
        metrikaClient.addUserCounterIds(uid, counters.map { it.counterId })
        counters.forEach {
            it.goalsIds.forEach { goalId ->
                metrikaClient.addCounterGoal(
                    it.counterId,
                    goalId.toInt()
                )
            }
        }
    }

    private fun getCountersIdsByGoalsIds(strategyGoalsId: List<Long>): List<Long> =
        strategyGoalsId.mapNotNull { goalIdToCounterIdMap[it] }.map { it.toLong() }.toSet().toList()

    protected fun printCurrentMobileGoals() {
        val clientForGetRmpMobileGoals = steps.clientSteps().createDefaultClient()
        steps.featureSteps()
            .addClientFeature(clientForGetRmpMobileGoals.clientId, FeatureName.RMP_STAT_CPA_ENABLED, true)

        val mobileGoalsPermissionsMap: Map<Goal, BiPredicate<RbacRole, ClientId>> =
            mobileGoalsPermissionService.getMobileGoalsWithPermissions()

        val allMobileGoals = mobileGoalsPermissionsMap.keys.map { it.id }.sorted()
        val operatorRole: RbacRole = rbacService.getUidRole(clientForGetRmpMobileGoals.uid)

        val clientMobileGoals = mobileGoalsPermissionsMap
            .filterValues { it.test(operatorRole, clientForGetRmpMobileGoals.clientId!!) }
            .keys
            .map { it.id }
            .sorted()

        println(
            "protected val allMobileGoals: List<Long> = " +
                "listOf(${allMobileGoals.joinToString { it.toString() }})"
        )
        println(
            "protected val clientMobileGoals: List<Long> = " +
                "listOf(${clientMobileGoals.joinToString { it.toString() }})"
        )
    }

    protected fun initCounters() {
        goalIdToCounterIdMap = listOf(
            availableCounterBothClients1,
            availableCounterBothClients2,
            availableCounterSourceClient,
            unAvailableCounter
        ).flatMap { it.goalsIds.map { id -> id to it.counterId } }.toMap()

        val invalidGoalsInTests: MutableSet<Long> =
            (avGoal1C_1 + avGoal1C_2 + avGoal2C_1 + avGoal2C_2 + avGoal2C_Engaged +
                semiAvGoal1C_1 + semiAvGoal1C_2 + semiAvGoal3C_1 + semiAvGoal3C_2 + semiAvGoal3C_Engaged +
                unAvGoal1C_1 + unAvGoal2C_1 + unAvGoal3C_1 + unAvGoal_Engaged +
                mobUnAvGoalS_1 + mobUnAvGoalM_1 + mobUnAvGoalL_1)
                .toMutableSet()

        invalidGoalsInTests.removeAll(
            goalIdToCounterIdMap.keys +
                zeroGoal + engagedGoal + MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID
        )

        if (invalidGoalsInTests.isNotEmpty()) {
            throw IllegalArgumentException("Invalid goals ids in test data: $invalidGoalsInTests")
        }

        val invalidMobileGoalsId =
            (mobSemiAvGoalS_1 + mobSemiAvGoalS_2 + mobSemiAvGoalM_1 + mobSemiAvGoalM_1).toMutableSet()

        invalidMobileGoalsId.removeAll(allMobileGoals)

        if (invalidMobileGoalsId.isNotEmpty()) {
            throw IllegalArgumentException("Invalid mobile goals ids in test data: $invalidMobileGoalsId")
        }

        val invalidRmpAvailableMobileGoalsId =
            (mobAvGoalS_1 + mobAvGoalS_2 + mobAvGoalM_1 + mobAvGoalM_2).toMutableSet()

        invalidRmpAvailableMobileGoalsId.removeAll(rmpClientMobileGoals)

        if (invalidRmpAvailableMobileGoalsId.isNotEmpty()) {
            throw IllegalArgumentException(
                "Invalid mobile rmp goals ids in test data: $invalidRmpAvailableMobileGoalsId"
            )
        }

        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        client = steps.clientSteps().createDefaultClient()
        targetClient = steps.clientSteps().createDefaultClient()

        steps.featureSteps().addClientFeature(client.clientId, FeatureName.RMP_STAT_CPA_ENABLED, true)
        steps.featureSteps().addClientFeature(targetClient.clientId, FeatureName.RMP_STAT_CPA_ENABLED, true)

        steps.featureSteps().addClientFeature(client.clientId, FeatureName.CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps().addClientFeature(targetClient.clientId, FeatureName.CRR_STRATEGY_ALLOWED, true)

        steps.featureSteps().addClientFeature(client.clientId, FeatureName.FIX_CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps().addClientFeature(targetClient.clientId, FeatureName.FIX_CRR_STRATEGY_ALLOWED, true)

        val metrikaClientStub: MetrikaClientStub = metrikaClient as MetrikaClientStub

        addMetricaCounter(
            metrikaClientStub,
            client.uid,
            listOf(availableCounterBothClients1, availableCounterBothClients2, availableCounterSourceClient)
        )
        addMetricaCounter(
            metrikaClientStub,
            targetClient.uid,
            listOf(availableCounterBothClients1, availableCounterBothClients2)
        )

        //printCurrentMobileGoals()
    }

    protected fun getRandomMobileGoal(): Long = allMobileGoals[rnd.nextInt(allMobileGoals.size)]
    protected fun getRandomRmpClientMobileGoal(): Long = rmpClientMobileGoals[rnd.nextInt(rmpClientMobileGoals.size)]

    protected fun unpackFromGoalsIds(strategyGoalsId: List<Long>): Triple<Long, List<MeaningfulGoal>, List<Long>> {
        val countersIdsByGoalsIds: List<Long> = getCountersIdsByGoalsIds(strategyGoalsId)
        return if (strategyGoalsId.size == 1) {
            Triple(strategyGoalsId[0], listOf(), countersIdsByGoalsIds)
        } else {
            Triple(
                MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID,
                strategyGoalsId
                    .filter { it != MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID }
                    .map {
                        MeaningfulGoal()
                            .withGoalId(it)
                            .withConversionValue(BigDecimal.TEN)
                    },
                countersIdsByGoalsIds
            )
        }
    }

    protected fun createAutobudgetStrategy(goalId: Long? = null): DbStrategy = DbStrategy()
        .withStrategyName(StrategyName.AUTOBUDGET)
        .withPlatform(CampaignsPlatform.BOTH)
        .withAutobudget(CampaignsAutobudget.YES)
        .withStrategyData(
            StrategyData()
                .withGoalId(goalId)
                .withName(CampaignsStrategyName.autobudget.literal)
                .withLastBidderRestartTime(LocalDateTime.now())
                .withSum(CurrencyRub.getInstance().defaultAutobudget)
                .withBid(BigDecimal.TEN)
        ) as DbStrategy

    protected fun createAutobudgetAvgCpaStrategy(goalId: Long? = null): DbStrategy = DbStrategy()
        .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA)
        .withPlatform(CampaignsPlatform.BOTH)
        .withAutobudget(CampaignsAutobudget.YES)
        .withStrategyData(
            StrategyData()
                .withGoalId(goalId)
                .withName(CampaignsStrategyName.autobudget_avg_cpa.literal)
                .withSum(CurrencyRub.getInstance().defaultAutobudget)
                .withBid(BigDecimal.valueOf(11))
                .withAvgCpa(CurrencyRub.getInstance().minAutobudgetAvgCpa.multiply(BigDecimal.TEN))
                .withLastBidderRestartTime(LocalDateTime.now())
        ) as DbStrategy

    protected fun createAutobudgetAvgCpaStrategyWithPayForConversion(goalId: Long? = null): DbStrategy {
        val strategy: DbStrategy = createAutobudgetAvgCpaStrategy(goalId)
        strategy.strategyData.payForConversion = true
        strategy.strategyData.bid = null
        return strategy
    }

    protected fun createAutobudgetAvgCpiStrategy(goalId: Long? = null): DbStrategy = DbStrategy()
        .withStrategyName(StrategyName.AUTOBUDGET_AVG_CPI)
        .withAutobudget(CampaignsAutobudget.YES)
        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
        .withPlatform(CampaignsPlatform.BOTH)
        .withStrategyData(
            StrategyData()
                .withGoalId(goalId)
                .withName(CampaignsStrategyName.autobudget_avg_cpi.getLiteral())
                .withSum(CurrencyRub.getInstance().defaultAutobudget)
                .withBid(BigDecimal.valueOf(12))
                .withAvgCpi(CurrencyRub.getInstance().minAutobudgetAvgCpa.multiply(BigDecimal.TEN))
        ) as DbStrategy

    protected fun createAutobudgetAvgCpiStrategyWithPayForConversion(goalId: Long? = null): DbStrategy {
        val strategy: DbStrategy = createAutobudgetAvgCpiStrategy(goalId)
        strategy.strategyData.payForConversion = true
        strategy.strategyData.bid = null
        return strategy
    }

    protected fun createAutobudgetCrrStrategy(goalId: Long? = null): DbStrategy = DbStrategy()
        .withStrategyName(StrategyName.AUTOBUDGET_CRR)
        .withPlatform(CampaignsPlatform.BOTH)
        .withAutobudget(CampaignsAutobudget.YES)
        .withStrategyData(
            StrategyData()
                .withName(CampaignsStrategyName.autobudget_crr.literal)
                .withGoalId(goalId)
                .withLastBidderRestartTime(LocalDateTime.now())
                .withCrr(13)
                .withPayForConversion(false)
                .withSum(CurrencyRub.getInstance().minAutobudget.multiply(BigDecimal.TEN))
        ) as DbStrategy

    protected fun createAutobudgetCrrStrategyWithPayForConversion(goalId: Long? = null): DbStrategy {
        val strategy: DbStrategy = createAutobudgetCrrStrategy(goalId)
        strategy.strategyData.payForConversion = true
        strategy.strategyData.bid = null
        return strategy
    }

    protected fun createAutobudgetRoiStrategy(goalId: Long? = null): DbStrategy = DbStrategy()
        .withStrategyName(StrategyName.AUTOBUDGET_ROI)
        .withPlatform(CampaignsPlatform.BOTH)
        .withAutobudget(CampaignsAutobudget.YES)
        .withStrategyData(
            StrategyData()
                .withName(CampaignsStrategyName.autobudget_roi.literal)
                .withGoalId(goalId)
                .withSum(CurrencyRub.getInstance().minAutobudget.multiply(BigDecimal.TEN))
                .withProfitability(BigDecimal.TEN)
                .withRoiCoef(BigDecimal.valueOf(15.75))
                .withReserveReturn(20L)
                .withBid(BigDecimal.valueOf(14))
        ) as DbStrategy

    protected fun createTextCampaignCopySameClientAndCheck(
        strategy: DbStrategy,
        countersIds: List<Long>,
        meaningfulGoals: List<MeaningfulGoal>,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val originalCampaignInfo: TextCampaignInfo = createTextCampaign(strategy, countersIds, meaningfulGoals)

        copySingleCampaignWithMeaningfulGoalsSameClientAndCheck(
            originalCampaignInfo,
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    protected fun createTextCampaignCopyBetweenClientsAndCheck(
        strategy: DbStrategy,
        countersIds: List<Long>,
        meaningfulGoals: List<MeaningfulGoal>,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val originalCampaignInfo: TextCampaignInfo = createTextCampaign(strategy, countersIds, meaningfulGoals)

        copySingleCampaignWithMeaningfulGoalsBetweenClientsAndCheck(
            originalCampaignInfo,
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    protected fun createTextCampaign(
        strategy: DbStrategy,
        metrikaCountersIds: List<Long>,
        meaningfulGoals: List<MeaningfulGoal>
    ): TextCampaignInfo = steps.textCampaignSteps().createCampaign(
        client,
        TestTextCampaigns
            .fullTextCampaign()
            .withStrategy(strategy)
            .withMetrikaCounters(metrikaCountersIds)
            .withMeaningfulGoals(meaningfulGoals)
    )

    private fun copySingleCampaignWithMeaningfulGoalsSameClientAndCheck(
        originalCampaignInfo: CampaignInfo<out CampaignWithMeaningfulGoalsWithRequiredFields>,
        originalStrategy: DbStrategy,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean,
    ) {
        val originalCampMetrikaGoals: List<CampMetrikaGoal> =
            saveStatisticsGoals(client, mapOf(originalCampaignInfo.campaignId to statisticsGoalsIds))

        val operation: CopyOperation<BaseCampaign, Long> = sameClientCampaignCopyOperation(
            originalCampaignInfo,
            flags = CopyCampaignFlags(
                isCopyConversionStrategies = isCopyConversionStrategies,
                isDoNotCheckRightsToMetrikaGoals = isDoNotCheckRightsToMetrikaGoals
            )
        )

        copySingleCampaignWithMeaningfulGoalsAndCheck(
            client,
            originalCampaignInfo.campaignId,
            originalStrategy,
            originalCampMetrikaGoals,
            operation,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    private fun copySingleCampaignWithMeaningfulGoalsBetweenClientsAndCheck(
        originalCampaignInfo: CampaignInfo<out CampaignWithMeaningfulGoalsWithRequiredFields>,
        originalStrategy: DbStrategy,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean,
    ) {
        val originalCampMetrikaGoals: List<CampMetrikaGoal> =
            saveStatisticsGoals(client, mapOf(originalCampaignInfo.campaignId to statisticsGoalsIds))

        val operation: CopyOperation<BaseCampaign, Long> = betweenClientsCampaignCopyOperation(
            originalCampaignInfo, targetClient,
            flags = CopyCampaignFlags(
                isCopyConversionStrategies = isCopyConversionStrategies,
                isDoNotCheckRightsToMetrikaGoals = isDoNotCheckRightsToMetrikaGoals
            )
        )

        copySingleCampaignWithMeaningfulGoalsAndCheck(
            targetClient,
            originalCampaignInfo.campaignId,
            originalStrategy,
            originalCampMetrikaGoals,
            operation,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    private fun copySingleCampaignWithMeaningfulGoalsAndCheck(
        targetClient: ClientInfo,
        originalCampaignId: Long,
        originalStrategy: DbStrategy,
        originalCampMetrikaGoals: List<CampMetrikaGoal>,
        operation: CopyOperation<BaseCampaign, Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean,
    ) {
        val campaignId = copyValidCampaigns(operation)[0]

        val originalCampaign: CampaignWithMeaningfulGoalsWithRequiredFields =
            getCampaign(originalCampaignId)

        val copiedCampaign: CampaignWithMeaningfulGoalsWithRequiredFields = getCampaign(campaignId, targetClient.shard)
        val copiedCampMetrikaGoals: List<CampMetrikaGoal> =
            testMetrikaCampaignRepository.getCampMetrikaGoalsByCampaignIds(
                targetClient.shard,
                targetClient.clientId,
                listOf(campaignId)
            )

        softly {
            checkCopiedCampaignWithMeaningfulGoals(
                this,
                originalStrategy,
                originalCampaign,
                originalCampMetrikaGoals,
                copiedCampaign,
                copiedCampMetrikaGoals,
                shouldDropStrategyToDefault,
                shouldDropGoalsStat
            )
        }
    }

    protected fun checkCopiedCampaignWithMeaningfulGoals(
        softAssertions: SoftAssertions,
        originalStrategy: DbStrategy,
        originalCampaign: CampaignWithMeaningfulGoalsWithRequiredFields,
        originalCampMetrikaGoals: List<CampMetrikaGoal>,
        copiedCampaign: CampaignWithMeaningfulGoalsWithRequiredFields,
        copiedCampMetrikaGoals: List<CampMetrikaGoal>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        lateinit var expectedStrategy: DbStrategy
        lateinit var expectedMeaningfulGoals: List<MeaningfulGoal>

        if (shouldDropStrategyToDefault) {
            expectedStrategy = createAutobudgetStrategy()
            expectedStrategy.strategyData.bid = originalStrategy.strategyData.bid
            expectedStrategy.strategyData.sum = originalStrategy.strategyData.sum
            expectedMeaningfulGoals = listOf()
        } else {
            expectedStrategy = originalStrategy
            expectedMeaningfulGoals = CommonUtils.nvl(originalCampaign.meaningfulGoals, listOf())
        }

        val expectedCampMetrikaGoals: List<CampMetrikaGoal> = if (shouldDropGoalsStat) {
            listOf()
        } else {
            originalCampMetrikaGoals
        }

        val actualMeaningfulGoals: List<MeaningfulGoal> = CommonUtils.nvl(copiedCampaign.meaningfulGoals, listOf())
        val actualCampMetricaGoals: List<CampMetrikaGoal> = CommonUtils.nvl(copiedCampMetrikaGoals, listOf())

        softAssertions.assertThat(copiedCampaign.strategy)
            .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .usingRecursiveComparison(CAMPAIGN_STRATEGY_COMPARE_STRATEGY)
            .isEqualTo(expectedStrategy)

        assertListAreEqualsIgnoringOrder(
            softAssertions,
            actualMeaningfulGoals,
            expectedMeaningfulGoals,
            MEANINGFUL_GOAL_COMPARE_STRATEGY
        )

        assertListAreEqualsIgnoringOrder(
            softAssertions,
            actualCampMetricaGoals,
            expectedCampMetrikaGoals,
            CAMP_METRIKA_GOAL_COMPARE_STRATEGY
        )
    }

    protected fun createMobileContentCampaignCopySameClientAndCheck(
        strategy: DbStrategy,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val originalCampaignInfo: MobileContentCampaignInfo = createMobileContentCampaign(strategy)

        copySingleMobileContentCampaignSameClientAndCheck(
            originalCampaignInfo,
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    protected fun createMobileContentCampaignCopyBetweenClientsAndCheck(
        strategy: DbStrategy,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        val originalCampaignInfo: MobileContentCampaignInfo = createMobileContentCampaign(strategy)

        copySingleMobileContentCampaignBetweenClientsAndCheck(
            originalCampaignInfo,
            strategy,
            statisticsGoalsIds,
            isCopyConversionStrategies,
            isDoNotCheckRightsToMetrikaGoals,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    protected fun createMobileContentCampaign(strategy: DbStrategy): MobileContentCampaignInfo {
        val mobileApp = steps.mobileAppSteps().createDefaultMobileApp(client)
        val campaign =
            steps.mobileContentCampaignSteps()
                .createCampaign(
                    client,
                    TestMobileContentCampaigns
                        .fullMobileContentCampaign(mobileApp.mobileAppId)
                        .withStrategy(strategy)
                )
        return campaign
    }

    private fun copySingleMobileContentCampaignSameClientAndCheck(
        originalCampaignInfo: CampaignInfo<out MobileContentCampaign>,
        originalStrategy: DbStrategy,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean,
    ) {
        val originalCampMetrikaGoals: List<CampMetrikaGoal> =
            saveStatisticsGoals(client, mapOf(originalCampaignInfo.campaignId to statisticsGoalsIds))

        val operation: CopyOperation<BaseCampaign, Long> = sameClientCampaignCopyOperation(
            originalCampaignInfo,
            flags = CopyCampaignFlags(
                isCopyConversionStrategies = isCopyConversionStrategies,
                isDoNotCheckRightsToMetrikaGoals = isDoNotCheckRightsToMetrikaGoals
            )
        )

        copySingleMobileContentCampaignAndCheck(
            client,
            originalStrategy,
            originalCampMetrikaGoals,
            operation,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    private fun copySingleMobileContentCampaignBetweenClientsAndCheck(
        originalCampaignInfo: CampaignInfo<out MobileContentCampaign>,
        originalStrategy: DbStrategy,
        statisticsGoalsIds: List<Long>,
        isCopyConversionStrategies: Boolean,
        isDoNotCheckRightsToMetrikaGoals: Boolean,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean,
    ) {
        val originalCampMetrikaGoals: List<CampMetrikaGoal> =
            saveStatisticsGoals(client, mapOf(originalCampaignInfo.campaignId to statisticsGoalsIds))

        val operation: CopyOperation<BaseCampaign, Long> = betweenClientsCampaignCopyOperation(
            originalCampaignInfo, targetClient,
            flags = CopyCampaignFlags(
                isCopyConversionStrategies = isCopyConversionStrategies,
                isDoNotCheckRightsToMetrikaGoals = isDoNotCheckRightsToMetrikaGoals
            )
        )

        copySingleMobileContentCampaignAndCheck(
            targetClient,
            originalStrategy,
            originalCampMetrikaGoals,
            operation,
            shouldDropStrategyToDefault,
            shouldDropGoalsStat
        )
    }

    private fun copySingleMobileContentCampaignAndCheck(
        targetClient: ClientInfo,
        originalStrategy: DbStrategy,
        originalCampMetrikaGoals: List<CampMetrikaGoal>,
        operation: CopyOperation<BaseCampaign, Long>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean,
    ) {
        val campaignId = copyValidCampaigns(operation)[0]
        val copiedCampaign: MobileContentCampaign = getCampaign(campaignId, targetClient.shard)
        val copiedCampMetrikaGoals: List<CampMetrikaGoal> =
            testMetrikaCampaignRepository.getCampMetrikaGoalsByCampaignIds(
                targetClient.shard,
                targetClient.clientId,
                listOf(campaignId)
            )

        softly {
            checkCopiedCampaignWithMobileContent(
                this,
                originalStrategy,
                originalCampMetrikaGoals,
                copiedCampaign,
                copiedCampMetrikaGoals,
                shouldDropStrategyToDefault,
                shouldDropGoalsStat
            )
        }
    }

    protected fun checkCopiedCampaignWithMobileContent(
        softAssertions: SoftAssertions,
        originalStrategy: DbStrategy,
        originalCampMetrikaGoals: List<CampMetrikaGoal>,
        copiedCampaign: MobileContentCampaign,
        copiedCampMetrikaGoals: List<CampMetrikaGoal>,
        shouldDropStrategyToDefault: Boolean,
        shouldDropGoalsStat: Boolean
    ) {
        lateinit var expectedStrategy: DbStrategy

        if (shouldDropStrategyToDefault) {
            expectedStrategy = createAutobudgetStrategy()
            expectedStrategy.strategyData.bid = originalStrategy.strategyData.bid
            expectedStrategy.strategyData.sum = originalStrategy.strategyData.sum
        } else {
            expectedStrategy = originalStrategy
            // Для AUTOBUDGET_AVG_CPI стратегий дефолтная цель (4) сбрасывается в null
            if (expectedStrategy.strategyName == StrategyName.AUTOBUDGET_AVG_CPI
                && expectedStrategy.strategyData.goalId == DEFAULT_CPI_GOAL_ID
            ) {
                expectedStrategy.strategyData.goalId = null
            }
        }

        val expectedCampMetrikaGoals: List<CampMetrikaGoal> = if (shouldDropGoalsStat) {
            listOf()
        } else {
            originalCampMetrikaGoals
        }

        val actualCampMetricaGoals: List<CampMetrikaGoal> = CommonUtils.nvl(copiedCampMetrikaGoals, listOf())

        softAssertions.assertThat(copiedCampaign.strategy)
            .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .usingRecursiveComparison(CAMPAIGN_STRATEGY_COMPARE_STRATEGY)
            .isEqualTo(expectedStrategy)

        assertListAreEqualsIgnoringOrder(
            softAssertions,
            actualCampMetricaGoals,
            expectedCampMetrikaGoals,
            CAMP_METRIKA_GOAL_COMPARE_STRATEGY
        )
    }

    protected fun saveStatisticsGoals(
        client: ClientInfo,
        statisticGoalsByCampaignIdMap: Map<Long, List<Long>>
    ): List<CampMetrikaGoal> {
        val campMetrikaGoals: List<CampMetrikaGoal> = statisticGoalsByCampaignIdMap.flatMap {
            it.value.map { goalId ->
                CampMetrikaGoal()
                    .withCampaignId(it.key)
                    .withGoalId(goalId)
                    .withId(
                        CampMetrikaGoalId()
                            .withCampaignId(
                                it.key
                            ).withGoalId(goalId)
                    )
                    .withGoalsCount(1L + rnd.nextInt(100))
                    .withContextGoalsCount(1L + rnd.nextInt(100))
                    .withLinksCount(1L + rnd.nextInt(100))
                    .withGoalRole(setOf(GoalRole.SINGLE))
            }
        }
        metrikaCampaignRepository.addCampMetrikaGoals(client.shard, campMetrikaGoals)
        return campMetrikaGoals
    }

    protected data class MetrikaCountersWithGoals(
        val counterId: Int,
        val goalsIds: List<Long>
    )
}
