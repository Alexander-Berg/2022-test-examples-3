package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.bolts.collection.Cf
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.Tuple2
import ru.yandex.inside.passport.PassportDomain
import ru.yandex.inside.passport.PassportUid
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxNewSession
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxSessionIdException
import ru.yandex.market.logistics.cte.base.AbstractContextualTest
import ru.yandex.market.logistics.cte.config.SecurityConfig
import ru.yandex.market.logistics.cte.config.tvm.BlackboxClientConfig
import ru.yandex.market.logistics.cte.config.tvm.BlackboxProperties
import ru.yandex.market.logistics.cte.config.tvm.TvmConfig
import ru.yandex.market.logistics.cte.config.tvm.TvmProperties
import ru.yandex.market.logistics.cte.service.auth.BlackboxRequestManager
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker
import ru.yandex.passport.tvmauth.TvmClient
import javax.servlet.http.Cookie

@Import(SecurityConfig::class, TvmConfig::class, BlackboxClientConfig::class)
@AutoConfigureMockMvc
@EnableConfigurationProperties(value = [BlackboxProperties::class, TvmProperties::class])
class  SecurityTest(
): AbstractContextualTest() {
    @MockBean lateinit var  blackboxRequestManager: BlackboxRequestManager
    @MockBean lateinit var  tvmClient: TvmClient
    @MockBean lateinit var  tvmTicketChecker: TvmTicketChecker
    @MockBean lateinit var  webInvocationPrivilegeEvaluator: WebInvocationPrivilegeEvaluator

    @Test
    @Throws(Exception::class)
    fun noSessionCookiesTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/quality_matrix/list")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @Throws(Exception::class)
    fun noSessionIdCookieTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/quality_matrix/list")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("sessionid2", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @Throws(Exception::class)
    fun invalidSessionCookiesTest() {

        whenever(blackboxRequestManager.checkBySessionCookies(any(), any(), any()))
            .thenReturn(blackboxResponse)

        whenever(blackboxResponse.status)
            .thenReturn(BlackboxSessionIdException.BlackboxSessionIdStatus.INVALID.id)
        mockMvc.perform(
            MockMvcRequestBuilders.get("/quality_matrix/list")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @Throws(Exception::class)
    fun blackboxResponseExceptionTest() {
        whenever(
            blackboxRequestManager.checkBySessionCookies(any(), any(), any())
        )
            .thenThrow(RuntimeException())
        mockMvc.perform(
            MockMvcRequestBuilders.get("/quality_matrix/list")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @Throws(Exception::class)
    fun invalidServiceTicketAuthTest() {
        Mockito.doThrow(RuntimeException())
            .`when`(tvmTicketChecker)?.isRequestedUrlAcceptable(ArgumentMatchers.anyString())
        mockMvc.perform(
            MockMvcRequestBuilders.get("/idm/info/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Ya-Service-Ticket", "sdgsgd43364")
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @Throws(Exception::class)
    fun noServiceTicketExceptionAuthTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/idm/info/")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @Throws(Exception::class)
    fun validServiceTicketAuthTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/idm/info/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Ya-Service-Ticket", "sdgsgd43364")
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @Throws(Exception::class)
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun validSessionCookiesTestForUserUrl() {
        whenever(blackboxRequestManager.checkBySessionCookies(any(), any(), any()))
            .thenReturn(blackboxResponse)

        whenever(blackboxResponse.status)
            .thenReturn(BlackboxSessionIdException.BlackboxSessionIdStatus.VALID.id)
        mockMvc.perform(
            MockMvcRequestBuilders.get("/quality_matrix/list")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @Throws(Exception::class)
    fun invalidSessionCookiesTestForManagerUrl() {
        whenever(blackboxResponse.status)
            .thenReturn(BlackboxSessionIdException.BlackboxSessionIdStatus.INVALID.id)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/quality_group/create")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @Throws(Exception::class)
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun validSessionCookiesTestForAscUserUrl() {
        whenever(blackboxRequestManager.checkBySessionCookies(any(), any(), any()))
            .thenReturn(blackboxResponse)

        whenever(blackboxResponse.status)
            .thenReturn(BlackboxSessionIdException.BlackboxSessionIdStatus.VALID.id)
        mockMvc.perform(
            MockMvcRequestBuilders.get("/service_center/asc/list")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @Throws(Exception::class)
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun invalidSessionCookiesTestForAscUserUrl() {
        whenever(blackboxRequestManager.checkBySessionCookies(any(), any(), any()))
            .thenReturn(blackboxResponse)

        whenever(blackboxResponse.status)
            .thenReturn(BlackboxSessionIdException.BlackboxSessionIdStatus.INVALID.id)
        mockMvc.perform(
            MockMvcRequestBuilders.get("/service_center/asc/list")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/idm/user_role.xml")
    )
    fun validUserRolesAuthTest() {
        whenever(blackboxRequestManager.checkBySessionCookies(any(), any(), any()))
            .thenReturn(blackboxResponse)
        whenever(blackboxResponse.status)
            .thenReturn(BlackboxSessionIdException.BlackboxSessionIdStatus.VALID.id)
        mockMvc.perform(
            MockMvcRequestBuilders.get("/cte/user/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie("Session_id", "123456"))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.content().string("[\"user\",\"asc_user\"]"))
    }

    @Test
    @Throws(Exception::class)
    fun tvmUrlWithValidServiceTicketTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/logistic_services/quality-attributes/find-by-unit_type")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Ya-Service-Ticket", "sdgsgd43364")
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @Throws(Exception::class)
    fun tmvUrlWithoutServiceTicketTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/logistic_services/quality-attributes/find-by-unit_type")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
    }

    companion object {
        private val blackboxResponse = Mockito.spy(
            BlackboxCorrectResponse(
                BlackboxMethod.SESSION_ID,
                Option.empty(),
                Option.empty(),
                BlackboxSessionIdException.BlackboxSessionIdStatus.VALID.id,
                Option.empty(),
                Option.of(Tuple2(PassportUid(1234465), PassportDomain.YANDEX_TEAM_RU)),
                Option.of("vasya"),
                Cf.map(),
                Cf.list(),
                Cf.list(),
                Cf.list(),
                Option.empty(),
                Cf.map(),
                Option.empty(),
                Option.empty(),
                Option.of(
                    BlackboxNewSession(
                        "1232gvb", PassportDomain.YANDEX_TEAM_RU.toString(),
                        123L, true
                    )
                ),
                Option.of(
                    BlackboxNewSession(
                        "dhrh124", PassportDomain.YANDEX_TEAM_RU.toString(),
                        123L, false
                    )
                ),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty()
            )
        )
    }
}
