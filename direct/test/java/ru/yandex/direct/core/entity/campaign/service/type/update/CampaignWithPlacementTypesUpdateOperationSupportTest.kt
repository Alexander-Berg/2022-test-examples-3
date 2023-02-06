package ru.yandex.direct.core.entity.campaign.service.type.update

import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import ru.yandex.direct.core.entity.campaign.PlacementTypesChangeLogger
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPlacementTypes
import ru.yandex.direct.core.entity.campaign.model.PlacementType.ADV_GALLERY
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges

private const val SHARD = 11
private const val OPERATOR_UID = 2892382L
private val CLIENT_ID: ClientId = ClientId.fromLong(4342323L)
private const val CAMPAIGN_ID = 643434343L

class CampaignWithPlacementTypesUpdateOperationSupportTest {
    private lateinit var placementTypesChangeLogger: PlacementTypesChangeLogger
    private lateinit var support: CampaignWithPlacementTypesUpdateOperationSupport
    private lateinit var campaign: CampaignWithPlacementTypes
    private lateinit var updateParameters: RestrictedCampaignsUpdateOperationContainer

    @Before
    fun before() {
        placementTypesChangeLogger = mock()
        support = CampaignWithPlacementTypesUpdateOperationSupport(placementTypesChangeLogger)
        campaign = TestCampaigns.defaultDynamicCampaign().withId(CAMPAIGN_ID)
        updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
            SHARD, OPERATOR_UID, CLIENT_ID, OPERATOR_UID, OPERATOR_UID)
    }

    @Test
    fun noPlacementTypesChange_noLog() {
        val mc = ModelChanges(CAMPAIGN_ID, CampaignWithPlacementTypes::class.java)
        val ac = mc.applyTo(campaign)

        support.afterExecution(updateParameters, listOf(ac))

        verify(placementTypesChangeLogger, never()).log(anyLong(), anyLong(), any(), any())
    }

    @Test
    fun changePlacementTypesChange_checkLog() {
        val mc = ModelChanges(CAMPAIGN_ID, CampaignWithPlacementTypes::class.java)
            .process(setOf(ADV_GALLERY), CampaignWithPlacementTypes.PLACEMENT_TYPES)
        val ac = mc.applyTo(campaign)

        support.afterExecution(updateParameters, listOf(ac))

        verify(placementTypesChangeLogger, times(1))
            .log(OPERATOR_UID, CAMPAIGN_ID, setOf(), setOf(ADV_GALLERY))
    }
}
