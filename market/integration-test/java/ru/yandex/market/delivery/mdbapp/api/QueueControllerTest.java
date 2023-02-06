package ru.yandex.market.delivery.mdbapp.api;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.QueueDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = "/data/health/queue/clean-tasks.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/data/health/queue/insert-tasks.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/data/health/queue/clean-tasks.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
// Запускаем с новым контекстом, чтобы другие тесты не влияли
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class QueueControllerTest extends MockContextualTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    @Qualifier("commonJsonMapper")
    private ObjectMapper mapper;

    @Test
    public void testCorrectStatisticReturned() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/queue/statistic")
                .accept(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andReturn();

        List<QueueDto> statsResponse = mapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            new TypeReference<>() {
            }
        );

        softly.assertThat(statsResponse.size()).as("Response size check").isEqualTo(2);
        softly.assertThat(statsResponse).as("Response objects check").isEqualTo(buildStats());
    }

    @Test
    public void whenResetThanQueuesFailedSizeSetToZero() throws Exception {
        MvcResult reset = mvc.perform(patch("/queue/resetAttempts")
            .accept(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode resetResponseNode = mapper.readTree(reset.getResponse().getContentAsString());
        ObjectNode resetExpectedNode = JsonNodeFactory.instance.objectNode().put("resetTasksCount", 15);

        MvcResult afterReset = mvc.perform(get("/queue/statistic").accept(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andReturn();

        List<QueueDto> responseAfterReset = mapper.readValue(
            afterReset.getResponse().getContentAsString(),
            new TypeReference<>() {
            }
        );

        softly.assertThat(resetResponseNode)
            .as("Check expected response from reset").isEqualTo(resetExpectedNode);
        softly.assertThat(responseAfterReset.size())
            .as("After reset Response size check").isEqualTo(2);
        softly.assertThat(responseAfterReset)
            .as("After reset Response objects check").isEqualTo(buildStats());
    }

    @Nonnull
    private List<QueueDto> buildStats() {
        QueueDto mailQueueDto;
        QueueDto cancelParcelQueueDto;

        mailQueueDto = new QueueDto("mail.queue", 8, 0);
        cancelParcelQueueDto = new QueueDto("parcel.cancel", 7, 0);

        mailQueueDto.setMaxAttempts(10);
        mailQueueDto.setMaxFailedTasks(3);

        cancelParcelQueueDto.setMaxAttempts(15);
        cancelParcelQueueDto.setMaxFailedTasks(5);

        return Arrays.asList(mailQueueDto, cancelParcelQueueDto);
    }
}
