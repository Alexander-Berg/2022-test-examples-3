package ru.yandex.direct.core.entity.campaign.service.type.add

import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import ru.yandex.direct.core.entity.campaign.PlacementTypesChangeLogger
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPlacementTypes
import ru.yandex.direct.core.entity.campaign.model.PlacementType.ADV_GALLERY
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.dbutil.model.ClientId

private const val SHARD = 11
private const val OPERATOR_UID = 2892382L
private val CLIENT_ID: ClientId = ClientId.fromLong(4342323L)
private const val CAMPAIGN_ID = 643434343L

class CampaignWithPlacementTypesAddOperationSupportTest {
    private lateinit var placementTypesChangeLogger: PlacementTypesChangeLogger
    private lateinit var support: CampaignWithPlacementTypesAddOperationSupport
    private lateinit var campaign: CampaignWithPlacementTypes
    private lateinit var parameters: RestrictedCampaignsAddOperationContainer

    @Before
    fun before() {
        placementTypesChangeLogger = mock()
        support = CampaignWithPlacementTypesAddOperationSupport(placementTypesChangeLogger)
        parameters = RestrictedCampaignsAddOperationContainer.create(
            SHARD, OPERATOR_UID, CLIENT_ID, OPERATOR_UID, OPERATOR_UID)
    }

    @Test
    fun changePlacementTypesChange_checkLog() {
        campaign = TestCampaigns.defaultDynamicCampaign()
            .withId(CAMPAIGN_ID)
            .withPlacementTypes(setOf(ADV_GALLERY))

        support.afterExecution(parameters, listOf(campaign))

        Mockito.verify(placementTypesChangeLogger, times(1))
            .log(OPERATOR_UID, CAMPAIGN_ID, null, setOf(ADV_GALLERY))
    }
}
