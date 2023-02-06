package ru.yandex.market.delivery.transport_manager.controller.transportation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/transportation/transportation_with_multiple_partner_info_and_register.xml")
public class TransportationControllerTest extends AbstractContextualTest {

    @DisplayName("Контроллер перемещений: успешное получение одного перемещения")
    @Test
    void getTransportationSuccess() throws Exception {
        mockMvc.perform(get("/transportations/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/transportations/response/success.json"));
    }

    @DisplayName("Контроллер перемещений: успешное получение перемещения с тегами")
    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_with_partner_info_and_items.xml",
        "/repository/transportation/transportation_3_with_tags.xml"
    })
    void getTransportationWithTagsSuccess() throws Exception {
        mockMvc.perform(get("/transportations/3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/transportations/response/success_with_tags.json"));
    }

    @DisplayName("Контроллер перемещений: перемещение не найдено")
    @Test
    void getTransportationNotFound() throws Exception {
        mockMvc.perform(get("/transportations/2"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/transportations/response/transportation_not_found.json"));
    }

    @DisplayName("Контроллер перемещений: нет информации о партнере")
    @Test
    void getTransportationNoPartnerInfo() throws Exception {
        mockMvc.perform(get("/transportations/3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/transportations/response/transportation_no_partner_info.json"));
    }

    @DisplayName("Поиск перемещений по requestId")
    @Test
    void getTransportationsByTag() throws Exception {
        mockMvc.perform(
            put("/transportations/search-by-tag")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/transportations/search-by-tag/request.json")
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/transportations/search-by-tag/response.json"));
    }

    @DisplayName("Установить AXAPTA Зпер для перемещения")
    @Test
    @DatabaseSetup("/repository/transportation/xdoc_to_ff_transportations.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportations_axapta_movement_order_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setAxaptaMovementOrderNumber() throws Exception {
        mockMvc.perform(
            put("/transportations/1/axaptaMovementOrderNumber")
            .contentType(MediaType.APPLICATION_JSON)
            .content("Зпер-0002")
        )
            .andExpect(status().isOk());
    }
}
