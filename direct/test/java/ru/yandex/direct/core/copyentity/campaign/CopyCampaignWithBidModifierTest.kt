package ru.yandex.direct.core.copyentity.campaign

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBidModifiers
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT
import ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierGeo
import ru.yandex.direct.regions.Region
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import java.time.LocalDateTime

@CoreTest
class CopyCampaignWithBidModifierTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `copying geo bid modifiers preserves hidden ajdustment`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(client)
        steps.bidModifierSteps().createCampaignBidModifier(
            createDefaultBidModifierGeo(null)
                .withRegionalAdjustments(listOf(
                    BidModifierRegionalAdjustment()
                        .withRegionId(Region.RUSSIA_REGION_ID)
                        .withHidden(false)
                        .withPercent(DEFAULT_PERCENT)
                        .withLastChange(LocalDateTime.now()),
                )),
            campaign
        )

        // check created campaign contains hidden regional adjustment
        val savedCampaign: CampaignWithBidModifiers = getCampaign(campaign.campaignId)
        val adjustments: List<BidModifierRegionalAdjustment> = savedCampaign.bidModifiers
            .filterIsInstance<BidModifierGeo>()
            .flatMap { it.regionalAdjustments }
        assumeThat {
            it.assertThat(adjustments)
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration()
                    .apply { setIgnoreAllExpectedNullFields(true) })
                .containsExactlyInAnyOrder(
                    BidModifierRegionalAdjustment()
                        .withRegionId(Region.RUSSIA_REGION_ID)
                        .withHidden(false),
                    BidModifierRegionalAdjustment()
                        .withRegionId(Region.CRIMEA_REGION_ID)
                        .withHidden(true),
                )
        }

        // copy campaign
        val copiedCampaign = copyValidCampaign(campaign)

        // check copied regional adjustment is still hidden
        val copiedAdjustments: List<BidModifierRegionalAdjustment> = copiedCampaign.bidModifiers
            .filterIsInstance<BidModifierGeo>()
            .flatMap { it.regionalAdjustments }
        assertThat(copiedAdjustments)
            .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration()
                .apply { setIgnoreAllExpectedNullFields(true) })
            .containsExactlyInAnyOrder(
                BidModifierRegionalAdjustment()
                    .withRegionId(Region.RUSSIA_REGION_ID)
                    .withHidden(false),
                BidModifierRegionalAdjustment()
                    .withRegionId(Region.CRIMEA_REGION_ID)
                    .withHidden(true),
            )
    }
}
