package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.doThrow
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.campaign.service.uc.UcCampaignService
import ru.yandex.direct.core.entity.campaign.service.validation.DisableDomainValidationService
import ru.yandex.direct.core.entity.client.service.ClientGeoService
import ru.yandex.direct.core.entity.client.service.ClientLimitsService
import ru.yandex.direct.core.entity.client.service.ClientService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.entity.hypergeo.service.HyperGeoService
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterStorage
import ru.yandex.direct.core.entity.retargeting.service.uc.UcRetargetingConditionService
import ru.yandex.direct.core.entity.retargeting.service.validation2.AddRetargetingConditionValidationService2
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.entity.uac.grut.GrutTransactionProvider
import ru.yandex.direct.core.entity.uac.grut.ThreadLocalGrutContext
import ru.yandex.direct.core.entity.uac.service.CpmBannerCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacCampaignService
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.entity.uac.service.RmpCampaignService
import ru.yandex.direct.core.entity.uac.service.UacDisabledDomainsService
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.grid.processing.service.campaign.uc.UcCampaignMutationService
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.entity.uac.service.GrutUacCampaignAddService
import ru.yandex.direct.web.entity.uac.service.GrutUacCampaignWebService
import ru.yandex.direct.web.entity.uac.service.RmpStrategyValidatorFactory
import ru.yandex.direct.web.entity.uac.service.UacAdjustmentsService
import ru.yandex.direct.web.entity.uac.service.UacCampaignValidationService
import ru.yandex.direct.web.entity.uac.service.UacCpmBannerService
import ru.yandex.direct.web.entity.uac.service.UacGoalsService
import ru.yandex.direct.web.entity.uac.service.UacMobileAppService
import ru.yandex.direct.web.entity.uac.service.UacModifyCampaignDataContainerFactory
import ru.yandex.direct.web.entity.uac.service.UacPropertiesService
import ru.yandex.direct.web.entity.uac.service.UacRetargetingConditionService
import ru.yandex.grut.client.GrutGrpcClient
import ru.yandex.grut.client.YtGenericException
import ru.yandex.yt.TError

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignControllerGrutFail : BaseGrutCreateCampaignTest() {

    @Autowired
    private lateinit var uacMobileAppService: UacMobileAppService

    @Autowired
    private lateinit var uacModifyCampaignDataContainerFactory: UacModifyCampaignDataContainerFactory

    @Autowired
    private lateinit var rmpCampaignService: RmpCampaignService

    @Autowired
    private lateinit var cpmBannerCampaignService: CpmBannerCampaignService

    @Autowired
    private lateinit var ucCampaignMutationService: UcCampaignMutationService

    @Autowired
    private lateinit var ucRetargetingConditionService: UcRetargetingConditionService

    @Autowired
    private lateinit var uacGoalsService: UacGoalsService

    @Autowired
    private lateinit var ucCampaignService: UcCampaignService

    @Autowired
    private lateinit var clientGeoService: ClientGeoService

    @Autowired
    private lateinit var uacCampaignValidationService: UacCampaignValidationService

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    @Autowired
    private lateinit var uacPropertiesService: UacPropertiesService

    @Autowired
    private lateinit var clientService: ClientService

    @Autowired
    private lateinit var uacAdjustmentsService: UacAdjustmentsService

    @Autowired
    private lateinit var sspPlatformsRepository: SspPlatformsRepository

    @Autowired
    private lateinit var hostingsHandler: HostingsHandler

    @Autowired
    private lateinit var bannerImageFormatRepository: BannerImageFormatRepository

    @Autowired
    private lateinit var shardHelper: ShardHelper

    @Autowired
    private lateinit var cpmBannerService: UacCpmBannerService

    @Autowired
    private lateinit var clientLimitsService: ClientLimitsService

    @Autowired
    private lateinit var disableDomainValidationService: DisableDomainValidationService

    @Autowired
    private lateinit var rmpStrategyValidatorFactory: RmpStrategyValidatorFactory

    @Autowired
    private lateinit var uacDisabledDomainsService: UacDisabledDomainsService

    @Autowired
    private lateinit var retargetingConditionValidationService: AddRetargetingConditionValidationService2

    @Autowired
    private lateinit var hyperGeoService: HyperGeoService

    @Autowired
    private lateinit var grutUacContentService: GrutUacContentService

    @Autowired
    private lateinit var grutUacCampaignWebService: GrutUacCampaignWebService

    @Autowired
    private lateinit var featureService: FeatureService

    @Autowired
    private lateinit var uacRetargetingConditionService: UacRetargetingConditionService

    @Autowired
    private lateinit var grutUacCampaignService: GrutUacCampaignService

    @Autowired
    private lateinit var campaignService: CampaignService

    @Mock
    private lateinit var grutClient: GrutGrpcClient

    @Autowired
    private lateinit var grutUacCampaignAddService: GrutUacCampaignAddService

    @Autowired
    private lateinit var feedService: FeedService

    @Autowired
    private lateinit var filterSchemaStorage: PerformanceFilterStorage

    private lateinit var grutContext: GrutContext
    private lateinit var grutTransactionProvider: GrutTransactionProvider

    @Before
    fun initTest() {

        grutClient = mock(GrutGrpcClient::class.java)
        doThrow(YtGenericException(TError.getDefaultInstance())).`when`(grutClient).startTransaction()
        grutContext = ThreadLocalGrutContext(grutClient)

        grutTransactionProvider = GrutTransactionProvider(grutContext, grutClient)
        grutUacCampaignAddService = GrutUacCampaignAddService(
            uacMobileAppService,
            uacModifyCampaignDataContainerFactory,
            rmpCampaignService,
            cpmBannerCampaignService,
            ucCampaignMutationService,
            ucRetargetingConditionService,
            uacGoalsService,
            ucCampaignService,
            clientGeoService,
            uacCampaignValidationService,
            bidModifierService,
            uacPropertiesService,
            clientService,
            uacAdjustmentsService,
            sspPlatformsRepository,
            hostingsHandler,
            bannerImageFormatRepository,
            shardHelper,
            cpmBannerService,
            clientLimitsService,
            disableDomainValidationService,
            rmpStrategyValidatorFactory,
            uacDisabledDomainsService,
            retargetingConditionValidationService,
            grutApiService,
            hyperGeoService,
            grutUacContentService,
            grutUacCampaignWebService,
            featureService,
            uacRetargetingConditionService,
            grutUacCampaignService,
            campaignService,
            grutTransactionProvider,
            feedService,
            filterSchemaStorage
        )
    }

    @Test
    fun avoidInconsistentCampaignsOnGrutFail() {
        val req = createUacCampaignRequest(listOf()).toInternal(null, null, null)

        val campaignsIdBefore = campaignService.getClientCampaignIds(clientInfo.clientId!!)
        try {
            grutUacCampaignAddService.addUacCampaign(
                userInfo.user!!,
                userInfo.user!!,
                req,
            )
        } catch (e: Throwable) {
            assert(e is YtGenericException)
        }

        val existingIds = campaignService.getClientCampaignIds(clientInfo.clientId!!)
        val campaignsAfter = campaignService.getCampaigns(clientInfo.clientId!!, existingIds)
        val campaignsIdAfter = campaignsAfter
            .filterNot { it.statusEmpty || it.type == CampaignType.WALLET }
            .map { it.id }

        val deletedCampaigns = campaignsAfter.filter { it.statusEmpty }

        SoftAssertions().apply {
            assertThat(campaignsIdBefore)
                .`as`("Assert client campaigns preserved in an initial state")
                .containsExactlyInAnyOrderElementsOf(campaignsIdAfter)
            assertThat(deletedCampaigns)
                .`as`("Assert correct removal")
                .isNotEmpty
                .hasSize(1)
            assertThat(deletedCampaigns[0].name)
                .`as`("Assert the deleted campaign name")
                .isEqualTo(req.displayName)
        }.assertAll()
    }
}
