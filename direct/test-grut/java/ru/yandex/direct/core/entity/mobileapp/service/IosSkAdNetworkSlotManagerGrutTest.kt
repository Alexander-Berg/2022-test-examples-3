package ru.yandex.direct.core.entity.mobileapp.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.mobileapp.repository.IosSkAdNetworkSlotRepository
import ru.yandex.direct.core.grut.api.CampaignGrutModel
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import java.util.UUID

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class IosSkAdNetworkSlotManagerGrutTest {
    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var mobileAppService: MobileAppService

    @Autowired
    private lateinit var iosSkAdNetworkSlotRepository: IosSkAdNetworkSlotRepository

    @Autowired
    private lateinit var grutSkadNetworkSlotService: GrutSkadNetworkSlotService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    private lateinit var slotManager: IosSkAdNetworkSlotManager

    private lateinit var config: SkAdNetworkSlotsConfig
    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        grutApiService.clientGrutDao.createOrUpdateClients(
            listOf(
                ClientGrutModel(
                    clientInfo.client!!,
                    listOf()
                )
            )
        )

        config = SkAdNetworkSlotsConfig(1, 1)
        slotManager = IosSkAdNetworkSlotManager(
            dslContextProvider, mobileAppService, { config }, iosSkAdNetworkSlotRepository, grutSkadNetworkSlotService
        )
    }

    @Test
    fun allocateCampaignSlot_One() {
        val bundleId = createBundleId()
        val campaignInfo = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo)
        val campaignFromMysql = campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignInfo.campaignId))[0]
        grutApiService.campaignGrutDao.createOrUpdateCampaign(
            CampaignGrutModel(campaign = campaignFromMysql as CommonCampaign, orderType = 1)
        )
        val allocateCampaignSlot = slotManager.allocateCampaignSlot(bundleId, campaignInfo.campaignId, true)

        val gotCampaign = grutApiService.campaignGrutDao.getCampaignByDirectId(campaignInfo.campaignId)
        assertThat(allocateCampaignSlot).isEqualTo(1)
        assertThat(gotCampaign!!.spec.skadNetwork.slot).isEqualTo(1)
    }

    private fun createBundleId(): String = "ru.yandex.super.puper." + UUID.randomUUID().toString()
}
