package ru.yandex.direct.core.copyentity.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator.ORDER_ID_OFFSET
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
class CopyCampaignWithPackageStrategyTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `copying campaign with package strategy creates new strategy`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(client)

        val copiedCampaign: TextCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.strategyId).isEqualTo(copiedCampaign.id + ORDER_ID_OFFSET)
    }
}
