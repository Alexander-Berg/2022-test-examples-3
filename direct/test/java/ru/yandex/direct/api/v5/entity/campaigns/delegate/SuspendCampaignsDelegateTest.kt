package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.yandex.direct.api.v5.campaigns.SuspendRequest
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
import ru.yandex.direct.api.v5.entity.campaigns.validation.CampaignsSuspendResumeRequestValidator
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.user.model.ApiUser
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps

@Api5Test
@RunWith(SpringRunner::class)
class SuspendCampaignsDelegateTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var requestValidator: CampaignsSuspendResumeRequestValidator

    @Autowired
    private lateinit var resultConverter: ResultConverter

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var featureService: FeatureService

    private lateinit var genericApiService: GenericApiService

    private lateinit var delegate: SuspendCampaignsDelegate

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

        delegate = SuspendCampaignsDelegate(
            auth,
            campaignService,
            requestValidator,
            resultConverter,
            ppcPropertiesSupport,
            featureService,
        )

        campaignId = steps.campaignSteps()
            .createActiveCampaign(clientInfo)
            .campaignId
    }

    @Test
    fun `suspend success`() {
        val idsCriteria = IdsCriteria().withIds(listOf(campaignId))
        val suspendRequest = SuspendRequest().withSelectionCriteria(idsCriteria)

        val response = genericApiService.doAction(delegate, suspendRequest)

        assertThat(response.suspendResults[0].errors).isEmpty()
        assertThat(response.suspendResults[0].warnings).isEmpty()

        val suspendedCampaigns = campaignTypedRepository
            .getSafely(clientInfo.shard, listOf(campaignId), TextCampaign::class.java)
        assertThat(suspendedCampaigns).hasSize(1)
        assertThat(suspendedCampaigns[0].statusShow).isFalse()
    }

    @Test
    fun `suspend not existing campaign`() {
        val idsCriteria = IdsCriteria().withIds(listOf(campaignId + 1))
        val suspendRequest = SuspendRequest().withSelectionCriteria(idsCriteria)

        val response = genericApiService.doAction(delegate, suspendRequest)

        assertThat(response.suspendResults)
            .flatExtracting("errors")
            .extracting("code")
            .containsExactly(8800)
    }
}
