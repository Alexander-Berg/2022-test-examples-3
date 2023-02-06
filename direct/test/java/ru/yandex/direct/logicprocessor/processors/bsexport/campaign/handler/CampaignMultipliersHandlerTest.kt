package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.campaign.Campaign
import ru.yandex.adv.direct.campaign.OptionalMultipliers
import ru.yandex.adv.direct.multipliers.Multiplier
import ru.yandex.direct.core.bsexport.repository.BsExportMultipliersRepository
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.MultiplierInfo
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.RetargetingConditionInfo
import ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler.CampaignHandlerAssertions.assertCampaignHandledCorrectly
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.BsExportMultipliersService
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.buildMultiplier

class CampaignMultipliersHandlerTest {
    private val bsExportMultipliersRepository = mock<BsExportMultipliersRepository> {
        on(mock.getCampaignIdsByHierarchicalMultiplierIds(
            eq(SHARD),
            containsExactly(HIERARCHICAL_MULTIPLIER_ID),
        )) doReturn listOf(CAMPAIGN_ID)

        on(mock.getMultiplierIdsByRetargetingConditionIds(
            eq(SHARD),
            containsExactly(RETARGETING_CONDITION_ID),
        )) doReturn listOf(HIERARCHICAL_MULTIPLIER_ID)
    }

    private val bsExportMultipliersService = mock<BsExportMultipliersService> {
        on(mock.getCampaignMultipliers(
            eq(SHARD),
            containsExactly(CAMPAIGN_ID)
        )) doReturn mapOf(CAMPAIGN_ID to listOf(TIME_TARGET_MULTIPLIER, OTHER_MULTIPLIER))
    }

    private val handler = CampaignMultipliersHandler(
        bsExportMultipliersRepository,
        bsExportMultipliersService,
    )

    @Test
    fun `object with known hierarchical_multiplier_id is handled correctly`() {
        val obj = BsExportCampaignObject.Builder()
            .setHierarchicalMultiplierId(42)
            .setAdditionalInfo(MultiplierInfo(42))
            .build()

        assertThat(handler.getCampaignsIdsToLoad(1, listOf(obj)))
            .containsExactlyInAnyOrder(CAMPAIGN_ID)

        assertCampaignHandledCorrectly(
            handler,
            campaign = CAMPAIGN,
            expectedResource = OPTIONAL_MULTIPLIERS,
        )
    }

    @Test
    fun `object with unknown hierarchical_multiplier_id is handled correctly`() {
        val obj = BsExportCampaignObject.Builder()
            .setHierarchicalMultiplierId(15)
            .setAdditionalInfo(MultiplierInfo(15))
            .build()

        assertThat(handler.getCampaignsIdsToLoad(1, listOf(obj)))
            .isEmpty()
    }

    @Test
    fun `object with known retargeting_condition_id is handled correctly`() {
        val obj = BsExportCampaignObject.Builder()
            .setRetargetingConditionId(RETARGETING_CONDITION_ID)
            .setAdditionalInfo(RetargetingConditionInfo(RETARGETING_CONDITION_ID))
            .build()

        assertThat(handler.getCampaignsIdsToLoad(1, listOf(obj)))
            .containsExactly(CAMPAIGN_ID)

        assertCampaignHandledCorrectly(
            handler,
            campaign = CAMPAIGN,
            expectedProto = Campaign.newBuilder().setMultipliers(OPTIONAL_MULTIPLIERS).buildPartial(),
        )
    }

    @Test
    fun `object with unknown retargeting_condition_id is handled correctly`() {
        val obj = BsExportCampaignObject.Builder()
            .setRetargetingConditionId(10)
            .setAdditionalInfo(RetargetingConditionInfo(10))
            .build()

        assertThat(handler.getCampaignsIdsToLoad(1, listOf(obj)))
            .isEmpty()
    }

    @Test
    fun `object with no multipliers`() = assertCampaignHandledCorrectly(
        handler,
        campaign = DynamicCampaign().withId(13),
        expectedProto = Campaign.newBuilder()
            .setMultipliers(OptionalMultipliers.getDefaultInstance())
            .buildPartial()
    )

    companion object {
        const val SHARD = 1
        const val CAMPAIGN_ID = 41L
        const val HIERARCHICAL_MULTIPLIER_ID = 42L
        const val RETARGETING_CONDITION_ID = 43L
        val CAMPAIGN = DynamicCampaign().withId(CAMPAIGN_ID)
        val TIME_TARGET_MULTIPLIER = buildMultiplier {
            value = 0
        }
        val OTHER_MULTIPLIER = buildMultiplier {
            value = 1
        }
        val OPTIONAL_MULTIPLIERS = OptionalMultipliers
            .newBuilder()
            .addAllMultiplier(listOf(TIME_TARGET_MULTIPLIER, OTHER_MULTIPLIER))
            .build()

        private fun <T> containsExactly(vararg elements: T): Collection<T> =
            argWhere { it.toList() == listOf(*elements) }

        private fun optionalMultipliersOf(vararg multipliers: Multiplier) =
            OptionalMultipliers.newBuilder().addAllMultiplier(multipliers.asIterable()).build()

        fun assertCampaignHandledCorrectly(
            handler: CampaignMultipliersHandler,
            campaign: BaseCampaign,
            expectedResource: OptionalMultipliers,
        ) {
            val expectedProto = Campaign.newBuilder()
                .setMultipliers(expectedResource)
                .buildPartial()
            assertCampaignHandledCorrectly(handler, campaign, expectedProto)
        }
    }
}
