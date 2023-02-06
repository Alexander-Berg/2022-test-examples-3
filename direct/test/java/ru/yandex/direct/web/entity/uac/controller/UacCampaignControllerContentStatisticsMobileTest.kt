package ru.yandex.direct.web.entity.uac.controller

import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.createImageCampaignContent
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignControllerYdbContentContentStatisticsMobileTest : UacCampaignControllerContentStatisticsMobileTestBase() {

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Before
    fun ydbBefore() {
        val uacCampaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo)
        uacCampaignId = uacCampaignInfo.campaign.campaignId

        val uacImageContent = createDefaultImageContent()
        val uacVideoContent = createDefaultVideoContent()
        uacYdbContentRepository.saveContents(listOf(uacImageContent, uacVideoContent))

        val uacImageCampaignContent = createImageCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            contentId = uacImageContent.id,
        )
        val uacImageCampaignContent2 = createImageCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            contentId = uacImageContent.id,
        )
        val uacImageCampaignContent3 = createImageCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            contentId = uacImageContent.id,
        )
        val uacVideoCampaignContent = createCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            contentId = uacVideoContent.id,
            type = MediaType.VIDEO,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacImageCampaignContent, uacImageCampaignContent2,
            uacImageCampaignContent3, uacVideoCampaignContent))

        imageAssetId = uacImageCampaignContent.id
        imageAssetId2 = uacImageCampaignContent2.id
        imageAssetId3 = uacImageCampaignContent3.id
        videoAssetId = uacVideoCampaignContent.id

        uacCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaignInfo.uacCampaign.id)
    }
}
