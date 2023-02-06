package ru.yandex.market.markup2.utils;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.io.Resources;
import ru.yandex.market.markup2.AppContext;
import ru.yandex.market.markup2.dao.DefaultPersister;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.group.TaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.general.ITaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.IdGenerator;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
import ru.yandex.market.markup2.workflow.resultMaker.ResultMakerContext;
import ru.yandex.market.markup2.workflow.taskDataUnique.FullTaskDataUniqueContext;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
public class Markup2TestUtils {

    private Markup2TestUtils() {

    }

    public static IdGenerator mockIdGenerator() {
        DefaultPersister defaultPersister = mock(DefaultPersister.class);
        AtomicLong idsGen = new AtomicLong(1);
        when(defaultPersister.generateNextLong()).thenReturn(idsGen.incrementAndGet());
        return new IdGenerator(defaultPersister);
    }

    public static TaskInfo createBasicTaskInfo(int categoryId, int count, Map<ParameterType, Object> parameters) {
        TaskConfigGroupInfo taskConfigGroupInfo = new TaskConfigGroupInfo.Builder()
            .setCategoryId(categoryId)
            .addParameters(parameters)
            .build();

        TaskConfigInfo taskConfigInfo = new TaskConfigInfo.Builder()
            .setGroupInfo(taskConfigGroupInfo)
            .setCount(count)
            .build();

        return new TaskInfo.Builder()
            .setId(1)
            .setConfig(taskConfigInfo)
            .setCreationTime(System.currentTimeMillis())
            .build();
    }

    public static TaskInfo createBasicTaskInfo(int categoryId, int count,
                                               int contentRetryInterval, Map<ParameterType, Object> parameters) {
        TaskConfigGroupInfo taskConfigGroupInfo = new TaskConfigGroupInfo.Builder()
            .setCategoryId(categoryId)
            .addParameters(parameters)
            .build();

        TaskConfigInfo taskConfigInfo = new TaskConfigInfo.Builder()
            .setGroupInfo(taskConfigGroupInfo)
            .setCount(count)
            .setContentRetryInterval(contentRetryInterval)
            .build();

        return new TaskInfo.Builder()
            .setId(1)
            .setConfig(taskConfigInfo)
            .setCreationTime(System.currentTimeMillis())
            .build();
    }

    public static <T> FullTaskDataUniqueContext<T> createBasicUniqueContext() {
        return new FullTaskDataUniqueContext<T>() {

            private Set<T> tasks = new HashSet<>();

            @Override
            public boolean addIfAbsent(T value) {
                return tasks.add(value);
            }

            @Override
            public Set<T> addIfAbsent(Collection<T> values) {
                Set<T> result = new HashSet<>();
                for (T value : values) {
                    if (tasks.add(value)) {
                        result.add(value);
                    }
                }
                return result;
            }

            @Override
            public void delete(Collection<T> values) {
                tasks.removeAll(values);
            }

            @Override
            public void delete(T value) {
                tasks.remove(value);
            }

            @Override
            public Set<T> getIdentities(int taskId) {
                return tasks;
            }

            @Override
            public boolean isProcessedByTask(Object value, int taskId) {
                return tasks.contains(value);
            }

            @Override
            public Set<T> getProcessedIdentifiers() {
                return new HashSet<>(tasks);
            }

            @Override
            public boolean isProcessed(T identifier) {
                return tasks.contains(identifier);
            }

        };
    }

    public static <I, D extends ITaskDataItemPayload<I>, R extends IResponseItem> RequestGeneratorContext<I, D, R>
        createGenerationContext(TaskInfo info, FullTaskDataUniqueContext<I> taskDataUniqueContext,
                                IdGenerator idGenerator) {
        return new RequestGeneratorContext<>(mock(AppContext.class), info, taskDataUniqueContext, idGenerator);
    }

    public static <I, D extends ITaskDataItemPayload<I>, R extends IResponseItem> ResultMakerContext<I, D, R>
        createResultMakerContext(TaskInfo taskInfo) {
        return new ResultMakerContext<>(mock(AppContext.class), taskInfo);
    }

    public static String getResource(String resourceName) throws IOException {
        URL resource = Resources.getResource(resourceName);
        return Resources.toString(resource, StandardCharsets.UTF_8);
    }

    public static ObjectMapper defaultMapper(Class<?>... classes) {
        SimpleModule module = new SimpleModule();

        for (Class<?> clazz : classes) {
            addConverters(module, clazz);
        }

        return new ObjectMapper().registerModule(module);
    }

    public static <T> ObjectMapper defaultMapper(Class<T> customSerializationClass,
                                             JsonSerializer<T> customSerializer,
                                             JsonDeserializer<T> customDeserializer,
                                             Class<?>... classes) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(customSerializationClass, customSerializer);
        if (customDeserializer != null) {
            module.addDeserializer(customSerializationClass, customDeserializer);
        }
        for (Class<?> clazz : classes) {
            addConverters(module, clazz);
        }

        return new ObjectMapper().registerModule(module);
    }

    private static <T> void addConverters(SimpleModule module, Class<T>  clazz) {
        module.addSerializer(clazz, new JsonUtils.DefaultJsonSerializer<>());
        module.addDeserializer(clazz, new JsonUtils.DefaultJsonDeserializer<>(clazz));
    }
}
