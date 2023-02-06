package ru.yandex.market.delivery.transport_manager.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/axapta/event/axapta_event.xml")
class AdminAxaptaEventControllerTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Получение грида событий, отправляемых в АХ")
    void search() {
        mockMvc.perform(get("/admin/axapta-events/search")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/axapta/grid.json", false));
    }

    @Test
    @SneakyThrows
    @DisplayName("Дублирование события для переотправки в АХ")
    @ExpectedDatabase(
        value = "/repository/axapta/event/after/new_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void resend() {
        mockMvc.perform(
            post("/admin/axapta-events/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/admin/axapta/request.json"))
        )
            .andExpect(status().isOk());
    }
}
