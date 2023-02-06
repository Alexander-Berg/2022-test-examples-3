package ru.yandex.direct.oneshot.oneshots.package_strategy_migration

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.entity.strategy.service.converter.CampaignToStrategyConverterService
import ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetCrrStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgClick
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpa
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpaPerFilter
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpcPerCamp
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpcPerFilter
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpiStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpv
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetAvgCpvCustomPeriod
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxImpressions
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxImpressionsCustomPeriod
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxReach
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMedia
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetRoi
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetWeekBundle
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetWeekSum
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultCpmDefault
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultDefaultManualStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultPeriodFixBid
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.package_strategy_migration.CampaignToPackageStrategyMigrationOneShot.Companion.PackageStrategyMigrationRow
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtOperator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.function.Consumer

@OneshotTest
@RunWith(JUnitParamsRunner::class)
class CampaignToPackageStrategyMigrationOneShotTest {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    private val compareStrategy = DefaultCompareStrategies
        .allFieldsExcept(
            BeanFieldPath.newPath("metrikaCounters"),
            BeanFieldPath.newPath("lastChange", ".*")
        )

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    lateinit var strategyTypedRepository: StrategyTypedRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    lateinit var converterService: CampaignToStrategyConverterService

    @Autowired
    lateinit var strategyMigrationService: StrategyMigrationService

    @Autowired
    lateinit var walletService: WalletService

    lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var campaignSteps: CampaignSteps

    lateinit var clientInfo: ClientInfo

    lateinit var operator: YtOperator

    var walletId: Long = 0L

    private val meaningfulGoals = listOf(MeaningfulGoal()
        .withGoalId(RandomNumberUtils.nextPositiveLong())
        .withConversionValue(BigDecimal.TEN))

    lateinit var oneshot: CampaignToPackageStrategyMigrationOneShot

    fun inputData(saveWithValidation: Boolean = false) =
        CampaignToPackageStrategyMigrationOneShot.Companion.InputData(
            YtCluster.HAHN.name,
            "/test",
            clientInfo.uid,
            saveWithValidation
        )

    fun parametrizedTestData() = listOf(
        listOf("AutobudgetCrr", autobudgetCrrStrategy(BigDecimal.valueOf(3000L), 10L, 1L), meaningfulGoals, false),
        listOf("AutobudgetAvgClick", defaultAutobudgetAvgClick(), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetAvgCpa", defaultAutobudgetAvgCpa(LocalDateTime.now()), meaningfulGoals, false),
        listOf("AutobudgetAvgCpaPerCamp", defaultAutobudgetAvgCpaPerCamp(LocalDateTime.now()), meaningfulGoals, false),
        listOf("AutobudgetAvgCpaPerFilter", defaultAutobudgetAvgCpaPerFilter(LocalDateTime.now()), meaningfulGoals, false),
        listOf("AutobudgetAvgCpcPerCamp", defaultAutobudgetAvgCpcPerCamp(), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetAvgCpcPerFilter", defaultAutobudgetAvgCpcPerFilter(), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetAvgCpiStrategy", defaultAutobudgetAvgCpiStrategy(1L, LocalDateTime.now()), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetAvgCpv", defaultAutobudgetAvgCpv(), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetAvgCpvCustomPeriod", defaultAutobudgetAvgCpvCustomPeriod(LocalDateTime.now().plusDays(1)), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetMaxImpressions", defaultAutobudgetMaxImpressions(), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetMaxImpressionsCustomPeriod", defaultAutobudgetMaxImpressionsCustomPeriod(LocalDateTime.now().plusDays(1)), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetMaxReach", defaultAutobudgetMaxReach(), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetMaxReachCustomPeriod", defaultAutobudgetMaxReachCustomPeriod(LocalDateTime.now().plusDays(1)), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetMedia", defaultAutobudgetMedia(LocalDateTime.now()), emptyList<MeaningfulGoal>(), false),
        listOf("AutobudgetRoi", defaultAutobudgetRoi(), meaningfulGoals, false),
        listOf("AutobudgetWeekBundle", defaultAutobudgetWeekBundle(), meaningfulGoals, false),
        listOf("AutobudgetWeekSum", defaultAutobudgetWeekSum(LocalDateTime.now()), emptyList<MeaningfulGoal>(), false),
        listOf("CpmDefault", defaultCpmDefault(LocalDateTime.now()), emptyList<MeaningfulGoal>(), false),
        listOf("DefaultManualStrategy", defaultDefaultManualStrategy(LocalDateTime.now()), emptyList<MeaningfulGoal>(), false),
        listOf("PeriodFixBid", defaultPeriodFixBid(LocalDateTime.now()), emptyList<MeaningfulGoal>(), false),

        listOf("AutobudgetCrr with validation", autobudgetCrrStrategy(BigDecimal.valueOf(3000L), 10L, 1L), meaningfulGoals, true),
        listOf("AutobudgetAvgClick with validation", defaultAutobudgetAvgClick(), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetAvgCpa with validation", defaultAutobudgetAvgCpa(LocalDateTime.now()), meaningfulGoals, true),
        listOf("AutobudgetAvgCpaPerCamp with validation", defaultAutobudgetAvgCpaPerCamp(LocalDateTime.now()), meaningfulGoals, true),
        listOf("AutobudgetAvgCpaPerFilter with validation", defaultAutobudgetAvgCpaPerFilter(LocalDateTime.now()), meaningfulGoals, true),
        listOf("AutobudgetAvgCpcPerCamp with validation", defaultAutobudgetAvgCpcPerCamp(), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetAvgCpcPerFilter with validation", defaultAutobudgetAvgCpcPerFilter(), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetAvgCpiStrategy with validation", defaultAutobudgetAvgCpiStrategy(1L, LocalDateTime.now()), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetAvgCpv with validation", defaultAutobudgetAvgCpv(), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetAvgCpvCustomPeriod with validation", defaultAutobudgetAvgCpvCustomPeriod(LocalDateTime.now().plusDays(1)), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetMaxImpressions with validation", defaultAutobudgetMaxImpressions(), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetMaxImpressionsCustomPeriod with validation", defaultAutobudgetMaxImpressionsCustomPeriod(LocalDateTime.now().plusDays(1)), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetMaxReach with validation", defaultAutobudgetMaxReach(), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetMaxReachCustomPeriod with validation", defaultAutobudgetMaxReachCustomPeriod(LocalDateTime.now().plusDays(1)), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetMedia with validation", defaultAutobudgetMedia(LocalDateTime.now()), emptyList<MeaningfulGoal>(), true),
        listOf("AutobudgetRoi with validation", defaultAutobudgetRoi(), meaningfulGoals, true),
        listOf("AutobudgetWeekBundle with validation", defaultAutobudgetWeekBundle(), meaningfulGoals, true),
        listOf("AutobudgetWeekSum with validation", defaultAutobudgetWeekSum(LocalDateTime.now()), emptyList<MeaningfulGoal>(), true),
        listOf("CpmDefault with validation", defaultCpmDefault(LocalDateTime.now()), emptyList<MeaningfulGoal>(), true),
        listOf("DefaultManualStrategy with validation", defaultDefaultManualStrategy(LocalDateTime.now()), emptyList<MeaningfulGoal>(), true),
        listOf("PeriodFixBid with validation", defaultPeriodFixBid(LocalDateTime.now()), emptyList<MeaningfulGoal>(), true)
    )

    @Before
    fun before() {
        val provider = mock<YtProvider>()
        ppcPropertiesSupport = mock()
        val relaxTime = mock<PpcProperty<Long>>()
        val chunkSize = mock<PpcProperty<Long>>()
        whenever(relaxTime.getOrDefault(any())).thenReturn(0L)
        whenever(chunkSize.getOrDefault(any())).thenReturn(1000L)
        operator = mock<YtOperator>()
        whenever(provider.getOperator(any()))
            .thenReturn(operator)
        whenever(ppcPropertiesSupport.get(eq(PpcPropertyNames.CAMPAIGN_TO_PACKAGE_STRATEGY_MIGRATION_CHUNK_SIZE)))
            .thenReturn(chunkSize)
        whenever(ppcPropertiesSupport.get(eq(PpcPropertyNames.CAMPAIGN_TO_PACKAGE_STRATEGY_MIGRATION_RELAX_TIME)))
            .thenReturn(relaxTime)

        oneshot = CampaignToPackageStrategyMigrationOneShot(
            provider,
            campaignTypedRepository,
            dslContextProvider,
            strategyMigrationService,
            ppcPropertiesSupport
        )

        clientInfo = steps.clientSteps().createDefaultClient()
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
    }

    @Test
    fun `do nothing on empty set`() {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(0)

        val result = oneshot.execute(inputData(), null, clientInfo.shard)

        assertThat(result, `is`(nullValue()))
        verify(operator, times(0)).readTableByKeyRange(any(), any(), any(), any<Long>(), any())
    }

    @Test
    fun `do nothing on campaigns from another shard`() {
        val strategy = defaultAutobudgetAvgClick()
        val campaign = createCampaign(
            strategy,
            emptyList()
        )
        mock(listOf(campaign))

        val result = oneshot.execute(inputData(), null, ClientSteps.ANOTHER_SHARD)

        assertThat(result, `is`(notNullValue()))
        val strategyId = converterService.toStrategyWithIdEqualToOrderId(
            clientInfo.clientId,
            campaign.lastChange,
            campaign
        ).id

        val actualStrategy = strategyTypedRepository.getTyped(clientInfo.shard, listOf(strategyId))
        assertThat(actualStrategy, `is`(empty()))
    }


    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun `successfully process campaign`(
        description: String,
        strategy: DbStrategy,
        meaningfulGoals: List<MeaningfulGoal>,
        saveWithValidation: Boolean
    ) {
        val campaign = createCampaign(
            strategy,
            meaningfulGoals
        )
        mock(listOf(campaign))

        oneshot.execute(inputData(saveWithValidation), null, clientInfo.shard)
        val expectedStrategy = converterService.toStrategyWithIdEqualToOrderId(
            clientInfo.clientId,
            campaign.lastChange,
            campaign
        )
        val actualStrategy = strategyTypedRepository.getTyped(
            clientInfo.shard,
            listOf(expectedStrategy.id),
        ).firstOrNull()

        assertThat(actualStrategy, beanDiffer(expectedStrategy).useCompareStrategy(compareStrategy))
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun `successfully process already saved strategies`(
        description: String,
        strategy: DbStrategy,
        meaningfulGoals: List<MeaningfulGoal>,
        saveWithValidation: Boolean
    ) {
        val campaign = createCampaign(
            strategy,
            meaningfulGoals
        )
        mock(listOf(campaign))

        oneshot.execute(inputData(saveWithValidation), null, clientInfo.shard)

        val expectedStrategy = converterService.toStrategyWithIdEqualToOrderId(
            clientInfo.clientId,
            campaign.lastChange,
            campaign
        )
        val actualStrategy = strategyTypedRepository.getTyped(
            clientInfo.shard,
            listOf(expectedStrategy.id),
        ).firstOrNull()

        assertThat(actualStrategy, beanDiffer(expectedStrategy).useCompareStrategy(compareStrategy))

        //Пытаемся перезаписать стратегии по заново
        oneshot.execute(inputData(saveWithValidation), null, clientInfo.shard)

        val actualStrategyAfterRewrite = strategyTypedRepository.getTyped(
            clientInfo.shard,
            listOf(expectedStrategy.id),
        ).firstOrNull()

        assertThat(actualStrategyAfterRewrite, beanDiffer(expectedStrategy).useCompareStrategy(compareStrategy))

    }

    private fun mock(campaigns: List<CampaignWithPackageStrategy>) {
        whenever(operator.readTableRowCount(any()))
            .thenReturn(campaigns.size.toLong())

        whenever(operator.readTableByRowRange(any(), any(), any(), any<Long>(), any()))
            .thenAnswer { invocation ->
                campaigns.forEach {
                    val row = mock<PackageStrategyMigrationRow>()
                    whenever(row.cid).thenReturn(it.id)
                    invocation.getArgument<Consumer<PackageStrategyMigrationRow>>(1).accept(row)
                }
            }
    }

    private fun createCampaign(strategy: DbStrategy,
                               meaningfulGoals: List<MeaningfulGoal>): CampaignWithPackageStrategy {
        val textCampaign = textCampaign(meaningfulGoals, strategy)
        campaignSteps.createActiveTextCampaign(clientInfo, textCampaign)
        return campaignTypedRepository.getStrictlyFullyFilled(
            clientInfo.shard,
            listOf(textCampaign.id),
            CampaignWithPackageStrategy::class.java
        ).first()
    }

    private fun textCampaign(meaningfulGoals: List<MeaningfulGoal>,
                             strategy: DbStrategy): TextCampaign =
        TestTextCampaigns.fullTextCampaign()
            .withStatusArchived(false)
            .withWalletId(walletId)
            .withMeaningfulGoals(meaningfulGoals)
            .withClientId(clientInfo.client?.clientId)
            .withUid(clientInfo.uid)
            .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
            .withOrderId(RandomNumberUtils.nextPositiveInteger().toLong())
            .withEndDate(LocalDate.now().plusDays(3))
            .withStrategy(strategy)
}
