package ru.yandex.direct.web.entity.uac.controller

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.createDefaultHtml5Content
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.model.DirectCampaignStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignTargetStatusControllerTest : UacCampaignTargetStatusControllerTestBase() {

    @Autowired
    private lateinit var uacCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    override fun createAsset(mediaType: ru.yandex.direct.core.entity.uac.model.MediaType): String {
        val content = when (mediaType) {
            ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE -> createDefaultImageContent()
            ru.yandex.direct.core.entity.uac.model.MediaType.HTML5 -> createDefaultHtml5Content()
            else -> createDefaultVideoContent()
        }
        uacYdbContentRepository.saveContents(listOf(content))
        return content.id
    }

    override fun getCampaign(campaignId: String): UacYdbCampaign? {
        return uacCampaignRepository.getCampaign(campaignId)
    }

    override fun getDirectCampaignId(uacCampaignId: String): Long {
        val uacYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(uacCampaignId)
        return uacYdbDirectCampaign!!.directCampaignId
    }

    override fun getDirectCampaignStatus(campaign: UacYdbCampaign): DirectCampaignStatus? {
        val uacYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaign.id)
        return uacYdbDirectCampaign?.status
    }
}
