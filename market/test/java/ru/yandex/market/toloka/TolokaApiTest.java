package ru.yandex.market.toloka;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.ResultItem;
import ru.yandex.market.toloka.model.Skill;
import ru.yandex.market.toloka.model.Solution;
import ru.yandex.market.toloka.model.TasksResponse;
import ru.yandex.market.toloka.operation.OperationStatus;
import ru.yandex.market.toloka.operation.OperationType;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author galaev
 * @since 2019-04-24
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TolokaApiTest {

    private HttpClient httpClient;
    private TolokaApi tolokaApi;

    @Before
    public void setup() throws IOException {
        httpClient = Mockito.mock(HttpClient.class);
        tolokaApi = new TolokaApi(new TolokaApiConfiguration(), httpClient);
    }

    @Test
    public void testCreatePool() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(invocation -> getHttpResponse("toloka/pool.json"));

        int testId = 7550030;
        String testName = "2019.05.14 MCP-43580 Одежда для слонов";
        Pool pool = new Pool()
            .setPrivateName(testName);
        Pool createdPool = tolokaApi.createPool(pool);
        assertThat(createdPool.getId()).isEqualTo(testId);
        assertThat(createdPool.getPrivateName()).isEqualTo(testName);
    }

    @Test
    public void testOpenPool() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(invocation -> getHttpResponse("toloka/pool_open_response.json"));

        TolokaResponse tolokaResponse = tolokaApi.openPool(1);
        assertThat(tolokaResponse.getId()).isNotEmpty();
        assertThat(tolokaResponse.getType()).isEqualTo(OperationType.POOL_OPEN);
        assertThat(tolokaResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void testClosePool() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(invocation -> getHttpResponse("toloka/pool_close_response.json"));

        TolokaResponse tolokaResponse = tolokaApi.closePool(1);
        assertThat(tolokaResponse.getId()).isNotEmpty();
        assertThat(tolokaResponse.getType()).isEqualTo(OperationType.POOL_CLOSE);
        assertThat(tolokaResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void testGetResults() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(invocation -> getHttpResponse("toloka/results_response.json"));

        List<ResultItem> resultItems = tolokaApi.getResult(1);
        assertThat(resultItems).hasSize(1);
        Solution solution = resultItems.get(0).getSolutions().get(0);
        assertThat(solution.getOutputValues()).containsOnlyKeys("output");
    }

    @Test
    public void testCreateSkill() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(invocation -> getHttpResponse("toloka/sample_skill.json"));

        String testId = "16484";
        String testName = "Определение цвета слона";
        Skill elephantSkill = tolokaApi.createSkill(testName);
        assertThat(elephantSkill.getName()).isEqualTo(testName);
        assertThat(elephantSkill.getId()).isEqualTo(testId);
    }

    @Test
    public void testGetOrCreateSkillByName() throws IOException {
        Mockito.when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(invocation -> getHttpResponse("toloka/skills.json"));

        String testId = "16484";
        String testName = "Определение цвета слона";
        Skill elephantSkill = tolokaApi.getOrCreateSkillByName("Определение цвета слона");
        // verify there is no creation call, just the get request
        Mockito.verify(httpClient, Mockito.only()).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
        assertThat(elephantSkill.getName()).isEqualTo(testName);
        assertThat(elephantSkill.getId()).isEqualTo(testId);
    }

    @Test
    public void createTasksTest() throws IOException{
        TasksResponse response = TolokaJsonUtils.parseJson(getHttpResponse("toloka/createTasks.json"),
                TasksResponse.class);
        Assert.assertTrue(response.toTaskList().get(0).getId().equals("id0"));
        Assert.assertTrue(response.toTaskList().get(1).getId().equals("id1"));
        Assert.assertTrue(response.toTaskList().get(0).getPoolId() == 21);
        Assert.assertTrue(response.toTaskList().get(1).getPoolId() == 21);
    }

    private byte[] getHttpResponse(String filePath) throws IOException {
        InputStream testResponse = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(filePath));
        return IOUtils.toByteArray(testResponse);
    }
}
