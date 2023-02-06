package ru.yandex.direct.grid.processing.service.dynamiccondition

import com.google.common.base.Preconditions.checkState
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.jooq.Select
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.model.GdStatRequirements
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetFilter
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetsContainer
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue
import ru.yandex.direct.grid.processing.util.GraphQLUtils.list
import ru.yandex.direct.grid.processing.util.GraphQLUtils.map
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.utils.FunctionalUtils.mapList
import ru.yandex.yt.ytclient.tables.ColumnSchema
import ru.yandex.yt.ytclient.tables.ColumnValueType
import ru.yandex.yt.ytclient.tables.ColumnValueType.DOUBLE
import ru.yandex.yt.ytclient.tables.ColumnValueType.INT64
import ru.yandex.yt.ytclient.tables.ColumnValueType.UINT64
import ru.yandex.yt.ytclient.tables.TableSchema.Builder
import ru.yandex.yt.ytclient.wire.UnversionedRow
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import ru.yandex.yt.ytclient.wire.UnversionedValue
import java.math.BigDecimal

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class DynamicConditionsGraphQlServiceGetStatTest {

    private companion object {
        private val QUERY_TEMPLATE = """
            {
                client(searchBy: {id: %s}) {
                    dynamicAdTargets(input: %s) {
                        cacheKey
                        rowset {
                            name
                            stats {
                                avgClickCost
                                avgClickPosition
                                avgDepth
                                avgGoalCost
                                avgShowPosition
                                bounceRate
                                clicks
                                conversionRate
                                cost
                                cpmPrice
                                ctr
                                day
                                goals
                                profitability
                                revenue
                                shows
                            }
                            goalStats {
                                conversionRate
                                costPerAction
                                goalId
                                goals
                            }
                        }
                        totalStats {
                            avgClickCost
                            avgGoalCost
                            bounceRate
                            clicks
                            conversionRate
                            cost
                            ctr
                            goals
                            profitability
                            revenue
                            shows
                        }
                        totalGoalStats {
                            conversionRate
                            costPerAction
                            goalId
                            goals
                        }
                    }
                }
            }
        """.trimIndent()
    }

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var gridYtSupport: YtDynamicSupport

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo
    private lateinit var adGroupInfo: AdGroupInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo)
    }

    @Test
    fun dynamicAdTargets_whenNoStat_success() {
        val adTarget = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, adTarget)
        val schema = Builder()
                .setStrict(true)
                .setUniqueKeys(false)
                .build()
        val rows = emptyList<UnversionedRow>()
        val rowset = UnversionedRowset(schema, rows)
        `when`(gridYtSupport.selectRows(any(Select::class.java)))
                .thenReturn(rowset)
        val data = sendRequest(null, null)
        val answerStats: MutableMap<String, Any> = getDataValue(data, "client/dynamicAdTargets/rowset/0/stats")
        val answerGoalStats: MutableList<String> = getDataValue(data, "client/dynamicAdTargets/rowset/0/goalStats")
        val answerTotalStats: MutableMap<String, Any> = getDataValue(data, "client/dynamicAdTargets/totalStats")
        val answerTotalGoalStats: MutableList<String> = getDataValue(data, "client/dynamicAdTargets/totalGoalStats")

        val expectedStats: MutableMap<String, Any> = map(
                "avgClickCost", BigDecimal.ZERO,
                "avgClickPosition", BigDecimal.ZERO,
                "avgDepth", BigDecimal.ZERO,
                "avgGoalCost", null,
                "avgShowPosition", BigDecimal.ZERO,
                "bounceRate", BigDecimal.ZERO,
                "clicks", 0L,
                "conversionRate", BigDecimal.ZERO,
                "cost", BigDecimal.ZERO,
                "cpmPrice", BigDecimal.ZERO,
                "ctr", BigDecimal.ZERO,
                "day", null,
                "goals", 0L,
                "profitability", null,
                "revenue", BigDecimal.ZERO,
                "shows", 0L,
        )
        val expectedTotalStats: MutableMap<String, Any> = map(
                "avgClickCost", null,
                "avgGoalCost", null,
                "bounceRate", null,
                "clicks", 0L,
                "conversionRate", null,
                "cost", BigDecimal.ZERO,
                "ctr", null,
                "goals", 0L,
                "profitability", null,
                "revenue", BigDecimal.ZERO,
                "shows", 0L,
        )

        assertSoftly {
            it.assertThat(answerStats).`as`("stats")
                    .`is`(matchedBy(beanDiffer(expectedStats)
                            .useCompareStrategy(DefaultCompareStrategies.allFields())))
            it.assertThat(answerGoalStats).`as`("goalStats")
                    .hasSize(0)
            it.assertThat(answerTotalStats).`as`("totalStats")
                    .`is`(matchedBy(beanDiffer(expectedTotalStats)
                            .useCompareStrategy(DefaultCompareStrategies.allFields())))
            it.assertThat(answerTotalGoalStats).`as`("totalGoalStats")
                    .hasSize(0)
        }
    }

    @Test
    fun dynamicAdTargets_entryStats_success() {
        val firstTarget = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, firstTarget)
        val secondTarget = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, secondTarget)

        data class ColumnValue(val name: String,
                               val type: ColumnValueType,
                               val firstTargetValue: Number,
                               val secondTargetValue: Number)

        val columnValues = listOf(
                //IDS
                ColumnValue("ExportID", INT64, adGroupInfo.campaignId, adGroupInfo.campaignId),
                ColumnValue("GroupExportID", INT64, adGroupInfo.adGroupId, adGroupInfo.adGroupId),
                ColumnValue("PhraseExportID", INT64, firstTarget.dynamicConditionId, secondTarget.dynamicConditionId),

                //Stat values
                ColumnValue("avgClickCost", INT64, 500000L, 400000L),
                ColumnValue("avgClickPosition", INT64, 6000000L, 4000000L),
                ColumnValue("avgDepth", INT64, 2500000L, 1500000L),
                ColumnValue("avgGoalCost", INT64, 5000000L, 3000000L),
                ColumnValue("avgShowPosition", INT64, 5000000L, 3000000L),
                ColumnValue("bounceRate", INT64, 40000000L, 20000000L),
                ColumnValue("bounces", INT64, 100L, 50L),
                ColumnValue("clicks", INT64, 200L, 100L),
                ColumnValue("conversionRate", INT64, 9000000L, 7000000L),
                ColumnValue("cost", INT64, 90000000L, 60000000L),
                ColumnValue("cpmPrice", INT64, 25000000L, 15000000L),
                ColumnValue("ctr", INT64, 5000000L, 3500000L),
                ColumnValue("firstPageClicks", INT64, 50L, 30L),
                ColumnValue("firstPageShows", INT64, 2000L, 1000L),
                ColumnValue("firstPageSumPosClicks", INT64, 300L, 200L),
                ColumnValue("firstPageSumPosShows", INT64, 10000L, 7000L),
                ColumnValue("goals", INT64, 20L, 15L),
                ColumnValue("profitability", DOUBLE, 1.5E8, 1.0E8),
                ColumnValue("revenue", INT64, 15000000000L, 9000000000L),
                ColumnValue("sessionDepth", INT64, 500L, 400L),
                ColumnValue("sessions", INT64, 200L, 150L),
                ColumnValue("sessionsLimited", INT64, 200L, 150L),
                ColumnValue("shows", UINT64, 4000L, 3000L)
        )
        val columns = mapList(columnValues) { v -> ColumnSchema(v.name, v.type) }
        val schema = Builder()
                .addAll(columns)
                .setStrict(true)
                .setUniqueKeys(false)
                .build()
        val firstTargetValues = columnValues.mapIndexed { i, v -> UnversionedValue(i, v.type, false, v.firstTargetValue) }
        val secondTargetValues = columnValues.mapIndexed { i, v -> UnversionedValue(i, v.type, false, v.secondTargetValue) }
        val rows = listOf(UnversionedRow(firstTargetValues), UnversionedRow(secondTargetValues))
        val unversionedRowset = UnversionedRowset(schema, rows)
        `when`(gridYtSupport.selectRows(any(Select::class.java)))
                .thenReturn(unversionedRowset)

        val data = sendRequest(null, null)
        val answerStats: MutableMap<String, Any> = getDataValue(data, "client/dynamicAdTargets/rowset/0/stats")
        val answerTotalStats: MutableMap<String, Any> = getDataValue(data, "client/dynamicAdTargets/totalStats")

        val expectedStats: MutableMap<String, Any> = map(
                "avgClickCost", "0.50".toBigDecimal(),
                "avgClickPosition", "6.00".toBigDecimal(),
                "avgDepth", "2.50".toBigDecimal(),
                "avgGoalCost", "5.00".toBigDecimal(),
                "avgShowPosition", "5.00".toBigDecimal(),
                "bounceRate", "40.00".toBigDecimal(),
                "clicks", 200L,
                "conversionRate", "9.00".toBigDecimal(),
                "cost", "90.00".toBigDecimal(),
                "cpmPrice", "25.00".toBigDecimal(),
                "ctr", "5.00".toBigDecimal(),
                "day", null,
                "goals", 20L,
                "profitability", "150.00".toBigDecimal(),
                "revenue", "15000.00".toBigDecimal(),
                "shows", 4000L
        )
        val expectedTotalStats: MutableMap<String, Any> = map(
                "avgClickCost", "0.50".toBigDecimal(),
                "avgGoalCost", "4.29".toBigDecimal(),
                "bounceRate", "33.33".toBigDecimal(),
                "clicks", 300L,
                "conversionRate", "11.67".toBigDecimal(),
                "cost", "150.00".toBigDecimal(),
                "ctr", "4.29".toBigDecimal(),
                "goals", 35L,
                "profitability", "159.00".toBigDecimal(),
                "revenue", "24000.00".toBigDecimal(),
                "shows", 7000L
        )

        assertSoftly {
            it.assertThat(answerStats).`as`("stats")
                    .`is`(matchedBy(beanDiffer(expectedStats)
                            .useCompareStrategy(DefaultCompareStrategies.allFields())))
            it.assertThat(answerTotalStats).`as`("totalStats")
                    .`is`(matchedBy(beanDiffer(expectedTotalStats)
                            .useCompareStrategy(DefaultCompareStrategies.allFields())))
        }
    }

    @Test
    fun dynamicAdTargets_goalStats_success() {
        val firstGoalId = 88888L
        val secondGoalId = 9999L
        val adTarget = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, adTarget)

        data class ColumnValue(val name: String,
                               val type: ColumnValueType,
                               val value: Number)

        val columnValues = listOf(
                //IDS
                ColumnValue("ExportID", INT64, adGroupInfo.campaignId),
                ColumnValue("GroupExportID", INT64, adGroupInfo.adGroupId),
                ColumnValue("PhraseExportID", INT64, adTarget.dynamicConditionId),

                // values for calculate TotalGoalStats
                ColumnValue("cost", INT64, 90000000L),
                ColumnValue("clicks", INT64, 200L),
                ColumnValue("shows", UINT64, 4000L),

                // first goals values
                ColumnValue("conversionRate" + firstGoalId.toString(), INT64, 2100000L),
                ColumnValue("costPerAction" + firstGoalId.toString(), INT64, 20000000L),
                ColumnValue("goals" + firstGoalId.toString(), INT64, 55L),
                ColumnValue("revenue" + firstGoalId.toString(), INT64, 0L),

                // second goal values
                ColumnValue("conversionRate" + secondGoalId.toString(), INT64, 1100000L),
                ColumnValue("costPerAction" + secondGoalId.toString(), INT64, 10000000L),
                ColumnValue("goals" + secondGoalId.toString(), INT64, 45L),
                ColumnValue("revenue" + secondGoalId.toString(), INT64, 0L),
        )
        val columns = mapList(columnValues) { v -> ColumnSchema(v.name, v.type) }
        val schema = Builder()
                .addAll(columns)
                .setStrict(true)
                .setUniqueKeys(false)
                .build()
        val firstTargetValues = columnValues.mapIndexed { i, v -> UnversionedValue(i, v.type, false, v.value) }
        val rows = listOf(UnversionedRow(firstTargetValues))
        val unversionedRowset = UnversionedRowset(schema, rows)
        `when`(gridYtSupport.selectRows(any(Select::class.java)))
                .thenReturn(unversionedRowset)

        val data = sendRequest(null, setOf(firstGoalId, secondGoalId))
        val answerGoalStats: MutableList<MutableMap<String, Any>> = getDataValue(data, "client/dynamicAdTargets/rowset/0/goalStats")
        val answerTotalGoalStats: MutableList<String> = getDataValue(data, "client/dynamicAdTargets/totalGoalStats")

        val expectedGoalStats = list(
                map(
                        "conversionRate", "2.10".toBigDecimal(),
                        "costPerAction", "20.00".toBigDecimal(),
                        "goalId", firstGoalId,
                        "goals", 55L),
                map(
                        "conversionRate", "1.10".toBigDecimal(),
                        "costPerAction", "10.00".toBigDecimal(),
                        "goalId", secondGoalId,
                        "goals", 45L
                )
        )
        val expectedTotalGoalStats = list(
                map(
                        "conversionRate", "27.50".toBigDecimal(),
                        "costPerAction", "1.64".toBigDecimal(),
                        "goalId", firstGoalId,
                        "goals", 55L),
                map(
                        "conversionRate", "22.50".toBigDecimal(),
                        "costPerAction", "2.00".toBigDecimal(),
                        "goalId", secondGoalId,
                        "goals", 45L
                )
        )

        assertSoftly {
            it.assertThat(answerGoalStats).`as`("goalStats")
                    .`is`(matchedBy(beanDiffer(expectedGoalStats)
                            .useCompareStrategy(DefaultCompareStrategies.allFields())))
            it.assertThat(answerTotalGoalStats).`as`("totalGoalStats")
                    .`is`(matchedBy(beanDiffer(expectedTotalGoalStats)
                            .useCompareStrategy(DefaultCompareStrategies.allFields())))
        }
    }

    @Test
    fun dynamicAdTargets_ytRequestCheck() {
        val goalId = 88888L
        val adTarget = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, adTarget)

        val schema = Builder()
                .setStrict(true)
                .setUniqueKeys(false)
                .build()
        val rows = emptyList<UnversionedRow>()
        val args: ArrayList<Any> = ArrayList()
        val rowset = UnversionedRowset(schema, rows)
        val answer = Answer<UnversionedRowset> { invocation ->
            args.addAll(invocation.getArguments().toList())
            rowset
        }
        doAnswer(answer).`when`(gridYtSupport).selectRows(any(Select::class.java))

        sendRequest(setOf(adTarget.dynamicConditionId), setOf(goalId))
        checkState(args.size == 1)

        val query: String = args.get(0).toString()
        val expectedIdConditions = "(ExportID, GroupExportID, PhraseExportID) IN ((%d, %d, %d))"
                .format(adGroupInfo.campaignId, adGroupInfo.adGroupId, adTarget.dynamicConditionId)
        val expectedGoalIds = " %d".format(goalId)
        assertSoftly {
            it.assertThat(query).`as`("ID conditions")
                    .contains(expectedIdConditions)
            it.assertThat(query).`as`("goalId")
                    .contains(expectedGoalIds)
        }
    }

    private fun sendRequest(dynamicConditionIds: Set<Long>?, goalIds: Set<Long>?): Map<String, Any> {
        val user = userService.getUser(clientInfo.uid)
        val context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null)
        gridContextProvider.gridContext = context
        val targetsContainer = GdDynamicAdTargetsContainer()
                .withFilter(GdDynamicAdTargetFilter()
                        .withAdGroupIdIn(setOf(adGroupInfo.adGroupId))
                        .withCampaignIdIn(setOf(adGroupInfo.campaignId))
                        .withIdIn(dynamicConditionIds)
                )
                .withStatRequirements(GdStatRequirements()
                        .withGoalIds(goalIds))
        val serializedContainer = GraphQlJsonUtils.graphQlSerialize(targetsContainer)
        val query = String.format(QUERY_TEMPLATE, clientInfo.clientId, serializedContainer)
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        return result.getData()
    }

}
