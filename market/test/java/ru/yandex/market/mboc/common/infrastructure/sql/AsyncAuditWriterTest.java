package ru.yandex.market.mboc.common.infrastructure.sql;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mbo.http.MboAuditService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class AsyncAuditWriterTest {
    private static final int CATEGORY_ID = 91491;

    private MboAuditService mboAuditService;
    private AsyncAuditWriter asyncAuditWriter;
    private SimpleMeterRegistry registry;


    @Before
    public void init() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(registry);

        mboAuditService = mock(MboAuditService.class);
        doAnswer(invocation -> {
            MboAudit.WriteActionsRequest writeActionsRequest = invocation.getArgument(0);
            if (writeActionsRequest.getActionsList().stream().anyMatch(mboAction -> mboAction.getActionId() > 200)) {
                Thread.sleep(100);
                return MboAudit.VoidResponse.getDefaultInstance();
            } else if (writeActionsRequest.getActionsList().stream().anyMatch(mboAction -> mboAction.getActionId() > 100)) {
                throw new RuntimeException("except writeActions");
            }
            return MboAudit.VoidResponse.getDefaultInstance();
        }).
        when(mboAuditService).writeActions(any());

        asyncAuditWriter = new AsyncAuditWriter(mboAuditService, Metrics.globalRegistry, 100, 10, 1000, 1000);
        asyncAuditWriter.start();
    }

    @Test
    public void writeActions() throws InterruptedException {
        List<MboAudit.MboAction.Builder> builderList =
            IntStream.range(1, 99).mapToObj(value -> MboAudit.MboAction.newBuilder()
            .setActionId(value)
            .setActionType(MboAudit.ActionType.CREATE)
            .setCategoryId(CATEGORY_ID))
            .collect(Collectors.toUnmodifiableList());

        asyncAuditWriter.writeActions(builderList);
        asyncAuditWriter.stop();
        Mockito.verify(mboAuditService, times((int) Math.ceil(builderList.size() / 10.))).writeActions(any());
        DistributionSummary addedAuditSummary = registry.get(AsyncAuditWriter.ADDED_AUDIT_SUMMARY).summary();
        assertThat(addedAuditSummary.count()).isGreaterThanOrEqualTo(1);
        assertThat(addedAuditSummary.totalAmount()).isEqualTo(builderList.size());

        DistributionSummary recordedAuditSummary = registry.get(AsyncAuditWriter.RECORDED_AUDIT_SUMMARY).summary();
        assertThat(recordedAuditSummary.count()).isGreaterThanOrEqualTo(1);
        assertThat(recordedAuditSummary.totalAmount()).isEqualTo(builderList.size());
    }

    @Test
    public void writeActionsFailed() throws InterruptedException {
        List<MboAudit.MboAction.Builder> builderList =
            IntStream.range(99, 110).mapToObj(value -> MboAudit.MboAction.newBuilder()
            .setActionId(value)
            .setActionType(MboAudit.ActionType.CREATE)
            .setCategoryId(CATEGORY_ID))
            .collect(Collectors.toUnmodifiableList());

        asyncAuditWriter.writeActions(builderList);
        asyncAuditWriter.stop();
        Mockito.verify(mboAuditService, times(3 * (int) Math.ceil(builderList.size() / 10.))).writeActions(any());
        assertCounter(AsyncAuditWriter.EXCEPTED_LOST_AUDIT_COUNT, 11);
    }

    @Test
    public void writeActionsFullQueue() throws InterruptedException {
        List<MboAudit.MboAction.Builder> builderList =
            IntStream.range(200, 400).mapToObj(value -> MboAudit.MboAction.newBuilder()
            .setActionId(value)
            .setActionType(MboAudit.ActionType.CREATE)
            .setCategoryId(CATEGORY_ID))
            .collect(Collectors.toUnmodifiableList());

        asyncAuditWriter.writeActions(builderList);

        asyncAuditWriter.stop();
        assertCounter(AsyncAuditWriter.FULL_QUEUE_LOST_AUDIT_COUNT, 200);
    }

    private void assertCounter(String name, double count) {
        assertThat(registry.get(name).counter().count()).isEqualTo(count);
    }

}
