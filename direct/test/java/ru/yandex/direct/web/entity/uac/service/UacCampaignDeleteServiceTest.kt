package ru.yandex.direct.web.entity.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.uac.model.CampaignContentStatus
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.service.UacCampaignService
import ru.yandex.direct.core.testing.data.TestCampaigns.activeMobileAppCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.validation.defect.CommonDefects.unableToDelete
import ru.yandex.direct.web.configuration.DirectWebTest
import java.math.BigDecimal

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignDeleteServiceTest  {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var uacCampaignService: UacCampaignService

    @Autowired
    private lateinit var uacCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun deleteCampaignTest() {
        val campaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo)

        val result = uacCampaignService.deleteCampaign(
            campaignInfo.uacCampaign.id,
            campaignInfo.uacDirectCampaign.directCampaignId,
            clientInfo.chiefUserInfo!!.uid,
            clientInfo.clientId!!
        )
        assertThat(result.isSuccessful).isTrue

        val gotYdbCampaign = uacCampaignRepository.getCampaign(campaignInfo.uacCampaign.id)
        val gotYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignInfo.uacDirectCampaign.id)
        val gotYdbContents = uacCampaignContentRepository.getCampaignContents(campaignInfo.uacCampaign.id)
        val gotCampaign = campaignTypedRepository.getSafely(
            campaignInfo.campaign.shard,
            listOf(campaignInfo.campaign.campaignId),
            CommonCampaign::class.java
        ).first()

        assertSoftly {
            it.assertThat(gotYdbCampaign).isNull()
            it.assertThat(gotYdbDirectCampaign).isNull()
            it.assertThat(gotYdbContents).isEmpty()
            it.assertThat(gotCampaign.statusEmpty).isTrue
        }
    }

    @Test
    fun deleteCampaignWithMoneyTest() {
        val campaignWithMoney = activeMobileAppCampaign(null, null)
            .withOrderId(0L)
            .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB).withSum(BigDecimal.valueOf(1000L)))

        val campaignInfo = uacCampaignSteps.createMobileAppCampaign(clientInfo, campaignWithMoney)

        val result = uacCampaignService.deleteCampaign(
            campaignInfo.uacCampaign.id,
            campaignInfo.uacDirectCampaign.directCampaignId,
            clientInfo.chiefUserInfo!!.uid,
            clientInfo.clientId!!
        )

        val gotYdbCampaign = uacCampaignRepository.getCampaign(campaignInfo.uacCampaign.id)
        val gotYdbDirectCampaign = uacDirectCampaignRepository.getDirectCampaignById(campaignInfo.uacDirectCampaign.id)
        val gotYdbContents = uacCampaignContentRepository.getCampaignContents(campaignInfo.uacCampaign.id)
            .filter { it.status != CampaignContentStatus.DELETED }
        val gotCampaign = campaignTypedRepository.getSafely(
            campaignInfo.campaign.shard,
            listOf(campaignInfo.campaign.campaignId),
            CommonCampaign::class.java
        )[0]

        assertSoftly {
            it.assertThat(result.isSuccessful).isFalse
            it.assertThat(result.validationResult?.flattenErrors()).hasSize(1)
            it.assertThat(result.validationResult?.flattenErrors()!![0].defect).isEqualTo(unableToDelete())
            it.assertThat(gotYdbCampaign).isEqualTo(campaignInfo.uacCampaign)
            it.assertThat(gotYdbDirectCampaign).isEqualTo(campaignInfo.uacDirectCampaign)
            it.assertThat(gotYdbContents).containsOnlyElementsOf(campaignInfo.contents)
            it.assertThat(gotCampaign.statusEmpty).isFalse
        }
    }
}
