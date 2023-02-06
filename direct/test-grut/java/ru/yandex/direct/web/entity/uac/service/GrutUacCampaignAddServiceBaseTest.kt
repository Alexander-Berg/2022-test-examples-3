package ru.yandex.direct.web.entity.uac.service

import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName.UAC_MULTIPLE_AD_GROUPS_ENABLED

open class GrutUacCampaignAddServiceBaseTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    lateinit var grutUacCampaignAddService: GrutUacCampaignAddService

    @Autowired
    lateinit var grutSteps: GrutSteps

    @Autowired
    lateinit var grutApiService: GrutApiService

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var campaignSteps: CampaignSteps

    @Autowired
    lateinit var uacModifyCampaignDataContainerFactory: UacModifyCampaignDataContainerFactory

    lateinit var clientInfo: ClientInfo
    lateinit var operator: User
    lateinit var subjectUser: User
    lateinit var directCampaign: Campaign

    operator fun <T> Array<T>.component6(): T = get(5)

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        grutSteps.createClient(clientInfo)

        steps.featureSteps().addClientFeature(clientInfo.clientId, UAC_MULTIPLE_AD_GROUPS_ENABLED, true)

        directCampaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(TestCampaigns.averageCpaStrategy())
            .withSource(CampaignSource.UAC)
        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)

        operator = campaignInfo.clientInfo?.chiefUserInfo?.user!!
        subjectUser = campaignInfo.clientInfo?.chiefUserInfo?.user!!
    }

    fun createMediaAssets(
        imagesHash: List<String> = emptyList(),
        videoSourceUrl: List<String> = emptyList(),
        html5SourceUrl: List<String> = emptyList(),
    ): List<String> {
        val imageAssetIds = imagesHash
            .map {
                grutSteps.createDefaultImageAsset(
                    clientId = subjectUser.clientId,
                    imageHash = it,
                    sourceUrl = it,
                )
            }
        val videoAssetIds = videoSourceUrl
            .map {
                grutSteps.createDefaultVideoAsset(
                    subjectUser.clientId,
                    sourceUrl = it,
                    mdsUrl = it,
                )
            }
        val html5AssetIds = html5SourceUrl
            .map {
                grutSteps.createDefaultHtml5Asset(
                    subjectUser.clientId,
                    sourceUrl = it,
                    mdsUrl = it,
                )
            }
        return imageAssetIds + videoAssetIds + html5AssetIds
    }
}
