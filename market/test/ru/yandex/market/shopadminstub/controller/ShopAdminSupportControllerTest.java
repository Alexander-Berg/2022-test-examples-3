package ru.yandex.market.shopadminstub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.shopadminstub.application.AbstractTestBase;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

class ShopAdminSupportControllerTest extends AbstractTestBase {

    @Test
    void shouldSetDSBSShipmentCalculationEnabled() throws Exception {
        mockMvc.perform(get("/properties"))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/shopAdminStubProperties/@isDbsShipmentDateCalculationEnabled")
                        .string(is("false")));

        mockMvc.perform(put("/properties/dbs-shipment-date-calculation-enabled")
                .content("true")
                .contentType(MediaType.TEXT_PLAIN_VALUE)
        ).andExpect(status().isOk())
                .andExpect(xpath("/shopAdminStubProperties/@isDbsShipmentDateCalculationEnabled")
                        .string(is("true")));
    }
}
