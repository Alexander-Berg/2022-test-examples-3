package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.databind.JsonNode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.avatars.client.AvatarsClient
import ru.yandex.direct.canvas.client.CanvasClient
import ru.yandex.direct.canvas.client.model.video.AdditionResponse
import ru.yandex.direct.canvas.client.model.video.VideoUploadResponse
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool
import ru.yandex.direct.core.entity.uac.avatarInfo
import ru.yandex.direct.core.entity.uac.avatarsConfig
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.entity.uac.thumbUrl
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.CanvasClientStub
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.utils.fromJson
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.CreateContentRequest
import java.util.*

abstract class UacContentCreateVideoControllerTestBase {

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
    protected lateinit var canvasClient: CanvasClient

    @Autowired
    protected lateinit var contentRepository: UacYdbContentRepository

    @Autowired
    private lateinit var steps: Steps

    protected lateinit var userInfo: UserInfo
    private lateinit var avatarsClient: AvatarsClient

    private val baseVideoUploadResponseRaw: Map<String, Any> = mapOf(
        "name" to "Video name",
        "preset_id" to 42,
        "thumbnail" to mapOf(
            "url" to "https://example.com/thumb400.jpeg",
            "width" to 400,
            "height" to 300,
            "preview" to mapOf(
                "url" to "https://example.com/preview.jpeg",
                "width" to 200,
                "height" to 150,
            ),
        ),
        "thumbnailUrl" to "https://example.com/thumb.jpeg",
        "mime_type" to "video/mp4",
        "url" to "https://example.com/video.mp4",
        "duration" to 102.0,
        "width" to 1200,
        "height" to 800,
        "formats" to listOf(
            mapOf(
                "type" to "video/mp4",
                "url" to "https://example.com/format.mp4",
                "delivery" to "progressive",
                "bitrate" to 954,
            )
        ),
    )

    private val videoUploadResponseRaw = baseVideoUploadResponseRaw + mapOf(
        "id" to "videoId",
        "status" to "ready",
        "create_early_creative" to true,
    )

    private val videoNotReadyUploadResponseRaw = baseVideoUploadResponseRaw + mapOf(
        "id" to "videoNotReadyId",
        "status" to "converting",
        "create_early_creative" to false,
    )

    protected val videoUploadResponse: VideoUploadResponse = JsonUtils.fromJson(
        JsonUtils.toJson(videoUploadResponseRaw),
        VideoUploadResponse::class.java
    )

    protected val videoNotReadyUploadResponse: VideoUploadResponse = JsonUtils.fromJson(
        JsonUtils.toJson(videoNotReadyUploadResponseRaw),
        VideoUploadResponse::class.java
    )

    private val additionResponseRaw = mapOf(
        "creative_id" to 987654321,
        "vast" to "<vast>"
    )

    private val additionResponse: AdditionResponse = JsonUtils.fromJson(
        JsonUtils.toJson(additionResponseRaw),
        AdditionResponse::class.java
    )

    private val baseContentMetaExpected = mapOf(
        "formats" to listOf(
            mapOf(
                "type" to "video/mp4",
                "url" to "https://example.com/format.mp4",
                "delivery" to "progressive",
                "bitrate" to 954,
            )
        ),
        "thumb" to mapOf(
            "url" to "https://example.com/thumb400.jpeg",
            "width" to 400,
            "height" to 300,
            "preview" to mapOf(
                "url" to "https://example.com/preview.jpeg",
                "width" to 200,
                "height" to 150,
            ),
        ),
    )

    protected val contentMetaExpected = baseContentMetaExpected + mapOf(
        "creative_id" to 987654321,
        "creative_type" to "rmp",
        "vast" to "<vast>",
        "status" to "ready",
    )

    protected val contentMetaNonSkippableExpected = baseContentMetaExpected + mapOf(
        "creative_id" to 987654321,
        "creative_type" to "non_skippable_cpm",
        "vast" to "<vast>",
        "status" to "ready",
    )


    private val notReadyContentMetaExpected = baseContentMetaExpected + mapOf(
        "status" to "converting",
        "creative_type" to "rmp",
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
        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.UAC_UPDATE_VIDEO_CONTENT)

        (canvasClient as CanvasClientStub).addCustomAdditionResponseWithVideoId(additionResponse, "videoId")

        avatarsClient = uacAvatarsClientPool.defaultClient
        whenever(avatarsClient.conf).thenReturn(avatarsConfig)
        whenever(avatarsClient.getReadUrl(any(), any())).thenReturn(thumbUrl)

        whenever(avatarsClient.uploadByUrl(any(), any())).thenReturn(avatarInfo)
    }

    protected fun checkCommonContentFields(content: Content) {
        content.type.checkEquals(ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO)
        content.thumb.checkEquals(thumbUrl)
        content.thumbId.checkEquals("42/some_name")
        content.directImageHash.checkEquals(null)
        content.iw.checkEquals(1200)
        content.ih.checkEquals(800)
        content.mdsUrl.checkEquals("https://example.com/video.mp4")
        content.videoDuration.checkEquals(102)
        content.tw.checkEquals(400)
        content.th.checkEquals(300)
    }

    abstract fun checkDbContentExists(id: String)

    @Test
    fun testUploadByFile() {
        (canvasClient as CanvasClientStub).addCustomVideoUploadReseponseWithFileName(videoUploadResponse, "image.jpeg")
        val multipartFile = MockMultipartFile(
            "upload", "image.jpeg", "video/mp4", "random bytes".toByteArray()
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
        val content: Content = fromJson(resultResult.toString())

        checkCommonContentFields(content)
        content.meta.checkEquals(contentMetaExpected)
        content.sourceUrl.checkEquals("https://example.com/video.mp4")

        checkDbContentExists(content.id)
    }

    @Test
    fun testUploadByUrl() {
        val sourceUrl = "https://example.com/somevideo.mp4"
        (canvasClient as CanvasClientStub).addCustomVideoUploadResponseWithUrl(videoUploadResponse, sourceUrl)
        val contentRequest = CreateContentRequest(
            type = ru.yandex.direct.core.entity.uac.model.MediaType.VIDEO,
            sourceUrl = sourceUrl,
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
        val content: Content = fromJson(resultResult.toString())

        checkCommonContentFields(content)
        content.meta.checkEquals(contentMetaExpected)
        content.sourceUrl.checkEquals(sourceUrl)

        checkDbContentExists(content.id)
    }

    @Test
    fun testVideoNotReady() {
        (canvasClient as CanvasClientStub).addCustomVideoUploadReseponseWithFileName(
            videoNotReadyUploadResponse, "video.mp4"
        )
        val multipartFile = MockMultipartFile(
            "upload", "video.mp4", "video/mp4", "random bytes".toByteArray()
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
        val content: Content = fromJson(resultResult.toString())

        checkCommonContentFields(content)
        content.meta.checkEquals(notReadyContentMetaExpected)
        content.sourceUrl.checkEquals("https://example.com/video.mp4")

        checkDbContentExists(content.id)
    }
}
