package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.adv.direct.expression.multipler.type.MultiplierTypeEnum
import ru.yandex.adv.direct.expression.operations.OperationEnum
import ru.yandex.adv.direct.multipliers.Multiplier
import ru.yandex.direct.core.bsexport.repository.BsExportMultipliersRepository
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType
import ru.yandex.direct.libs.timetarget.TimeTarget
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.container.MultiplierAndDeleteInfos
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.container.MultiplierInfo
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.TimeMultiplierHandler
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.WeatherMultiplierHandler

class BsExportMultipliersServiceCampaignAdGroupTest {
    companion object {
        val WEATHER_MULTIPLIER = buildMultiplierAtom {
            buildCondition {
                buildAnd {
                    buildOr {
                        keyword = KeywordEnum.Cloudness
                        operation = OperationEnum.Equal
                        value = "30"
                    }
                }
            }
            multiplier = 500
        }
    }

    object Campaign {
        const val ID = 100L
        const val ORDER_ID = 1234L
        const val HIERARCHICAL_MULTIPLIER_ID = 444L
        const val TIME_TARGET_COEF = "1IJnKLnMNOnPQRnST2IJnKLnMNOnPQRnST3IJnKLnMNOnPQRnST4IJnKLnMNOnPQRnST5IJnKLnMNOnPQRnST6IJnKLnMNOnPQRnST7IJnKLnMNOnPQRnST;p:o"
        val WEATHER_MODIFIER_KEY = BidModifierKey(ID, null, BidModifierType.WEATHER_MULTIPLIER)
        val WEATHER_MODIFIER = BidModifierWeather()
            .withCampaignId(ID)
            .withAdGroupId(null)
            .withType(BidModifierType.WEATHER_MULTIPLIER)
            .withEnabled(true)
            .withId(HIERARCHICAL_MULTIPLIER_ID)

        val TIME_TARGET_MULTIPLIER = buildMultiplierAtom {
            buildCondition {
                buildAnd {
                    buildOr {
                        keyword = KeywordEnum.TimetableSimple
                        operation = OperationEnum.TimeLike
                        value = "1BCDEFGHIJKLMNOPQRSTUVWX"
                    }
                }
            }
            multiplier = 500
        }
        val TIME_TARGET_MULTIPLIER_INFO = MultiplierInfo(
            MultiplierType.TIME_TARGET,
            ID,
            null,
            true,
            listOf(TIME_TARGET_MULTIPLIER),
        )

        val WEATHER_MULTIPLIER_INFO = MultiplierInfo(
            MultiplierType.WEATHER,
            ID,
            null,
            true,
            listOf(WEATHER_MULTIPLIER),
        )
    }

    object AdGroup {
        const val ID = 1000L
        const val HIERARCHICAL_MULTIPLIER_ID = 445L
        val WEATHER_MODIFIER_KEY = BidModifierKey(Campaign.ID, ID, BidModifierType.WEATHER_MULTIPLIER)
        val WEATHER_MODIFIER = BidModifierWeather()
            .withCampaignId(Campaign.ID)
            .withAdGroupId(ID)
            .withType(BidModifierType.WEATHER_MULTIPLIER)
            .withEnabled(true)
            .withId(Campaign.HIERARCHICAL_MULTIPLIER_ID)
        val WEATHER_MULTIPLIER_INFO = MultiplierInfo(
            MultiplierType.WEATHER,
            Campaign.ID,
            ID,
            true,
            listOf(WEATHER_MULTIPLIER),
        )
    }

    val bsExportMultipliersRepository = mock<BsExportMultipliersRepository> {
        on(it.getCampaignsTimeTargetWithoutAutobudget(1, listOf(Campaign.ID))) doReturn
            mapOf(Campaign.ID to TimeTarget.parseRawString(Campaign.TIME_TARGET_COEF))
    }

    val bidModifierRepository = mock<BidModifierRepository> {
        on(it.getBidModifierIdsByCampaignIds(1, listOf(Campaign.ID))) doReturn
            mapOf(Campaign.ID to listOf(Campaign.HIERARCHICAL_MULTIPLIER_ID))

        on(it.getBidModifierIdsByAdGroupIds(1, listOf(AdGroup.ID))) doReturn
            mapOf(AdGroup.ID to listOf(AdGroup.HIERARCHICAL_MULTIPLIER_ID))

        on(it.getBidModifierKeysByIds(1, listOf(Campaign.HIERARCHICAL_MULTIPLIER_ID))) doReturn
            mapOf(Campaign.HIERARCHICAL_MULTIPLIER_ID to Campaign.WEATHER_MODIFIER_KEY)

        on(it.getBidModifiersByKeys(1, setOf(Campaign.WEATHER_MODIFIER_KEY))) doReturn
            mapOf(Campaign.WEATHER_MODIFIER_KEY to Campaign.WEATHER_MODIFIER)

        on(it.getBidModifierKeysByIds(1, listOf(AdGroup.HIERARCHICAL_MULTIPLIER_ID))) doReturn
            mapOf(AdGroup.HIERARCHICAL_MULTIPLIER_ID to AdGroup.WEATHER_MODIFIER_KEY)

        on(it.getBidModifiersByKeys(1, setOf(AdGroup.WEATHER_MODIFIER_KEY))) doReturn
            mapOf(AdGroup.WEATHER_MODIFIER_KEY to AdGroup.WEATHER_MODIFIER)
    }

    val bsOrderIdCalculator = mock<BsOrderIdCalculator> {
        on(mock.calculateOrderIdIfNotExist(1, setOf(Campaign.ID))) doReturn
            mapOf(Campaign.ID to Campaign.ORDER_ID)
    }

    val weatherMultiplierHandler = spy(WeatherMultiplierHandler()) {
        doReturn(listOf(Campaign.WEATHER_MULTIPLIER_INFO)).whenever(it).convert(listOf(Campaign.WEATHER_MODIFIER))
        doReturn(listOf(AdGroup.WEATHER_MULTIPLIER_INFO)).whenever(it).convert(listOf(AdGroup.WEATHER_MODIFIER))
    }

    val timeMultiplierHandler = spy(TimeMultiplierHandler()) {
        val result = MultiplierAndDeleteInfos(
            listOf(Campaign.TIME_TARGET_MULTIPLIER_INFO),
            listOf(),
        )
        doReturn(result).whenever(it).handle(any(), any())
    }

    val bsExportMultipliersService = BsExportMultipliersService(
        mock(),
        bidModifierRepository,
        bsExportMultipliersRepository,
        mock(),
        bsOrderIdCalculator,
        listOf(weatherMultiplierHandler),
        timeMultiplierHandler,
    )

    @Test
    fun `multipliers by campaign`() {
        val expectedMultipliers = listOf(
            Campaign.TIME_TARGET_MULTIPLIER.toExpression2Format(MultiplierTypeEnum.Time),
            WEATHER_MULTIPLIER.toExpression2Format(MultiplierTypeEnum.Weather),
        )
        val actual = bsExportMultipliersService.getCampaignMultipliers(1, listOf(Campaign.ID))
        assertThat(actual).isEqualTo(mapOf(Campaign.ID to expectedMultipliers))
    }

    @Test
    fun `multipliers by ad group`() {
        val expectedMultipliers = listOf(
            WEATHER_MULTIPLIER.toExpression2Format(MultiplierTypeEnum.Weather),
        )
        val actual = bsExportMultipliersService.getAdGroupMultipliers(1, listOf(AdGroup.ID))
        assertThat(actual).isEqualTo(mapOf(AdGroup.ID to expectedMultipliers))
    }

    @Test
    fun `handler returns null where list was expected`() {
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val mockMultiplierInfo = MultiplierInfo(
            MultiplierType.WEATHER,
            Campaign.ID,
            AdGroup.ID,
            true,
            null,
        )
        doReturn(listOf(mockMultiplierInfo))
            .whenever(weatherMultiplierHandler)
            .convert(listOf(AdGroup.WEATHER_MODIFIER))

        val expectedMultipliers = listOf<Multiplier>()
        val actual = bsExportMultipliersService.getAdGroupMultipliers(1, listOf(AdGroup.ID))
        assertThat(actual).isEqualTo(mapOf(AdGroup.ID to expectedMultipliers))
    }

    @Test
    fun `disabled multipliers are ignored`() {
        val disabledMultiplierInfo = MultiplierInfo(
            MultiplierType.WEATHER,
            Campaign.ID,
            AdGroup.ID,
            false,
            listOf(WEATHER_MULTIPLIER),
        )

        doReturn(listOf(disabledMultiplierInfo))
            .whenever(weatherMultiplierHandler)
            .convert(listOf(AdGroup.WEATHER_MODIFIER))

        val expectedMultipliers = listOf<Multiplier>()
        val actual = bsExportMultipliersService.getAdGroupMultipliers(1, listOf(AdGroup.ID))
        assertThat(actual).isEqualTo(mapOf(AdGroup.ID to expectedMultipliers))
    }
}
