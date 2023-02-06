package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.adgroup.AdGroup
import ru.yandex.adv.direct.adgroup.OptionalInternalLevel
import ru.yandex.adv.direct.showcondition.RfOptions
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.utils.TimeConvertUtils

internal class AdGroupInternalAdFieldsHandlerTest {

    companion object {
        const val SHARD = 2
    }

    private lateinit var rfOptionsExportByFilterEnabledProp: PpcProperty<Boolean>
    private lateinit var filterByCampaignIdsProp: PpcProperty<Set<Long>>
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport
    private lateinit var adGroupInternalAdFieldsHandler: AdGroupInternalAdFieldsHandler

    private lateinit var adGroup: InternalAdGroup
    private lateinit var adGroupWithBuilder: AdGroupWithBuilder

    @BeforeEach
    fun before() {
        rfOptionsExportByFilterEnabledProp = mock()
        filterByCampaignIdsProp = mock()
        ppcPropertiesSupport = mock {
            on { get(eq(PpcPropertyNames.BS_EXPORT_RF_OPTIONS_FILTER_INTERNAL_AD_GROUPS_ENABLED), any()) }
                .thenReturn(rfOptionsExportByFilterEnabledProp)

            on { get(eq(PpcPropertyNames.BS_EXPORT_RF_OPTIONS_FILTER_BY_CAMPAIGN_IDS), any()) }
                .thenReturn(filterByCampaignIdsProp)
        }

        whenever(rfOptionsExportByFilterEnabledProp.getOrDefault(any()))
            .thenReturn(false)
        whenever(filterByCampaignIdsProp.getOrDefault(any()))
            .thenReturn(emptySet())

        adGroupInternalAdFieldsHandler = AdGroupInternalAdFieldsHandler(ppcPropertiesSupport)

        adGroup = TestGroups.internalAdGroup(randomPositiveLong(), randomPositiveInt().toLong())
            .withId(randomPositiveLong())
            .withType(AdGroupType.INTERNAL)
        adGroupWithBuilder = AdGroupWithBuilder(
            adGroup,
            AdGroup.newBuilder().setAdGroupId(adGroup.campaignId)
        )
    }

    @Test
    fun withInternalLevel() {
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "internalLevel",
                OptionalInternalLevel.newBuilder().setValue(adGroup.level.toInt()).build()
            )
    }

    @Test
    fun withoutInternalLevel() {
        adGroup.level = null
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "internalLevel",
                OptionalInternalLevel.newBuilder().build()
            )
    }

    @Test
    fun withFullRfOptions() {
        adGroup.rf = randomPositiveInt()
        adGroup.rfReset = randomPositiveInt()
        adGroup.maxClicksCount = randomPositiveInt()
        adGroup.maxClicksPeriod = randomPositiveInt()
        adGroup.maxStopsCount = randomPositiveInt()
        adGroup.maxStopsPeriod = randomPositiveInt()
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        val rfResetInSeconds = TimeConvertUtils.daysToSecond(adGroup.rfReset)
        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxShowsCount(adGroup.rf)
                    .setMaxShowsPeriod(rfResetInSeconds)
                    .setStopShowsPeriod(rfResetInSeconds)
                    .setMaxClicksCount(adGroup.maxClicksCount)
                    .setMaxClicksPeriod(adGroup.maxClicksPeriod)
                    .setMaxStopsCount(adGroup.maxStopsCount)
                    .setMaxStopsPeriod(adGroup.maxStopsPeriod)
                    .build()
            )
    }

    @Test
    fun withRf_And_WithNullRfReset() {
        adGroup.rf = randomPositiveInt()
        adGroup.rfReset = null
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        val rfResetInSeconds = TimeConvertUtils.daysToSecond(90)
        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxShowsCount(adGroup.rf)
                    .setMaxShowsPeriod(rfResetInSeconds)
                    .setStopShowsPeriod(rfResetInSeconds)
                    .build()
            )
    }

    @Test
    fun withRf_And_WithZeroRfReset() {
        adGroup.rf = randomPositiveInt()
        adGroup.rfReset = 0
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        val rfResetInSeconds = TimeConvertUtils.daysToSecond(90)
        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxShowsCount(adGroup.rf)
                    .setMaxShowsPeriod(rfResetInSeconds)
                    .setStopShowsPeriod(rfResetInSeconds)
                    .build()
            )
    }

    @Test
    fun withMaxClicksCount_And_WithMaxClicksPeriod() {
        adGroup.maxClicksCount = randomPositiveInt()
        adGroup.maxClicksPeriod = randomPositiveInt()
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxClicksCount(adGroup.maxClicksCount)
                    .setMaxClicksPeriod(adGroup.maxClicksPeriod)
                    .build()
            )
    }

    @Test
    fun withMaxStopsCount_And_WithMaxStopsPeriod() {
        adGroup.maxStopsCount = randomPositiveInt()
        adGroup.maxStopsPeriod = randomPositiveInt()
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .setMaxStopsCount(adGroup.maxStopsCount)
                    .setMaxStopsPeriod(adGroup.maxStopsPeriod)
                    .build()
            )
    }

    @Test
    fun withoutRfOptions() {
        adGroup.rf = 0
        adGroupInternalAdFieldsHandler.handle(SHARD, mapOf(adGroup.id to adGroupWithBuilder))

        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue(
                "rfOptions",
                RfOptions.newBuilder()
                    .build()
            )
    }
}
