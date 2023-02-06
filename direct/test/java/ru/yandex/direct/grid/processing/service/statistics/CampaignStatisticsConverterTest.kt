package ru.yandex.direct.grid.processing.service.statistics

import java.time.LocalDate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.data.DEFAULT_CLICKS
import ru.yandex.direct.core.testing.data.DEFAULT_SHOWS
import ru.yandex.direct.core.testing.data.TODAY
import ru.yandex.direct.core.testing.data.defaultStatisticsItem
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsColumnValues
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsItem
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsValueHolder
import ru.yandex.direct.grid.processing.model.statistics.GdStatisticsAgeType
import ru.yandex.direct.grid.processing.model.statistics.GdStatisticsGenderType
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsAgeType._35_44
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsGenderType.MALE
import ru.yandex.direct.regions.Region.MOSCOW_REGION_ID
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.randomPositiveDouble
import ru.yandex.direct.test.utils.randomPositiveLong

private const val REGION_ID = MOSCOW_REGION_ID.toInt()

private val AGE_TYPE = _35_44
private val GD_AGE_TYPE = GdStatisticsAgeType.fromSource(AGE_TYPE)!!
private val GENDER_TYPE = MALE
private val GD_GENDER_TYPE = GdStatisticsGenderType.fromSource(GENDER_TYPE)!!

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignStatisticsConverterTest {

    @Autowired
    private lateinit var converter: CampaignStatisticsConverter

    private var valueA: Long = 0
    private var valueB: Long = 0
    private var absDelta: Long = 0
    private var prcDelta: Double = 0.0

    @Before
    fun setUp() {
        valueA = randomPositiveLong()
        valueB = randomPositiveLong()
        absDelta = randomPositiveLong()
        prcDelta = randomPositiveDouble()
    }

    @Test
    fun convert_SingleStatisticsByDateItem() {
        val statisticsItem = defaultStatisticsItem(
            date = TODAY, region = REGION_ID, ageType = AGE_TYPE,
            genderType = GENDER_TYPE
        )

        converter.convert(statisticsItem)
            .checkEquals(
                defaultGdCampaignStatisticsItem(
                    date = TODAY, regionId = REGION_ID, ageType = GD_AGE_TYPE,
                    genderType = GD_GENDER_TYPE
                )
            )
    }

    @Test
    fun convert_MultipleStatisticsOfMultipleTypesItems() {
        val statisticsItems = listOf(
            defaultStatisticsItem(date = TODAY),
            defaultStatisticsItem(region = REGION_ID),
            defaultStatisticsItem(ageType = AGE_TYPE, genderType = GENDER_TYPE)
        )

        converter.convert(statisticsItems)
            .checkEquals(
                listOf(
                    defaultGdCampaignStatisticsItem(date = TODAY),
                    defaultGdCampaignStatisticsItem(regionId = REGION_ID),
                    defaultGdCampaignStatisticsItem(ageType = GD_AGE_TYPE, genderType = GD_GENDER_TYPE)
                )
            )
    }

    @Test
    fun convert_WithComparePeriods_ClicksFieldIsCorrect() {
        val statisticsItem = defaultStatisticsItem(date = TODAY).apply {
            withClicks(null)
            withClicksA(valueA)
            withClicksB(valueB)
            withClicksAbsDelta(absDelta)
            withClicksPctDelta(prcDelta)
        }

        converter.convert(statisticsItem)
            .checkEquals(defaultGdCampaignStatisticsItem(date = TODAY).apply {
                columnValues.clicks = GdCampaignStatisticsValueHolder().apply {
                    this.value = valueA
                    this.valueToCompare = valueB
                    this.valueAbsDelta = absDelta
                    this.valuePrcDelta = prcDelta
                }
            })
    }

    @Test
    fun convert_WithComparePeriods_ShowsFieldIsCorrect() {
        val statisticsItem = defaultStatisticsItem(date = TODAY).apply {
            withShows(null)
            withShowsA(valueA)
            withShowsB(valueB)
            withShowsAbsDelta(absDelta)
            withShowsPctDelta(prcDelta)
        }

        converter.convert(statisticsItem)
            .checkEquals(defaultGdCampaignStatisticsItem(date = TODAY).apply {
                columnValues.shows = GdCampaignStatisticsValueHolder().apply {
                    this.value = valueA
                    this.valueToCompare = valueB
                    this.valueAbsDelta = absDelta
                    this.valuePrcDelta = prcDelta
                }
            })
    }

    @Test
    fun convert_WithComparePeriods_ConversionsFieldIsCorrect() {
        val statisticsItem = defaultStatisticsItem(date = TODAY).apply {
            withConversions(null)
            withConversionsA(valueA)
            withConversionsB(valueB)
            withConversionsAbsDelta(absDelta)
            withConversionsPctDelta(prcDelta)
        }

        converter.convert(statisticsItem)
            .checkEquals(defaultGdCampaignStatisticsItem(date = TODAY).apply {
                columnValues.conversions = GdCampaignStatisticsValueHolder().apply {
                    this.value = valueA
                    this.valueToCompare = valueB
                    this.valueAbsDelta = absDelta
                    this.valuePrcDelta = prcDelta
                }
            })
    }

    @Test
    fun convert_WithComparePeriods_AvgCpcFieldIsCorrect() {
        val statisticsItem = defaultStatisticsItem(date = TODAY).apply {
            withAvgCpc(null)
            withAvgCpcA(valueA.toDouble())
            withAvgCpcB(valueB.toDouble())
            withAvgCpcAbsDelta(absDelta.toDouble())
            withAvgCpcPctDelta(prcDelta)
        }

        converter.convert(statisticsItem)
            .checkEquals(defaultGdCampaignStatisticsItem(date = TODAY).apply {
                columnValues.avgCpc = GdCampaignStatisticsValueHolder().apply {
                    this.value = valueA.toDouble()
                    this.valueToCompare = valueB.toDouble()
                    this.valueAbsDelta = absDelta.toDouble()
                    this.valuePrcDelta = prcDelta
                }
            })
    }

    @Test
    fun convert_WithComparePeriods_CostPerConversionFieldIsCorrect() {
        val statisticsItem = defaultStatisticsItem(date = TODAY).apply {
            withCostPerConversion(null)
            withCostPerConversionA(valueA.toDouble())
            withCostPerConversionB(valueB.toDouble())
            withCostPerConversionAbsDelta(absDelta.toDouble())
            withCostPerConversionPctDelta(prcDelta)
        }

        converter.convert(statisticsItem)
            .checkEquals(defaultGdCampaignStatisticsItem(date = TODAY).apply {
                columnValues.costPerConversion = GdCampaignStatisticsValueHolder().apply {
                    this.value = valueA.toDouble()
                    this.valueToCompare = valueB.toDouble()
                    this.valueAbsDelta = absDelta.toDouble()
                    this.valuePrcDelta = prcDelta
                }
            })
    }

    @Test
    fun convert_WithComparePeriods_CostFieldIsCorrect() {
        val statisticsItem = defaultStatisticsItem(date = TODAY).apply {
            withCost(null)
            withCostA(valueA.toDouble())
            withCostB(valueB.toDouble())
            withCostAbsDelta(absDelta.toDouble())
            withCostPctDelta(prcDelta)
        }

        converter.convert(statisticsItem)
            .checkEquals(defaultGdCampaignStatisticsItem(date = TODAY).apply {
                columnValues.cost = GdCampaignStatisticsValueHolder().apply {
                    this.value = valueA.toDouble()
                    this.valueToCompare = valueB.toDouble()
                    this.valueAbsDelta = absDelta.toDouble()
                    this.valuePrcDelta = prcDelta
                }
            })
    }

    @Test
    fun convert_WithComparePeriods_RevenueFieldIsCorrect() {
        val statisticsItem = defaultStatisticsItem(date = TODAY).apply {
            withRevenue(null)
            withRevenueA(valueA.toDouble())
            withRevenueB(valueB.toDouble())
            withRevenueAbsDelta(absDelta.toDouble())
            withRevenuePctDelta(prcDelta)
        }

        converter.convert(statisticsItem)
            .checkEquals(defaultGdCampaignStatisticsItem(date = TODAY).apply {
                columnValues.revenue = GdCampaignStatisticsValueHolder().apply {
                    this.value = valueA.toDouble()
                    this.valueToCompare = valueB.toDouble()
                    this.valueAbsDelta = absDelta.toDouble()
                    this.valuePrcDelta = prcDelta
                }
            })
    }
}

private fun defaultGdCampaignStatisticsItem(
    date: LocalDate? = null,
    regionId: Int? = null,
    ageType: GdStatisticsAgeType? = null,
    genderType: GdStatisticsGenderType? = null,
    shows: Long = DEFAULT_SHOWS,
    clicks: Long = DEFAULT_CLICKS
): GdCampaignStatisticsItem =
    GdCampaignStatisticsItem().apply {
        this.date = date
        this.region = regionId
        this.age = ageType
        this.gender = genderType
        fillDefaultCampaignStatisticsFields(shows, clicks)
    }

private fun <T : GdCampaignStatisticsItem> T.fillDefaultCampaignStatisticsFields(
    shows: Long = DEFAULT_SHOWS,
    clicks: Long = DEFAULT_CLICKS
): T {
    return this.apply {
        columnValues = GdCampaignStatisticsColumnValues().apply {
            this.shows = GdCampaignStatisticsValueHolder().apply {
                value = shows
            }
            this.clicks = GdCampaignStatisticsValueHolder().apply {
                value = clicks
            }
            this.avgCpc = GdCampaignStatisticsValueHolder()
            this.conversions = GdCampaignStatisticsValueHolder()
            this.costPerConversion = GdCampaignStatisticsValueHolder()
            this.cost = GdCampaignStatisticsValueHolder()
            this.revenue = GdCampaignStatisticsValueHolder()
            this.avgCpm = GdCampaignStatisticsValueHolder()
            this.uniqViewers = GdCampaignStatisticsValueHolder()
            this.videoAvgTrueViewCost = GdCampaignStatisticsValueHolder()
            this.videoFirstQuartileRate = GdCampaignStatisticsValueHolder()
            this.videoMidpointRate =  GdCampaignStatisticsValueHolder()
            this.videoThirdQuartileRate = GdCampaignStatisticsValueHolder()
            this.videoCompleteRate =  GdCampaignStatisticsValueHolder()
            this.videoTrueView = GdCampaignStatisticsValueHolder()
            this.avgViewFreq = GdCampaignStatisticsValueHolder()
            this.ctr = GdCampaignStatisticsValueHolder()
            this.avgNShow = GdCampaignStatisticsValueHolder()
            this.avgNShowComplete = GdCampaignStatisticsValueHolder()
        }
    }
}
