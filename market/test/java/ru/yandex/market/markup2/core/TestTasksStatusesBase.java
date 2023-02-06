package ru.yandex.market.markup2.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.yandex.market.markup2.TasksCache;
import ru.yandex.market.markup2.workflow.TaskTypesContainers;
import ru.yandex.market.markup2.workflow.generation.IRequestGenerator;
import ru.yandex.market.markup2.workflow.pipe.Pipes;
import ru.yandex.market.markup2.workflow.resultMaker.IResultMaker;
import ru.yandex.market.markup2.workflow.taskDataUnique.TaskDataUniqueStrategy;
import ru.yandex.market.markup2.workflow.taskType.TaskTypeContainer;
import ru.yandex.market.markup2.workflow.taskType.processor.SkippingTaskTypeProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author york
 * @since 16.04.2019
 */
public class TestTasksStatusesBase extends MockMarkupTestBase {
    private static final Logger log = LogManager.getLogger();

    protected static final int TYPE_ID = 1;
    protected static final int CATEGORY_ID = 1;

    protected List<Integer> pool = new ArrayList<>();
    protected AllBeans allbeans;

    protected void init() throws Exception {
        allbeans = createNew();
        clearStepLocks();
    }

    @Override
    protected TaskTypesContainers createTaskTypeContainers(TasksCache tasksCache) {
        TaskTypeContainer<I, P, R> typeContainer = new TaskTypeContainer<>();

        typeContainer.setHitmanCode("yo");
        typeContainer.setMinBatchCount(1);
        typeContainer.setMaxBatchCount(2);
        typeContainer.setName("n");
        typeContainer.setDataUniqueStrategy(TaskDataUniqueStrategy.TASK);
        typeContainer.setPipe(Pipes.SEQUENTIALLY);

        typeContainer.setDataItemPayloadClass(P.class);
        typeContainer.setDataItemResponseClass(R.class);
        typeContainer.setDataItemIdentifierClass(I.class);

        IRequestGenerator<I, P, R> requestGenerator = context -> {
            Iterator<Integer> iter = pool.iterator();
            while (context.getLeftToGenerate() > 0 && iter.hasNext()) {
                Integer item = iter.next();
                P payload = new P(item, "payload" + item);
                if (context.createTaskDataItem(payload)) {
                    log.debug("Taking {} for ({}) ", item, context.getTask().getId());
                    iter.remove();
                } else {
                    log.debug("Not created {} for ({}) ", item, context.getTask().getId());
                }
            }
        };

        IResultMaker<I, P, R> resultCollector = resultsContext -> { };
        SkippingTaskTypeProcessor<I, P, R> skippingTaskTypeProcessor = createProcessor(
            requestGenerator,
            resultCollector,
            null,
            R.class
        );

        typeContainer.setProcessor(skippingTaskTypeProcessor);

        TaskTypesContainers taskTypesContainers = new TaskTypesContainers();
        taskTypesContainers.setTaskTypeContainers(Collections.singletonMap(1, typeContainer));
        return taskTypesContainers;
    }
}
