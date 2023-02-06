package ru.yandex.market.logistics.dbqueue.controller;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.logistics.dbqueue.AbstractContextualTest;
import ru.yandex.market.logistics.dbqueue.dao.DbQueueTaskDao;
import ru.yandex.market.logistics.dbqueue.domain.DbQueueTask;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;

@WebAppConfiguration
@DatabaseSetup({
    "/tasks.xml",
    "/tasks_log.xml",
})
class DbQueueControllerTest extends AbstractContextualTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DbQueueTaskDao taskDao;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .build();
    }

    @Test
    public void testList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
            .get("/admin/dbqueue"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all.json"));
    }

    @Test
    public void testUnapplicableFieldFilterIgnore() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("(DROP TABLE dbqueue)", "true")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all.json"));
    }

    @Test
    public void testListWithPaging0() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("page", "0")
                .param("size", "1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("page0.json"));
    }

    @Test
    public void testListWithPaging1() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("page", "1")
                .param("size", "1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("page1.json"));
    }

    @Test
    public void testFilteringQueueIdLike() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("queueName", "test.queue.")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all.json"));
    }

    @Test
    public void testFilteringQueueId() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("queueName", "test.queue.1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("only0.json"));
    }


    @Test
    public void testFilteringAttempt2() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("attempt", "2")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("only0.json"));
    }

    @Test
    public void testFilteringAttemptNone() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("attempt", "1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("empty.json"));
    }

    @Test
    public void testFilteringAttemptExceededTrue() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("retryCompleted", "true")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("only0.json"));
    }

    @Test
    public void testFilteringAttemptExceededFalse() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("retryCompleted", "false")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("only1.json"));
    }

    @Test
    public void testSortQueueNameAsc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "queueName,asc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all.json"));
    }

    @Test
    public void testSortQueueNameDesc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "queueName,desc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all_reversed.json"));
    }


    @Test
    public void testSortCreateTimeAsc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "createTime,asc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all.json"));
    }

    @Test
    public void testSortCreateTimeDesc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "createTime,desc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all_reversed.json"));
    }


    @Test
    public void testSortProcessTimeAsc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "processTime,asc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all_reversed.json"));
    }

    @Test
    public void testSortProcessTimeDesc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "processTime,desc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all.json"));
    }


    @Test
    public void testSortAttemptAsc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "attempt,asc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all.json"));
    }

    @Test
    public void testSortAttemptDesc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/dbqueue")
                .param("sort", "attempt,desc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("all_reversed.json"));
    }

    @Test
    @DatabaseSetup("/tasks-unsupported-queue.xml")
    public void testMissingQueueMeta() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
            .get("/admin/dbqueue"))
            .andExpect(MockMvcResultMatchers.status().is5xxServerError())
            .andExpect(errorMessage("Queue with id test.queue.3 is not registered"));
    }

    @Test
    public void getSingleTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
            .get("/admin/dbqueue/1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("single.json"));
    }

    @Test
    public void getSingleNotFoundTest() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
            .get("/admin/dbqueue/3"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(errorMessage("404 NOT_FOUND \"DBQueue task with id 3 not found\""))
            .andReturn();

        assertNotFoundException(result.getResolvedException());
    }

    @Test
    public void reEnqueueMissing() throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/reenqueue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [3]}")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(errorMessage("404 NOT_FOUND \"DBQueue task with id 3 not found\""))
            .andReturn();

        assertReEnqueueAttemptCount(1L, 0L);
        assertReEnqueueAttemptCount(2L, 0L);


        assertNotFoundException(result.getResolvedException());
    }

    @Test
    public void reEnqueueEmpty() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/reenqueue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": []}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        assertReEnqueueAttemptCount(1L, 0L);
        assertReEnqueueAttemptCount(2L, 0L);
    }

    @Test
    public void reEnqueue() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/reenqueue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [1]}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        assertReEnqueueAttemptCount(1L, 1L);
        assertReEnqueueAttemptCount(2L, 0L);
    }

    @Test
    public void reEnqueueSingleMissing() throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/reenqueue/single")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 3}")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(errorMessage("404 NOT_FOUND \"DBQueue task with id 3 not found\""))
            .andReturn();

        assertReEnqueueAttemptCount(1L, 0L);
        assertReEnqueueAttemptCount(2L, 0L);

        assertNotFoundException(result.getResolvedException());
    }

    @Test
    public void reEnqueueSingle() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/reenqueue/single")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 1, \"some_other_field\":\"abcd\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        assertReEnqueueAttemptCount(1L, 1L);
        assertReEnqueueAttemptCount(2L, 0L);
    }


    @Test
    public void removeMissing() throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [3]}")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(errorMessage("404 NOT_FOUND \"DBQueue task with id 3 not found\""))
            .andReturn();

        assertTasksExists(1L, 2L);

        assertNotFoundException(result.getResolvedException());
    }

    @Test
    public void removeEmpty() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": []}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        assertTasksExists(1L, 2L);
    }

    @Test
    public void remove() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\": [1]}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        assertTasksNotExists(1L);
        assertTasksExists(2L);
    }

    @Test
    public void removeSingleMissing() throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/remove/single")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 3}")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(errorMessage("404 NOT_FOUND \"DBQueue task with id 3 not found\""))
            .andReturn();

        assertTasksExists(1L, 2L);
        assertNotFoundException(result.getResolvedException());
    }

    @Test
    public void removeSingle() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .post("/admin/dbqueue/remove/single")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 1, \"some_other_field\": \"abcd\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());

        assertTasksNotExists(1L);
        assertTasksExists(2L);
    }

    @Test
    public void testListExt() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
            .get(DbQueueSlug.TASK_LOG))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_all.json"));
    }

    @Test
    public void testUnapplicableFieldFilterIgnoreExt() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("(DROP TABLE dbqueue)", "true")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_all.json"));
    }

    @Test
    public void testListExtWithPaging0() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("page", "0")
                .param("size", "1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_page0.json"));
    }

    @Test
    public void testListExtWithPaging1() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("page", "1")
                .param("size", "1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_page1.json"));
    }

    @Test
    public void testFilteringExtTaskId1() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("taskId", "1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_empty.json"));
    }

    @Test
    public void testFilteringExtTaskId2() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("taskId", "2")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_all.json"));
    }

    @Test
    public void testFilteringExtMessageLike() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("message", "mess")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_all.json"));
    }

    @Test
    public void testFilteringExtMessageLikeNothing() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("message", "abc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_empty.json"));
    }

    @Test
    public void testFilteringExtMessage1() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("message", "message.1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_only0.json"));
    }


    @Test
    public void testFilteringExtRequestId() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("requestId", "abc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_only0.json"));
    }

    @Test
    public void testSortExtRequestIdAsc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("sort", "requestId,asc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_all.json"));
    }

    @Test
    public void testSortExtRequestIdDesc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("sort", "requestId,desc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_reversed.json"));
    }

    @Test
    public void testSortExtMessageAsc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("sort", "message,asc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_all.json"));
    }

    @Test
    public void testSortExtMessageDesc() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(DbQueueSlug.TASK_LOG)
                .param("sort", "message,desc")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("log_reversed.json"));
    }

    private void assertReEnqueueAttemptCount(long id, long count) {
        softly
            .assertThat(taskDao.getById(id))
            .map(DbQueueTask::getReenqueueAttempt)
            .contains(count);
    }

    private void assertTasksExists(long... ids) {
        for (long id : ids) {
            softly
                .assertThat(taskDao.getById(id))
                .isPresent();
        }
    }

    private void assertTasksNotExists(long id) {
        softly
            .assertThat(taskDao.getById(id))
            .isNotPresent();
    }

    private void assertNotFoundException(Exception ex) {
        softly.assertThat(ex).isNotNull();
        softly.assertThat(ex instanceof ResourceNotFoundException).isTrue();
        softly.assertThat(((ResourceNotFoundException) ex).getReason()).isEqualTo("DBQueue task with id 3 not found");
    }


    public static ResultMatcher jsonContent(String file) throws IOException {
        String json = IOUtils.toString(new ClassPathResource("tasks/response/" + file).getInputStream());
        return content().json(json, true);
    }

    public static ResultMatcher textContent(String file) throws IOException {
        String text = IOUtils.toString(new ClassPathResource("tasks/response/" + file).getInputStream());
        return content().string(text);
    }
}
