package ru.yandex.market.delivery.transport_manager.controller.support.xdoc;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.xdoc.XdocSupportStateService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class XdocSupportStateControllerTest extends AbstractContextualTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    XdocSupportStateService xdocSupportStateService;

    @SneakyThrows
    @Test
    @DatabaseSetup("/repository/view/xdock_transportation_view.xml")
    void xdocState() {
        mockMvc.perform(get("/support/xdoc/state/3"))
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponse()));
    }

    private String getExpectedResponse() {
        return "{\"inbounds\":[" +
                "{\"informationListCode\": \"0000000625\"," +
                "\"status\": \"SHIPPED_FROM_DC\"," +
                "\"palletNumber\": 0," +
                "\"boxNumber\": 0," +
                "\"destinationId\": \"2\"," +
                "\"destinationName\": \"Томилино\"," +
                "\"axaptaMovementRequestId\": null" +
                "}," +
                "{\"informationListCode\": \"0000000624\"," +
                "\"status\": \"SHIPPED_FROM_DC\"," +
                "\"palletNumber\": 3," +
                "\"boxNumber\": 0," +
                "\"destinationId\": \"2\"," +
                "\"destinationName\": \"Томилино\"," +
                "\"axaptaMovementRequestId\": null" +
                "}," +
                "{\"informationListCode\": \"Зп-370098316\"," +
                "\"status\": \"SHIPPED_FROM_DC\"," +
                "\"palletNumber\": 0," +
                "\"boxNumber\": 4," +
                "\"destinationId\": \"2\"," +
                "\"destinationName\": \"Томилино\"," +
                "\"axaptaMovementRequestId\": \"ЗПер0011951\"" +
                "}" +
                "]" +
                "}";
    }
}
