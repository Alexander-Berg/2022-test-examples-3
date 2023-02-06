package ru.yandex.direct.bannersystem.container.masterreport.converter

import java.io.IOException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions
import org.assertj.core.util.BigDecimalComparator
import org.junit.Test
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportResponse
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportRow
import ru.yandex.direct.bannersystem.container.masterreport.MasterReportRow.MultiGoalsData
import ru.yandex.direct.bannersystem.container.masterreport.dict.MasterReportCampaignPlatform
import ru.yandex.direct.utils.JsonUtils

class MasterReportResponseDeserializerTest {
    @Test
    @Throws(IOException::class)
    fun simpleResponseTest() {
        val source = MasterReportResponseDeserializerTest::class.java.getResource(
                "/masterreport.converter/simple_response.json"
        ).readText(StandardCharsets.UTF_8)

        val actualResponse = JsonUtils.MAPPER.readValue(
                source,
                MasterReportResponse::class.java
        )

        val expectedTotals = MasterReportRow().apply {
            roi = 57.4234360102059
            costPerConversion = BigDecimal.valueOf(1774446.34461683)
            shows = 32686L
            rawSessionDepth = 3620L
            income = BigDecimal.valueOf(147003000000L)
            ctr = 3.55503885455547
            clicks = 1162L
            avgCpc = BigDecimal.valueOf(2165374.28284567)
            uniqViewers = 13498L
            crr = 1.71164188259197
            rawSessionNumLim = 1198L
            profit = BigDecimal.valueOf(144486835083.333)
            rawSessionNum = 1198L
            bounces = 174L
            conversionRate = 122.030981067126
            bounceRatio = 14.5242070116861
            conversions = 1418L
            cost = BigDecimal.valueOf(2516164916.66667)
        }

        val expectedFirstRow = MasterReportRow().apply {
            period = "2021-11-10"
            campaignId = 1234567L
            platform = MasterReportCampaignPlatform.SEARCH
            shows = 16353L
            clicks = 585L
            cost = BigDecimal.valueOf(1230805416.66667)
            conversions = 702L
            income = BigDecimal.valueOf(50296000000L)
            rawSessionNum = 584L
            rawSessionNumLim = 584L
            rawSessionDepth = 1668L
            bounces = 90L
            ctr = 3.57732526141992
            avgCpc = BigDecimal.valueOf(2103940.88319088)
            bounceRatio = 15.4109589041096
            conversionRate = 120.0
            costPerConversion = BigDecimal.valueOf(1753284.06932574)
            roi = 39.8642985470557
            crr = 2.44712386008165
            profit = BigDecimal.valueOf(49065194583.3333)
            uniqViewers = 7549L
        }

        val expectedSecondRow = MasterReportRow().apply {
            period = "2021-11-11"
            campaignId = 1234568L
            platform = MasterReportCampaignPlatform.CONTEXT
            shows = 16333L
            clicks = 577L
            cost = BigDecimal.valueOf(1285359500)
            conversions = 716L
            income = BigDecimal.valueOf(96707000000L)
            rawSessionNum = 614L
            rawSessionNumLim = 614L
            rawSessionDepth = 1952L
            bounces = 84L
            ctr = 3.53272515765628
            avgCpc = BigDecimal.valueOf(2227659.44540728)
            bounceRatio = 13.6807817589577
            conversionRate = 124.090121317158
            costPerConversion = BigDecimal.valueOf(1795194.83240224)
            roi = 74.2373168751621
            crr = 1.3291276743152
            profit = BigDecimal.valueOf(95421640500L)
            uniqViewers = 7947L
        }

        val expectedResponse = MasterReportResponse().apply {
            status = 0
            errorText = ""
            totals = expectedTotals
            data = listOf(expectedFirstRow, expectedSecondRow)
        }

        Assertions.assertThat(actualResponse)
                .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expectedResponse)
    }

    @Test
    @Throws(IOException::class)
    fun multiGoalsResponseTest() {
        val source = MasterReportResponseDeserializerTest::class.java.getResource(
                "/masterreport.converter/multi_goals_response.json",
        ).readText(StandardCharsets.UTF_8)
        val actualResponse = JsonUtils.MAPPER.readValue(
                source,
                MasterReportResponse::class.java
        )
        val expectedTotals = MasterReportRow().apply {
            clicks = 47L
            cost = BigDecimal.valueOf(78270083.1111111)
            conversions = 17L
            costPerConversion = BigDecimal.valueOf(4604122.54901961)
            multiGoalsData = listOf(
                    MultiGoalsData(
                            3,
                            16261155,
                            11L,
                            BigDecimal.valueOf(7115462.13131313),
                            25.5813953488372
                    ),
                    MultiGoalsData(
                            3,
                            16361955,
                            6L,
                            BigDecimal.valueOf(13045013.9999999),
                            13.953488372093
                    )
            )
        }

        val expectedRow = MasterReportRow().apply {
            clicks = 43L
            cost = BigDecimal.valueOf(78270083.3333333)
            conversions = 17L
            costPerConversion = BigDecimal.valueOf(4604122.54901961)
            multiGoalsData = listOf(
                    MultiGoalsData(
                            3,
                            16261155,
                            11L,
                            BigDecimal.valueOf(7115462.12121212),
                            25.5813953488372
                    ),
                    MultiGoalsData(
                            3,
                            16361955,
                            6L,
                            BigDecimal.valueOf(13045013.8888889),
                            13.953488372093
                    )
            )
        }

        val expectedResponse = MasterReportResponse().apply {
            status = 0
            errorText = ""
            totals = expectedTotals
            data = listOf(expectedRow)
        }

        Assertions.assertThat(actualResponse)
                .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expectedResponse)
    }
}
