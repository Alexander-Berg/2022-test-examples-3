package ru.yandex.market.wms.datacreator.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class InventoryHoldControllerTest extends DataCreatorIntegrationTest {

    @Test
    @DatabaseSetup(value = "/controller/inventory-hold/get-statuses-by-lot/before.xml",
            connection = "wmwhse1Connection")
    void getStatusesByLot() throws Exception {
        mockMvc.perform(get("/inventory-hold/get-statuses-by-lot/0001927201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*]").value(containsInAnyOrder("EXPIRED", "DAMAGE")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inventory-hold/get-statuses-by-lot/before.xml",
            connection = "wmwhse1Connection")
    void getStatusesByLotWhenLotHasNoHolds() throws Exception {
        mockMvc.perform(get("/inventory-hold/get-statuses-by-lot/0001927202"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inventory-hold/place-hold-on-lot/lot-has-allocated-qty/before.xml",
            connection = "wmwhse1Connection")
    void placeHoldOnLotThrowsExceptionWhenLotHasAllocatedQty() throws Exception {
        mockMvc.perform(post("/inventory-hold/place-hold-on-lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/inventory-hold/" +
                                "place-hold-on-lot/lot-has-allocated-qty/request.json")))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$['message']")
                        .value("Lot should have no allocated or picked qty"))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inventory-hold/place-hold-on-lot/lot-has-picked-qty/before.xml",
            connection = "wmwhse1Connection")
    void placeHoldOnLotThrowsExceptionWhenLotHasPickedQty() throws Exception {
        mockMvc.perform(post("/inventory-hold/place-hold-on-lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/inventory-hold/" +
                                "place-hold-on-lot/lot-has-picked-qty/request.json")))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$['message']")
                        .value("Lot should have no allocated or picked qty"))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inventory-hold/place-hold-on-lot/" +
            "add-hold-to-lot/before-lot-has-damage-hold.xml",
            connection = "wmwhse1Connection")
    @ExpectedDatabase(value = "/controller/inventory-hold/place-hold-on-lot/" +
            "add-hold-to-lot/before-lot-has-damage-hold.xml",
            connection = "wmwhse1Connection",
            assertionMode = NON_STRICT)
    void placeHoldOnLotThatAlreadyHasThisHold() throws Exception {
        mockMvc.perform(post("/inventory-hold/place-hold-on-lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/inventory-hold/" +
                                "place-hold-on-lot/add-hold-to-lot/add-damage-hold-request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inventory-hold/place-hold-on-lot/" +
            "add-hold-to-lot/before-lot-has-damage-hold.xml",
            connection = "wmwhse1Connection")
    @ExpectedDatabase(value = "/controller/inventory-hold/place-hold-on-lot/" +
            "add-hold-to-lot/after-lot-has-damage-hold.xml",
            connection = "wmwhse1Connection",
            assertionMode = NON_STRICT)
    void placeHoldOnLotThatHasDifferentHold() throws Exception {
        mockMvc.perform(post("/inventory-hold/place-hold-on-lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/inventory-hold/" +
                                "place-hold-on-lot/add-hold-to-lot/add-expired-hold-request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inventory-hold/place-hold-on-lot/add-hold-to-lot/before-lot-without-hold.xml",
    connection = "wmwhse1Connection")
    @ExpectedDatabase(value = "/controller/inventory-hold/place-hold-on-lot/add-hold-to-lot/after-lot-without-hold.xml",
            connection = "wmwhse1Connection",
    assertionMode = NON_STRICT)
    public void placeHoldOnLotWithoutHold() throws Exception {
        mockMvc.perform(post("/inventory-hold/place-hold-on-lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/inventory-hold/place-hold-on-lot/" +
                                "add-hold-to-lot/add-cis-quar-hold-request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }
}
