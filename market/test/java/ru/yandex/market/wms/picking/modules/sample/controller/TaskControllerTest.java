package ru.yandex.market.wms.picking.modules.sample.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.dimensionmanagement.client.DimensionManagementClient;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class TaskControllerTest extends IntegrationTest {

    @Autowired
    @SpyBean
    private DimensionManagementClient dimensionManagementClient;

    @BeforeEach
    public void before() {
        Mockito.reset(dimensionManagementClient);
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsFirstPage() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/first-page.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsNextPage() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("offset", "2")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/next-page.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsPageWhenOffsetIsEqualToTasksCount() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("offset", "5"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/offset-is-equal-to-tasks-count.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsPageWhenOffsetIsGreaterThanTasksCount() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("offset", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/offset-is-greater-than-tasks-count.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsFirstPageWithFilterByStatus() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("filter", "status==COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/filter-by-status.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsFirstPageWithFilterBySerialNumberWithRegex() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("filter", "serialNumber==%987%"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/filter-by-serial-number-regex.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsFirstPageWithFilterByAddDateAndEditDate() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("filter", "addDate=='2020-01-01 03:01:00';editDate=='2020-01-01 03:01:00'"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/filter-by-add-date-and-edit-date.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsFirstPageWithSortByUserKeyDescending() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("sort", "userKey")
                        .param("order", "desc")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/sort-by-user-key-desc.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsFirstPageWithFilterByUserKeyAndSortBySerialNumberAscending() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("filter", "(userKey==test,userKey=='')")
                        .param("sort", "serialNumber")
                        .param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/filter-by-user-key-sort-by-serial-number.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsPageWithFilterBySameTaskType() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("filter", "taskType=='MSRMNT_STK'")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/filter-by-same-task-type.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksReturnsPageWithFilterByAnotherTaskType() throws Exception {
        mockMvc.perform(get("/sample/task/MSRMNT_STK")
                        .param("filter", "taskType=='PK'"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/filter-by-another-task-type.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/list-tasks/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/list-tasks/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void listTasksFailsWhenTaskTypeIsUnsupported() throws Exception {
        mockMvc.perform(get("/sample/task/PK")
                        .param("limit", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/list-tasks/task-type-unsupported.json"), true));
    }

    @Test
    @DatabaseSetup("/sample/controller/task/base.xml")
    @DatabaseSetup("/sample/controller/task/start-task-happy-pass/before.xml")
    @ExpectedDatabase(value = "/sample/controller/task/start-task-happy-pass/after.xml", assertionMode = NON_STRICT)
    public void startTaskHappyPass() throws Exception {
        mockMvc.perform(put("/sample/task/0000000203/type/MSRMNT_STK/start"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/base.xml")
    @DatabaseSetup("/sample/controller/task/start-task-of-another-user/immutable.xml")
    @ExpectedDatabase(value = "/sample/controller/task/start-task-of-another-user/immutable.xml",
            assertionMode = NON_STRICT)
    public void startTaskOfAnotherUser() throws Exception {
        mockMvc.perform(put("/sample/task/0000000203/type/MSRMNT_STK/start"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/start-task-of-another-user/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/base.xml")
    @DatabaseSetup("/sample/controller/task/start-task-when-not-exists/immutable.xml")
    @ExpectedDatabase(value = "/sample/controller/task/start-task-when-not-exists/immutable.xml",
            assertionMode = NON_STRICT)
    public void startTaskWhenNotExists() throws Exception {
        mockMvc.perform(put("/sample/task/0000000204/type/MSRMNT_STK/start"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/start-task-when-not-exists/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/containers/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/containers/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void containersHappyPath() throws Exception {
        mockMvc.perform(get("/sample/task/containers"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/containers/happy-path.json"
                )))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/containers/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/containers/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void containersSecondPage() throws Exception {
        mockMvc.perform(get("/sample/task/containers")
                .param("limit", "3")
                .param("offset", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/containers/second-page.json"
                )))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/containers/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/task/containers/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void containersFilterCompleted() throws Exception {
        mockMvc.perform(get("/sample/task/containers")
                .param("filter", "status==COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/containers/filter-completed.json"
                )))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/base.xml")
    @DatabaseSetup("/sample/controller/task/cancel-task-happy-pass/before.xml")
    @ExpectedDatabase(value = "/sample/controller/task/cancel-task-happy-pass/after.xml", assertionMode = NON_STRICT)
    public void cancelTaskHappyPass() throws Exception {
        Mockito.doNothing().when(dimensionManagementClient)
                .cancelOrder(ArgumentMatchers.any());

        mockMvc.perform(put("/sample/task/0000000203/cancel"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/base.xml")
    @DatabaseSetup("/sample/controller/task/cancel-task-in-terminal-state/immutable.xml")
    @ExpectedDatabase(value = "/sample/controller/task/cancel-task-in-terminal-state/immutable.xml",
            assertionMode = NON_STRICT)
    public void cancelTaskInTerminalState() throws Exception {
        Mockito.doNothing()
                .when(dimensionManagementClient)
                .cancelOrder(ArgumentMatchers.any());

        mockMvc.perform(put("/sample/task/0000000204/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/cancel-task-in-terminal-state/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/task/base.xml")
    @DatabaseSetup("/sample/controller/task/cancel-task-in-not-cancellable-type/immutable.xml")
    @ExpectedDatabase(value = "/sample/controller/task/cancel-task-in-not-cancellable-type/immutable.xml",
            assertionMode = NON_STRICT)
    public void cancelTaskInNotCancellableType() throws Exception {
        Mockito.doNothing()
                .when(dimensionManagementClient)
                .cancelOrder(ArgumentMatchers.any());

        mockMvc.perform(put("/sample/task/0000000205/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "sample/controller/task/cancel-task-in-not-cancellable-type/response.json")))
                .andReturn();
    }
}
