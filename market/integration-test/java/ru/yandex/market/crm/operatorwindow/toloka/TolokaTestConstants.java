package ru.yandex.market.crm.operatorwindow.toloka;

import java.util.List;
import java.util.Map;

import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.toloka.model.TolokaAssignmentList;
import ru.yandex.market.jmf.module.toloka.model.TolokaAsyncOperation;
import ru.yandex.market.jmf.module.toloka.model.TolokaCreateTaskResponse;
import ru.yandex.market.jmf.module.toloka.model.TolokaTask;
import ru.yandex.market.jmf.module.toloka.model.TolokaTaskList;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.jmf.module.toloka.model.TolokaAsyncOperationStatus.FAIL;
import static ru.yandex.market.jmf.module.toloka.model.TolokaAsyncOperationStatus.PENDING;
import static ru.yandex.market.jmf.module.toloka.model.TolokaAsyncOperationStatus.SUCCESS;

public class TolokaTestConstants {

    public static final Fqn ASSESSMENT_TICKET_FQN = Fqn.of("ticket$tolokaAssessmentTest");
    public static final Fqn NO_ASSESSMENT_TICKET_FQN = Fqn.of("ticket$tolokaNoAssessmentTest");

    public static final String REFERENCE_POOL_ID = "pool_1";
    public static final String CREATED_POOL_ID = "pool_888";
    public static final String POOL_CLONE_OPERATION_ID = "5";
    public static final String POOL_OPEN_OPERATION_ID = "6";

    //region Clone pool operations
    public static final TolokaAsyncOperation POOL_CLONE_PENDING_OPERATION =
            new TolokaAsyncOperation(POOL_CLONE_OPERATION_ID, PENDING, Map.of());

    public static final TolokaAsyncOperation POOL_CLONE_FAIL_OPERATION =
            new TolokaAsyncOperation(POOL_CLONE_OPERATION_ID, FAIL, Map.of());

    public static final TolokaAsyncOperation POOL_CLONE_SUCCESS_OPERATION =
            new TolokaAsyncOperation(POOL_CLONE_OPERATION_ID, SUCCESS, Map.of("pool_id", CREATED_POOL_ID));
    //endregion

    //region Open pool operations
    public static final TolokaAsyncOperation POOL_OPEN_PENDING_OPERATION =
            new TolokaAsyncOperation(POOL_OPEN_OPERATION_ID, PENDING, Map.of());

    public static final TolokaAsyncOperation POOL_OPEN_FAIL_OPERATION =
            new TolokaAsyncOperation(POOL_OPEN_OPERATION_ID, FAIL, Map.of());

    public static final TolokaAsyncOperation POOL_OPEN_SUCCESS_OPERATION =
            new TolokaAsyncOperation(POOL_OPEN_OPERATION_ID, SUCCESS, Map.of());
    //endregion

    //region Toloka tasks
    public static final Map<Integer, TolokaTask> TOLOKA_TASKS = Map.of(
            0, new TolokaTask("t1", CREATED_POOL_ID, Map.of()),
            1, new TolokaTask("t2", CREATED_POOL_ID, Map.of()),
            2, new TolokaTask("t3", CREATED_POOL_ID, Map.of()));

    public static final TolokaTaskList EMPTY_TASKS_LIST = new TolokaTaskList(List.of(), false);
    public static final TolokaTaskList NOT_EMPTY_TASKS_LIST =
            new TolokaTaskList(List.of(mock(TolokaTask.class)), false);

    public static final TolokaCreateTaskResponse TASKS_CREATE_SUCCESS_RESPONSE =
            new TolokaCreateTaskResponse(TOLOKA_TASKS);

    public static final TolokaCreateTaskResponse TASKS_CREATE_EMPTY_RESPONSE =
            new TolokaCreateTaskResponse(Map.of());
    //endregion

    //region Toloka assignments
    public static final TolokaAssignmentList ASSIGNMENTS_EMPTY = new TolokaAssignmentList(List.of(), false);
    public static final String ASSESSMENT_RESULTS_PATH = "/toloka/positive.json";
    public static final String ASSESSMENT_BROKEN_RESULTS_PATH = "/toloka/negative.json";
    //endregion

}
