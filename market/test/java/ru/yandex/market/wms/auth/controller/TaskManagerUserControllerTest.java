package ru.yandex.market.wms.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.model.enums.TaskManagerUserGender;
import ru.yandex.market.wms.auth.model.request.TaskManagerUserCreateRequest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TaskManagerUserControllerTest extends AuthIntegrationTest {

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/insert/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/insert/after.xml", assertionMode = NON_STRICT)
    public void insert() throws Exception {
        TaskManagerUserCreateRequest request = TaskManagerUserCreateRequest.builder()
                .userKey("user3")
                .build();

        ResultActions result = mockMvc.perform(post("/task-manager-user")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().string(matchesRegex("[0-9]+")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/insert-male/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/insert-male/after.xml", assertionMode = NON_STRICT)
    public void insertMaleGender() throws Exception {
        TaskManagerUserCreateRequest request = TaskManagerUserCreateRequest.builder()
                .userKey("user3")
                .gender(TaskManagerUserGender.MALE)
                .build();

        ResultActions result = mockMvc.perform(post("/task-manager-user")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().string(matchesRegex("[0-9]+")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/insert-staff/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/insert-staff/after.xml", assertionMode = NON_STRICT)
    public void insertStaff() throws Exception {
        TaskManagerUserCreateRequest request = TaskManagerUserCreateRequest.builder()
                .userKey("user3")
                .staffLogin("staff_user3")
                .build();

        ResultActions result = mockMvc.perform(post("/task-manager-user")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk()).andExpect(content().string(matchesRegex("[0-9]+")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/insert/before.xml")
    public void insertDuplicate() throws Exception {
        TaskManagerUserCreateRequest request = TaskManagerUserCreateRequest.builder()
                .userKey("user2")
                .build();
        ResultActions result = mockMvc.perform(post("/task-manager-user")
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isInternalServerError());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/delete/after.xml", assertionMode = NON_STRICT)
    public void delete() throws Exception {
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/task-manager-user/user3")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/task-manager-user/delete/before.xml")
    @ExpectedDatabase(value = "/db/dao/task-manager-user/delete/before.xml", assertionMode = NON_STRICT)
    public void deleteNonExistent() throws Exception {
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.delete("/task-manager-user/user5")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }
}
