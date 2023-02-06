package onetime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.markup2.dao.HitmanExecutionDataPersister;
import ru.yandex.market.markup2.dao.HitmanExecutionToTaskItemsPersister;
import ru.yandex.market.markup2.dao.TaskConfigGroupPersister;
import ru.yandex.market.markup2.dao.TaskConfigGroupStatisticPersister;
import ru.yandex.market.markup2.dao.TaskConfigPersister;
import ru.yandex.market.markup2.dao.TaskDataItemPersister;
import ru.yandex.market.markup2.dao.TaskPersister;
import ru.yandex.market.markup2.dao.TaskStatisticPersister;
import ru.yandex.market.markup2.dao.YangPoolPersister;
import ru.yandex.market.markup2.dao.dataUnique.TypeCategoryDataUniquePersister;
import ru.yandex.market.markup2.entries.config.TaskConfigData;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.ITaskConfigGroupInfo;
import ru.yandex.market.markup2.entries.task.TaskData;
import ru.yandex.market.markup2.workflow.general.IResponseItem;
import ru.yandex.market.markup2.workflow.general.ITaskDataItemPayload;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.general.TaskDataItemState;
import ru.yandex.market.markup2.workflow.general.TaskDataItemStub;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.toloka.TolokaApi;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Important!! Multitask configs aren't supported.
 * @author anmalysh
 */
@Ignore("Don't need to run data migration with unit tests")
public class ToolBase {

    protected static final Logger log = LogManager.getLogger();

    @Resource
    protected TransactionTemplate markupTransactionTemplate;

    @Resource
    protected TaskDataItemPersister dataItemPersister;

    @Resource
    protected TaskConfigGroupPersister taskConfigGroupPersister;

    @Resource
    protected TaskConfigGroupStatisticPersister taskConfigGroupStatisticPersister;

    @Resource
    protected TaskConfigPersister taskConfigPersister;

    @Resource
    protected TaskPersister taskPersister;

    @Resource
    protected TaskStatisticPersister taskStatisticPersister;

    @Resource
    protected TypeCategoryDataUniquePersister typeCategoryDataUniquePersister;

    @Resource
    protected HitmanExecutionDataPersister executionDataPersister;

    @Resource
    protected HitmanExecutionToTaskItemsPersister hitmanExecutionToTaskItemsPersister;

    @Resource
    protected YangPoolPersister yangPoolPersister;

    @Resource
    protected YangLogStorageService yangLogStorageService;

    @Resource
    protected TolokaApi tolokaApi;

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected ITaskConfigGroupInfo getTaskConfigGroup(int taskType, int categoryId) {
        return taskConfigGroupPersister.getAllValues().stream()
            .filter(tcg -> tcg.getTypeInfoId() == taskType && tcg.getCategoryId() == categoryId)
            .findFirst().orElse(null);
    }

    protected TaskConfigData getActiveTaskConfig(int taskType, int categoryId) {
        ITaskConfigGroupInfo tcg = getTaskConfigGroup(taskType, categoryId);
        if (tcg == null) {
            return null;
        }
        return taskConfigPersister.getAllValues().stream()
            .filter(tc -> tc.getGroupId() == tcg.getId() &&
                tc.getState() != TaskConfigState.DEACTIVATED &&
                tc.getState() != TaskConfigState.CANCELED)
            .findFirst().orElse(null);
    }

    protected TaskData getLastTask(int taskType, int categoryId) {
        TaskConfigData tc = getActiveTaskConfig(taskType, categoryId);
        if (tc == null || tc.getCurrentTaskIds().isEmpty()) {
            return null;
        }
        return taskPersister.getValues(tc.getCurrentTaskIds().get(0)).stream().findFirst().orElse(null);
    }

    protected <P extends ITaskDataItemPayload, R> List<TaskDataItem<P, R>>
    getLastTaskDataItems(int taskType, int categoryId, TaskDataItemState... states) {
        TaskData task = getLastTask(taskType, categoryId);
        if (task == null) {
            return new ArrayList();
        }
        List<TaskDataItemStub> stubs = dataItemPersister.getValuesByTaskId(Arrays.asList(task.getId()));

        Set<TaskDataItemState> requestedStates = new HashSet<>(Arrays.asList(states));

        List<TaskDataItem<P, R>> result = new ArrayList<>();
        for (TaskDataItemStub dataItemStub : stubs) {
            final int taskId = dataItemStub.getTaskId();
            final long generateTime = dataItemStub.getGenerateTime();
            final long dataItemId = dataItemStub.getId();
            final TaskDataItemState state = dataItemStub.getState();
            final ITaskDataItemPayload payload;
            final IResponseItem responseData;
            try {
                payload = dataItemPersister.readDataItemPayload(taskType, dataItemStub.getMainData());
            } catch (IOException e) {
                log.error(
                    "Can't load dataItemPayload for id=" + dataItemId +
                        " taskId=" + taskId + " typeId=" + taskType, e
                );
                continue;
            }
            try {
                responseData = dataItemPersister.readResponseData(taskType, dataItemStub.getRespData());
            } catch (IOException e) {
                log.error(
                    "Can't load itemResponseData for id=" + dataItemId +
                        " taskId=" + taskId + " typeId=" + taskType, e
                );
                continue;
            }
            result.add((TaskDataItem<P, R>) new TaskDataItem<>(dataItemId, generateTime, state, payload, responseData));
        }
        return result.stream()
            .filter(tdi -> requestedStates.contains(tdi.getState()))
            .collect(Collectors.toList());
    }

    protected int countTaskDataItems(int taskId) {
        List<TaskDataItemStub> stubs = dataItemPersister.getValuesByTaskId(Arrays.asList(taskId));
        return stubs.size();
    }
}
