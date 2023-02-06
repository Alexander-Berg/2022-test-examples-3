package ru.yandex.direct.core.copyentity.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.common.testing.assertThatKt
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.DialogInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.rbac.RbacRole

@CoreTest
class CopyCampaignWithDialogTest : BaseCopyCampaignTest() {

    private lateinit var dialogInfo: DialogInfo
    private lateinit var campaignWithDialog: TextCampaignInfo

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        targetClient = steps.clientSteps().createDefaultClient()
        dialogInfo = steps.dialogSteps().createDefaultDialog(client, "skillId1")
        campaignWithDialog = steps.textCampaignSteps()
            .createCampaign(client, fullTextCampaign().withClientDialogId(dialogInfo.dialog.id))
    }

    @Test
    fun `copy campaign with dialog same client`() {
        val copiedCampaign = copyValidCampaign(campaignWithDialog)
        assertThat(copiedCampaign.clientDialogId).isEqualTo(dialogInfo.dialog.id)
    }

    @Test
    fun `copy campaign with dialog different client`() {
        val copiedCampaign = copyValidCampaignBetweenClients(campaignWithDialog)
        assertThatKt(copiedCampaign.clientDialogId).isNull()
    }
}
