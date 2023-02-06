package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.WmsErrorCode;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingIdMovingControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/controller/pick-id/happy-path/before.xml")
    @ExpectedDatabase(value = "/controller/pick-id/happy-path/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void moveIdHappyPath() throws Exception {
        mockMvc.perform(post("/pick-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pick-id/happy-path/request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("{\"items\":[]}"))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/pick-id/happy-path-replace-id/before.xml")
    @ExpectedDatabase(
            value = "/controller/pick-id/happy-path-replace-id/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void moveIdReplaceContainerHappyPath() throws Exception {
        mockMvc.perform(post("/pick-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pick-id/happy-path-replace-id/request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("{\"items\":[]}"))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/pick-id/wrong-id/before.xml")
    @ExpectedDatabase(value = "/controller/pick-id/wrong-id/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void moveWrongId() throws Exception {
        mockMvc.perform(post("/pick-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pick-id/wrong-id/request.json")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(StringContains.containsString(WmsErrorCode.ID_WAS_WRONG.name())))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/pick-id/not-enough-items/before.xml")
    @ExpectedDatabase(value = "/controller/pick-id/not-enough-items/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void moveIdHasNotEnoughItems() throws Exception {
        mockMvc.perform(post("/pick-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pick-id/not-enough-items/request.json")))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(StringContains.containsString(WmsErrorCode.ID_HAS_NOT_ENOUGH_ITEMS.name())))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/pick-id/excessive-items/before.xml")
    @ExpectedDatabase(value = "/controller/pick-id/excessive-items/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void moveIdHasExcessiveItems() throws Exception {
        mockMvc.perform(post("/pick-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pick-id/excessive-items/request.json")))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string(StringContains.containsString(WmsErrorCode.ID_HAS_EXCESSIVE_ITEMS.name())))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/pick-id/replace-id-not-empty/before.xml")
    @ExpectedDatabase(
            value = "/controller/pick-id/replace-id-not-empty/before.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void moveIdReplaceContainerNotEmpty() throws Exception {
        mockMvc.perform(post("/pick-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pick-id/replace-id-not-empty/request.json")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/pick-id/replace-id-not-empty/response.json")))
                .andReturn();
    }


    @Test
    @DatabaseSetup("/controller/pick-id/sku-qty-more-than-one/before.xml")
    @ExpectedDatabase(
            value = "/controller/pick-id/sku-qty-more-than-one/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void moveIdSkuQtyMoreThanOne() throws Exception {
        mockMvc.perform(post("/pick-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pick-id/sku-qty-more-than-one/request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("{\"items\":[]}"))
                .andReturn();
    }
}
