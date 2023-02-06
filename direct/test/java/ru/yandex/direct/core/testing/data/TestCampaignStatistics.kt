package ru.yandex.direct.core.testing.data

import ru.yandex.direct.intapi.client.model.request.statistics.CampaignStatisticsRequest
import ru.yandex.direct.intapi.client.model.request.statistics.ReportOptions
import ru.yandex.direct.intapi.client.model.request.statistics.ReportType
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionColumn
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionConditionOperator
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionFilterColumn
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupBy
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate.DAY
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionOrder
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionOrderByField
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsGenderType
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsItem
import java.time.LocalDate

const val DEFAULT_SHOWS = 111L
const val DEFAULT_CLICKS = 222L

const val CLIENT_UID = 2L
const val OPERATOR_UID = 3L
const val CAMPAIGN_ID = 12345L

const val DEFAULT_LIMIT = 1
const val DEFAULT_OFFSET = 0

val DEFAULT_KEYWORD_IDS = listOf(1L, 2L, 3L)

val TODAY: LocalDate = LocalDate.now()
val YESTERDAY: LocalDate = LocalDate.now().minusDays(1)
val TODAY_MINUS_MONTH: LocalDate = TODAY.minusMonths(1)
val YESTERDAY_MINUS_MONTH: LocalDate = YESTERDAY.minusMonths(1)

val DEFAULT_REPORT_TYPE = ReportType.SEARCH_QUERIES
val DEFAULT_ORDER = ReportOptionOrder.ASC
val DEFAULT_ORDER_BY_FIELD = ReportOptionOrderByField.CONVERSIONS

fun defaultStatisticsItem(
    date: LocalDate? = null,
    genderType: CampaignStatisticsGenderType? = null,
    ageType: CampaignStatisticsAgeType? = null,
    region: Int? = null,
    shows: Long = DEFAULT_SHOWS,
    clicks: Long = DEFAULT_CLICKS
): CampaignStatisticsItem =
    CampaignStatisticsItem()
        .withDate(date)
        .withGenderType(genderType)
        .withAgeType(ageType)
        .withRegion(region)
        .withShows(shows)
        .withClicks(clicks)

fun defaultCampaignStatisticsRequest(
    campaignId: Long = CAMPAIGN_ID,
    dateFrom: LocalDate = TODAY,
    dateTo: LocalDate = TODAY,
    clientUid: Long = CLIENT_UID,
    operatorUid: Long = OPERATOR_UID
): CampaignStatisticsRequest =
    CampaignStatisticsRequest()
        .withUid(clientUid)
        .withOperatorUid(operatorUid)
        .withReportOptions(defaultReportOptions(campaignId, dateFrom, dateTo))

fun defaultReportOptions(
    campaignId: Long = CAMPAIGN_ID,
    dateFrom: LocalDate = YESTERDAY,
    dateTo: LocalDate = TODAY,
    comparePeriodDateFrom: LocalDate? = null,
    comparePeriodDateTo: LocalDate? = null,
    columns: Set<ReportOptionColumn> = ReportOptionColumn.values().toSet(),
    groupBy: Set<ReportOptionGroupBy> = ReportOptionGroupBy.values().toSet(),
    groupByDate: ReportOptionGroupByDate = DAY,
    limit: Int = DEFAULT_LIMIT,
    offset: Int = DEFAULT_OFFSET,
    reportType: ReportType = DEFAULT_REPORT_TYPE,
    order: ReportOptionOrder = DEFAULT_ORDER,
    orderByField: ReportOptionOrderByField = DEFAULT_ORDER_BY_FIELD,
    filters: Map<ReportOptionFilterColumn, Map<ReportOptionConditionOperator, Any>> = emptyMap()
): ReportOptions =
    ReportOptions()
        .withCampaignId(campaignId)
        .withDateFrom(dateFrom)
        .withDateTo(dateTo)
        .withDateFromB(comparePeriodDateFrom)
        .withDateToB(comparePeriodDateTo)
        .withFilters(filters)
        .withColumns(columns)
        .withGroupBy(groupBy)
        .withGroupByDate(groupByDate)
        .withLimit(limit)
        .withOffset(offset)
        .withOrder(order)
        .withOrderByField(orderByField)
        .withReportType(reportType)
