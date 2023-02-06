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

public class MoveItemControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/move-item/happy-path/before.xml")
    @ExpectedDatabase(value = "/move-item/happy-path/after.xml", assertionMode = NON_STRICT)
    public void moveItemHappyPath() throws Exception {
        mockMvc.perform(post("/move-item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("move-item/happy-path/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/happy-path-with-id/before.xml")
    @ExpectedDatabase(value = "/move-item/happy-path-with-id/after.xml", assertionMode = NON_STRICT)
    public void moveItemHappyPathWithContainerId() throws Exception {
        mockMvc.perform(post("/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-item/happy-path-with-id/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * В ячейке лежат 2 НЗН с одинаковыми товарами, юзер сканирует УИТ из неправильного НЗН
     */
    @Test
    @DatabaseSetup("/move-item/happy-path-with-id/before.xml")
    @ExpectedDatabase(value = "/move-item/happy-path-with-id/before.xml", assertionMode = NON_STRICT)
    public void moveItemFromWrongContainer() throws Exception {
        mockMvc.perform(post("/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-item/happy-path-with-id/request-wrong-id.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(StringContains.containsString(WmsErrorCode.ID_WAS_WRONG.name())))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/happy-path-tote-no-assignment-number/before.xml")
    @ExpectedDatabase(value = "/move-item/happy-path-tote-no-assignment-number/after.xml", assertionMode = NON_STRICT)
    public void moveItemHappyPathWhenPickingToteHasNoAssignmentNumber() throws Exception {
        mockMvc.perform(post("/move-item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("move-item/happy-path-tote-no-assignment-number/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/picking-tote-with-no-user/immutable.xml")
    @ExpectedDatabase(value = "/move-item/picking-tote-with-no-user/immutable.xml", assertionMode = NON_STRICT)
    public void moveItemPickingToteNotAttachedToUser() throws Exception {
        mockMvc.perform(post("/move-item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("move-item/picking-tote-with-no-user/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("move-item/picking-tote-with-no-user/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/assignments-not-match/immutable.xml")
    @ExpectedDatabase(value = "/move-item/assignments-not-match/immutable.xml", assertionMode = NON_STRICT)
    public void moveItemTaskDetailAndPickingToteAssignmentsDifferent() throws Exception {
        mockMvc.perform(post("/move-item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("move-item/assignments-not-match/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("move-item/assignments-not-match/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/weight-exceeded/immutable.xml")
    @ExpectedDatabase(value = "/move-item/weight-exceeded/immutable.xml", assertionMode = NON_STRICT)
    public void moveItemWeightExceeded() throws Exception {
        mockMvc.perform(post("/move-item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("move-item/weight-exceeded/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("move-item/weight-exceeded/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/another-assigment/before.xml")
    @ExpectedDatabase(value = "/move-item/another-assigment/before.xml", assertionMode = NON_STRICT)
    public void moveSingleItemAnotherAssigmentNumber() throws Exception {
        mockMvc.perform(post("/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-item/another-assigment/request.json")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("move-item/another-assigment/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/move-to-specific-loc/before.xml")
    @ExpectedDatabase(value = "/move-item/move-to-specific-loc/after.xml", assertionMode = NON_STRICT)
    public void moveItemToContainerWithSpecificLoc() throws Exception {
        mockMvc.perform(post("/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-item/move-to-specific-loc/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/move-item/move-to-loc-from-task-details/before.xml")
    @ExpectedDatabase(value = "/move-item/move-to-loc-from-task-details/after.xml", assertionMode = NON_STRICT)
    public void moveItemToContainerWithLocFromTaskDetails() throws Exception {
        mockMvc.perform(post("/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("move-item/move-to-loc-from-task-details/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }
}
