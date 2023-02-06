package ru.yandex.direct.intapi.client

import java.time.LocalDate
import org.junit.Assert.assertThat
import org.junit.Test
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.intapi.client.CampaignStatisticsMethodTest.Companion.statisticsHandle
import ru.yandex.direct.intapi.client.model.handle.CampaignStatisticsIntApiHandle
import ru.yandex.direct.intapi.client.model.request.statistics.CampaignStatisticsRequest
import ru.yandex.direct.intapi.client.model.request.statistics.ReportOptions
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionColumn
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupBy
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate.DAY
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType._0_17
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType._18_24
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType._25_34
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType._35_44
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType._45_54
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType._55_
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsGenderType
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsGenderType.FEMALE
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsGenderType.MALE
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsGenderType.UNKNOWN
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsItem
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsResponse
import ru.yandex.direct.liveresource.LiveResourceFactory

class CampaignStatisticsMethodTest {

    companion object {
        val statisticsHandle = CampaignStatisticsIntApiHandle("stub")
    }

    @Test
    fun deserializeResponseFromJson_CampaignStatisticsByDate() {
        deserializeStatisticsResponseFrom("classpath:///campaign_statistics_by_date_example.json")
            .checkStatisticsContains(
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 9), shows = 22, clicks = 0,
                    cost = 0.0, revenue = 0.0, ctr = 0.0, avgCpm = 0.0
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 10),
                    endDate = LocalDate.of(2020, 5, 10), shows = 16, clicks = 0,
                    cost = 0.0, revenue = 0.0, ctr = 0.0, avgCpm = 0.0
                )
            )
    }

    @Test
    fun deserializeResponseFromJson_CampaignStatisticsByRegion() {
        deserializeStatisticsResponseFrom("classpath:///campaign_statistics_by_region_example.json")
            .checkStatisticsContains(
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    region = 98745, shows = 1, clicks = 0, cost = 1.23, revenue = 0.0, ctr = 0.0, avgCpm = 0.0
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    region = 10987,
                    shows = 1,
                    clicks = 0,
                    cost = 5.34,
                    revenue = 0.0,
                    ctr = 0.0,
                    avgCpm = 0.0
                )
            )
    }

    @Test
    fun deserializeResponseFromJson_CampaignStatisticsBySocdem() {
        deserializeStatisticsResponseFrom("classpath:///campaign_statistics_by_socdem_example.json")
            .checkStatisticsContains(
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    genderType = UNKNOWN,
                    ageType = CampaignStatisticsAgeType.UNKNOWN,
                    shows = 164,
                    clicks = 4,
                    avgCpc = 41.55,
                    conversions = 4,
                    cost = 166.22,
                    costPerConversion = 41.55,
                    revenue = 0.0,
                    ctr = 2.44,
                    avgCpm = 1013.52
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    genderType = UNKNOWN,
                    ageType = _35_44,
                    shows = 2,
                    clicks = 0,
                    cost = 0.0,
                    revenue = 0.0,
                    ctr = 0.0,
                    avgCpm = 0.0
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    genderType = MALE,
                    ageType = _0_17,
                    shows = 2,
                    clicks = 0,
                    cost = 0.0,
                    revenue = 0.0,
                    ctr = 0.0,
                    avgCpm = 0.0
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    genderType = MALE,
                    ageType = _18_24,
                    shows = 2,
                    clicks = 0,
                    cost = 0.0,
                    revenue = 0.0,
                    ctr = 0.0,
                    avgCpm = 0.0
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    genderType = FEMALE,
                    ageType = _25_34,
                    shows = 27,
                    clicks = 3,
                    avgCpc = 59.79,
                    conversions = 6,
                    cost = 179.36,
                    costPerConversion = 29.89,
                    revenue = 0.0,
                    ctr = 11.11,
                    avgCpm = 6643.11
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    genderType = FEMALE,
                    ageType = _45_54,
                    shows = 14,
                    clicks = 1,
                    avgCpc = 75.81,
                    conversions = 2,
                    cost = 75.81,
                    costPerConversion = 37.9,
                    revenue = 0.0,
                    ctr = 7.14,
                    avgCpm = 5414.89
                ),
                statisticsItem(
                    date = LocalDate.of(2020, 5, 9),
                    endDate = LocalDate.of(2020, 5, 16),
                    genderType = MALE,
                    ageType = _55_,
                    shows = 8,
                    clicks = 3,
                    avgCpc = 78.46,
                    conversions = 3,
                    cost = 235.37,
                    costPerConversion = 78.46,
                    revenue = 0.0,
                    ctr = 37.5,
                    avgCpm = 29421.69
                )
            )
    }

    @Test
    fun deserializeResponseFromJson_CampaignStatisticsReach() {
        deserializeStatisticsResponseFrom("classpath:///campaign_statistics_reach_example.json")
        /*
        .checkStatisticsContains(
            statisticsItem(
                date = "2021-08-11",
                video = 60.13,
                avg_cpm = 141.94,
                camp_name = Тест Мастера охватных - макс показов,
                video_avg_true_view_cost = 0.24,
                sum = 654.08,
                video_midpoint_rate = 84.35,
                av_sum = 93.44,
                uniq_viewers = 2592,
                AvgGoalsCost = null,
                goal_id = 0,
                date = 11.08.2021,
                cid = 64384605,
                video_third_quartile_rate = 68.53,
                clicks = 7,
                avg_view_freq = 1.78,
                video_first_quartile_rate = 96.83,
                bonus = 0.00,
                stat_date = 2021 - 08 - 11,
                OrderID = 164384605,
                shows = 4608,
                ctr = 0.15,
                agoalincome = 0.00
            )
        )

         */
    }

    @Test
    fun serializeRequestToJson_FullRequest() {
        val statisticsRequest = campaignStatisticsRequest(
            uid = 1,
            operatorUid = 2,
            campaignId = 3,
            dateFrom = LocalDate.of(2020, 5, 27),
            dateTo = LocalDate.of(2020, 5, 28),
            columns = ReportOptionColumn.values().toSet(),
            groupBy = ReportOptionGroupBy.values().toSet(),
            groupByDate = DAY
        )

        serializeStatisticsRequest(statisticsRequest)
            .checkEqualsToContentsFromFile("classpath:///campaign_statistics_request_example.json")
    }
}

private fun serializeStatisticsRequest(request: CampaignStatisticsRequest): String =
    statisticsHandle.serializeRequest(request)

private fun deserializeStatisticsResponseFrom(filePath: String): CampaignStatisticsResponse {
    val campaignStatisticsRaw = readFileToString(filePath)
    return statisticsHandle.deserializeResponse(campaignStatisticsRaw)
}

private fun CampaignStatisticsResponse.checkStatisticsContains(vararg expectedData: CampaignStatisticsItem) =
    assertThat(this.data, beanDiffer(expectedData.asList()))

private fun String.checkEqualsToContentsFromFile(filePath: String): Boolean {
    val fileContent = readFileToString(filePath)
    return equals(fileContent)
}

private fun readFileToString(filePath: String): String = LiveResourceFactory.get(filePath).content

private fun statisticsItem(
    date: LocalDate? = null,
    endDate: LocalDate? = null,
    genderType: CampaignStatisticsGenderType? = null,
    ageType: CampaignStatisticsAgeType? = null,
    region: Int? = null,
    shows: Long,
    clicks: Long,
    cost: Double? = null,
    avgCpc: Double? = null,
    conversions: Long? = null,
    costPerConversion: Double? = null,
    revenue: Double? = null,
    avgCpm: Double? = null,
    ctr: Double? = null,
): CampaignStatisticsItem =
    CampaignStatisticsItem().apply { 
        this.date = date
        this.endDate = endDate
        this.genderType = genderType
        this.ageType = ageType
        this.region = region
        this.shows = shows
        this.clicks = clicks
        this.cost = cost
        this.avgCpc = avgCpc
        this.conversions = conversions
        this.costPerConversion = costPerConversion
        this.revenue = revenue
        this.ctr = ctr
        this.avgCpm = avgCpm
    }

private fun campaignStatisticsRequest(
    uid: Long,
    operatorUid: Long,
    campaignId: Long,
    dateFrom: LocalDate,
    dateTo: LocalDate,
    columns: Set<ReportOptionColumn>,
    groupBy: Set<ReportOptionGroupBy>,
    groupByDate: ReportOptionGroupByDate
): CampaignStatisticsRequest =
    CampaignStatisticsRequest()
        .withUid(uid)
        .withOperatorUid(operatorUid)
        .withReportOptions(
            ReportOptions()
                .withCampaignId(campaignId)
                .withDateFrom(dateFrom)
                .withDateTo(dateTo)
                .withColumns(columns)
                .withGroupBy(groupBy)
                .withGroupByDate(groupByDate)
        )
