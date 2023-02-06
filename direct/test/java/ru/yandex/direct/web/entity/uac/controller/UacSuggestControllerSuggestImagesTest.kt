package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.apache.commons.lang3.tuple.Pair
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.avatars.client.AvatarsClient
import ru.yandex.direct.core.entity.adgeneration.ImageGenerationService.REQUEST_IMAGES_LIMIT
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool
import ru.yandex.direct.core.entity.image.container.BannerImageType.BANNER_TEXT
import ru.yandex.direct.core.entity.image.model.BannerImageSource.UAC
import ru.yandex.direct.core.entity.image.service.ImageConstants
import ru.yandex.direct.core.entity.image.service.ImageService
import ru.yandex.direct.core.entity.uac.avatarInfo
import ru.yandex.direct.core.entity.uac.avatarsConfig
import ru.yandex.direct.core.entity.uac.createCampaignContent
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.createDirectCampaign
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.thumbUrl
import ru.yandex.direct.core.testing.data.TestImages.createImageDocs
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbContentRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.imagesearch.ImageSearchClient
import ru.yandex.direct.result.Result
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.core.security.authentication.DirectCookieAuthProvider.PARAMETER_ULOGIN
import ru.yandex.direct.web.entity.uac.directBannerImageInformation
import ru.yandex.direct.web.entity.uac.model.UacContentsResponse
import ru.yandex.direct.web.entity.uac.service.UacDirectImageUploader

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacSuggestControllerSuggestImagesTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacYdbCampaignContentRepository: UacYdbCampaignContentRepository

    @Autowired
    private lateinit var uacYdbCampaignRepository: UacYdbCampaignRepository

    @Autowired
    private lateinit var uacYdbContentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacAvatarsClientPool: AvatarsClientPool

    @Autowired
    private lateinit var uacDirectImageUploader: UacDirectImageUploader

    @Autowired
    private lateinit var imageSearchClient: ImageSearchClient

    @Autowired
    private lateinit var testUacYdbContentRepository: TestUacYdbContentRepository

    @Autowired
    private lateinit var uacYdbAccountRepository: UacYdbAccountRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo
    private lateinit var avatarsClient: AvatarsClient
    private lateinit var imageService: ImageService
    private lateinit var uacAccount: UacYdbAccount
    private lateinit var uacCampaignId: String
    private lateinit var uacCampaign: UacYdbCampaign
    private var directCampaignId: Long = 0L

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        imageService = mock()
        MockitoAnnotations.openMocks(this)

        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )

        uacAccount = UacYdbAccount(
            uid = clientInfo.uid,
            directClientId = clientInfo.clientId!!.asLong(),
        )
        uacYdbAccountRepository.saveAccount(uacAccount)

        val campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo)
        directCampaignId = campaignInfo.campaignId

        uacCampaignId = randomPositiveLong().toString()

        uacCampaign = createYdbCampaign(
            id = uacCampaignId,
            accountId = uacAccount.id,
        )
        uacYdbCampaignRepository.addCampaign(uacCampaign)

        uacYdbDirectCampaignRepository.saveDirectCampaign(
            createDirectCampaign(
                directCampaignId = directCampaignId,
                id = uacCampaignId,
            )
        )

        avatarsClient = uacAvatarsClientPool.defaultClient
        whenever(avatarsClient.conf)
            .thenReturn(avatarsConfig)
        whenever(avatarsClient.getReadUrl(any(), any()))
            .thenReturn(thumbUrl)

        whenever(imageService.saveImageFromUrl(clientInfo.clientId!!, thumbUrl, BANNER_TEXT, UAC, null))
            .thenReturn(Result.successful(directBannerImageInformation))
        ReflectionTestUtils.setField(uacDirectImageUploader, "imageService", imageService)

        whenever(avatarsClient.upload(any(), any()))
            .thenReturn(avatarInfo)
        whenever(avatarsClient.uploadByUrl(any(), any()))
            .thenReturn(avatarInfo)
    }

    @After
    fun after() {
        testUacYdbContentRepository.clean()
    }

    @Test
    fun `get two suggest images with one already existed`() {
        val existSourceUrl = "http://de.er.tr.exist-source-url.com/one/two?f=1&n=5#three"
        val notExistSourceUrl = "https://not-exist-source-url.com?g=1"

        val expectExistSourceUrl = "https://de.er.tr.exist-source-url.com/one/two?f=1&n=13#three"
        val expectNotExistSourceUrl = "https://not-exist-source-url.com?g=1&n=13"

        val uacContent = createContent(expectExistSourceUrl)

        whenever(imageSearchClient.getImagesByTextsAndDomain(setOf(), "play.google.com", REQUEST_IMAGES_LIMIT))
            .thenReturn(
                mapOf(
                    "main" to createImageDocs(
                        listOf(
                            Pair.of(existSourceUrl, ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE),
                            Pair.of(notExistSourceUrl, ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE),
                        )
                    )
                )
            )

        val uacContentsResponse = getResultByRequest()
        val resultContentIdToSourceUrl = uacContentsResponse.results
            .associateBy({ it.id }, { it.sourceUrl })
        val newContentId = uacContentsResponse.results
            .map { it.id }
            .first { it != uacContent.id }

        val clientContents = testUacYdbContentRepository.getContentsByAccountId(uacAccount.id)

        val soft = SoftAssertions()
        soft.assertThat(clientContents)
            .`as`("Один из новых ассетов был удален, т.к. он уже есть в кампании")
            .hasSize(2)
        soft.assertThat(resultContentIdToSourceUrl)
            .`as`("Один новый ассет и один старый")
            .hasSize(2)
            .containsEntry(uacContent.id, expectExistSourceUrl)
            .containsEntry(newContentId, expectNotExistSourceUrl)
        soft.assertAll()
    }

    @Test
    fun `get suggests without any contents in campaign`() {
        val sourceUrl1 = "http://source-url-1.ru"
        val sourceUrl2 = "https://source-url-2.tr?n=1&b=2"

        whenever(imageSearchClient.getImagesByTextsAndDomain(setOf(), "play.google.com", REQUEST_IMAGES_LIMIT))
            .thenReturn(
                mapOf(
                    "main" to createImageDocs(
                        listOf(
                            Pair.of(sourceUrl1, ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE),
                            Pair.of(sourceUrl2, ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE),
                        )
                    )
                )
            )

        val uacContentsResponse = getResultByRequest()
        val resultSourceUrls = uacContentsResponse.results
            .map { it.sourceUrl }

        val actualClientContents = testUacYdbContentRepository.getContentsByAccountId(uacAccount.id)

        val expectSourceUrl1 = "https://source-url-1.ru?n=13"
        val expectSourceUrl2 = "https://source-url-2.tr?b=2&n=13"

        val soft = SoftAssertions()
        soft.assertThat(actualClientContents)
            .`as`("Ассеты клиента")
            .hasSize(2)
        soft.assertThat(resultSourceUrls)
            .`as`("Source url в ответе")
            .hasSize(2)
            .containsOnly(expectSourceUrl1, expectSourceUrl2)
        soft.assertAll()
    }

    @Test
    fun `get empty result when no suggestions`() {
        val uacContent = createContent("https://source-url.ru")

        whenever(imageSearchClient.getImagesByTextsAndDomain(setOf(), "play.google.com", REQUEST_IMAGES_LIMIT))
            .thenReturn(emptyMap())

        val uacContentsResponse = getResultByRequest()

        val actualContentIdToSourceUrl = testUacYdbContentRepository.getContentsByAccountId(uacAccount.id)
            .associateBy({ it.id }) { it.sourceUrl }

        val soft = SoftAssertions()
        soft.assertThat(actualContentIdToSourceUrl)
            .`as`("У клиента не изменились ассеты")
            .hasSize(1)
            .containsEntry(uacContent.id, uacContent.sourceUrl)
        soft.assertThat(uacContentsResponse.results)
            .`as`("В ответе нет саджестов")
            .isEmpty()
        soft.assertAll()
    }

    @Test
    fun `get suggests without any contents in campaign 2`() {
        val goodSourceUrl = "http://good-source-url.ch"
        val sourceUrlWithBadImageSize = "https://image-with-bad-size.by"

        whenever(imageSearchClient.getImagesByTextsAndDomain(setOf(), "play.google.com", REQUEST_IMAGES_LIMIT))
            .thenReturn(
                mapOf(
                    "main" to createImageDocs(
                        listOf(
                            Pair.of(goodSourceUrl, ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE),
                            Pair.of(sourceUrlWithBadImageSize, ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE - 1),
                        )
                    )
                )
            )

        val uacContentsResponse = getResultByRequest()
        val resultSourceUrls = uacContentsResponse.results
            .map { it.sourceUrl }

        val actualClientContents = testUacYdbContentRepository.getContentsByAccountId(uacAccount.id)

        val expectGoodSourceUrl = "https://good-source-url.ch?n=13"

        val soft = SoftAssertions()
        soft.assertThat(actualClientContents)
            .`as`("Ассеты клиента")
            .hasSize(1)
        soft.assertThat(resultSourceUrls)
            .`as`("Source url в ответе")
            .hasSize(1)
            .containsOnly(expectGoodSourceUrl)
        soft.assertAll()
    }

    private fun createContent(sourceUrl: String): UacYdbContent {
        val uacContent = createDefaultImageContent(
            sourceUrl = sourceUrl,
            accountId = uacCampaign.account,
        )
        uacYdbContentRepository.saveContents(listOf(uacContent))

        val uacCampaignContent = createCampaignContent(
            campaignId = uacCampaignId,
            type = IMAGE,
            contentId = uacContent.id,
        )
        uacYdbCampaignContentRepository.addCampaignContents(listOf(uacCampaignContent))

        return uacContent
    }

    private fun getResultByRequest(): UacContentsResponse {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/suggest_images?" + PARAMETER_ULOGIN + "=" + clientInfo.login + "&campaign_id=" + directCampaignId)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        return JsonUtils.fromJson(result, UacContentsResponse::class.java)
    }
}
