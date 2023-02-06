package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.avatars.client.AvatarsClient
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool
import ru.yandex.direct.core.entity.image.container.BannerImageType
import ru.yandex.direct.core.entity.image.model.BannerImageSource
import ru.yandex.direct.core.entity.image.service.ImageService
import ru.yandex.direct.core.entity.uac.avatarInfo
import ru.yandex.direct.core.entity.uac.avatarsConfig
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.thumbUrl
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.result.Result
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.defect.FileDefects
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.directBannerImageInformation
import ru.yandex.direct.web.entity.uac.model.CreateContentRequest
import ru.yandex.direct.web.entity.uac.service.UacDirectImageUploader
import ru.yandex.direct.web.validation.model.WebValidationResult
import java.util.Locale

abstract class UacContentCreateImageControllerTestBase {

    protected lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var uacAvatarsClientPool: AvatarsClientPool

    @Autowired
    private lateinit var uacDirectImageUploader: UacDirectImageUploader

    @Autowired
    protected lateinit var contentRepository: UacYdbContentRepository

    protected lateinit var userInfo: UserInfo
    private lateinit var avatarsClient: AvatarsClient
    private lateinit var imageService: ImageService

    private val contentMetaExpected = mapOf(
        "ColorWiz" to mapOf(
            "ColorWizBack" to "#FFFFFF",
            "ColorWizButton" to "#EEEEEE",
            "ColorWizButtonText" to "#DDDDDD",
            "ColorWizText" to "#CCCCCC",
        ),
        "direct_image_hash" to "direct_image_hash",
        "direct_mds_meta" to mapOf(
            "x90" to mapOf(
                "height" to 90,
                "width" to 90,
            ),
        ),
        "wx1080" to mapOf(
            "width" to 1080,
            "height" to 603,
            "smart-center" to mapOf(
                "h" to 300,
                "w" to 500,
                "x" to 80,
                "y" to 3,
            ),
            "smart-centers" to emptyList<Any>()
        )
    )

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        // нужно для проверки регионов, так как из ручки возвращается регион в локали en
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        avatarsClient = uacAvatarsClientPool.defaultClient
        whenever(avatarsClient.conf).thenReturn(avatarsConfig)
        whenever(avatarsClient.getReadUrl(any(), any())).thenReturn(thumbUrl)

        imageService = mock(ImageService::class.java)
        whenever(
            imageService.saveImageFromUrl(
                userInfo.clientId,
                thumbUrl,
                BannerImageType.BANNER_TEXT,
                BannerImageSource.UAC,
                null
            )
        ).thenReturn(Result.successful(directBannerImageInformation))
        ReflectionTestUtils.setField(uacDirectImageUploader, "imageService", imageService)
    }

    private fun mockUploadAvatarsImageByFile() {
        whenever(avatarsClient.upload(any(), any())).thenReturn(avatarInfo)
    }

    protected fun mockUploadAvatarsImageByUrl() {
        whenever(avatarsClient.uploadByUrl(any(), any())).thenReturn(avatarInfo)
    }

    protected fun checkCommonContentFields(content: Content) {
        content.type.checkEquals(ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE)
        content.thumb.checkEquals(thumbUrl)
        content.thumbId.checkEquals("42/some_name")
        content.directImageHash.checkEquals("direct_image_hash")
        content.iw.checkEquals(1200)
        content.ih.checkEquals(800)
        content.mdsUrl.checkEquals(null)
        content.meta.checkEquals(contentMetaExpected)
        content.videoDuration.checkEquals(null)
        content.tw.checkEquals(null)
        content.th.checkEquals(null)
    }

    protected abstract fun checkDbContentExists(id: String)

    @Test
    fun testUploadByFile() {
        mockUploadAvatarsImageByFile()

        val multipartFile = MockMultipartFile(
            "upload", "image.jpeg", "image/jpeg", "random bytes".toByteArray()
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/uac/content")
                .file(multipartFile)
                .accept(MediaType.APPLICATION_JSON)
                .param("ulogin", userInfo.clientInfo!!.login)
        )

        val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultResult: JsonNode = JsonUtils.MAPPER.readTree(resultContent)["result"]
        val content: Content = JsonUtils.fromJson(JsonUtils.toJson(resultResult), object : TypeReference<Content>() {})

        checkCommonContentFields(content)
        content.sourceUrl.checkEquals(thumbUrl)
        content.filename.checkEquals("image.jpeg")

        checkDbContentExists(content.id)
    }

    @Test
    fun testUploadByUrl() {
        mockUploadAvatarsImageByUrl()

        val contentRequest = CreateContentRequest(
            type = ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE,
            sourceUrl = "https://example.com/someimage.jpeg",
            mdsUrl = null,
            thumb = null,
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/content")
                .content(JsonUtils.toJson(contentRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .param("ulogin", userInfo.clientInfo!!.login)
        )

        val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultResult: JsonNode = JsonUtils.MAPPER.readTree(resultContent)["result"]
        val content: Content = JsonUtils.fromJson(JsonUtils.toJson(resultResult), object : TypeReference<Content>() {})

        checkCommonContentFields(content)
        content.sourceUrl.checkEquals("https://example.com/someimage.jpeg")
        content.filename.checkEquals(null)

        checkDbContentExists(content.id)
    }

    @Test
    fun testUploadByThumb() {
        mockUploadAvatarsImageByUrl()

        val contentRequest = CreateContentRequest(
            type = ru.yandex.direct.core.entity.uac.model.MediaType.IMAGE,
            sourceUrl = null,
            mdsUrl = null,
            thumb = "https://avatars.mds.yandex.net/get-rmp_stores_data/42/name/orig",
        )

        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/content")
                .content(JsonUtils.toJson(contentRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .param("ulogin", userInfo.clientInfo!!.login)
        )

        val resultContent = result.andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultResult: JsonNode = JsonUtils.MAPPER.readTree(resultContent)["result"]
        val content: Content = JsonUtils.fromJson(JsonUtils.toJson(resultResult), object : TypeReference<Content>() {})

        checkCommonContentFields(content)
        content.sourceUrl.checkEquals("https://avatars.mds.yandex.net/get-rmp_stores_data/42/name/orig")
        content.filename.checkEquals(null)

        checkDbContentExists(content.id)
    }

    @Test
    fun testUploadFileWithInvalidContentType() {
        val multipartFile = MockMultipartFile(
            "upload", "image.pdf", "application/pdf", "random bytes".toByteArray()
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/uac/content")
                .file(multipartFile)
                .accept(MediaType.APPLICATION_JSON)
                .param("ulogin", userInfo.clientInfo!!.login)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)

        SoftAssertions.assertSoftly {
            it.assertThat(validationResult.errors).isNotEmpty
            it.assertThat(validationResult.errors[0].code)
                .isEqualTo(FileDefects.fileMimeTypeIsNotSupported().defectId().code)
        }
    }

    @Test
    fun testUploadInvalidFile() {
        val multipartFile = MockMultipartFile(
            "upload", "image", null, "random bytes".toByteArray()
        )
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/uac/content")
                .file(multipartFile)
                .accept(MediaType.APPLICATION_JSON)
                .param("ulogin", userInfo.clientInfo!!.login)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult = JsonUtils
            .fromJson(JsonUtils.fromJson(result)["validation_result"].toString(), WebValidationResult::class.java)

        SoftAssertions.assertSoftly {
            it.assertThat(validationResult.errors).isNotEmpty
            it.assertThat(validationResult.errors[0].code).isEqualTo(FileDefects.noContentType().defectId().code)
        }
    }
}
