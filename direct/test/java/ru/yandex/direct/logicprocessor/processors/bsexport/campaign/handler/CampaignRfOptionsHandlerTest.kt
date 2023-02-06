package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.adv.direct.showcondition.RfOptions
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.container.CampaignWithBuilder
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.utils.TimeConvertUtils

class CampaignRfOptionsHandlerTest {

    companion object {
        const val SHARD = 3
    }

    private lateinit var rfOptionsExportByFilterEnabledProp: PpcProperty<Boolean>
    private lateinit var filterByClientIdsProp: PpcProperty<Set<Long>>
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport
    private lateinit var campaignRfOptionsHandler: CampaignRfOptionsHandler

    private lateinit var campaign: InternalAutobudgetCampaign
    private lateinit var campaignWithBuilder: CampaignWithBuilder

    @BeforeEach
    fun before() {
        rfOptionsExportByFilterEnabledProp = mock()
        filterByClientIdsProp = mock()
        ppcPropertiesSupport = mock {
            on { get(eq(PpcPropertyNames.BS_EXPORT_RF_OPTIONS_FILTER_INTERNAL_CAMPAIGN_ENABLED), any()) }
                .thenReturn(rfOptionsExportByFilterEnabledProp)

            on { get(eq(PpcPropertyNames.BS_EXPORT_RF_OPTIONS_FILTER_BY_CLIENT_IDS), any()) }
                .thenReturn(filterByClientIdsProp)
        }

        whenever(rfOptionsExportByFilterEnabledProp.getOrDefault(any()))
            .thenReturn(false)
        whenever(filterByClientIdsProp.getOrDefault(any()))
            .thenReturn(emptySet())

        campaignRfOptionsHandler = CampaignRfOptionsHandler(ppcPropertiesSupport)

        campaign = TestCampaigns.defaultInternalAutobudgetCampaign()
            .withId(randomPositiveLong())
            .withClientId(randomPositiveLong())
        campaignWithBuilder = CampaignWithBuilder(
            campaign,
            Campaign.newBuilder()
        )
    }

    @Test
    fun withFullRfOptions() {
        campaign.impressionRateCount = randomPositiveInt()
        campaign.impressionRateIntervalDays = randomPositiveInt()
        campaign.maxClicksCount = randomPositiveInt()
        campaign.maxClicksPeriod = randomPositiveInt()
        campaign.maxStopsCount = randomPositiveInt()
        campaign.maxStopsPeriod = randomPositiveInt()
        campaignRfOptionsHandler.handle(SHARD, mapOf(campaign.id to campaignWithBuilder))

        val rfResetInSeconds = TimeConvertUtils.daysToSecond(campaign.impressionRateIntervalDays)
        assertThat(campaignWithBuilder.builder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxShowsCount(campaign.impressionRateCount)
                    .setMaxShowsPeriod(rfResetInSeconds)
                    .setStopShowsPeriod(rfResetInSeconds)
                    .setMaxClicksCount(campaign.maxClicksCount)
                    .setMaxClicksPeriod(campaign.maxClicksPeriod)
                    .setMaxStopsCount(campaign.maxStopsCount)
                    .setMaxStopsPeriod(campaign.maxStopsPeriod)
                    .build()
            )
    }

    @Test
    fun withRf_And_WithNullRfReset() {
        campaign.impressionRateCount = randomPositiveInt()
        campaign.impressionRateIntervalDays = null
        campaignRfOptionsHandler.handle(SHARD, mapOf(campaign.id to campaignWithBuilder))

        val rfResetInSeconds = TimeConvertUtils.daysToSecond(90)
        assertThat(campaignWithBuilder.builder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxShowsCount(campaign.impressionRateCount)
                    .setMaxShowsPeriod(rfResetInSeconds)
                    .setStopShowsPeriod(rfResetInSeconds)
                    .build()
            )
    }

    @Test
    fun withRf_And_WithZeroRfReset() {
        campaign.impressionRateCount = randomPositiveInt()
        campaign.impressionRateIntervalDays = 0
        campaignRfOptionsHandler.handle(SHARD, mapOf(campaign.id to campaignWithBuilder))

        val rfResetInSeconds = TimeConvertUtils.daysToSecond(90)
        assertThat(campaignWithBuilder.builder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxShowsCount(campaign.impressionRateCount)
                    .setMaxShowsPeriod(rfResetInSeconds)
                    .setStopShowsPeriod(rfResetInSeconds)
                    .build()
            )
    }

    @Test
    fun withMaxClicksCount_And_WithMaxClicksPeriod() {
        campaign.impressionRateCount = 0
        campaign.maxClicksCount = randomPositiveInt()
        campaign.maxClicksPeriod = randomPositiveInt()
        campaignRfOptionsHandler.handle(SHARD, mapOf(campaign.id to campaignWithBuilder))

        assertThat(campaignWithBuilder.builder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxClicksCount(campaign.maxClicksCount)
                    .setMaxClicksPeriod(campaign.maxClicksPeriod)
                    .build()
            )
    }

    @Test
    fun withMaxStopsCount_And_WithMaxStopsPeriod() {
        campaign.impressionRateCount = 0
        campaign.maxStopsCount = randomPositiveInt()
        campaign.maxStopsPeriod = randomPositiveInt()
        campaignRfOptionsHandler.handle(SHARD, mapOf(campaign.id to campaignWithBuilder))

        assertThat(campaignWithBuilder.builder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxStopsCount(campaign.maxStopsCount)
                    .setMaxStopsPeriod(campaign.maxStopsPeriod)
                    .build()
            )
    }

    @Test
    fun withoutRfOptions() {
        campaign.impressionRateCount = 0
        campaignRfOptionsHandler.handle(SHARD, mapOf(campaign.id to campaignWithBuilder))

        assertThat(campaignWithBuilder.builder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .build()
            )
    }
}
