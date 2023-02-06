package ru.yandex.market.logistics.logistrator.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.logistrator.queue.payload.RequestIdPayload
import ru.yandex.market.logistics.logistrator.queue.processor.RequestValidationProcessor
import ru.yandex.market.logistics.logistrator.service.idm.IdmAuthenticationFilter
import ru.yandex.market.logistics.logistrator.utils.REQUEST_ID
import ru.yandex.market.logistics.logistrator.utils.TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN
import ru.yandex.market.logistics.logistrator.utils.USER_LOGIN_HEADER
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@DisplayName("Работа с состоянием запросов с проверкой безопасности и доступов")
@TestPropertySource(properties = ["logistrator.security-disabled=false"])
internal class RequestControllerSecurityTest : AbstractContextualTest() {

    @Autowired
    private lateinit var idmAuthenticationManager: AuthenticationManager

    private lateinit var securedMockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        whenever(clock.instant()).thenReturn(LocalDateTime.of(2022, 1, 7, 12, 0, 0).toInstant(ZoneOffset.UTC))

        securedMockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(
                SecurityMockMvcConfigurers.springSecurity(IdmAuthenticationFilter(idmAuthenticationManager))
            )
            .defaultRequest<DefaultMockMvcBuilder>(MockMvcRequestBuilders.get("/").characterEncoding("utf-8"))
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.log())
            .build()
    }

    @AfterEach
    private fun tearDown() {
        verifyNoMoreInteractions(dbQueueService)
    }

    @Test
    @DatabaseSetup("/db/request/before/ready.xml", "/db/user_role/user_role_admin.xml")
    @ExpectedDatabase("/db/request/after/ready.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получение данных о запросе с правами администратора - ок")
    fun testGetRequestByAdminOk() {
        securedMockMvc.perform(
            MockMvcRequestBuilders
                .get("/requests/$REQUEST_ID")
                .header(USER_LOGIN_HEADER, TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json().isEqualTo(IntegrationTestUtils.extractFileContent("response/ready.json")))
    }

    @Test
    @DatabaseSetup("/db/request/before/ready.xml", "/db/user_role/user_role_viewer.xml")
    @ExpectedDatabase("/db/request/after/ready.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получение данных о запросе с правами просматривающего - ок")
    fun testGetRequestByViewerOk() {
        securedMockMvc.perform(
            MockMvcRequestBuilders
                .get("/requests/$REQUEST_ID")
                .header(USER_LOGIN_HEADER, TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json().isEqualTo(IntegrationTestUtils.extractFileContent("response/ready.json")))
    }

    @Test
    @DatabaseSetup("/db/request/before/ready.xml")
    @ExpectedDatabase("/db/request/after/ready.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получение данных о запросе с неизвестным Логистратору пользователем - доступ запрещён")
    fun testGetRequestByUnknownUserForbidden() {
        securedMockMvc.perform(
            MockMvcRequestBuilders
                .get("/requests/$REQUEST_ID")
                .header(USER_LOGIN_HEADER, TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN)
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DatabaseSetup("/db/request/before/ready.xml", "/db/user_role/user_role_viewer.xml")
    @ExpectedDatabase("/db/request/after/ready.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получение данных о запросе без логина - доступ запрещён")
    fun testGetRequestWithoutLoginForbidden() {
        securedMockMvc.perform(MockMvcRequestBuilders.get("/requests/$REQUEST_ID"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DatabaseSetup("/db/request/before/draft.xml", "/db/user_role/user_role_admin.xml")
    @ExpectedDatabase(
        "/db/request/after/sent_to_validation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Перевод запроса из статуса драфта в статус созданного запроса с правами администратора - ок")
    fun testCommitDraftByAdminOk() {
        securedMockMvc.perform(
            MockMvcRequestBuilders
                .post("/requests/$REQUEST_ID/commit")
                .header(USER_LOGIN_HEADER, TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json()
                    .isEqualTo(IntegrationTestUtils.extractFileContent("response/sent_to_validation.json")))

        verify(dbQueueService).produceTask(eq(RequestValidationProcessor::class.java), eq(RequestIdPayload(REQUEST_ID)))
    }

    @Test
    @DatabaseSetup("/db/request/before/draft.xml", "/db/user_role/user_role_viewer.xml")
    @DisplayName(
        "Перевод запроса из статуса драфта в статус созданного запроса с правами просматривающего - доступ запрещён"
    )
    fun testCommitDraftByViewerForbidden() {
        securedMockMvc.perform(
            MockMvcRequestBuilders
                .post("/requests/$REQUEST_ID/commit")
                .header(USER_LOGIN_HEADER, TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN)
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DatabaseSetup("/db/request/before/draft.xml")
    @DisplayName(
        "Перевод запроса из статуса драфта в статус созданного запроса с неизвестным Логистратору пользователем - " +
                "доступ запрещён"
    )
    fun testCommitDraftByUnknownUserForbidden() {
        securedMockMvc.perform(
            MockMvcRequestBuilders
                .post("/requests/$REQUEST_ID/commit")
                .header(USER_LOGIN_HEADER, TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN)
        )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DatabaseSetup("/db/request/before/draft.xml", "/db/user_role/user_role_viewer.xml")
    @DisplayName("Перевод запроса из статуса драфта в статус созданного запроса без логина - доступ запрещён")
    fun testCommitDraftWithoutLoginForbidden() {
        securedMockMvc.perform(MockMvcRequestBuilders.post("/requests/$REQUEST_ID/commit"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }
}
