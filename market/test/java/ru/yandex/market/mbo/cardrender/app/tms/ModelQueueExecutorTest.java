package ru.yandex.market.mbo.cardrender.app.tms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cardrender.app.model.base.EventType;
import ru.yandex.market.mbo.cardrender.app.model.base.RenderEvent;
import ru.yandex.market.mbo.cardrender.app.service.CardQueueService;
import ru.yandex.market.mbo.cardrender.app.task.model.BaseRenderTask;
import ru.yandex.market.mbo.cardrender.app.task.model.DeleteTask;
import ru.yandex.market.mbo.cardrender.app.task.model.FastSkuRenderTask;
import ru.yandex.market.mbo.cardrender.app.task.model.GroupsRenderTask;
import ru.yandex.market.mbo.cardrender.app.task.model.SkuRenderTask;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator;
import ru.yandex.market.mbo.taskqueue.TaskQueueTask;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

/**
 * @author apluhin
 * @created 5/24/21
 */
public class ModelQueueExecutorTest {

    private ModelQueueExecutor executor;

    private CardQueueService cardQueueService;
    private TaskQueueRegistrator taskQueueRegistrator;
    private TransactionHelper transactionHelper;

    @Before
    public void setUp() throws Exception {
        cardQueueService = Mockito.mock(CardQueueService.class);
        taskQueueRegistrator = Mockito.mock(TaskQueueRegistrator.class);
        transactionHelper = TransactionHelper.MOCK;
        executor = new ModelQueueExecutor(
                taskQueueRegistrator, cardQueueService, transactionHelper
        );
    }

    @Test
    public void testSimpleBatch() {
        int i = 0;
        Mockito.when(cardQueueService.getNextEventBatch()).thenReturn(Arrays.asList(
                new RenderEvent(1, 2, 1L, CommonModel.Source.SKU, 1, EventType.UPDATE, i++),
                new RenderEvent(1, 2, 2L, CommonModel.Source.SKU, 1, EventType.UPDATE, i++),
                new RenderEvent(1, 2, 3L, CommonModel.Source.GURU, 1, EventType.UPDATE, i++),
                new RenderEvent(1, 2, 5L, CommonModel.Source.FAST_SKU, 1, EventType.UPDATE, i++),
                new RenderEvent(1, 2, 6L, CommonModel.Source.FAST_SKU, 1, EventType.UPDATE, i++),
                new RenderEvent(1, 2, 4L, CommonModel.Source.GURU, 1, EventType.DELETE, i++)
        ));

        executor.doRealJob(null);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<TaskQueueTask> captorDeleteEvent = ArgumentCaptor.forClass(TaskQueueTask.class);
        Mockito.verify(taskQueueRegistrator, Mockito.times(3)).registerTasks(captor.capture());
        Mockito.verify(taskQueueRegistrator, Mockito.times(1)).registerTask(captorDeleteEvent.capture());

        List<List> allValues = captor.getAllValues();
        Map<Class<? extends BaseRenderTask>, List> byType = new HashMap<>();
        allValues.forEach(lst -> byType.put((Class<? extends BaseRenderTask>) lst.get(0).getClass(), lst));

        List<SkuRenderTask> skuRenderTasks = byType.get(SkuRenderTask.class);
        List<GroupsRenderTask> groupRenderTasks = byType.get(GroupsRenderTask.class);
        List<FastSkuRenderTask> fastSkuRenderTasks = byType.get(FastSkuRenderTask.class);

        DeleteTask deleteTask = (DeleteTask) captorDeleteEvent.getValue();
        Assert.assertEquals(2, skuRenderTasks.get(0).getModels().size());
        Assert.assertEquals(1, groupRenderTasks.get(0).getModels().size());
        Assert.assertEquals(2, fastSkuRenderTasks.get(0).getModels().size());

        Assert.assertEquals(
                Set.of(1L, 2L),
                skuRenderTasks.get(0).getModels().stream().map(it -> it.getModelId()).collect(Collectors.toSet())
        );
        Assert.assertEquals(
                Arrays.asList(3L),
                groupRenderTasks.get(0).getModels().stream().map(it -> it.getModelId()).collect(Collectors.toList())
        );
        Assert.assertEquals(
                Arrays.asList(4L),
                deleteTask.getModels().stream().map(it -> it.getModel().getModelId()).collect(Collectors.toList())
        );
        Assert.assertEquals(
                Set.of(5L, 6L),
                fastSkuRenderTasks.get(0).getModels().stream().map(it -> it.getModelId()).collect(Collectors.toSet())
        );
    }

}
