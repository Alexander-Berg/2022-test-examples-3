package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/pick_locations.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/2/skus.xml", connection = "wmwhseConnection")
})
public class PickingItemCheckControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-valid-uit.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-valid-uit.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckValidUit() throws Exception {
        mockMvc.perform(
                post("/check-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/check-item/request-uit.json"))
        )
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-uit.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-uit-user-has-no-assignments.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-uit-user-has-no-assignments.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckUitUserHasNoAssignments() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-uit.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-user-has-no-assignments.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-uit-wrong-sku.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-uit-wrong-sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckUitWrongSku() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-uit.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-wrong-sku.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-uit-wrong-lot.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-uit-wrong-lot.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckUitWrongLot() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-uit.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-wrong-lot.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-uit-wrong-loc.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-uit-wrong-loc.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckUitWrongLoc() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-uit.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-wrong-loc.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-uit-wrong-id.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-uit-wrong-id.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckUitWrongFromId() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-uit.json"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-wrong-id.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-uit-invalid-balance.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-uit-invalid-balance.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckUitInvalidBalance() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-uit.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-invalid-balance.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-valid-id.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-valid-id.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckValidId() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-id.json"))
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-id.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-id-invalid-balances.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-id-invalid-balances.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckIdInvalidBalances() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-id.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-invalid-balances.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-id-has-excessive-skus.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-id-has-excessive-skus.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckIdHasExcessiveSkus() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-id.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-id-has-excessive-items.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-id-has-not-enough-skus.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-id-has-not-enough-skus.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckIdHasNotEnoughSkus() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-id.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-id-has-not-enough-items.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-id-has-excessive-items.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-id-has-excessive-items.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckIdHasExcessiveItems() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-id.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-id-has-excessive-items.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-id-has-not-enough-items.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-id-has-not-enough-items.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckIdHasNotEnoughItems() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-id.json"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-id-has-not-enough-items.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/check-item/before-id-has-multiple-assignments.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/check-item/before-id-has-multiple-assignments.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testCheckIdHasMultipleAssignments() throws Exception {
        mockMvc.perform(
                        post("/check-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/check-item/request-id.json"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/check-item/response-id-has-multiple-assignments.json"),
                                STRICT
                        )
                )
                .andReturn();
    }
}
