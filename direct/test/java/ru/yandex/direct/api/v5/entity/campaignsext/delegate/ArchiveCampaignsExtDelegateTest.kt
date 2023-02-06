package ru.yandex.direct.api.v5.entity.campaignsext.delegate

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.yandex.direct.api.v5.campaignsext.ArchiveRequest
import com.yandex.direct.api.v5.general.IdsCriteria
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.context.ApiContext
import ru.yandex.direct.api.v5.context.ApiContextHolder
import ru.yandex.direct.api.v5.converter.ResultConverter
import ru.yandex.direct.api.v5.entity.GenericApiService
import ru.yandex.direct.api.v5.entity.campaignsext.validation.CampaignsExtArchiveRequestValidator
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.user.model.ApiUser
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.activeMcBannerCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode

@Api5Test
@RunWith(SpringRunner::class)
class ArchiveCampaignsExtDelegateTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var requestValidator: CampaignsExtArchiveRequestValidator

    @Autowired
    private lateinit var resultConverter: ResultConverter

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var featureService: FeatureService

    private lateinit var genericApiService: GenericApiService

    private lateinit var delegate: ArchiveCampaignsExtDelegate

    private lateinit var clientInfo: ClientInfo

    private var campaignId: Long = -1

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        val user = ApiUser()
            .withUid(clientInfo.uid)
            .withClientId(clientInfo.clientId)

        val auth = mock<ApiAuthenticationSource> { auth ->
            on(auth.operator) doReturn user
            on(auth.chiefSubclient) doReturn user
        }

        val apiContextHolder = mock<ApiContextHolder> { apiContextHolder ->
            on(apiContextHolder.get()) doReturn ApiContext()
        }

        genericApiService = GenericApiService(apiContextHolder, mock(), mock(), mock())

        delegate = ArchiveCampaignsExtDelegate(
            auth,
            campaignService,
            requestValidator,
            resultConverter,
            ppcPropertiesSupport,
            featureService,
        )
    }

    @Test
    fun `archive campaign with content promotion type operation success`() {
        campaignId = steps.campaignSteps()
            .createCampaign(
                activeContentPromotionCampaign(clientInfo.clientId, clientInfo.uid)
                    .withStatusShow(false)
                    .withStatusActive(false)
                    .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)),
                clientInfo
            ).campaignId

        val idsCriteria = IdsCriteria().withIds(listOf(campaignId))
        val archiveRequest = ArchiveRequest().withSelectionCriteria(idsCriteria)

        val response = genericApiService.doAction(delegate, archiveRequest)

        assertThat(response.archiveResults[0].errors).isEmpty()
        assertThat(response.archiveResults[0].warnings).isEmpty()

        val archivedCampaigns = campaignTypedRepository
            .getSafely(clientInfo.shard, listOf(campaignId), ContentPromotionCampaign::class.java)
        assertThat(archivedCampaigns).hasSize(1)
        assertThat(archivedCampaigns[0].statusShow).isFalse
        assertThat(archivedCampaigns[0].statusArchived).isTrue
        assertThat(archivedCampaigns[0].statusBsSynced).isEqualTo(CampaignStatusBsSynced.NO)
    }

    @Test
    fun `archive campaign with mcb type operation error`() {
        campaignId = steps.campaignSteps()
            .createCampaign(
                activeMcBannerCampaign(clientInfo.clientId, clientInfo.uid)
                    .withStatusShow(false)
                    .withStatusActive(false)
                    .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)),
                clientInfo
            ).campaignId

        val idsCriteria = IdsCriteria().withIds(listOf(campaignId + 123))
        val archiveRequest = ArchiveRequest().withSelectionCriteria(idsCriteria)

        val response = genericApiService.doAction(delegate, archiveRequest)

        assertThat(response.archiveResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(8800)
    }
}
