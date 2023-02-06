package ru.yandex.market.wms.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.model.request.TaskManagerUserDetailCreateRequest;
import ru.yandex.market.wms.auth.model.request.TaskManagerUserDetailDeleteRequest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TaskManagerUserDetailControllerTest extends AuthIntegrationTest {

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/insert/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/insert/after.xml", assertionMode = NON_STRICT)
    public void insert() throws Exception {
        TaskManagerUserDetailCreateRequest request = TaskManagerUserDetailCreateRequest.builder()
                .userKey("user2")
                .userLineNumber("00006")
                .permissionType("")
                .areaKey("")
                .description("")
                .addWho("auth")
                .editWho("auth")
                .build();

        ResultActions result = mockMvc.perform(post("/task-manager-user-detail")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().string(matchesRegex("[0-9]+")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/insert/before.xml")
    public void insertDuplicate() throws Exception {
        TaskManagerUserDetailCreateRequest request = TaskManagerUserDetailCreateRequest.builder()
                .userKey("user2")
                .userLineNumber("00004")
                .addWho("auth")
                .editWho("auth")
                .build();
        ResultActions result = mockMvc.perform(post("/task-manager-user-detail")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isInternalServerError());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/after.xml", assertionMode = NON_STRICT)
    public void deleteBySerialKey() throws Exception {
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/task-manager-user-detail/234")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/after.xml", assertionMode = NON_STRICT)
    public void deleteByPrimaryKey() throws Exception {
        TaskManagerUserDetailDeleteRequest request = TaskManagerUserDetailDeleteRequest.builder()
                .userKey("user1")
                .userLineNumber("00005")
                .build();
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/task-manager-user-detail")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(
            value = "/db/dao/task-manager-user-detail/delete/after-delete-by-login.xml",
            assertionMode = NON_STRICT
    )
    public void deleteByLogin() throws Exception {
        TaskManagerUserDetailDeleteRequest request = TaskManagerUserDetailDeleteRequest.builder()
                .userKey("user1")
                .build();
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/task-manager-user-detail")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistentBySerialKey() throws Exception {
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/task-manager-user-detail/656")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user-detail/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user-detail/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistentByPrimaryKey() throws Exception {
        TaskManagerUserDetailDeleteRequest request = TaskManagerUserDetailDeleteRequest.builder()
                .userKey("user10")
                .userLineNumber("00005")
                .build();
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/task-manager-user-detail")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }
}
