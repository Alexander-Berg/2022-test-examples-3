package ru.yandex.market.delivery.transport_manager.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

public class AdminMovementTest extends AbstractContextualTest {
    @Test
    @SneakyThrows
    @DisplayName("Получение детальной карточки перемещения в админке")
    @DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
    @DatabaseSetup("/repository/transportation/multiple_transportations.xml")
    @DatabaseSetup("/repository/transportation/metadata.xml")
    void getMovement() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/movement/1")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/admin/movement/movement-details.json", false));
    }
}
