package ru.yandex.market.wms.autostart.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickDetailsControllerLargeTest extends TestcontainersConfiguration {

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/1/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveLostPickDetails() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/pickdetails/1/reserve-lost-pickdetails.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/1/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/1_1/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/1_1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveSeveralLostPickDetailsFromOneOrderDetail() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/pickdetails/1_1/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/1_1/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/1_2/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/1_2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveSeveralLostPickDetailsFromOneOrderDetailSomeNotReserved() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("testcontainers/controller/pickdetails/1_2/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/1_2/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/1_3/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/1_3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveLostPickDetailsFromDifferentOrderDetailsSomeNotReserved() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("testcontainers/controller/pickdetails/1_3/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/1_3/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/1_4/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/1_4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveLostPickDetailsFromRealOrderDetailsWithBatchOrderNumber() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("testcontainers/controller/pickdetails/1_4/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/1_4/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/1_5/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/1_5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveLostPickDetailsFromRealOrderDetailsWithoutBatchOrderNumber() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("testcontainers/controller/pickdetails/1_5/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/1_5/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/2/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveLostPickDetailsNoAnaloguesOnStock() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/pickdetails/2/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/2/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/3/immutable-state.xml")
    public void testReserveLostPickDetailsNotInPickedStatus() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("testcontainers/controller/pickdetails/3/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/3/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/4/immutable-state.xml")
    public void testReserveLostPickDetailsFromDifferentWaves() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("testcontainers/controller/pickdetails/4/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/4/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/5/immutable-state.xml")
    public void testReserveLostPickDetailsWhenBatchWasDisassembled() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("testcontainers/controller/pickdetails/5/request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/5/response.json")));
    }

    @Test
    @DatabaseSetup("/testcontainers/controller/pickdetails/bigWithdrawal/before.xml")
    @ExpectedDatabase(value = "/testcontainers/controller/pickdetails/bigWithdrawal/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testReserveBigWithdrawalLostPickDetails() throws Exception {
        mockMvc.perform(post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("testcontainers/controller/pickdetails/bigWithdrawal/" +
                                "reserve-lost-pickdetails.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("testcontainers/controller/pickdetails/bigWithdrawal/" +
                        "response.json")));
    }
}
