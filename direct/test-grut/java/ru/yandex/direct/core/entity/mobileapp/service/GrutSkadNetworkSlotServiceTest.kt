package ru.yandex.direct.core.entity.mobileapp.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.grut.api.CampaignGrutModel
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.api.UpdatedObject
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.grut.objects.proto.CampaignV2
import ru.yandex.grut.objects.proto.CampaignV2.TCampaignV2Spec.TSkAdNetwork
import ru.yandex.grut.objects.proto.client.Schema

@GrutCoreTest
@ExtendWith(SpringExtension::class)
class GrutSkadNetworkSlotServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSkadNetworkSlotService: GrutSkadNetworkSlotService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    private lateinit var clientInfo: ClientInfo

    @BeforeEach
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
    }

    @Test
    fun setSlotsForCampaigns_NoCampaignInGrutTest() {
        val campaignId = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo).campaignId
        assertThatCode {
            grutSkadNetworkSlotService.setSlotForCampaign(
                campaignId,
                "bundle1",
                1
            )
        }.doesNotThrowAnyException()
        val gotCampaign = grutApiService.campaignGrutDao.getCampaignByDirectId(campaignId)
        assertThat(gotCampaign).isNull()
    }

    @Test
    fun setSlotForCampaignTest() {
        val campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo)
        val campaignId = campaign.campaignId

        val campaignFromMysql = campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignId))[0]
        grutApiService.campaignGrutDao.createOrUpdateCampaign(
          CampaignGrutModel(campaign = campaignFromMysql as CommonCampaign, orderType = 1)
        )
        grutSkadNetworkSlotService.setSlotForCampaign(campaignId, "bundle1", 1)
        val gotCampaign = grutApiService.campaignGrutDao.getCampaignByDirectId(campaignId)

        assertThat(gotCampaign!!.spec.skadNetwork.bundleId).isEqualTo("bundle1")
        assertThat(gotCampaign.spec.skadNetwork.slot).isEqualTo(1)
        // Проверка, что другие поля не сбросились
        assertThat(gotCampaign.spec.name).isEqualTo(campaign.campaign.name)
    }

    @Test
    fun freeSlotForCampaigns_NoCampaignInGrutTest() {
        val campaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).campaignId
        assertThatCode { grutSkadNetworkSlotService.freeSlotForCampaigns(listOf(campaignId)) }.doesNotThrowAnyException()
    }

    @Test
    fun freeSlotForCampaignsTest() {
        val campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo)
        val campaignId = campaign.campaignId
        val campaignFromMysql = campaignTypedRepository.getTyped(clientInfo.shard, listOf(campaignId))[0]
        grutApiService.campaignGrutDao.createOrUpdateCampaign(
            CampaignGrutModel(campaign = campaignFromMysql as CommonCampaign, orderType = 1)
        )
        grutApiService.campaignGrutDao.updateCampaigns(
            listOf(
                UpdatedObject(
                    meta = Schema.TCampaignV2Meta.newBuilder().setDirectId(campaignId).build().toByteString(),
                    spec = CampaignV2.TCampaignV2Spec.newBuilder()
                        .setSkadNetwork(TSkAdNetwork.newBuilder().setBundleId("bundle_test").setSlot(2).build()).build()
                        .toByteString(),
                    setPaths = listOf("/spec/skad_network")
                )
            )
        )
        grutSkadNetworkSlotService.freeSlotForCampaigns(listOf(campaignId))
        val campaignFromGrut = grutApiService.campaignGrutDao.getCampaignByDirectId(campaignId)


        assertThat(campaignFromGrut!!.spec.skadNetwork.bundleId).isEqualTo("bundle_test")
        assertThat(campaignFromGrut.spec.skadNetwork.slot).isEqualTo(0)
        // Проверка, что другие поля не сбросились
        assertThat(campaignFromGrut.spec.name).isEqualTo(campaign.campaign.name)
    }
}
