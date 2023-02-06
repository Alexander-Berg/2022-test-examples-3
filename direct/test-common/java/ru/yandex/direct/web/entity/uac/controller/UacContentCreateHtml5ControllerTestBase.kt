package ru.yandex.direct.web.entity.uac.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
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
import ru.yandex.direct.canvas.client.CanvasClient
import ru.yandex.direct.canvas.client.model.html5.Html5BatchResponse
import ru.yandex.direct.canvas.client.model.html5.Html5SourceResponse
import ru.yandex.direct.core.entity.uac.model.Content
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbContentRepository
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.stub.CanvasClientStub
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import java.util.Locale

abstract class UacContentCreateHtml5ControllerTestBase {

    protected lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    protected lateinit var canvasClient: CanvasClient

    @Autowired
    protected lateinit var contentRepository: UacYdbContentRepository

    protected lateinit var userInfo: UserInfo

    private val html5SourceResponseRaw = mapOf(
        "id" to "source_id",
        "screenshot_url" to "https://avatars.mds.yandex.net:443/get-media-adv-screenshooter/1/name/orig",
        "preview_url" to "http://example.com/preview.jpeg",
        "url" to "http://example.com/source_url.zip",
        "width" to 1600,
        "height" to 900,
        "source_image_info" to mapOf(
            "metadataInfo" to mapOf(
                "width" to 800,
                "height" to 450,
            )
        )
    )

    private val html5SourceResponse: Html5SourceResponse = JsonUtils.fromJson(
        JsonUtils.toJson(html5SourceResponseRaw),
        Html5SourceResponse::class.java
    )

    private val html5BatchResponseRaw = mapOf(
        "id" to "batch_id",
        "creatives" to listOf(
            mapOf(
                "id" to 42,
            )
        )
    )

    private val html5BatchResponse: Html5BatchResponse = JsonUtils.fromJson(
        JsonUtils.toJson(html5BatchResponseRaw),
        Html5BatchResponse::class.java
    )

    private val contentMetaExpected = mapOf(
        "screenshot_url" to "https://avatars.mds.yandex.net:443/get-media-adv-screenshooter/1/name/orig",
        "preview_url" to "http://example.com/preview.jpeg",
        "url" to "http://example.com/source_url.zip",
        "creative_id" to 42,
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

        (canvasClient as CanvasClientStub).addCustomHtml5SourceResponseWithClientId(
            html5SourceResponse, userInfo.clientId.asLong()
        )
        (canvasClient as CanvasClientStub).addCustomHtml5BatchResponseWithClientId(
            html5BatchResponse, userInfo.clientId.asLong()
        )
        (canvasClient as CanvasClientStub).addCustomUploadedCreativesByClientId(
            listOf(42), userInfo.clientId.asLong()
        )
    }

    protected fun checkContentFields(content: Content) {
        content.type.checkEquals(ru.yandex.direct.core.entity.uac.model.MediaType.HTML5)
        content.sourceUrl.checkEquals("http://example.com/source_url.zip")
        content.thumb.checkEquals("https://avatars.mds.yandex.net:443/get-media-adv-screenshooter/1/name/orig")
        content.thumbId.checkEquals("1/name")
        content.directImageHash.checkEquals(null)
        content.iw.checkEquals(1600)
        content.ih.checkEquals(900)
        content.ow.checkEquals(800)
        content.oh.checkEquals(450)
        content.mdsUrl.checkEquals("http://example.com/source_url.zip")
        content.meta.checkEquals(contentMetaExpected)
        content.videoDuration.checkEquals(null)
        content.tw.checkEquals(null)
        content.th.checkEquals(null)
    }

    abstract fun checkDbContentExists(id: String)

    @Test
    fun testUploadByFile() {
        val multipartFile = MockMultipartFile(
            "upload", "banner.zip", "application/zip", "random bytes".toByteArray()
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

        checkContentFields(content)

        checkDbContentExists(content.id)
    }
}
