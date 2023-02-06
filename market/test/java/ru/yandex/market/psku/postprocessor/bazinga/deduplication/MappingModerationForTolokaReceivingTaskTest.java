package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.markup3.api.Markup3Api;
import ru.yandex.market.markup3.api.Markup3Api.ConsumeResultRequest;
import ru.yandex.market.markup3.api.Markup3Api.ConsumeResultResponse;
import ru.yandex.market.markup3.api.Markup3Api.TaskResultData;
import ru.yandex.market.markup3.api.Markup3Api.TasksResultPollResponse;
import ru.yandex.market.markup3.api.Markup3Api.TasksResultPollResponse.TaskResult;
import ru.yandex.market.markup3.api.Markup3Api.TolokaMappingModerationResult;
import ru.yandex.market.markup3.api.Markup3Api.TolokaMappingModerationResult.MappingModerationResultItem;
import ru.yandex.market.markup3.api.Markup3Api.TolokaMappingModerationResult.MappingModerationStatus;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc;
import ru.yandex.market.markup3.api.Markup3ApiTaskServiceGrpc.Markup3ApiTaskServiceImplBase;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.TolokaTaskStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.TOLOKA_TASKS;

public class MappingModerationForTolokaReceivingTaskTest extends BaseDBTest {

    private static final Long TASK_ID_1 = 1L;
    private static final Long MBOC_ID_1 = 11L;
    private static final Long RESULT_TASK_ID_1 = 10L;
    private static final Long RESULT_MSKU_ID_1 = 100L;

    private static final Long TASK_ID_2 = 2L;
    private static final Long MBOC_ID_2 = 22L;
    private static final Long RESULT_TASK_ID_2 = 20L;
    private static final Long RESULT_MSKU_ID_2 = 200L;

    private static final Long TASK_ID_NONEXISTING = 3L;
    private static final Long MBOC_ID_NONEXISTING = 33L;
    private static final Long RESULT_TASK_ID_NONEXISTING = 30L;
    private static final Long RESULT_MSKU_ID_NONEXISTING = 300L;

    private static final Long CANCELLED_TASK_ID = 4L;
    private static final Long CANCELLED_MBOC_ID = 44L;
    private static final Long CANCELLED_RESULT_TASK_ID = 40L;
    private static final Long CANCELLED_RESULT_MSKU_ID = 400L;

    @Autowired
    ClusterContentDao clusterContentDao;

    @Autowired
    ClusterMetaDao clusterMetaDao;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    MappingModerationForTolokaReceivingTask mappingModerationForTolokaReceivingTask;

    List<Long> taskResultIdsConsumed = Collections.emptyList();

    AtomicBoolean isConsumeInvoked = new AtomicBoolean(false);

    @Before
    public void setUp() throws Exception {
        TasksResultPollResponse response = TasksResultPollResponse.newBuilder().addResults(
                TaskResult.newBuilder()
                        .setExternalKey(StringValue.newBuilder().setValue(TASK_ID_1.toString()).build())
                        .setTaskid(TASK_ID_1)
                        .setTaskResultId(RESULT_TASK_ID_1)
                        .setResult(
                                TaskResultData.newBuilder().setTolokaMappingModerationResult(
                                        TolokaMappingModerationResult.newBuilder()
                                                .addFinishedOffers(
                                                        MappingModerationResultItem.newBuilder()
                                                                .setMsku(
                                                                        Int64Value.newBuilder()
                                                                                .setValue(RESULT_MSKU_ID_1)
                                                                                .build()
                                                                )
                                                                .setOfferId(MBOC_ID_1.toString())
                                                                .setStatus(MappingModerationStatus.ACCEPTED)
                                                                .build()
                                                )
                                                .build()
                                ).build()
                        )
                        .build()
        ).addResults(
                TaskResult.newBuilder()
                        .setExternalKey(StringValue.newBuilder().setValue(TASK_ID_2.toString()).build())
                        .setTaskid(TASK_ID_2)
                        .setTaskResultId(RESULT_TASK_ID_2)
                        .setResult(
                                TaskResultData.newBuilder().setTolokaMappingModerationResult(
                                        TolokaMappingModerationResult.newBuilder()
                                                .addFinishedOffers(
                                                        MappingModerationResultItem.newBuilder()
                                                                .setMsku(
                                                                        Int64Value.newBuilder()
                                                                                .setValue(RESULT_MSKU_ID_2)
                                                                                .build()
                                                                )
                                                                .setOfferId(MBOC_ID_2.toString())
                                                                .setStatus(MappingModerationStatus.REJECTED)
                                                                .build()
                                                )
                                                .build()
                                ).build()
                        )
                        .build()
        ).addResults(
                TaskResult.newBuilder()
                        .setExternalKey(StringValue.newBuilder().setValue(TASK_ID_NONEXISTING.toString()).build())
                        .setTaskid(TASK_ID_NONEXISTING)
                        .setTaskResultId(RESULT_TASK_ID_NONEXISTING)
                        .setResult(
                                TaskResultData.newBuilder().setTolokaMappingModerationResult(
                                        TolokaMappingModerationResult.newBuilder()
                                                .addFinishedOffers(
                                                        MappingModerationResultItem.newBuilder()
                                                                .setMsku(
                                                                        Int64Value.newBuilder()
                                                                                .setValue(RESULT_MSKU_ID_NONEXISTING)
                                                                                .build()
                                                                )
                                                                .setOfferId(MBOC_ID_NONEXISTING.toString())
                                                                .setStatus(MappingModerationStatus.NEED_INFO)
                                                                .build()
                                                )
                                                .build()
                                ).build()
                        )
                        .build()
        ).addResults(
                TaskResult.newBuilder()
                        .setExternalKey(StringValue.newBuilder().setValue(CANCELLED_TASK_ID.toString()).build())
                        .setTaskid(CANCELLED_TASK_ID)
                        .setTaskResultId(CANCELLED_RESULT_TASK_ID)
                        .setResult(
                                TaskResultData.newBuilder().setTolokaMappingModerationResult(
                                        TolokaMappingModerationResult.newBuilder()
                                                .addAllCancelledOffers(Collections.singletonList(CANCELLED_MBOC_ID))
                                                .build()
                                ).build()
                        )
                        .build()
        )
                .build();

        Markup3ApiTaskServiceImplBase markup3ApiTaskServiceImplBase = new Markup3ApiTaskServiceImplBase() {
            @Override
            public void pollResults(
                    Markup3Api.TasksResultPollRequest request,
                    StreamObserver<TasksResultPollResponse> responseObserver
            ) {
                if (!isConsumeInvoked.get()) {
                    responseObserver.onNext(response);
                } else {
                    responseObserver.onNext(TasksResultPollResponse.getDefaultInstance());
                }
                responseObserver.onCompleted();
            }

            @Override
            public void consumeResults(
                    ConsumeResultRequest request,
                    StreamObserver<ConsumeResultResponse> responseObserver
            ) {
                taskResultIdsConsumed = request.getTaskResultIdsList();
                if (taskResultIdsConsumed.containsAll(Arrays.asList(RESULT_TASK_ID_1, RESULT_TASK_ID_2, CANCELLED_RESULT_TASK_ID))) {
                    isConsumeInvoked.set(true);
                }

                responseObserver.onNext(ConsumeResultResponse.getDefaultInstance());
                responseObserver.onCompleted();

            }
        };

        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder
                        .forName(serverName)
                        .directExecutor()
                        .addService(markup3ApiTaskServiceImplBase)
                        .build()
                        .start()
        );

        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder
                        .forName(serverName)
                        .directExecutor()
                        .build()
        );

        mappingModerationForTolokaReceivingTask = new MappingModerationForTolokaReceivingTask(
                Markup3ApiTaskServiceGrpc.newBlockingStub(channel), clusterContentDao, clusterMetaDao
        );


        clusterMetaDao.transaction(() -> {
            ClusterMeta clusterMeta = new ClusterMeta();
            clusterMeta.setId(1L);
            clusterMeta.setStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_IN_PROCESS);
            clusterMeta.setType(ClusterType.DSBS);
            clusterMetaDao.insert(clusterMeta);

            ClusterMeta clusterMeta2 = new ClusterMeta();
            clusterMeta2.setId(2L);
            clusterMeta2.setStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_IN_PROCESS);
            clusterMeta2.setType(ClusterType.DSBS);
            clusterMetaDao.insert(clusterMeta2);

            ClusterMeta clusterMeta3 = new ClusterMeta();
            clusterMeta3.setId(3L);
            clusterMeta3.setStatus(ClusterStatus.MAPPING_MODERATION_TOLOKA_IN_PROCESS);
            clusterMeta3.setType(ClusterType.DSBS);
            clusterMetaDao.insert(clusterMeta3);

            clusterMetaDao.dsl()
                    .insertInto(TOLOKA_TASKS)
                    .set(TOLOKA_TASKS.ID, TASK_ID_1)
                    .set(TOLOKA_TASKS.STATUS, TolokaTaskStatus.CREATED)
                    .execute();

            clusterMetaDao.dsl()
                    .insertInto(TOLOKA_TASKS)
                    .set(TOLOKA_TASKS.ID, TASK_ID_2)
                    .set(TOLOKA_TASKS.STATUS, TolokaTaskStatus.CREATED)
                    .execute();

            clusterMetaDao.dsl()
                    .insertInto(TOLOKA_TASKS)
                    .set(TOLOKA_TASKS.ID, CANCELLED_TASK_ID)
                    .set(TOLOKA_TASKS.STATUS, TolokaTaskStatus.CREATED)
                    .execute();
        });

        clusterContentDao.transaction(() -> {
            ClusterContent clusterContent = new ClusterContent();
            clusterContent.setTaskId(TASK_ID_1);
            clusterContent.setClusterMetaId(1L);
            clusterContent.setMbocId(MBOC_ID_1);
            clusterContent.setType(ClusterContentType.DSBS);
            clusterContent.setStatus(ClusterContentStatus.NEW);
            clusterContent.setSupposedTargetSkuId(RESULT_MSKU_ID_1);
            clusterContentDao.insert(clusterContent);

            ClusterContent clusterContent2 = new ClusterContent();
            clusterContent2.setTaskId(TASK_ID_2);
            clusterContent2.setClusterMetaId(2L);
            clusterContent2.setMbocId(MBOC_ID_2);
            clusterContent2.setType(ClusterContentType.DSBS);
            clusterContent2.setStatus(ClusterContentStatus.NEW);
            clusterContent2.setSupposedTargetSkuId(RESULT_MSKU_ID_2);
            clusterContentDao.insert(clusterContent2);

            ClusterContent clusterContent3 = new ClusterContent();
            clusterContent3.setTaskId(CANCELLED_TASK_ID);
            clusterContent3.setClusterMetaId(3L);
            clusterContent3.setMbocId(CANCELLED_MBOC_ID);
            clusterContent3.setType(ClusterContentType.DSBS);
            clusterContent3.setStatus(ClusterContentStatus.NEW);
            clusterContent3.setSupposedTargetSkuId(CANCELLED_RESULT_MSKU_ID);
            clusterContentDao.insert(clusterContent3);
        });

    }

    @Test
    public void testStatusesAreCorrect() {
        mappingModerationForTolokaReceivingTask.execute(null);

        List<ClusterContent> clusterContents = clusterContentDao.findAll();
        List<ClusterMeta> clusterMetas = clusterMetaDao.findAll();

        assertThat(taskResultIdsConsumed).containsExactlyInAnyOrder(
                RESULT_TASK_ID_1, RESULT_TASK_ID_2, CANCELLED_RESULT_TASK_ID
        );

        assertThat(clusterContents).isNotEmpty();

        assertThat(clusterMetas.size()).isEqualTo(3);
        assertThat(clusterMetas.get(0).getStatus()).isEqualTo(ClusterStatus.MAPPING_MODERATION_TOLOKA_FINISHED);
        assertThat(clusterMetas.get(1).getStatus()).isEqualTo(ClusterStatus.MAPPING_MODERATION_TOLOKA_FINISHED);
        assertThat(clusterMetas.get(2).getStatus()).isEqualTo(ClusterStatus.INVALID);
        assertThat(clusterContents).extracting(ClusterContent::getTargetSkuId)
                .filteredOn(Objects::nonNull)
                .containsOnly(RESULT_MSKU_ID_1, -1L);

    }
}
