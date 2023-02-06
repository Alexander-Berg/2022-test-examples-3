package ru.yandex.market.partner.content.common.service.report.ui;

import com.google.protobuf.Message;
import io.qameta.allure.Issue;
import org.jooq.DSLContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.PipelineService;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.TaskStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Task;
import ru.yandex.market.partner.content.common.engine.manager.PipelineManager;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessFileData;

import java.sql.Timestamp;

import static ru.yandex.market.partner.content.common.db.jooq.Tables.ERROR_PROTO_MESSAGE;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.LOCK_INFO;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.PROBLEM;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.PROCESS_INFO;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.PROCESS_PROBLEM;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SERVICE_INSTANCE;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.TASK;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.TASK_PROCESS;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.THROWABLE_CONTAINER;

@Issue("MARKETIR-9511")
@Issue("MARKETIR-9852")
public class InternalErrorReportServiceTest extends DBStateGenerator {

    private static final Timestamp NOW_TS = new Timestamp(System.currentTimeMillis());
    public static final PipelineType PIPELINE_TYPE = PipelineType.SINGLE_XLS;
    public static final PartnerContentUi.Pipeline.Type PROTO_PIPELINE_TYPE = PartnerContentUi.Pipeline.Type.SINGLE_XLS;
    private InternalErrorReportService internalErrorReportService;

    @Autowired
    private PipelineService pipelineService;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        internalErrorReportService = new InternalErrorReportService(configuration);
    }

    @Test
    public void listData() {
        RequestProcessFileData pipelineInputData = new RequestProcessFileData();
        pipelineInputData.setRequestId(requestId);
        ProcessDataBucketData taskData = new ProcessDataBucketData();
        taskData.setDataBucketId(dataBucketId);

        long pipelineId = pipelineService.createPipeline(
            pipelineInputData, PIPELINE_TYPE, 0
        );

        Task task = createTask(taskData, pipelineId);

        String serviceInstance = "service_instance_1";
        Long serviceInstanceId = dsl()
            .insertInto(SERVICE_INSTANCE)
            .set(SERVICE_INSTANCE.HOST, serviceInstance)
            .set(SERVICE_INSTANCE.PORT, 123456)
            .set(SERVICE_INSTANCE.LAST_ALIVE_TIME, NOW_TS)
            .set(SERVICE_INSTANCE.IS_ALIVE, true)
            .returningResult(SERVICE_INSTANCE.ID)
            .fetchOne()
            .value1();

        long processInfoId = insertProcessInfo(task.getId(), serviceInstanceId);

        String problemDescription = "problem description";
        Long problemId = insertProblem(problemDescription);

        dsl().insertInto(PROCESS_PROBLEM)
            .set(PROCESS_PROBLEM.PROCESS_INFO_ID, processInfoId)
            .set(PROCESS_PROBLEM.PROBLEM_ID, problemId)
            .execute();

        ModelCardApi.SaveModelsGroupOperationResponse protoResponse = ModelCardApi.SaveModelsGroupOperationResponse
            .newBuilder()
            .setStatus(ModelStorage.OperationStatusType.OK)
            .build();

        insertErrorProtoMessage(problemId, protoResponse);

        String exceptionClassName = "className";
        String exceptionMessage = "message";
        String exceptionStacktrace = "stacktrace";
        insertThrowable(
            problemId,
            exceptionClassName,
            exceptionMessage,
            exceptionStacktrace
        );

        //---

        PartnerContentUi.ListInternalErrorResponse response = internalErrorReportService.listData(
            PartnerContentUi.ListInternalErrorRequest.newBuilder()
                .setRequestType(PartnerContentUi.RequestType.ALL)
                .setPaging(PartnerContentUi.Paging.newBuilder().setStartRow(0).setPageSize(100).build())
                .build()
        );

        //---

        PartnerContentUi.ListInternalErrorResponse expected = PartnerContentUi.ListInternalErrorResponse.newBuilder()
            .setCount(1)
            .addData(
                PartnerContentUi.ListInternalErrorResponse.Row.newBuilder()
                    .setProblemId(problemId)
                    .setProblemTs(NOW_TS.getTime())
                    .setPipelineId(pipelineId)
                    .setPipelineType(PROTO_PIPELINE_TYPE)
                    .setTaskId(task.getId())
                    .setTaskStartTs(task.getStartDate().getTime())
                    .setTaskInputData("ProcessDataBucketData{dataBucketId=" + dataBucketId + "}")
                    .setRequestId(requestId)
                    .addProcessId(processId)
                    .setProblemDescription(problemDescription)
                    .addThrowable(
                        PartnerContentUi.ListInternalErrorResponse.Throwable.newBuilder()
                            .setClassName(exceptionClassName)
                            .setThrowableMessage(exceptionMessage)
                            .setStacktrace(exceptionStacktrace)
                            .build()
                    )
                    .addProtoResponse("{" +
                        "\"status\":\"OK\"," +
                        "\"__proto_message_class\":\"" + protoResponse.getClass().getName() + "\"}"
                    )
                    .setServiceInstance(serviceInstance)
                    .build()
            )
            .build();

        Assert.assertEquals(expected, response);
    }

    public Long insertProblem(String description) {
        return dsl()
            .insertInto(PROBLEM)
            .set(PROBLEM.DESCRIPTION, description)
            .set(PROBLEM.PROBLEM_TS, NOW_TS)
            .set(PROBLEM.PIPELINE_TYPE, PipelineType.SINGLE_XLS)
            .returningResult(PROBLEM.ID)
            .fetchOne().value1();
    }

    public Long insertErrorProtoMessage(Long problemId, Message response) {
        return dsl().insertInto(ERROR_PROTO_MESSAGE)
            .set(ERROR_PROTO_MESSAGE.SERVICE, "test")
            .set(ERROR_PROTO_MESSAGE.REQUEST, ModelStorage.Model.newBuilder().build())
            .set(ERROR_PROTO_MESSAGE.MESSAGE, response)
            .set(ERROR_PROTO_MESSAGE.PROBLEM_ID, problemId)
            .returningResult(ERROR_PROTO_MESSAGE.ID)
            .fetchOne()
            .value1();
    }

    public Long insertThrowable(Long problemId, String className, String message, String stacktrace) {
        return dsl().insertInto(THROWABLE_CONTAINER)
            .set(THROWABLE_CONTAINER.CLASS_NAME, className)
            .set(THROWABLE_CONTAINER.MESSAGE, message)
            .set(THROWABLE_CONTAINER.STACKTRACE, stacktrace)
            .set(THROWABLE_CONTAINER.PROBLEM_ID, problemId)
            .returningResult(THROWABLE_CONTAINER.ID)
            .fetchOne()
            .value1();
    }


    private Task createTask(ProcessDataBucketData taskData, long pipelineId) {
        final Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        final DSLContext dsl = dsl();
        final long lockId = dsl
            .insertInto(LOCK_INFO)
            .set(LOCK_INFO.CREATE_TIME, currentTimestamp)
            .set(LOCK_INFO.UPDATE_TIME, currentTimestamp)
            .set(LOCK_INFO.STATUS, LockStatus.FREE)
            .returningResult(LOCK_INFO.ID)
            .fetchOne()
            .value1();
        return dsl
            .insertInto(TASK)
            .set(TASK.PIPELINE_ID, pipelineId)
            .set(TASK.STATUS, TaskStatus.NEW)
            .set(TASK.START_DATE, currentTimestamp)
            .set(TASK.UPDATE_DATE, currentTimestamp)
            .set(TASK.LOCK_ID, lockId)
            .set(TASK.INPUT_DATA, taskData)
            .returningResult(TASK.fields())
            .fetchOne()
            .into(Task.class);
    }

    public long insertProcessInfo(long taskId, long serviceInstanceId) {
        long processInfoId = insertProcessInfo(serviceInstanceId);
        dsl()
            .insertInto(TASK_PROCESS)
            .set(TASK_PROCESS.TASK_ID, taskId)
            .set(TASK_PROCESS.PROCESS_INFO_ID, processInfoId)
            .execute();
        return processInfoId;
    }

    public long insertProcessInfo(long serviceInstanceId) {
        final Timestamp now = new Timestamp(System.currentTimeMillis());
        return dsl()
            .insertInto(PROCESS_INFO)
            .set(PROCESS_INFO.START_DATE, now)
            .set(PROCESS_INFO.UPDATE_DATE, now)
            .set(PROCESS_INFO.SERVICE_INSTANCE_ID, serviceInstanceId)
            .set(PROCESS_INFO.STATUS, ProcessStatus.RUNNING)
            .returningResult(PROCESS_INFO.ID)
            .fetchOne().value1();
    }
}