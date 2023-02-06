package ru.yandex.market.delivery.transport_manager.controller.transportation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/multiple_transportations_status_history.xml"
})
public class TransportationStatusHistoryTest extends AbstractContextualTest {

    @DisplayName("Контроллер перемещений: успешное получение истории изменения статусов")
    @Test
    void getStatusHistorySuccess() throws Exception {
        getStatusHistory(
            "controller/transportations/status_history/request/by_ids.json",
            status().isOk(),
            jsonContent("controller/transportations/status_history/response/success.json")
        );
    }

    @DisplayName(
        "Контроллер перемещений: успешное получение истории изменения статусов перемещения и связанных сущностей"
    )
    @Test
    void getStatusHistoryWithUnitsSuccess() throws Exception {
        getStatusHistory(
            "controller/transportations/status_history/request/by_ids_with_units_history.json",
            status().isOk(),
            jsonContent("controller/transportations/status_history/response/success_with_units_history.json")
        );
    }

    @DisplayName("Контроллер перемещений: пустой ответ")
    @Test
    void getStatusHistoryEmpty() throws Exception {
        getStatusHistory(
            "controller/transportations/status_history/request/empty.json",
            status().isOk(),
            content().json("[]")
        );
    }

    @DisplayName("Контроллер перемещений: список идентификаторов перемещений пустой")
    @Test
    void getStatusHistoryTransportationIdsEmpty() throws Exception {
        getStatusHistory(
            "controller/transportations/status_history/request/no_ids.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'transportationIds', message: 'must not be empty'")
        );
    }


    @DisplayName("Контроллер перемещений: не заданы идентификаторы перемещений")
    @Test
    void getStatusHistoryTransportationIdsNull() throws Exception {
        getStatusHistory(
            "controller/transportations/status_history/request/null_ids.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'transportationIds', message: 'must not be empty'")
        );
    }

    void getStatusHistory(
        String content,
        ResultMatcher statusMatcher,
        ResultMatcher resultMatcher
    ) throws Exception {
        mockMvc.perform(put("/transportations/status-history")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(content))
        )
            .andExpect(statusMatcher)
            .andExpect(resultMatcher);
    }
}
