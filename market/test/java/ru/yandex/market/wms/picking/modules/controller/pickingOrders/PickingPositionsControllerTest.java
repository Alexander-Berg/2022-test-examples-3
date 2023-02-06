package ru.yandex.market.wms.picking.modules.controller.pickingOrders;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/2/pick_locations.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/fixtures/autostart/2/skus.xml", connection = "wmwhseConnection"),
        @DatabaseSetup(value = "/controller/picking-positions/pick-id/config.xml", connection = "wmwhseConnection")
})
public class PickingPositionsControllerTest extends IntegrationTest {

    public static final boolean STRICT = true;

    @Test
    @Disabled
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/autostart/2/pick_locations.xml", connection = "wmwhseConnection"),
            @DatabaseSetup(value = "/fixtures/autostart/2/skus.xml", connection = "wmwhseConnection"),
            @DatabaseSetup(value = "/fixtures/pickingOrders/1/taskdetail.xml", connection = "wmwhseConnection")
    })
    public void get__exists() throws Exception {

        //language=JSON5
        String expected = "{\"items\":[{\"qty\":2,\"waveKey\":\"W1\",\"sku\":\"ROV0000000000000000005\"," +
                "\"fromLoc\":\"C4-10-0001\",\"lot\":\"L5\",\"descr\":\"SKU ROV0000000000000000005\"," +
                "\"zone\":\"FLOOR\",\"pickPositionKey\":[\"PDK5\"],\"box\":null,\"fromId\":\"\"},{\"qty\":1," +
                "\"waveKey\":\"W5\",\"sku\":\"ROV0000000000000000005\",\"fromLoc\":\"C4-10-0001\",\"lot\":\"L5\"," +
                "\"descr\":\"SKU ROV0000000000000000005\",\"zone\":\"FLOOR\",\"pickPositionKey\":[\"PDK5\"]," +
                "\"box\":null,\"fromId\":\"\"}]}";
        mockMvc.perform(get("/picking-positions/AN0001"))
                .andExpect(status().isOk())
                .andExpect(content().json(expected, STRICT));
    }

    @Test
    @DatabaseSetup(value = "/controller/picking-positions/auction/before.xml", connection = "wmwhseConnection")
    public void outboundAuctionPickingPositionsHappyPath() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent("controller/picking-positions/auction/response.json"), STRICT)
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-can-pick-id.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-can-pick-id.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCanPickIdPickingPositionsHappyPath() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                "controller/picking-positions/pick-id/can-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-can-pick-id-but-order-type.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-can-pick-id-but-order-type.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCanPickIdPickingPositionsButOrderTypeNotAllowed() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-id-has-excessive-skus.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-id-has-excessive-skus.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsIdHasExcessiveSkus() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-not-enough-items.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-not-enough-items.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsNotEnoughItems() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-too-many-items.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-too-many-items.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsTooManyItems() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-not-enough-allocated-items.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-not-enough-allocated-items.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsNotEnoughAllocatedItems() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-too-many-allocated-items.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-too-many-allocated-items.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsTooManyAllocatedItems() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-id-has-excessive-lots.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-id-has-excessive-lots.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsIdHasExcessiveLots() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-id-has-wrong-skus.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-id-has-wrong-skus.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsIdHasWrongSkus() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-id-has-wrong-lots.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-id-has-wrong-lots.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCantPickIdPickingPositionsIdHasWrongLots() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-not-pick-id-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-can-pick-multiple-ids.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-can-pick-multiple-ids.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCanPickMultipleIdsPickingPositions() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/" +
                                                "can-pick-multiple-ids-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-can-pick-some-ids.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-can-pick-some-ids.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCanPickSomeIdsPickingPositions() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/can-pick-some-ids-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }

    @Test
    @DatabaseSetup(
            value = "/controller/picking-positions/pick-id/before-id-name-is-empty.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/controller/picking-positions/pick-id/before-id-name-is-empty.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void outboundCanPickIdPickingPositionsIdNameIsEmpty() throws Exception {
        mockMvc.perform(get("/picking-positions/0007140586"))
                .andExpect(status().isOk())
                .andExpect(
                        content().json(getFileContent(
                                        "controller/picking-positions/pick-id/id-is-empty-response.json"),
                                STRICT
                        )
                )
                .andReturn();
    }
}
