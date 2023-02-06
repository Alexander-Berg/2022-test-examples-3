package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.eq
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.exception.YaAudienceClientException
import ru.yandex.direct.audience.client.exception.YaAudienceClientTypedException
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentStatus
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacRetargetingControllerFileUploadTest {
    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    private lateinit var userInfo: UserInfo
    private lateinit var mockMvc: MockMvc

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun testUploadFile_Successful() {
        val segmentName = "name"
        val file = MockMultipartFile(
            "file",
            null,
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            "fileContent".toByteArray(Charsets.UTF_8)
        )

        `when`(yaAudienceClient.uploadSegment(
            eq(userInfo.login),
            eq(file.bytes)))
            .thenReturn(AudienceSegment().withStatus(SegmentStatus.UPLOADED).withId(1L))

        `when`(yaAudienceClient.confirmYuidSegment(
            eq(userInfo.login),
            eq(1L),
            eq(segmentName),
            any()))
            .thenReturn(AudienceSegment().withStatus(SegmentStatus.UPLOADED).withId(1L))

        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/uac/retargeting/segments/file")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .param("segmentName", segmentName)
                .param("ulogin", userInfo.login))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.id").value(1))
            .andExpect(jsonPath("$.req_id").exists())
            .andExpect(jsonPath("$.result.status").value("uploaded"))
    }

    @Test
    fun testUploadFile_Unsuccessful() {
        val segmentName = "name"
        val file = MockMultipartFile(
            "file",
            null,
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            "fileContent".toByteArray(Charsets.UTF_8)
        )

        `when`(yaAudienceClient.uploadSegment(
            eq(userInfo.login),
            eq(file.bytes)))
            .thenThrow(YaAudienceClientException("Test error"))

        `when`(yaAudienceClient.confirmYuidSegment(
            eq(userInfo.login),
            eq(1L),
            eq(segmentName),
            any()))
            .thenThrow(YaAudienceClientTypedException("error", "invalid_type"))

        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/uac/retargeting/segments/file")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                .param("segmentName", segmentName)
                .param("ulogin", userInfo.login))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }
}
