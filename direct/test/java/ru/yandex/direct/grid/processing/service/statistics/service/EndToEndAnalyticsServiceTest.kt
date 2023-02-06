package ru.yandex.direct.grid.processing.service.statistics.service

import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import ru.yandex.common.util.collections.Cu
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.model.campaign.GdCampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE
import ru.yandex.direct.grid.processing.model.client.GdClient
import ru.yandex.direct.grid.processing.model.client.GdClientInfo
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsPeriod
import ru.yandex.direct.grid.processing.model.statistics.metrika.GdMetrikaStatisticsContainer
import ru.yandex.direct.grid.processing.model.statistics.metrika.GdMetrikaStatisticsFilter
import ru.yandex.direct.grid.processing.service.statistics.service.EndToEndAnalyticsService.Companion.OTHER_CATEGORY
import ru.yandex.direct.grid.processing.service.statistics.service.EndToEndAnalyticsService.Companion.convertToGdStatsItem
import ru.yandex.direct.grid.processing.service.statistics.validation.EndToEndAnalyticsValidationService
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.metrika.client.MetrikaClient
import ru.yandex.direct.metrika.client.internal.Dimension
import ru.yandex.direct.metrika.client.internal.MetrikaByTimeStatisticsParams
import ru.yandex.direct.metrika.client.model.response.Counter
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.metrika.client.model.response.sources.SourcesResponse
import ru.yandex.direct.metrika.client.model.response.sources.TrafficSource
import ru.yandex.direct.metrika.client.model.response.statistics.StatisticsResponse
import ru.yandex.direct.metrika.client.model.response.statistics.StatisticsResponseRow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

private const val COUNTER_ID = 1L
private const val GOAL_ID = 1L

class EndToEndAnalyticsServiceTest {
    lateinit var service: EndToEndAnalyticsService

    lateinit var metrikaClient: MetrikaClient

    private var metrikaParamsCaptor = ArgumentCaptor.forClass(MetrikaByTimeStatisticsParams::class.java)

    @Before
    fun setUp() {
        metrikaClient = mock(MetrikaClient::class.java)
        val endToEndAnalyticsValidationService = mock(EndToEndAnalyticsValidationService::class.java)
        whenever(metrikaClient.getCounter(COUNTER_ID))
            .thenReturn(
                Counter()
                    .withId(COUNTER_ID.toInt())
                    .withDomain("site.ru")
                    .withFeatures(setOf(EXPENSES_FEATURE_NAME))
            )
        val defaultGoal = CounterGoal()
            .withId(GOAL_ID.toInt())
        whenever(metrikaClient.getMassCountersGoalsFromMetrika(any()))
            .thenReturn(
                mapOf(COUNTER_ID.toInt() to listOf(defaultGoal))
            )
        whenever(
            metrikaClient.getEndToEndStatistics(any())
        )
            .thenReturn(
                StatisticsResponse("RUB", defaultEndToEndResponseRows())
            )
        whenever(
            metrikaClient.getAvailableSources(any())
        )
            .thenReturn(
                SourcesResponse(defaultAvailableSources())
            )
        whenever(
            metrikaClient.getTrafficSourceStatistics(any())
        )
            .thenReturn(
                StatisticsResponse("BYN", defaultTrafficSourceResponseRows() + defaultTotalTrafficResponseRow())
            )
        service = EndToEndAnalyticsService(metrikaClient, endToEndAnalyticsValidationService)
    }

    private val clientId = ClientId.fromLong(2L)

    @Test
    fun getStatistics_useWorkCurrency() {
        service.getEndToEndStatistics(
            defaultGdMetrikaStatisticsContainer(),
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            defaultClientWithRubCurrency()
        )

        verify(metrikaClient).getCounter(eq(COUNTER_ID))
        verify(metrikaClient).getEndToEndStatistics(metrikaParamsCaptor.capture())
        verifyNoMoreInteractions(metrikaClient)

        val capturedParams = metrikaParamsCaptor.value
        SoftAssertions.assertSoftly {
            it.assertThat(capturedParams.currencyCode)
                .describedAs("params.currencyCode")
                .isEqualTo(CurrencyCode.RUB)
            it.assertThat(capturedParams.skipGoalData)
                .describedAs("params.skipGoalData")
                .isFalse()
        }
    }

    @Test
    fun getStatistics_whenCurrencyYndFixed() {
        service.getEndToEndStatistics(
            defaultGdMetrikaStatisticsContainer(),
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            clientWithCurrency(CurrencyCode.YND_FIXED)
        )

        verify(metrikaClient).getCounter(eq(COUNTER_ID))
        verify(metrikaClient).getEndToEndStatistics(metrikaParamsCaptor.capture())
        verifyNoMoreInteractions(metrikaClient)

        val capturedParams = metrikaParamsCaptor.value
        Assertions.assertThat(capturedParams.currencyCode)
            .describedAs("params.currencyCode")
            .isNull()
    }

    @Test
    fun getStatistics_doNotGetAbsentGoalStatistics() {
        val absentGoalId = 12345L
        val container = GdMetrikaStatisticsContainer().withAttributionModel(LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE)
            .withFilter(
                GdMetrikaStatisticsFilter()
                    .withCounterId(COUNTER_ID)
                    .withGoalId(absentGoalId)
                    .withPeriod(
                        GdCampaignStatisticsPeriod()
                            .withFrom(LocalDate.of(2021, Month.DECEMBER, 25))
                            .withTo(LocalDate.of(2021, Month.DECEMBER, 25))
                    )
            )
        service.getEndToEndStatistics(
            container,
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            defaultClientWithRubCurrency()
        )

        verify(metrikaClient).getCounter(eq(COUNTER_ID))
        verify(metrikaClient).getMassCountersGoalsFromMetrika(eq(setOf(COUNTER_ID.toInt())))
        verify(metrikaClient).getEndToEndStatistics(metrikaParamsCaptor.capture())
        verifyNoMoreInteractions(metrikaClient)

        val capturedParams = metrikaParamsCaptor.value
        SoftAssertions.assertSoftly {
            it.assertThat(capturedParams.goalId)
                .describedAs("params.goalId")
                .isNull()
            it.assertThat(capturedParams.skipGoalData)
                .describedAs("params.skipGoalData")
                .isTrue()
        }
    }

    @Test
    fun getStatistics_doNotGetHiddenGoalStatistics() {
        val hiddenGoalId = 31415L
        val hiddenGoal = CounterGoal()
            .withId(hiddenGoalId.toInt())
            .withType(CounterGoal.Type.E_CART)
            .withStatus(CounterGoal.Status.HIDDEN)
        whenever(metrikaClient.getMassCountersGoalsFromMetrika(any()))
            .thenReturn(
                mapOf(COUNTER_ID.toInt() to listOf(hiddenGoal))
            )

        val container = inputContainerWithGoalId(hiddenGoalId)

        service.getEndToEndStatistics(
            container,
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            defaultClientWithRubCurrency()
        )

        verify(metrikaClient).getCounter(eq(COUNTER_ID))
        verify(metrikaClient).getMassCountersGoalsFromMetrika(eq(setOf(COUNTER_ID.toInt())))
        verify(metrikaClient).getEndToEndStatistics(metrikaParamsCaptor.capture())
        verifyNoMoreInteractions(metrikaClient)

        val capturedParams = metrikaParamsCaptor.value
        SoftAssertions.assertSoftly {
            it.assertThat(capturedParams.goalId)
                .describedAs("params.goalId")
                .isNull()
            it.assertThat(capturedParams.skipGoalData)
                .describedAs("params.skipGoalData")
                .isTrue()
        }
    }

    @Test
    fun getStatistics_useChiefLogin() {
        val chiefLogin = "test_chief_login"

        service.getEndToEndStatistics(
            defaultGdMetrikaStatisticsContainer(),
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            defaultClientWithRubCurrency().withChiefLogin(chiefLogin)
        )

        verify(metrikaClient).getCounter(eq(COUNTER_ID))
        verify(metrikaClient).getEndToEndStatistics(metrikaParamsCaptor.capture())
        verifyNoMoreInteractions(metrikaClient)

        val capturedParams = metrikaParamsCaptor.value
        Assertions.assertThat(capturedParams.chiefLogin)
            .describedAs("params.chiefLogin")
            .isEqualTo(chiefLogin)
    }

    @Test
    fun getStatistics_whenNoExpenseFeature() {
        whenever(metrikaClient.getCounter(COUNTER_ID))
            .thenReturn(
                Counter()
                    .withId(COUNTER_ID.toInt())
                    .withDomain("site.ru")
                    .withFeatures(emptySet())
            )
        val r = service.getEndToEndStatistics(
            defaultGdMetrikaStatisticsContainer(),
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            defaultClientWithRubCurrency()
        )
        verify(metrikaClient).getCounter(eq(COUNTER_ID))
        verifyNoMoreInteractions(metrikaClient)
        Assertions.assertThat(r.meta).isNull()
        Assertions.assertThat(r.rowset).isEmpty()
    }

    @Test
    fun getMetrikaMarketingStatistics_useWorkCurrency() {
        service.getMetrikaMarketingStatistics(
            defaultGdMetrikaStatisticsContainer(),
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            clientWithCurrency(CurrencyCode.BYN)
        )
        verify(metrikaClient).getCounter(eq(COUNTER_ID))
        // check that default counter currency is used
        verify(metrikaClient).getAvailableSources(any())
        verify(metrikaClient).getTrafficSourceStatistics(metrikaParamsCaptor.capture())
        verifyNoMoreInteractions(metrikaClient)

        val capturedParams = metrikaParamsCaptor.value
        Assertions.assertThat(capturedParams.currencyCode)
            .describedAs("params.currencyCode")
            .isEqualTo(CurrencyCode.BYN)
    }

    @Test
    fun getMetrikaMarketingStatistics_extractOther() {
        val r = service.getMetrikaMarketingStatistics(
            inputContainerWithGoalId(GOAL_ID).apply { filter.advChannelIds = setOf(OTHER_CATEGORY.id) },
            ContextHelper.buildContext(TestUsers.generateNewUser(), TestUsers.generateNewUser().withClientId(clientId)),
            defaultClientWithRubCurrency()
        )
        val expectedTrafficSum = sumMetrics(listOf(), "2021-12-25", defaultTrafficSourceResponseRows())
        val expectedTotalStats = defaultTotalTrafficResponseRow()
        val expectedOtherStats =
            subMetrics(listOf(OTHER_CATEGORY), "2021-12-25", expectedTotalStats, expectedTrafficSum)
        Assertions.assertThat(r.rowset)
            .containsExactlyInAnyOrder(convertToGdStatsItem(expectedOtherStats))
    }

    private fun defaultClientWithRubCurrency() =
        clientWithCurrency(CurrencyCode.RUB)

    private fun clientWithCurrency(currencyCode: CurrencyCode) =
        GdClient().withInfo(GdClientInfo().withWorkCurrency(currencyCode))

    private fun defaultGdMetrikaStatisticsContainer() =
        inputContainerWithGoalId(goalId = null)

    private fun inputContainerWithGoalId(goalId: Long?) =
        GdMetrikaStatisticsContainer().withAttributionModel(LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE)
            .withFilter(
                GdMetrikaStatisticsFilter()
                    .withCounterId(COUNTER_ID)
                    .withGoalId(goalId)
                    .withPeriod(
                        GdCampaignStatisticsPeriod()
                            .withFrom(LocalDate.of(2021, Month.DECEMBER, 25))
                            .withTo(LocalDate.of(2021, Month.DECEMBER, 25))
                    )
            )

    private fun sumMetrics(
        dimensions: List<Dimension>,
        period: String?,
        rows: List<StatisticsResponseRow>
    ): StatisticsResponseRow {
        val total = rows.reduce { accRow, row ->
            StatisticsResponseRow(
                dimensions = dimensions,
                period = period,
                clicks = accRow.clicks!! + row.clicks!!,
                goalVisits = accRow.goalVisits!! + row.goalVisits!!,
                expenses = accRow.expenses!! + row.expenses!!,
                revenue = accRow.revenue!! + row.revenue!!
            )
        }
        total.conversionRate = total.goalVisits!! * 100.0 / total.clicks!!
        return total
    }

    private fun subMetrics(
        dimensions: List<Dimension>,
        period: String?,
        row1: StatisticsResponseRow,
        row2: StatisticsResponseRow
    ): StatisticsResponseRow {
        val res = StatisticsResponseRow(
            dimensions = dimensions,
            period = period,
            clicks = row1.clicks!! - row2.clicks!!,
            goalVisits = row1.goalVisits!! - row2.goalVisits!!,
            expenses = row1.expenses!! - row2.expenses!!,
            revenue = row1.revenue!! - row2.revenue!!
        )
        res.conversionRate = res.goalVisits!! * 100.0 / res.clicks!!
        return res
    }

    private fun defaultAvailableSources() = listOf(
        TrafficSource(
            Dimension("ad", "Ad traffic"),
            Dimension("ad.Google Ads", "Google Ads")
        ),
        TrafficSource(
            Dimension("social", "Social network traffic"),
            Dimension("social.vkontakte", "Vkontakte")
        )
    )

    /**
     * В тестовых данных намеренно `conversionRate != goalVisits / clicks`.
     * Так сделано для удобства тестирования разных вариантов вычисления `goalVisits`.
     */
    private fun defaultEndToEndResponseRows() = listOf(
        StatisticsResponseRow(
            dimensions = listOf(Dimension("1.yandex", "yandex")),
            period = "2021-12-25",
            clicks = 10L,
            goalVisits = 7L,
            expenses = BigDecimal.TEN,
            revenue = null
        )
    )

    private fun defaultTrafficSourceResponseRows() = listOf(
        StatisticsResponseRow(
            dimensions = listOf(
                Dimension("ad", "Ad traffic"),
                Dimension("ad.Google Ads", "Google Ads")
            ),
            period = "2021-12-25",
            clicks = 10L,
            goalVisits = 10L,
            expenses = BigDecimal.ONE,
            revenue = BigDecimal.TEN
        ),
        StatisticsResponseRow(
            dimensions = listOf(
                Dimension("social", "Social network traffic"),
                Dimension("social.vkontakte", "Vkontakte")
            ),
            period = "2021-12-25",
            clicks = 10L,
            goalVisits = 10L,
            expenses = BigDecimal.ONE,
            revenue = BigDecimal.ZERO
        )
    )

    private fun defaultTotalTrafficResponseRow() =
        StatisticsResponseRow(
            dimensions = listOf(),
            period = "2021-12-25",
            clicks = 100L,
            goalVisits = 50L,
            expenses = BigDecimal.TEN,
            revenue = BigDecimal.TEN
        )
}

