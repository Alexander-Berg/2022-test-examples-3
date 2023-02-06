package ru.yandex.market.logistics.logistrator.controller

import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.logistrator.utils.TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN
import ru.yandex.market.logistics.logistrator.utils.USER_LOGIN_HEADER
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

@DisplayName("Логирование запросов пользователей")
class ControllerLoggingTest : AbstractContextualTest() {

    @Test
    @ExpectedDatabase(
        "/db/request/after/user_activity_log_possible_order_changes_suggest.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    @DisplayName("Получение всех существующих типов возможных изменений заказа - с логированием запроса пользователя")
    fun testSuggestPossibleOrderChangeTypesWithUserActivityLog() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/suggests/possible-order-change-types")
                .header(USER_LOGIN_HEADER, TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(JsonUnitResultMatchers.json().isEqualTo(IntegrationTestUtils.extractFileContent(
                "response/suggest/possible_order_change_types_suggestion.json"
            )))
    }

    @Test
    @ExpectedDatabase("/db/request/after/empty.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Получение всех существующих типов возможных изменений заказа - без логина")
    fun testSuggestPossibleOrderChangeTypesWithoutLogin() {
        mockMvc.perform(MockMvcRequestBuilders.get("/suggests/possible-order-change-types"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(JsonUnitResultMatchers.json().isEqualTo(IntegrationTestUtils.extractFileContent(
                "response/suggest/possible_order_change_types_suggestion.json"
            )))
    }

    @Test
    @ExpectedDatabase("/db/request/after/empty.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @DisplayName("Ping - без логина")
    fun testPing() {
        mockMvc.perform(MockMvcRequestBuilders.get("/ping"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("0;OK"))
    }
}
