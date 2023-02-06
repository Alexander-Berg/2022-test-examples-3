package ru.yandex.direct.web.entity.uac.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository
import ru.yandex.direct.core.entity.uac.UacCommonUtils.CREATIVE_ID_KEY
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.service.UacContentService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.intapi.client.IntApiClient
import ru.yandex.direct.intapi.client.model.request.statistics.CampaignStatisticsRequest
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsItem
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsResponse
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.converter.UacContentStatisticsConverter.toContentStatistics
import ru.yandex.direct.web.entity.uac.model.CreativeStatistics
import ru.yandex.direct.web.entity.uac.model.EMPTY_STAT
import ru.yandex.direct.web.entity.uac.model.UacContentStatisticsResponse
import java.time.LocalDateTime
import java.time.ZoneOffset

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCpmCampaignControllerContentStatisticsTest {

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacCampaignSteps: UacCampaignSteps

    @Autowired
    private lateinit var uacContentService: UacContentService

    @Autowired
    private lateinit var intApiClient: IntApiClient

    @Autowired
    private lateinit var bannerCreativeRepository: BannerCreativeRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo
    private lateinit var uacCampaignInfo: UacCampaignSteps.UacCampaignInfo
    private lateinit var uacVideoCampaignContent: UacYdbCampaignContent
    private lateinit var uacVideoCampaignContent2: UacYdbCampaignContent
    private lateinit var videoContent: Content
    private lateinit var videoContent2: Content

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        uacCampaignInfo = uacCampaignSteps.createCpmCampaign(clientInfo)
        createMediaContents()

        val uacCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaignInfo.uacCampaign.id)
        uacVideoCampaignContent = uacCampaignContents.first { it.type == VIDEO && it.removedAt == null }
        uacVideoCampaignContent2 =
            uacCampaignContents.first { it.type == VIDEO && it.removedAt == null && it != uacVideoCampaignContent }

        val uacContentIds = uacCampaignContents.mapNotNull { it.contentId }
        val uacContentById = uacYdbContentRepository.getContents(uacContentIds)
            .associateBy { it.id }

        videoContent = uacContentService.fillContent(uacContentById[uacVideoCampaignContent.contentId])!!
        videoContent2 = uacContentService.fillContent(uacContentById[uacVideoCampaignContent2.contentId])!!
    }

    private fun createMediaContents() {
        var creativeCanvasId = steps.creativeSteps().nextCreativeId
        creativeCanvasId = steps.creativeSteps()
            .addDefaultVideoAdditionCreative(clientInfo, creativeCanvasId).creativeId

        val uacVideoContent = createDefaultVideoContent(
            creativeId = creativeCanvasId,
        )

        val uacVideoContent2 = createDefaultVideoContent(
            creativeId = creativeCanvasId,
        )
        uacYdbContentRepository.saveContents(listOf(uacVideoContent2, uacVideoContent))

        val uacVideoCampaignContent = createCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            contentId = uacVideoContent.id,
            type = VIDEO,
        )

        val uacVideoCampaignContent2 = createCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            contentId = uacVideoContent2.id,
            type = VIDEO,
        )

        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacVideoCampaignContent2, uacVideoCampaignContent))
    }

    private fun mockStatFromIntApi(
        items: List<CampaignStatisticsItem>,
    ) {
        Mockito.doReturn(
            mapOf(
                1L to videoContent.meta["creative_id"],
                2L to videoContent.meta["creative_id"]
            )
        )
            .`when`(bannerCreativeRepository).getBannerIdToCreativeId(anyInt(), anyCollection())

        val intApiResponse = CampaignStatisticsResponse().apply {
            data = items
            totals = CampaignStatisticsItem()
        }

        Mockito.doReturn(intApiResponse)
            .`when`(intApiClient).getCampaignStatistics(ArgumentMatchers.any(CampaignStatisticsRequest::class.java))
    }

    @Test
    fun `get statistics for creatives`() {

        mockStatFromIntApi(
            listOf(
                CampaignStatisticsItem().withCreativeId(videoContent.meta[CREATIVE_ID_KEY] as Long).withShows(100).withClicks(12).withCtr(12.0),
                CampaignStatisticsItem().withCreativeId(videoContent2.meta[CREATIVE_ID_KEY] as Long).withShows(100).withClicks(12).withCtr(12.0)
            )
        )

        val from = LocalDateTime.now().minusDays(1).toEpochSecond(ZoneOffset.UTC)
        val to = LocalDateTime.now().plusDays(1).toEpochSecond(ZoneOffset.UTC)

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${uacCampaignInfo.campaign.campaignId}/content_statistics?" +
                    "ulogin=${clientInfo.login}&from=${from}&to=${to}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val uacContentStatisticsResponse = JsonUtils.fromJson(result, UacContentStatisticsResponse::class.java)
        val uacContentStatistics = uacContentStatisticsResponse.results.toSet()

        val expectedContentStatistics = listOf(
            toContentStatistics(
                uacVideoCampaignContent2, videoContent2,
                creativeStatistics = CreativeStatistics(
                    clicks = 12,
                    shows = 100,
                    cost = 0.0,
                    costMicros = 0,
                    costCurMicros = 0,
                    costTaxFreeMicros = 0,
                    avgCpm = 0.0,
                    avgCpc = 0.0,
                    videoAvgTrueViewCost = 0.0,
                    uniqViewers = 0,
                    ctr = 12.0,
                    videoFirstQuartileRate = 0.0,
                    videoMidpointRate = 0.0,
                    videoThirdQuartileRate = 0.0,
                    videoCompleteRate = 0.0,
                    videoTrueView = 0,
                    avgNShow = 0.0,
                    avgNShowComplete = 0.0,
                    conversions = 0,
                    installs = 0,
                    postViewConversions = 0,
                    postViewInstalls = 0,
                )
            ),
            toContentStatistics(uacVideoCampaignContent, videoContent),
        )

        assertThat(uacContentStatistics).isSubsetOf(expectedContentStatistics)
    }

    @Test
    fun `get content statistics`() {
        mockStatFromIntApi(
            listOf(
                CampaignStatisticsItem(),
                CampaignStatisticsItem()
            )
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get(
                    "/uac/campaign/${uacCampaignInfo.campaign.campaignId}/" +
                        "content_statistics?ulogin=${clientInfo.login}&from=1611993283&to=1632999312"
                )
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val uacContentStatisticsResponse = JsonUtils.fromJson(result, UacContentStatisticsResponse::class.java)
        val uacContentStatistics = uacContentStatisticsResponse.results.toSet()

        val expectedContentStatistics = listOf(
            toContentStatistics(uacVideoCampaignContent, videoContent, null),
            toContentStatistics(uacVideoCampaignContent2, videoContent2, null),
        )

        assertThat(uacContentStatistics).isSubsetOf(expectedContentStatistics)
    }

    @Test
    fun `bannerId is sent`() {
        val createActiveCpmBanner = steps.bannerSteps().createActiveCpmBanner(videoContent.meta[CREATIVE_ID_KEY] as Long, uacCampaignInfo.campaign)
        mockStatFromIntApi(
            listOf(
                CampaignStatisticsItem().withCreativeId(videoContent.meta[CREATIVE_ID_KEY] as Long).withBid(createActiveCpmBanner.banner.id),
                CampaignStatisticsItem().withCreativeId(videoContent2.meta[CREATIVE_ID_KEY] as Long).withBid(createActiveCpmBanner.banner.id)
            )
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get(
                    "/uac/campaign/${uacCampaignInfo.campaign.campaignId}/" +
                        "content_statistics?ulogin=${clientInfo.login}&from=1611993283&to=1632999312"
                )
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val uacContentStatisticsResponse = JsonUtils.fromJson(result, UacContentStatisticsResponse::class.java)
        val uacContentStatistics = uacContentStatisticsResponse.results.toSet()

        val expectedContentStatistics = listOf(
            toContentStatistics(
                uacVideoCampaignContent2, videoContent2,
                creativeStatistics = EMPTY_STAT,
                bid = createActiveCpmBanner.bannerId.toString(),
            ),
            toContentStatistics(
                uacVideoCampaignContent, videoContent,
                bid = createActiveCpmBanner.bannerId.toString(),
            ),
        )

        assertThat(uacContentStatistics).isSubsetOf(expectedContentStatistics)
    }

    @Test
    fun `bannerId is sent if multiple banners have same creativeId(null)`() {
        val createActiveCpmBanner = steps.bannerSteps().createActiveCpmBanner(videoContent.meta[CREATIVE_ID_KEY] as Long, uacCampaignInfo.campaign)
        val createActiveCpmBanner2 = steps.bannerSteps().createActiveCpmBanner(videoContent.meta[CREATIVE_ID_KEY] as Long, uacCampaignInfo.campaign)
        mockStatFromIntApi(
            listOf(
                CampaignStatisticsItem().withCreativeId(videoContent.meta[CREATIVE_ID_KEY] as Long).withBid(createActiveCpmBanner.banner.id),
                CampaignStatisticsItem().withCreativeId(videoContent2.meta[CREATIVE_ID_KEY] as Long).withBid(createActiveCpmBanner2.banner.id)
            )
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get(
                    "/uac/campaign/${uacCampaignInfo.campaign.campaignId}/" +
                        "content_statistics?ulogin=${clientInfo.login}&from=1611993283&to=1632999312"
                )
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val uacContentStatisticsResponse = JsonUtils.fromJson(result, UacContentStatisticsResponse::class.java)
        val uacContentStatistics = uacContentStatisticsResponse.results.toSet()

        val expectedContentStatistics = listOf(
            toContentStatistics(
                uacVideoCampaignContent2, videoContent2,
                creativeStatistics = EMPTY_STAT,
                bid = createActiveCpmBanner.bannerId.toString()
            ),
            toContentStatistics(
                uacVideoCampaignContent, videoContent,
                bid = createActiveCpmBanner.bannerId.toString()
            ),
        )

        assertThat(uacContentStatistics).isSubsetOf(expectedContentStatistics)
    }
}
