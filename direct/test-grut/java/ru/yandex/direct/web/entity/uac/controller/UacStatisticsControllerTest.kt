package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
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
import ru.yandex.direct.core.entity.uac.model.GroupByDateType
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacStatisticsControllerTest {
    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        clientId = clientInfo.clientId!!

        testAuthHelper.setOperatorAndSubjectUser(clientInfo.uid)
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
    }

    @Test
    fun `export nonexistent statistics`() {
        val from = UacYdbUtils.toEpochSecond(LocalDateTime.now().minusDays(1))
        val to = UacYdbUtils.toEpochSecond(LocalDateTime.now())
        val groupByDate = GroupByDateType.DAY

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(
                    "/uac/campaign/123123/export_statistics" +
                        "?ulogin=${clientInfo.login}" +
                        "&groupByDate=${groupByDate.name}" +
                        "&from=${from}" +
                        "&to=${to}"
                )
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
