package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.DeleteInfo
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.buildCustomCondition
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.buildExperimentSegment
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.buildMultiplierAtom
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.container.MultiplierInfo
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.MultiplierHandler.MULTIPLIER_COEF

class AbSegmentMultiplierHandlerTest {

    val handler = AbSegmentMultiplierHandler()

    @Test
    fun `inaccessible adjustments are ignored`() {
        val bidModifier = BidModifierABSegment().apply {
            adGroupId = null
            campaignId = 12
            id = 13
            enabled = true
            abSegmentAdjustments = listOf(
                BidModifierABSegmentAdjustment().apply {
                    abSegmentRetargetingConditionId = 14
                    accessible = true
                    sectionId = 15
                    segmentId = 16
                    percent = 123
                },
                BidModifierABSegmentAdjustment().apply {
                    abSegmentRetargetingConditionId = 14
                    accessible = false
                    sectionId = 17
                    segmentId = 18
                    percent = 123
                },
            )
        }

        val expectedMultiplierInfo = MultiplierInfo(
            MultiplierType.AB_SEGMENT,
            bidModifier.campaignId,
            bidModifier.adGroupId,
            true,
            listOf(
                buildMultiplierAtom {
                    multiplier = 123 * MULTIPLIER_COEF
                    buildCustomCondition {
                        buildExperimentSegment {
                            dimensionID = 15
                            segmentID = 16
                        }
                    }
                }
            )
        )
        val result = handler.handle(1, listOf(bidModifier))
        assertThat(result.deleteInfos).isEmpty()
        assertThat(result.multiplierInfos)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(expectedMultiplierInfo)
    }

    @Test
    fun `multiplier with only inaccessible adjustments will be deleted`() {
        val bidModifier = BidModifierABSegment().apply {
            adGroupId = null
            campaignId = 12
            id = 13
            enabled = true
            abSegmentAdjustments = listOf(
                BidModifierABSegmentAdjustment().apply {
                    abSegmentRetargetingConditionId = 14
                    accessible = false
                    sectionId = 15
                    segmentId = 16
                    percent = 123
                }
            )
        }

        val result = handler.handle(1, listOf(bidModifier))
        assertThat(result.deleteInfos)
            .containsExactly(
                DeleteInfo(MultiplierType.AB_SEGMENT, bidModifier.campaignId, bidModifier.adGroupId)
            )
        assertThat(result.multiplierInfos).isEmpty()
    }
}
