package ru.yandex.market.logistics.les.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent

@DisplayName("Получение информации о подписках")
@DatabaseSetup("/admin/subscription/before/subscriptions.xml")
class SubscriptionControllerTest : AbstractContextualTest() {

    @Test
    fun getSubscriptions() {
        mockMvc.perform(get("/admin/subscription/all"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonContent("admin/subscription/response/subscriptions.json", false))
    }

    @Test
    @ExpectedDatabase(
        "/admin/subscription/after/new_subscription.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createNewSubscription() {
        mockMvc.perform(
            post("/admin/subscription/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subscriberId\": 1, \"sourceId\": 2, \"eventType\": \"PODCAST\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @ExpectedDatabase(
        "/admin/subscription/after/updated_subscription_event_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateSubscriptionEventType() {
        mockMvc.perform(
            put("/admin/subscription/edit/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventType\": \"STREAM\", \"active\": true}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonContent("admin/subscription/response/edit_subscription_event_type.json", false))
    }

    @Test
    @ExpectedDatabase(
        "/admin/subscription/after/updated_subscription_activity.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateSubscriptionActivity() {
        mockMvc.perform(
            put("/admin/subscription/edit/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventType\": \"VIDEO\", \"active\": false}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonContent("admin/subscription/response/edit_subscription_activity.json", false))
    }
}
