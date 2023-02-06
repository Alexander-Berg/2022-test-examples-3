package ru.yandex.direct.grid.processing.service.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.grid.model.campaign.GdCampaignType
import ru.yandex.direct.grid.processing.service.client.ClientDataService.getDefaultCampaignNotification

class GetDefaultCampaignNotificationTest {

    private lateinit var operator: User
    private lateinit var client: User

    @Before
    fun initTestData() {
        operator = TestUsers.generateNewUser()
        client = TestUsers.generateNewUser()
    }


    @Test
    fun checkGetDefaultCampaignNotification_ForInternalCampaign() {
        val result = getDefaultCampaignNotification(operator, client, false, GdCampaignType.INTERNAL_AUTOBUDGET)

        assertThat(result.emailSettings.email)
                .isEqualTo(CampaignConstants.INTERNAL_CAMPAIGN_EMAIL)
        assertThat(result.emailSettings.warningBalance)
                .isEqualTo(CampaignConstants.DEFAULT_INTERNAL_CAMPAIGN_WARNING_BALANCE)
    }

}
