package ru.yandex.market.clickhouse.dealer.operation;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import ru.yandex.market.clickhouse.dealer.config.DealerGlobalConfig;
import ru.yandex.market.clickhouse.dealer.state.PartitionClickHouseState;
import ru.yandex.market.clickhouse.dealer.state.PartitionState;
import ru.yandex.market.clickhouse.dealer.state.PartitionYtState;
import ru.yandex.market.clickhouse.dealer.tm.TmTaskState;

import java.time.Instant;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 06/06/2018
 */
public class CopyOperationTest {

    private String ypPartition = "2017-01-01";
    private String tmTaskId = "i-d-4-2";
    private String clickHousePartition = "201701";
    private Instant now = Instant.now();
    private PartitionYtState ytState = new PartitionYtState(now, 42L, now, 1);
    private TmTaskState tmTaskState = new TmTaskState(tmTaskId, TmTaskState.Status.COMPLETED, "", null, now, now, now);

    private PartitionState expectedPartitionState = new PartitionState(
        ypPartition, clickHousePartition, new PartitionClickHouseState(ytState, now, now),
        ytState, PartitionState.Status.TRANSFERRED, 1
    );

    private PartitionState partitionState() {
        return new PartitionState(
            ypPartition, clickHousePartition, null, ytState, PartitionState.Status.NEW, 0
        );
    }

    @Test
    public void testOperation() throws Exception {
        OperationContext context = defaultOperationContextMock();
        CopyOperation operation = new CopyOperation(partitionState(), 1000);
        operation.runOperation(context);

        InOrder order = Mockito.inOrder(context);
        order.verify(context).applyDdl();
        order.verify(context).clearTempTable();
        order.verify(context).saveState();
        order.verify(context).replacePartitionFromTargetTable(clickHousePartition);
        order.verify(context).countTempTableRows();
        order.verify(context).saveState();
        order.verify(context).startTmCopyOperation(ypPartition);
        order.verify(context).saveState();
        order.verify(context).pollTmTask(tmTaskId);
        order.verify(context).saveState();
        order.verify(context).countTempTableRows();
        order.verify(context).saveState();
        order.verify(context).replacePartitionFromTempTable(clickHousePartition);
        order.verify(context).countTargetTableRows(clickHousePartition);
        order.verify(context).updatePartitionState(expectedPartitionState);
        order.verify(context).saveState();
        order.verify(context).cleanError();
        order.verifyNoMoreInteractions();
        Assert.assertEquals(operation.getStep(), CopyOperation.Step.DONE);
    }

    @Test
    public void testNotCancelableStates() throws Exception {
        OperationContext context = defaultOperationContextMock();
        Mockito.doThrow(RuntimeException.class).doNothing().when(context).replacePartitionFromTempTable(clickHousePartition);
        CopyOperation operation = new CopyOperation(partitionState(), 1000);
        try {
            operation.runOperation(context);
            Assert.fail(); //Unreachable cause of throw
        } catch (RuntimeException ignored) {
        }
        Assert.assertFalse(operation.canBeCanceled());
        Assert.assertEquals(operation.getStep(), CopyOperation.Step.REPLACE_TARGET_PARTITION);
        Mockito.doThrow(RuntimeException.class).doNothing().when(context).saveState();
        try {
            operation.runOperation(context);
            Assert.fail(); //Unreachable cause of throw
        } catch (RuntimeException ignored) {
        }
        Assert.assertFalse(operation.canBeCanceled());
        Assert.assertEquals(operation.getStep(), CopyOperation.Step.DONE);
        operation.runOperation(context);
    }

    @Test(expected = Exception.class)
    public void testValidation() throws Exception {
        OperationContext context = defaultOperationContextMock();
        Mockito.when(context.countTempTableRows()).thenReturn(1000L, 41L);
        CopyOperation operation = new CopyOperation(partitionState(), 1000);
        operation.runOperation(context);
    }

    private OperationContext defaultOperationContextMock() throws Exception {
        OperationContext context = Mockito.mock(OperationContext.class);
        DealerGlobalConfig dealerGlobalConfig = Mockito.mock(DealerGlobalConfig.class);
        Mockito.when(context.startTmCopyOperation(ypPartition)).thenReturn(tmTaskId);
        Mockito.when(context.pollTmTask(tmTaskId)).thenReturn(tmTaskState);
        Mockito.when(context.countTempTableRows()).thenReturn(1000L, 1042L);
        Mockito.when(context.countTargetTableRows(clickHousePartition)).thenReturn(1042L);
        Mockito.when(dealerGlobalConfig.getRowCountValidationAttempts()).thenReturn(3);
        Mockito.when(dealerGlobalConfig.getRowCountValidationSleepBeforeRetriesSeconds()).thenReturn(0);
        Mockito.when(context.getGlobalConfig()).thenReturn(dealerGlobalConfig);

        return context;
    }
}