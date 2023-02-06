package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideoAdjustment
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.buildCustomCondition
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.buildMultiplierAtom

class AutoVideoDirectMultiplierHandlerTest {
    private val handler = AutoVideoDirectMultiplierHandler()

    @Test
    fun `resource is extracted correctly`() {
        val bidModifier = BidModifierVideo().apply {
            adGroupId = 12
            campaignId = 13
            enabled = true
            id = 14
            videoAdjustment = BidModifierVideoAdjustment()
                .withId(14)
                .withPercent(100)
        }

        val expectedMultiplier = buildMultiplierAtom {
            multiplier = 100 * MultiplierHandler.MULTIPLIER_COEF
            buildCustomCondition {
                autoVideoDirect = "AutoVideoDirect"
            }
        }
        val result = handler.convert(listOf(bidModifier))
        assertThat(result).hasSize(1)

        val info = result[0]
        assertThat(info.multiplierType).isEqualTo(MultiplierType.AUTO_VIDEO_DIRECT)
        assertThat(info.adGroupId).isEqualTo(bidModifier.adGroupId)
        assertThat(info.campaignId).isEqualTo(bidModifier.campaignId)
        assertThat(info.enabled).isEqualTo(bidModifier.enabled)
        assertThat(info.multipliers)
            .containsExactly(expectedMultiplier)
    }
}
