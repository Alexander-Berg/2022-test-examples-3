package ru.yandex.direct.logicprocessor.processors.bsexport.campaign.handler

import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.BsExportCampaignObject
import ru.yandex.direct.ess.logicobjects.bsexport.campaing.CampaignResourceType

internal class CampaignDeleteHandlerTest {
    private val campaignTypedRepository = mock<CampaignTypedRepository>()
    private val handler = CampaignDeleteHandler(campaignTypedRepository)

    @Test
    fun `delete existing campaign`() {
        whenever(campaignTypedRepository.getSafely(
            eq(1),
            argWhere<Collection<Long>> { CID in it },
            eq(CommonCampaign::class.java)),
        ) doReturn listOf(DynamicCampaign().withId(CID))

        val deletedCampaignIds = handler.getDeletedCampaignIds(1, listOf(LOGIC_OBJECT))
        assertThat(deletedCampaignIds).isEmpty()
    }

    @Test
    fun `delete non-existing campaign`() {
        whenever(campaignTypedRepository.getSafely(1, listOf(CID), CommonCampaign::class.java)) doReturn
            listOf()

        val deletedCampaignIds = handler.getDeletedCampaignIds(1, listOf(LOGIC_OBJECT))
        assertThat(deletedCampaignIds).containsExactly(CID)
    }

    companion object {
        private const val CID = 1234L
        private val LOGIC_OBJECT = BsExportCampaignObject.Builder()
            .setCid(CID)
            .setCampaignResourceType(CampaignResourceType.CAMPAIGN_DELETE)
            .build()
    }
}
