package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDefaultVideoContent
import ru.yandex.direct.core.entity.uac.createImageCampaignContent
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE
import ru.yandex.direct.core.entity.uac.model.MediaType.TEXT
import ru.yandex.direct.core.entity.uac.model.MediaType.TITLE
import ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO
import ru.yandex.direct.core.entity.uac.model.campaign_content.ContentEfficiency
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaignContent
import ru.yandex.direct.core.entity.uac.service.UacContentService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.converter.UacContentStatisticsConverter.toContentStatistics
import ru.yandex.direct.web.entity.uac.model.EMPTY_STAT
import ru.yandex.direct.web.entity.uac.model.UacContentStatisticsResponse

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacCampaignControllerContentStatisticsTest {

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

    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo
    private lateinit var uacCampaignInfo: UacCampaignSteps.UacCampaignInfo
    private lateinit var uacTitleCampaignContent: UacYdbCampaignContent
    private lateinit var uacTextCampaignContent: UacYdbCampaignContent
    private lateinit var uacTextDeletedCampaignContent: UacYdbCampaignContent
    private lateinit var uacImageCampaignContent: UacYdbCampaignContent
    private lateinit var uacVideoCampaignContent: UacYdbCampaignContent
    private lateinit var imageContent: Content
    private lateinit var videoContent: Content

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        uacCampaignInfo = uacCampaignSteps.createTextCampaign(clientInfo)
        createMediaContents()

        val uacCampaignContents = uacYdbCampaignContentRepository.getCampaignContents(uacCampaignInfo.uacCampaign.id)
        uacTitleCampaignContent = uacCampaignContents.first { it.type == TITLE && it.removedAt == null }
        uacTextCampaignContent = uacCampaignContents.first { it.type == TEXT && it.removedAt == null }
        uacTextDeletedCampaignContent = uacCampaignContents.first { it.type == TEXT && it.removedAt != null }
        uacImageCampaignContent = uacCampaignContents.first { it.type == IMAGE && it.removedAt == null }
        uacVideoCampaignContent = uacCampaignContents.first { it.type == VIDEO && it.removedAt == null }

        val uacContentIds = uacCampaignContents.mapNotNull { it.contentId }
        val uacContentById = uacYdbContentRepository.getContents(uacContentIds)
            .associateBy { it.id }
        imageContent = uacContentService.fillContent(uacContentById[uacImageCampaignContent.contentId])!!
        videoContent = uacContentService.fillContent(uacContentById[uacVideoCampaignContent.contentId])!!
    }

    private fun createMediaContents() {
        val imageHash = steps.bannerSteps().createWideImageFormat(clientInfo).imageHash

        var creativeCanvasId = steps.creativeSteps().nextCreativeId
        creativeCanvasId = steps.creativeSteps()
            .addDefaultVideoAdditionCreative(clientInfo, creativeCanvasId).creativeId

        val uacImageContent = createDefaultImageContent(
            imageHash = imageHash,
        )
        val uacVideoContent = createDefaultVideoContent(
            creativeId = creativeCanvasId,
        )
        uacYdbContentRepository.saveContents(listOf(uacImageContent, uacVideoContent))

        val uacImageCampaignContent = createImageCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            createdAt = LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.SECONDS),
            contentId = uacImageContent.id,
        )
        val uacVideoCampaignContent = createCampaignContent(
            campaignId = uacCampaignInfo.uacCampaign.id,
            contentId = uacVideoContent.id,
            createdAt = LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.SECONDS),
            type = VIDEO,
        )

        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacImageCampaignContent, uacVideoCampaignContent))
    }

    @Test
    fun `get content statistics`() {
        val from = UacYdbUtils.toEpochSecond(LocalDateTime.now().minusDays(1))
        val to = UacYdbUtils.toEpochSecond(LocalDateTime.now())

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${uacCampaignInfo.campaign.campaignId}/content_statistics?ulogin=${clientInfo.login}&from=${from}&to=${to}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val uacContentStatisticsResponse = JsonUtils.fromJson(result, UacContentStatisticsResponse::class.java)
        val uacContentStatistics = uacContentStatisticsResponse.results.toSet()

        val expectedContentStatistics = listOf(
            toContentStatistics(uacTitleCampaignContent, null, ContentEfficiency.COLLECTING, false, EMPTY_STAT),
            toContentStatistics(uacTextCampaignContent, null, ContentEfficiency.COLLECTING, false, EMPTY_STAT),
            toContentStatistics(uacTextDeletedCampaignContent, null, ContentEfficiency.COLLECTING, false, EMPTY_STAT),
            toContentStatistics(uacImageCampaignContent, imageContent, ContentEfficiency.COLLECTING, false, EMPTY_STAT),
            toContentStatistics(uacVideoCampaignContent, videoContent, ContentEfficiency.COLLECTING, false, EMPTY_STAT),
        )

        assertThat(uacContentStatistics).containsExactlyInAnyOrderElementsOf(expectedContentStatistics)
    }
}
