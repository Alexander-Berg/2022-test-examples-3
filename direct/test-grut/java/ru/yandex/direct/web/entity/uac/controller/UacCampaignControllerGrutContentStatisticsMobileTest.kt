package ru.yandex.direct.web.entity.uac.controller

import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toUacYdbCampaign
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.web.configuration.GrutDirectWebTest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignControllerGrutContentContentStatisticsMobileTest : UacCampaignControllerContentStatisticsMobileTestBase() {

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var uacContentService: GrutUacContentService

    @Before
    fun grutBefore() {
        grutSteps.createClient(clientInfo)
        uacCampaignId = grutSteps.createMobileAppCampaign(clientInfo, createInDirect = true)

        imageAssetId = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        imageAssetId2 = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        imageAssetId3 = grutSteps.createDefaultImageAsset(clientInfo.clientId!!)
        videoAssetId = grutSteps.createDefaultVideoAsset(clientInfo.clientId!!)

        grutSteps.setAssetLinksToCampaign(uacCampaignId, listOf(imageAssetId, imageAssetId2, imageAssetId3, videoAssetId))

        val campaign = grutApiService.briefGrutApi.getBrief(uacCampaignId)!!.toUacYdbCampaign()
        uacCampaignContents = uacContentService.getCampaignContents(campaign)
    }


}
