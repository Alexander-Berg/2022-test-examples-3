package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.rbac.RbacRole

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithFavoriteTest : BaseCopyCampaignTest() {

    private lateinit var representative: UserInfo

    @Before
    fun before() {
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        client = steps.clientSteps().createDefaultClient()
        targetClient = steps.clientSteps().createDefaultClient()
        representative = steps.userSteps().createRepresentative(client)
    }

    @Test
    fun `favorite uids are copied to same client`() {
        val favoriteForUids: Set<Long> = setOf(client.uid, representative.uid)
        val campaign: TextCampaignInfo = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withFavoriteForUids(favoriteForUids))

        val copiedCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.favoriteForUids).isEqualTo(favoriteForUids)
    }

    @Test
    fun `favorite uids are reset when copying between clients`() {
        val campaign: TextCampaignInfo = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withFavoriteForUids(setOf(client.uid, representative.uid)))

        val copiedCampaign: TextCampaign = copyValidCampaignBetweenClients(campaign)

        assertThat(copiedCampaign.favoriteForUids).isNull()
    }

}
