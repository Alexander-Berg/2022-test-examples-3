package ru.yandex.market.delivery.transport_manager.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

class AdminTransportationTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Получение детальной карточки перемещения в админке")
    @DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
    @DatabaseSetup("/repository/transportation/multiple_transportations.xml")
    @DatabaseSetup("/repository/transportation/metadata.xml")
    void getTransportation() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/transportations/1")
            .header(HttpHeaders.ACCEPT, "Application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/transportation/transportation-details.json", false));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение детальной карточки XDock перемещения в админке")
    @DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
    @DatabaseSetup("/repository/transportation/multiple_transportations.xml")
    @DatabaseSetup("/repository/transportation/metadata.xml")
    void getXdockTransportation() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/transportations/5")
            .header(HttpHeaders.ACCEPT, "Application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/transportation/transportation-details-xdock.json", false));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение детальной карточки Linehaul перемещения в админке")
    @DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
    @DatabaseSetup("/repository/transportation/multiple_transportations.xml")
    @DatabaseSetup("/repository/transportation/metadata.xml")
    void getLinehaulTransportation() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/transportations/8")
            .header(HttpHeaders.ACCEPT, "Application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/transportation/transportation-details-linehaul.json", false));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение детальной карточки межскладского перемещения в админке")
    @DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
    @DatabaseSetup("/repository/transportation/multiple_transportations.xml")
    @DatabaseSetup("/repository/transportation/metadata.xml")
    void getInterwarehouseTransportation() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/transportations/9")
            .header(HttpHeaders.ACCEPT, "Application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/transportation/transportation-details-interwarehouse.json",
                false
            ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение детальной карточки перемещения со слотами в админке")
    @DatabaseSetup("/repository/transportation/multiple_transportations_deps.xml")
    @DatabaseSetup("/repository/transportation/multiple_transportations.xml")
    @DatabaseSetup("/repository/transportation/metadata.xml")
    @DatabaseSetup(
        type = DatabaseOperation.REFRESH,
        value = "/repository/transportation/transportation_units_with_slots.xml"
    )
    void getTransportationWithSlots() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/transportations/5")
            .header(HttpHeaders.ACCEPT, "Application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/transportation/transportation-with-slots-details.json", false));
    }
}
