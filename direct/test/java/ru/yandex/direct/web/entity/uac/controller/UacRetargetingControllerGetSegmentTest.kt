package ru.yandex.direct.web.entity.uac.controller

import com.nhaarman.mockitokotlin2.eq
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.exception.YaAudienceClientException
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentStatus
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacRetargetingControllerGetSegmentTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

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

    fun statuses() = arrayOf(
        arrayOf(SegmentStatus.IS_PROCESSED),
        arrayOf(SegmentStatus.UPLOADED),
        arrayOf(SegmentStatus.FEW_DATA),
        arrayOf(SegmentStatus.IS_UPDATED),
        arrayOf(SegmentStatus.PROCESSED),
        arrayOf(SegmentStatus.PROCESSING_FAILED)
    )

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    @TestCaseName("Test case for status: {0}")
    @Parameters(method = "statuses")
    fun testRetargetingSegments_Successful(status: SegmentStatus) {
        val segmentName = "SegmentName"
        val differentStatus = SegmentStatus.values().first { it != status }
        Mockito.`when`(yaAudienceClient.getSegments(eq(userInfo.login)))
            .thenReturn(
                listOf(AudienceSegment()
                    .withStatus(differentStatus)
                    .withId(1L)
                    .withName("Uploaded"),
                    AudienceSegment()
                        .withStatus(status)
                        .withId(2L)
                        .withName(segmentName))
            )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/segments")
                .param("status", status.name)
                .param("ulogin", userInfo.login))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.result.size()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("result[?(@.id == 2)]").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("result[?(@.id == 2)].name").value(segmentName))
            .andExpect(MockMvcResultMatchers.jsonPath("result[?(@.id == 2)].status").value(status.name.lowercase()))
    }

    @Test
    @TestCaseName("Unsuccessful test case for status: {0}")
    @Parameters(method = "statuses")
    fun testRetargetingSegments_Unsuccessful(status: SegmentStatus) {
        Mockito.`when`(yaAudienceClient.getSegments(eq(userInfo.login)))
            .thenThrow(YaAudienceClientException("Error"))

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/retargeting/segments")
                .param("status", SegmentStatus.IS_PROCESSED.name)
                .param("ulogin", userInfo.login))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
    }
}
