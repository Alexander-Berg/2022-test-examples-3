package ru.yandex.market.markup2.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.yandex.market.markup2.dao.HitmanExecutionDataPersister;
import ru.yandex.market.markup2.entries.hitman.HitmanExecutionData;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.hitman.HitmanApiHandler;
import ru.yandex.qe.hitman.main.api.v1.dto.JobStatusDto;
import ru.yandex.qe.hitman.main.api.v1.dto.StartedExecution;
import ru.yandex.qe.hitman.main.data.model.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author york
 * @since 25.05.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class HitmanApiHandlerStub extends HitmanApiHandler {
    private static final Logger log = LogManager.getLogger();
    private HitmanExecutionDataPersister hitmanExecutionDataPersister;
    private Integer idSeq = 0;
    private Map<String, List<JsonNode>> resultsMap = new HashMap<>();

    @Override
    public <T> StartedExecution startExecution(String processCode,
                                               Collection<T> data,
                                               Function<T, JsonNode> converter,
                                               Map<String, String> hitmanProperties) {

        String executionId = String.valueOf(idSeq++);
        return new StartedExecution(executionId, "url_" + executionId);
    }

    @Override
    public JobStatusDto checkStatus(String executionId) {
        JobStatusDto dto = new JobStatusDto(
                0L,
            Long.parseLong(executionId),
            "",
            resultsMap.containsKey(executionId) ? Status.SUCCEEDED : Status.RUNNING,
            0.1d,
            "kuku"
        );
        return dto;
    }

    public <T extends IResponseItem> void addResults(String executionId, List<T> results) {
        addResults(executionId, results, new JsonUtils.DefaultJsonSerializer<>());
    }

    public <T extends IResponseItem> void addResults(String executionId, List<T> results,
                                                     JsonSerializer<T> serializer) {
        ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(
                new SimpleModule()
                    .addSerializer((Class<? extends T>) results.get(0).getClass(), serializer)
            );

        List<JsonNode> nodes = new ArrayList<>();

        for (T result : results) {
            nodes.add(objectMapper.valueToTree(result));
        }
        resultsMap.put(executionId, nodes);
        log.debug("{} is now finished ", executionId);
    }

    public <T extends IResponseItem> void addResultsForTask(int taskId, List<T> results) {
        List<HitmanExecutionData> data = hitmanExecutionDataPersister.getByTaskId(taskId);
        //expecting only one execution per task
        String executionId = data.get(0).getHitmanExecutionId();
        addResults(executionId, results);
    }

    public void addJsonResultsForTask(int taskId, List<JsonNode> results) {
        List<HitmanExecutionData> data = hitmanExecutionDataPersister.getByTaskId(taskId);
        //expecting only one execution per task
        String executionId = data.get(0).getHitmanExecutionId();
        resultsMap.put(executionId, results);
        log.debug("{} is now finished ", executionId);
    }

    @Override
    public void downloadResult(String executionId, Consumer<JsonNode> resultConsumer) {
        List<JsonNode> result = resultsMap.remove(executionId);
        result.forEach(resultConsumer);
    }

    public void setHitmanExecutionDataPersister(HitmanExecutionDataPersister hitmanExecutionDataPersister) {
        this.hitmanExecutionDataPersister = hitmanExecutionDataPersister;
    }
}
