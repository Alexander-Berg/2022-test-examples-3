package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.utils

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject

internal class BsExportCampaignOrderIdLoaderTest {

    private val bsOrderIdCalculator = mock<BsOrderIdCalculator>()
    private val campaignOrderIdLoader = BsExportCampaignOrderIdLoader(bsOrderIdCalculator)

    @Test
    fun `order id is calculated if equals to 0`() {
        val cid = 56L
        val orderId = 13L
        whenever(bsOrderIdCalculator.calculateOrderIdIfNotExist(1, listOf(cid))) doReturn mapOf(cid to orderId)
        val logicObject = BsExportCampaignObject.Builder()
            .setCid(cid)
            .setOrderId(0)
            .build()
        val orderIdsByCid = campaignOrderIdLoader.getOrderIdForExistingCampaigns(1, listOf(cid), listOf(logicObject))
        assertThat(orderIdsByCid).isEqualTo(mapOf(cid to orderId))
    }
}
