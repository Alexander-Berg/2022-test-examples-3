package ru.yandex.direct.grid.processing.service.statistics

import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.CAMPAIGN_ID
import ru.yandex.direct.core.testing.data.CLIENT_UID
import ru.yandex.direct.core.testing.data.DEFAULT_CLICKS
import ru.yandex.direct.core.testing.data.DEFAULT_KEYWORD_IDS
import ru.yandex.direct.core.testing.data.DEFAULT_LIMIT
import ru.yandex.direct.core.testing.data.DEFAULT_OFFSET
import ru.yandex.direct.core.testing.data.OPERATOR_UID
import ru.yandex.direct.core.testing.data.TODAY_MINUS_MONTH
import ru.yandex.direct.core.testing.data.YESTERDAY_MINUS_MONTH
import ru.yandex.direct.core.testing.data.defaultCampaignStatisticsRequest
import ru.yandex.direct.grid.model.GdStatPreset
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.GdLimitOffset
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsColumn
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsConditions
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsContainer
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsFilter
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsFilterColumn.CLICKS
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsFilterColumn.KEYWORD_IDS
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupBy
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupByDate
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsOrder
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsOrderBy
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsOrderByField
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsPeriod
import ru.yandex.direct.grid.processing.model.statistics.GdConditionOperator.EQ
import ru.yandex.direct.grid.processing.model.statistics.GdConditionOperator.GT
import ru.yandex.direct.grid.processing.model.statistics.GdFilterConditionListLong
import ru.yandex.direct.grid.processing.model.statistics.GdFilterConditionLong
import ru.yandex.direct.grid.processing.service.statistics.utils.StatisticsUtils.convertRequest
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionConditionOperator
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionFilterColumn
import ru.yandex.direct.test.utils.checkEquals
import java.time.Instant

class StatisticsUtilsTest {

    private lateinit var context: GridGraphQLContext

    @Before
    fun before() {
        context = GridGraphQLContext(
            User().apply {
                uid = OPERATOR_UID
            },
            User().apply {
                uid = CLIENT_UID
            }).apply {
                instant = Instant.now()
            }
    }

    @Test
    fun convertRequest_DefaultFullInput() {
        val campaignStatisticsContainer = defaultGdCampaignStatisticsContainer()
        val expected = defaultCampaignStatisticsRequest()

        convertRequest(campaignStatisticsContainer, context)
            .checkEquals(expected)
    }

    @Test
    fun convertRequest_WithPeriodToCompare() {
        val campaignStatisticsContainer = defaultGdCampaignStatisticsContainer(
            periodToCompare = GdCampaignStatisticsPeriod().apply {
                from = YESTERDAY_MINUS_MONTH
                to = TODAY_MINUS_MONTH
            }
        )
        val expected = defaultCampaignStatisticsRequest().apply {
            reportOptions
                .comparePeriods()
                .withDateFromB(YESTERDAY_MINUS_MONTH)
                .withDateToB(TODAY_MINUS_MONTH)
        }

        convertRequest(campaignStatisticsContainer, context)
            .checkEquals(expected)
    }

    @Test
    fun convertRequest_WithFilterConditions() {
        val campaignStatisticsContainer = defaultGdCampaignStatisticsContainer(conditions = GdCampaignStatisticsConditions().apply {
            longFieldConditions = listOf(GdFilterConditionLong().apply {
                field = CLICKS
                operator = GT
                value = DEFAULT_CLICKS
            })
            listLongFieldConditions = listOf(GdFilterConditionListLong().apply {
                field = KEYWORD_IDS
                operator = EQ
                value = DEFAULT_KEYWORD_IDS
            })
        })
        val expected = defaultCampaignStatisticsRequest().apply {
            reportOptions
                .withFilters(mapOf(
                    ReportOptionFilterColumn.CLICKS to mapOf(ReportOptionConditionOperator.GT to DEFAULT_CLICKS),
                    ReportOptionFilterColumn.KEYWORD_IDS to mapOf(ReportOptionConditionOperator.EQ to DEFAULT_KEYWORD_IDS)))
        }

        convertRequest(campaignStatisticsContainer, context)
            .checkEquals(expected)
    }
}

private fun defaultGdCampaignStatisticsContainer(
    campaignId: Long = CAMPAIGN_ID,
    period: GdCampaignStatisticsPeriod = GdCampaignStatisticsPeriod().apply {
        preset = GdStatPreset.TODAY
    },
    periodToCompare: GdCampaignStatisticsPeriod? = null,
    conditions: GdCampaignStatisticsConditions = GdCampaignStatisticsConditions(),
    columns: Set<GdCampaignStatisticsColumn> = GdCampaignStatisticsColumn.values().toSet(),
    groupBy: Set<GdCampaignStatisticsGroupBy> = GdCampaignStatisticsGroupBy.values().toSet(),
    groupByDate: GdCampaignStatisticsGroupByDate = GdCampaignStatisticsGroupByDate.DAY,
    orderBy: GdCampaignStatisticsOrderBy = GdCampaignStatisticsOrderBy().apply {
        field = GdCampaignStatisticsOrderByField.CONVERSIONS
        order = GdCampaignStatisticsOrder.ASC
    },
    limitOffset: GdLimitOffset = GdLimitOffset().apply {
        limit = DEFAULT_LIMIT
        offset = DEFAULT_OFFSET
    }
): GdCampaignStatisticsContainer =
    GdCampaignStatisticsContainer().apply {
        this.columns = columns
        this.filter = GdCampaignStatisticsFilter().apply {
            this.campaignId = campaignId
            this.period = period
            this.periodToCompare = periodToCompare
            this.conditions = conditions
        }
        this.groupBy = groupBy
        this.groupByDate = groupByDate
        this.orderBy = orderBy
        this.limitOffset = limitOffset
    }
