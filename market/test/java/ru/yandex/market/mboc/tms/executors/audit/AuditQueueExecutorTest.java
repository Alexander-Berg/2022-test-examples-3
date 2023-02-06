package ru.yandex.market.mboc.tms.executors.audit;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAuditService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.AuditQueue;
import ru.yandex.market.mboc.common.repository.AuditQueueRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class AuditQueueExecutorTest extends BaseDbTestClass {
    private static final int CATEGORY_ID = 91491;

    @Autowired
    private AuditQueueRepository auditQueueRepository;

    private MboAuditService mboAuditService;

    private SimpleMeterRegistry registry;

    private AuditQueueExecutor executor;


    @Before
    public void setUp() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(registry);

        mboAuditService = mock(MboAuditService.class);
        doAnswer(invocation -> {
            MboAudit.WriteActionsRequest writeActionsRequest = invocation.getArgument(0);
            if (writeActionsRequest.getActionsList().stream().anyMatch(mboAction -> mboAction.getActionId() > 100)) {
                throw new RuntimeException("except writeActions");
            }
            return MboAudit.VoidResponse.getDefaultInstance();
        }).when(mboAuditService).writeActions(any());

        executor = new AuditQueueExecutor(auditQueueRepository, mboAuditService);
    }

    @Test
    public void executeTest() {
        List<MboAudit.MboAction.Builder> builderList =
            IntStream.range(1, 99).mapToObj(value -> MboAudit.MboAction.newBuilder()
                    .setActionId(value)
                    .setActionType(MboAudit.ActionType.CREATE)
                    .setCategoryId(CATEGORY_ID))
                .collect(Collectors.toUnmodifiableList());

        List<AuditQueue> auditQueues = builderList.stream().map(actionBuilder -> new AuditQueue()
                .setMessage(actionBuilder.build().toByteArray())
                .setCreated(Instant.now())
                .setNextTry(Instant.now()))
            .collect(Collectors.toList());

        auditQueueRepository.save(auditQueues);

        executor.execute();

        Counter resultProcessed =
            registry.get("PgAuditWriter.result").tags("result", "countProcessed").counter();
        assertThat(resultProcessed.count()).isEqualTo(auditQueues.size());
    }

    @Test
    public void executeFailedTest() {
        List<MboAudit.MboAction.Builder> builderList =
            IntStream.range(99, 110).mapToObj(value -> MboAudit.MboAction.newBuilder()
                    .setActionId(value)
                    .setActionType(MboAudit.ActionType.CREATE)
                    .setCategoryId(CATEGORY_ID))
                .collect(Collectors.toUnmodifiableList());

        List<AuditQueue> auditQueues = builderList.stream().map(actionBuilder -> new AuditQueue()
                .setMessage(actionBuilder.build().toByteArray())
                .setCreated(Instant.now())
                .setNextTry(Instant.now()))
            .collect(Collectors.toList());

        auditQueueRepository.save(auditQueues);

        executor.execute();

        Counter resultFailed =
            registry.get("PgAuditWriter.result").tags("result", "countFailed").counter();
        assertThat(resultFailed.count()).isEqualTo(11);
    }


}
