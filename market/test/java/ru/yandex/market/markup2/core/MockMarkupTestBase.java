package ru.yandex.market.markup2.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

import ru.yandex.market.markup2.AppContext;
import ru.yandex.market.markup2.HitmanExecutionsCache;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.YangPoolsCache;
import ru.yandex.market.markup2.core.stubs.TaskConfigSchedulerStub;
import ru.yandex.market.markup2.core.stubs.TaskProcessManagerStub;
import ru.yandex.market.markup2.core.stubs.persisters.DefaultPersisterStub;
import ru.yandex.market.markup2.core.stubs.persisters.IPersisterStub;
import ru.yandex.market.markup2.dao.DefaultPersister;
import ru.yandex.market.markup2.dao.HitmanExecutionDataPersister;
import ru.yandex.market.markup2.dao.HitmanExecutionPersister;
import ru.yandex.market.markup2.dao.MarkupDao;
import ru.yandex.market.markup2.dao.YangPoolPersister;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.entries.task.TaskState;
import ru.yandex.market.markup2.loading.MarkupLoader;
import ru.yandex.market.markup2.processors.IMarkupTasksProcessor;
import ru.yandex.market.markup2.processors.MarkupManager;
import ru.yandex.market.markup2.processors.task.DataItems;
import ru.yandex.market.markup2.processors.task.ITaskProcessor;
import ru.yandex.market.markup2.processors.task.TaskProgress;
import ru.yandex.market.markup2.processors.task.processors.AbstractTaskProcessor;
import ru.yandex.market.markup2.processors.task.processors.CanceledTaskProcessor;
import ru.yandex.market.markup2.processors.task.processors.CompletedTaskProcessor;
import ru.yandex.market.markup2.processors.task.processors.ForceFinishedTaskProcessor;
import ru.yandex.market.markup2.processors.task.processors.ForceFinishingTaskProcessor;
import ru.yandex.market.markup2.processors.task.processors.NewTaskProcessor;
import ru.yandex.market.markup2.processors.task.processors.PausedTaskProcessor;
import ru.yandex.market.markup2.processors.task.processors.RunningTaskProcessor;
import ru.yandex.market.markup2.processors.taskConfig.ITaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.TaskConfigScheduler;
import ru.yandex.market.markup2.processors.taskConfig.processors.AbstractTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.ActiveTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.CanceledTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.DeactivatedTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.DisabledTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.FinishedTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.ForceFinishedTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.ForceFinishingTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.NewTaskConfigProcessor;
import ru.yandex.market.markup2.processors.taskConfig.processors.PausedTaskConfigProcessor;
import ru.yandex.market.markup2.utils.JsonUtils;
import ru.yandex.market.markup2.workflow.AbstractPipeStepProcessor;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskProcessManager;
import ru.yandex.market.markup2.workflow.TaskRunnable;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.general.AbstractTaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.general.IdGenerator;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.generation.IRequestGenerator;
import ru.yandex.market.markup2.workflow.generation.RequestsGenerator;
import ru.yandex.market.markup2.workflow.hitman.HitmanApiHandler;
import ru.yandex.market.markup2.workflow.requestSender.RequestsSender;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponseReceiverContext;
import ru.yandex.market.markup2.workflow.responseReceiver.ResponsesReceiver;
import ru.yandex.market.markup2.workflow.resultMaker.IResultMaker;
import ru.yandex.market.markup2.workflow.resultMaker.ResultsMaker;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueCache;
import ru.yandex.market.markup2.workflow.taskFinalizer.TaskFinalizer;
import ru.yandex.market.markup2.workflow.taskType.processor.SimpleDataItemsProcessor;
import ru.yandex.market.markup2.workflow.taskType.processor.SkippingTaskTypeProcessor;
import ru.yandex.market.toloka.ReadthroughResultsDownloader;
import ru.yandex.market.toloka.TolokaApi;
import ru.yandex.market.toloka.TolokaApiConfiguration;
import ru.yandex.market.toloka.TolokaApiStub;
import ru.yandex.market.toloka.YangResultsDownloader;

/**
 * @author york
 * @since 25.05.2018
 */
public abstract class MockMarkupTestBase {
    private static final Logger log = LogManager.getLogger();
    private final DefaultPersister defaultPersisterStub = new DefaultPersisterStub();

    protected AllBeans allBeans;

    private Multimap<Integer, AbstractPipeStepProcessor> taskStepLocks = HashMultimap.create();
    private Set<AbstractPipeStepProcessor> globalStepLocks = new HashSet<>();

    private Map<Class<? extends AbstractPipeStepProcessor>, AbstractPipeStepProcessor> processorMap = new HashMap<>();

    protected void initAllBeans() throws Exception {
        createNew();
    }

    protected AllBeans createNew() throws Exception {
        setField(YangPoolsCache.getInstance(), "yangPoolsByTaskId", new ConcurrentHashMap<>());
        setField(HitmanExecutionsCache.getInstance(), "executionsByTaskId", new ConcurrentHashMap<>());
        return createForPersisters(new AllPersisters());
    }

    protected AllBeans doRestart(AllBeans context) throws Exception {
        return createForPersisters(context.allPersisters.copy());
    }

    private AllBeans createForPersisters(AllPersisters allPersisters) throws Exception {
        log.debug("create all classes");
        Map<Class<?>, Object> objectMap = new HashMap<>();
        allBeans = new AllBeans(objectMap, allPersisters);
        objectMap.put(AppContext.class, new AppContext());
        objectMap.put(HitmanExecutionPersister.class, new HitmanExecutionPersister());
        objectMap.put(YangPoolPersister.class, new YangPoolPersister());
        MarkupManager markupManager = new MarkupManager();
        objectMap.put(MarkupManager.class, markupManager);
        objectMap.put(IMarkupTasksProcessor.class, markupManager);
        objectMap.put(MarkupDao.class, new MarkupDao());
        TasksCache tasksCache = new TasksCache();
        objectMap.put(TasksCache.class, tasksCache);
        MarkupLoader markupLoader = new MarkupLoader();
        objectMap.put(MarkupLoader.class, markupLoader);
        TaskProcessManager taskProcessManager = new TaskProcessManagerStub();
        objectMap.put(ITaskProcessManager.class, taskProcessManager);
        objectMap.put(TaskConfigScheduler.class, new TaskConfigSchedulerStub());
        TaskDataUniqueCache taskDataUniqueCache = new TaskDataUniqueCache();
        objectMap.put(TaskDataUniqueCache.class, taskDataUniqueCache);

        TransactionTemplate transactionTemplate = Mockito.mock(TransactionTemplate.class);
        Mockito.when(transactionTemplate.execute(Mockito.any())).then(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        objectMap.put(TransactionTemplate.class, transactionTemplate);
        RequestsGenerator requestsGenerator = new RequestsGenerator();
        requestsGenerator.setTransactionTemplate(transactionTemplate);
        ResultsMaker resultsMaker = new ResultsMaker();
        resultsMaker.setTransactionTemplate(transactionTemplate);
        RequestsSender requestsSender = new RequestsSender();
        requestsSender.setTransactionTemplate(transactionTemplate);
        ResponsesReceiver responsesReceiver = new ResponsesReceiver();
        responsesReceiver.setTransactionTemplate(transactionTemplate);
        TaskFinalizer taskFinalizer = new TaskFinalizer();
        taskFinalizer.setTransactionTemplate(transactionTemplate);

        objectMap.put(RequestsGenerator.class, requestsGenerator);
        objectMap.put(ResultsMaker.class, resultsMaker);
        objectMap.put(RequestsSender.class, requestsSender);
        objectMap.put(ResponsesReceiver.class, responsesReceiver);
        objectMap.put(TaskFinalizer.class, taskFinalizer);

        setField(requestsGenerator, "idGenerator", new IdGenerator(defaultPersisterStub));
        setField(requestsGenerator, "threadsPool", createExecutorFor(requestsGenerator));
        setField(resultsMaker, "threadsPool", createExecutorFor(resultsMaker));
        setField(requestsSender, "threadsPool", createExecutorFor(requestsSender));
        setField(responsesReceiver, "threadsPool", createExecutorFor(responsesReceiver));
        setField(taskFinalizer, "threadsPool", createExecutorFor(taskFinalizer));

        HitmanApiHandlerStub hitmanApiHandlerStub = new HitmanApiHandlerStub();
        hitmanApiHandlerStub.setHitmanExecutionDataPersister(allBeans.get(HitmanExecutionDataPersister.class));
        objectMap.put(HitmanApiHandler.class, hitmanApiHandlerStub);

        TolokaApiConfiguration tolokaApiConfiguration = new TolokaApiConfiguration()
            .setApiUrl("https://yang.yandex.ru/api/v1/");
        objectMap.put(TolokaApiConfiguration.class, tolokaApiConfiguration);
        TolokaApiStub tolokaApiStub = new TolokaApiStub(tolokaApiConfiguration);
        objectMap.put(TolokaApi.class, tolokaApiStub);
        YangResultsDownloader yangResultsDownloader = new ReadthroughResultsDownloader();
        yangResultsDownloader.setTolokaApi(tolokaApiStub); // by default readthrough
        yangResultsDownloader.setTransactionTemplate(transactionTemplate);
        objectMap.put(YangResultsDownloader.class, yangResultsDownloader);

        Map<TaskConfigState, ITaskConfigProcessor> taskConfigProcessors =
            ImmutableMap.<TaskConfigState, ITaskConfigProcessor>builder()
                .put(TaskConfigState.NEW, new NewTaskConfigProcessor())
                .put(TaskConfigState.ACTIVE, new ActiveTaskConfigProcessor())
                .put(TaskConfigState.PAUSED, new PausedTaskConfigProcessor())
                .put(TaskConfigState.FORCE_FINISHING, new ForceFinishingTaskConfigProcessor())
                .put(TaskConfigState.FORCE_FINISHED, new ForceFinishedTaskConfigProcessor())
                .put(TaskConfigState.CANCELED, new CanceledTaskConfigProcessor())
                .put(TaskConfigState.DISABLED, new DisabledTaskConfigProcessor())
                .put(TaskConfigState.DEACTIVATED, new DeactivatedTaskConfigProcessor())
                .put(TaskConfigState.FINISHED, new FinishedTaskConfigProcessor())
            .build();

        taskConfigProcessors.values().forEach(p -> objectMap.put(p.getClass(), p));

        Map<TaskState, ITaskProcessor> taskProcessors =
            ImmutableMap.<TaskState, ITaskProcessor>builder()
                .put(TaskState.NEW, new NewTaskProcessor())
                .put(TaskState.RUNNING, new RunningTaskProcessor())
                .put(TaskState.PAUSED, new PausedTaskProcessor())
                .put(TaskState.FORCE_FINISHING, new ForceFinishingTaskProcessor())
                .put(TaskState.FORCE_FINISHED, new ForceFinishedTaskProcessor())
                .put(TaskState.COMPLETED, new CompletedTaskProcessor())
                .put(TaskState.CANCELED, new CanceledTaskProcessor())
            .build();
        taskProcessors.values().forEach(p -> objectMap.put(p.getClass(), p));

        markupManager.setTaskConfigProcessors(taskConfigProcessors);
        markupManager.setTaskProcessors(taskProcessors);
        markupManager.setAlive();

        TaskTypesContainers taskTypeContainers = createTaskTypeContainers(tasksCache);
        objectMap.put(TaskTypesContainers.class, taskTypeContainers);

        fillByType(objectMap);
        for (ITaskConfigProcessor tc : taskConfigProcessors.values()) {
            try {
                ((AbstractTaskConfigProcessor) tc).afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        for (ITaskProcessor tc : taskProcessors.values()) {
            try {
                ((AbstractTaskProcessor) tc).afterPropertiesSet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        taskTypeContainers.initContainers();
        taskDataUniqueCache.afterPropertiesSet();
        markupLoader.afterPropertiesSet();
        return allBeans;
    }

    protected ScheduledExecutorService createExecutorFor(AbstractPipeStepProcessor processor) {
        processorMap.put(processor.getClass(), processor);

        ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);

        Mockito.when(executorService.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any()))
            .thenAnswer(
                invocation -> {
                    TaskRunnable taskRunnable = invocation.getArgument(0);
                    if (!globalStepLocks.contains(processor)
                            && taskStepLocks.put(taskRunnable.getTaskId(), processor)) {
                        taskRunnable.run();
                    } else {
                        log.debug("Task ID={}. step {} is locked", taskRunnable.getTaskId(),
                            processor.getClass().getSimpleName());
                    }
                    return null;
                }
            );
        return executorService;
    }

    protected void clearStepLocks() {
        globalStepLocks.clear();
        taskStepLocks.clear();
    }

    protected void clearStepLocksWithScheduling(TaskProcessManagerStub taskProcessManager) {
        taskStepLocks.keySet().forEach(taskProcessManager::addTask);
        taskStepLocks.clear();
        globalStepLocks.clear();
    }

    protected void processAllTasksWithUnlock(TaskProcessManagerStub taskProcessManager) {
        log.debug("clear step locks");
        clearStepLocksWithScheduling(taskProcessManager);
        taskProcessManager.processAll((t) -> { });
    }

    protected void removeStepLock(int taskId,
                                  Class<? extends AbstractPipeStepProcessor> clazz,
                                  TaskProcessManagerStub taskProcessManager) {
        log.debug("removing {} lock for task {}", clazz.getSimpleName(), taskId);
        log.debug(taskStepLocks);
        taskStepLocks.remove(taskId, processorMap.get(clazz));
        taskProcessManager.addTask(taskId);
    }

    protected void lockStep(int taskId, Class<? extends AbstractPipeStepProcessor> clazz) {
        taskStepLocks.put(taskId, processorMap.get(clazz));
    }

    protected void lockStepForAllTasks(Class<? extends AbstractPipeStepProcessor> clazz) {
        globalStepLocks.add(processorMap.get(clazz));
    }

    protected abstract TaskTypesContainers createTaskTypeContainers(TasksCache cache);

    private void fillByType(Map<Class<?>, Object> objectMap) {
        Set<Object> visited = new HashSet<>();
        Queue<Object> objects = new ArrayDeque<>(objectMap.values());

        while (!objects.isEmpty()) {
            Object inst = objects.poll();

            forEachFields(inst, field -> {
                if (getField(inst, field.getName()) != null) {
                    //already initialized
                    return;
                }
                Object instFromMap = objectMap.get(field.getType());
                if (instFromMap != null) {
//                    log.debug("setting field {} in {} with value {}", field.getName(), inst, instFromMap);
                    setField(inst, field.getName(), instFromMap);
                }
            });
            visited.add(inst);
        }
    }

    protected class AllBeans {
        final Map<Class<?>, Object> map;
        final AllPersisters allPersisters;

        public AllBeans(Map<Class<?>, Object> map, AllPersisters allPersisters) {
            this.map = map;
            this.allPersisters = allPersisters;
            allPersisters.addToMap(map);
        }
        public <T> T get(Class<T> claz) {
            T result = (T) map.get(claz);
            if (result == null) {
                throw new IllegalArgumentException("No such bean " + claz);
            }
            return result;
        }
    }

    protected class AllPersisters {
        private final Map<Class<?>, Object> persisters = new HashMap<>();

        public AllPersisters() {
            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new AssignableTypeFilter(IPersisterStub.class));
            Set<BeanDefinition> components = provider.findCandidateComponents(
                "ru/yandex/market/markup2/core/stubs/persisters");
            for (BeanDefinition component : components) {
                try {
                    Class cls = Class.forName(component.getBeanClassName());
                    log.debug("Adding persister {}", cls.getSuperclass().getSimpleName());
                    persisters.put(cls.getSuperclass(), cls.newInstance());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate persister " + component.getBeanClassName());
                }
            }

        }
        private AllPersisters(Map<Class<?>, Object> other) {
            other.forEach((k, v) -> persisters.put(k, ((IPersisterStub) v).copy()));
        }

        AllPersisters copy() {
            return new AllPersisters(this.persisters);
        }

        public void addToMap(Map<Class<?>, Object> objectMap) {
            objectMap.putAll(this.persisters);
        }
    }

    private void forEachFields(Object object, Consumer<Field> fieldConsumer) {
//        log.debug("Checking " + object);
        ReflectionUtils.doWithFields(object.getClass(), field -> {
            if (field.getType().getName().startsWith("ru.yandex")) {
                fieldConsumer.accept(field);
            }
        }, field -> !Modifier.isStatic(field.getModifiers()));
    }

    public static void setField(Object target, String name, Object value) {
        try {
            Field field = ReflectionUtils.findField(target.getClass(), name, null);
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, target, value);
        } catch (RuntimeException e) {
            throw new RuntimeException("Cannot set field " + name + " for " + target, e);
        }
    }

    public static Object getField(Object target, String name) {
        try {
            Field field = ReflectionUtils.findField(target.getClass(), name);
            ReflectionUtils.makeAccessible(field);
            return ReflectionUtils.getField(field, target);
        } catch (RuntimeException e) {
            throw new RuntimeException("Cannot get field " + name + " for " + target, e);
        }
    }

    public interface HasId {
        int getId();
    }

    public static class I implements HasId {
        private final int id;
        @JsonCreator
        I(@JsonProperty("id") int id) {
            this.id = id;
        }
        public int getId() {
            return id;
        }
        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof I && ((I) obj).id == this.id;
        }
    }

    public static class I2 implements HasId {
        private final int id;
        @JsonCreator
        I2(@JsonProperty("id") int id) {
            this.id = id;
        }
        public int getId() {
            return id;
        }
        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof I2 && ((I2) obj).id == this.id;
        }
    }

    public static class P extends AbstractTaskDataItemPayload<I> {
        private final String data;

        public P(int id, String data) {
            this(new I(id), data);
        }

        @JsonCreator
        public P(@JsonProperty("dataIdentifier") I dataIdentifier, @JsonProperty("data") String data) {
            super(dataIdentifier);
            this.data = data;
        }
        public String getData() {
            return data;
        }
    }

    public static class P2 extends AbstractTaskDataItemPayload<I2> {
        private final String data;

        public P2(int id, String data) {
            this(new I2(id), data);
        }

        @JsonCreator
        public P2(@JsonProperty("dataIdentifier") I2 dataIdentifier, @JsonProperty("data") String data) {
            super(dataIdentifier);
            this.data = data;
        }
        public String getData() {
            return data;
        }
    }

    public static class R implements IResponseItem {
        private final boolean result;
        private final long id;

        @JsonCreator
        public R(@JsonProperty("id") long id, @JsonProperty("result") boolean result) {
            this.id = id;
            this.result = result;
        }
        public boolean isResult() {
            return result;
        }
        @Override
        public long getId() {
            return id;
        }
        @Override
        @JsonIgnore
        public boolean hasResult() {
            return false;
        }
    }

    protected <I, D extends AbstractTaskDataItemPayload<I>, R extends IResponseItem>
    SkippingTaskTypeProcessor<I, D, R> createProcessor(
            IRequestGenerator<I, D, R> requestsGenerator,
            IResultMaker<I, D, R> resultMaker,
            BiFunction<TaskInfo, TaskDataItem<D, R>, TaskDataItemState> newStateFunction,
            Class<R> responseClass
    ) {
        SimpleDataItemsProcessor proc = new SimpleDataItemsProcessor<I, D, R>() {
            @Override
            public JsonSerializer<? super TaskDataItem<D, R>> getRequestSerializer() {
                return new JsonUtils.DefaultJsonSerializer<>();
            }
            @Override
            public Class<R> getResponseClass() {
                return responseClass;
            }
            @Override
            public JsonDeserializer<R> getResponseDeserializer() {
                return new JsonUtils.DefaultJsonDeserializer<>(responseClass);
            }
        };
        proc.setRequestGenerator(requestsGenerator);
        proc.setResultMaker(resultMaker);

        SkippingTaskTypeProcessor<I, D, R> result = new SkippingTaskTypeProcessor<I, D, R>(proc) {
            @Override
            public void receiveResponses(ResponseReceiverContext context) {
                TaskInfo task = context.getTask();
                TaskProgress progress = task.getProgress();
                DataItems<I, D, R> dataItems = progress.getDataItemsByState(TaskDataItemState.SENT);
                Collection<TaskDataItem<D, R>> items = new ArrayList<>(dataItems.getItems());
                items.forEach(i -> {
                    task.getProgress().changeDataItemsState(Collections.singletonList(i),
                        TaskDataItemState.SENT,
                        getNewState(task, i));
                });
                context.addDataItems(items);
            }

            private TaskDataItemState getNewState(TaskInfo task, TaskDataItem<D, R> item) {
                if (newStateFunction == null) {
                    return TaskDataItemState.SUCCESSFUL_RESPONSE;
                }
                return newStateFunction.apply(task, item);
            }
        };

        return result;
    }
}
