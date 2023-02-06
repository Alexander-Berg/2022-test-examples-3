package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.enums.WmsErrorCode;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingItemMovingControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/move-single-item/happy-path/before.xml")
    @ExpectedDatabase(value = "/move-single-item/happy-path/after.xml", assertionMode = NON_STRICT)
    public void moveSingleItemHappyPath() throws Exception {
        mockMvc.perform(post("/move-single-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-single-item/happy-path/request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-single-item/happy-path-with-id/before.xml")
    @ExpectedDatabase(value = "/move-single-item/happy-path-with-id/after.xml", assertionMode = NON_STRICT)
    public void moveSingleItemHappyPathWithContainerId() throws Exception {
        mockMvc.perform(post("/move-single-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-single-item/happy-path-with-id/request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * В ячейке лежат 2 НЗН с одинаковыми товарами, юзер сканирует УИТ из неправильного НЗН
     */
    @Test
    @DatabaseSetup("/move-single-item/happy-path-with-id/before.xml")
    @ExpectedDatabase(value = "/move-single-item/happy-path-with-id/before.xml", assertionMode = NON_STRICT)
    public void moveItemFromWrongContainer() throws Exception {
        mockMvc.perform(post("/move-single-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-single-item/happy-path-with-id/request-wrong-id.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(StringContains.containsString(WmsErrorCode.ID_WAS_WRONG.name())))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-single-item/another-assigment/before.xml")
    @ExpectedDatabase(value = "/move-single-item/another-assigment/before.xml", assertionMode = NON_STRICT)
    public void moveSingleItemAnotherAssigmentNumber() throws Exception {
        mockMvc.perform(post("/move-single-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-single-item/another-assigment/request.json")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("move-single-item/another-assigment/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-single-item/uit-lock/before.xml")
    @ExpectedDatabase(value = "/move-single-item/uit-lock/after.xml", assertionMode = NON_STRICT)
    public void moveSingleItemWithUitLock() throws Exception {
        mockMvc.perform(post("/move-single-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-single-item/uit-lock/request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }
}
