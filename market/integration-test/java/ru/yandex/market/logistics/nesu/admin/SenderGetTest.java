package ru.yandex.market.logistics.nesu.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/sender/before/prepare_for_search.xml")
class SenderGetTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить сендера")
    @JpaQueriesCount(2)
    void getSender() throws Exception {
        getSender(40001L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get/1.json"));
    }

    @Test
    @DisplayName("Получить отключенного сендера")
    @JpaQueriesCount(2)
    void getSenderDeleted() throws Exception {
        getSender(40003L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get/40003.json"));
    }

    @Test
    @DisplayName("Сендер не найден")
    @JpaQueriesCount(1)
    void getSenderNotFound() throws Exception {
        getSender(10L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/admin/sender-get/not_found.json"));
    }

    @Nonnull
    private ResultActions getSender(long senderId) throws Exception {
        return mockMvc.perform(get("/admin/senders/" + senderId));
    }
}
