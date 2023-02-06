package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.util.FileContentUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MiscControllerTest extends MvcIntegrationTest {
    private static final String IN_TRANSIT_COUNT_RESPONSE = "" +
            "[{\"supplierId\":1,\"marketSku\":1,\"warehouseId\":100,\"count\":3}," +
            "{\"supplierId\":1,\"marketSku\":2,\"warehouseId\":100,\"count\":8}," +
            "{\"supplierId\":1,\"marketSku\":3,\"warehouseId\":100,\"count\":3}," +
            "{\"supplierId\":1,\"marketSku\":7,\"warehouseId\":100,\"count\":5}]";

    @Test
    @DatabaseSetup("classpath:controller/request-item-api/items.xml")
    void findItemsCountInTransit() throws Exception {
        MvcResult result = mockMvc.perform(get("/requests/items/in-transit-count"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(IN_TRANSIT_COUNT_RESPONSE))
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:controller/misc/put-booking-slot-insert/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/misc/put-booking-slot-insert/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void insertSlot() throws Exception {
        mockMvc.perform(put("/booking/slot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/misc/put-booking-slot-insert/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/misc/put-booking-slot-upsert/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/misc/put-booking-slot-upsert/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void upsertSlot() throws Exception {
        mockMvc.perform(put("/booking/slot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent("controller/misc/put-booking-slot-upsert/request.json")))
                .andExpect(status().isOk());
    }


    @Test
    @DatabaseSetup("classpath:controller/misc/cancel-booked-slots/before.xml")
    @ExpectedDatabase(
            value = "classpath:controller/misc/cancel-booked-slots/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void deactivateBooking() throws Exception {
        mockMvc.perform(delete("/booking/slot")
                .param("bookingId", "1", "3"))
                .andExpect(status().isOk());
    }

}
