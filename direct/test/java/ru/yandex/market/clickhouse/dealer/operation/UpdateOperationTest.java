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
import java.util.Arrays;

/**
 * @author Aleksei Malygin <a href="mailto:Malygin-Me@yandex-team.ru"></a>
 * Date: 06.07.18
 */
public class UpdateOperationTest {

    private String ypPartition1 = "2017-01-01";
    private String tmTaskId1 = "i-d-4-1";

    private String ypPartition2 = "2017-01-02";
    private String tmTaskId2 = "i-d-4-2";

    private String ypPartition3 = "2017-01-03";
    private String tmTaskId3 = "i-d-4-3";

    private String clickHousePartition = "201701";
    private Instant now = Instant.now();

    private PartitionYtState ytState1 = new PartitionYtState(now, 41L, now, 1);
    private PartitionYtState ytState2 = new PartitionYtState(now, 42L, now, 1);
    private PartitionYtState ytState3 = new PartitionYtState(now, 43L, now, 1);

    private TmTaskState tmTaskState1 = new TmTaskState(tmTaskId1, TmTaskState.Status.COMPLETED, "", null, now, now, now);
    private TmTaskState tmTaskState2 = new TmTaskState(tmTaskId2, TmTaskState.Status.COMPLETED, "", null, now, now, now);
    private TmTaskState tmTaskState3 = new TmTaskState(tmTaskId3, TmTaskState.Status.COMPLETED, "", null, now, now, now);

    @Test
    public void testOperation() throws Exception {
        OperationContext context = defaultOperationContextMock();

        UpdateOperation operation = new UpdateOperation(clickHousePartition, Arrays.asList(partitionState1(), partitionState2()));
        operation.runOperation(context);

        InOrder order = Mockito.inOrder(context);
        order.verify(context).applyDdl();
        order.verify(context).clearTempTable();
        order.verify(context).startTmCopyOperation(ypPartition1);
        order.verify(context).saveState();
        order.verify(context).pollTmTask(tmTaskId1);
        order.verify(context).saveState();
        order.verify(context).countTempTableRows();
        order.verify(context).startTmCopyOperation(ypPartition2);
        order.verify(context).saveState();
        order.verify(context).pollTmTask(tmTaskId2);
        order.verify(context).saveState();
        order.verify(context).countTempTableRows();
        order.verify(context).saveState();
        order.verify(context).replacePartitionFromTempTable(clickHousePartition);
        order.verify(context).countTargetTableRows(clickHousePartition);
        order.verify(context).updatePartitionStates(Arrays.asList(expectedPartitionState1, expectedPartitionState2));
        order.verify(context).saveState();
        order.verify(context).cleanError();
        order.verifyNoMoreInteractions();
        Assert.assertEquals(operation.getStep(), UpdateOperation.Step.DONE);
    }

    @Test
    public void testNotCancelableStates() throws Exception {
        OperationContext context = defaultOperationContextMock();
        Mockito.doThrow(RuntimeException.class).doNothing().when(context).replacePartitionFromTempTable(clickHousePartition);
        UpdateOperation operation = new UpdateOperation(clickHousePartition, Arrays.asList(partitionState1(), partitionState2()));

        try {
            operation.runOperation(context);
            Assert.fail(); //Unreachable cause of throw
        } catch (RuntimeException ignored) {
        }

        Assert.assertFalse(operation.canBeCanceled());
        Assert.assertEquals(operation.getStep(), UpdateOperation.Step.REPLACE_TARGET_PARTITION);
        Mockito.doThrow(RuntimeException.class).doNothing().when(context).saveState();

        try {
            operation.runOperation(context);
            Assert.fail(); //Unreachable cause of throw
        } catch (RuntimeException ignored) {
        }

        Assert.assertFalse(operation.canBeCanceled());
        Assert.assertEquals(operation.getStep(), UpdateOperation.Step.DONE);
        operation.runOperation(context);
    }

    @Test(expected = Exception.class)
    public void testValidation() throws Exception {
        OperationContext context = defaultOperationContextMock();
        Mockito.when(context.countTempTableRows()).thenReturn(41L, 41L + 42L + 43L);
        UpdateOperation operation = new UpdateOperation(clickHousePartition, Arrays.asList(partitionState1(), partitionState2()));
        operation.runOperation(context);
    }

    @Test
    public void testFinalPartitionStates() throws Exception {
        PartitionState partitionState1 = partitionState1();
        PartitionState partitionState2 = partitionState2();

        OperationContext context = defaultOperationContextMock();
        UpdateOperation operation = new UpdateOperation(clickHousePartition, Arrays.asList(partitionState1, partitionState2));
        operation.runOperation(context);

        Assert.assertSame("Required status: TRANSFERRED", PartitionState.Status.TRANSFERRED,
            partitionState1.getStatus());
        Assert.assertSame("Required status: TRANSFERRED", PartitionState.Status.TRANSFERRED,
            partitionState2.getStatus());
    }

    @Test
    public void testTransferringCount() throws Exception {
        PartitionState partitionState1 = partitionState1();
        PartitionState partitionState2 = partitionState2();
        PartitionState partitionState3 = partitionState3();

        int transferringCount1 = partitionState1.getTransferCount();
        int transferringCount2 = partitionState2.getTransferCount();
        int transferringCount3 = partitionState3.getTransferCount();

        OperationContext context = defaultOperationContextMock();
        UpdateOperation operation = new UpdateOperation(clickHousePartition, Arrays.asList(partitionState1, partitionState2));
        operation.runOperation(context);

        Assert.assertSame(transferringCount1 + 1, partitionState1.getTransferCount());
        Assert.assertSame(transferringCount2 + 1, partitionState2.getTransferCount());

        partitionState2.setStatus(PartitionState.Status.TRANSFERRED_NEED_UPDATE);
        Mockito.when(context.countTempTableRows()).thenReturn(41L, 41L + 42L);

        operation = new UpdateOperation(clickHousePartition, Arrays.asList(partitionState1, partitionState2));
        operation.runOperation(context);

        Assert.assertSame(transferringCount1 + 2, partitionState1.getTransferCount());
        Assert.assertSame(transferringCount2 + 2, partitionState2.getTransferCount());

        partitionState2.setStatus(PartitionState.Status.TRANSFERRED_NEED_UPDATE);
        Mockito.when(context.countTempTableRows()).thenReturn(41L, 41L + 42L, 41L + 42L + 43L);
        Mockito.when(context.countTargetTableRows(clickHousePartition)).thenReturn(41L + 42L + 43L);
        operation = new UpdateOperation(clickHousePartition, Arrays.asList(partitionState1, partitionState2, partitionState3));
        operation.runOperation(context);

        Assert.assertSame(transferringCount3 + 1, partitionState3.getTransferCount());

    }

    private PartitionState expectedPartitionState1 = new PartitionState(
        ypPartition1, clickHousePartition, new PartitionClickHouseState(ytState1, now, now),
        ytState1, PartitionState.Status.TRANSFERRED, 2
    );

    private PartitionState partitionState1() {
        return new PartitionState(
            ypPartition1, clickHousePartition, null, ytState1, PartitionState.Status.TRANSFERRED_NEED_UPDATE, 1
        );
    }

    private PartitionState expectedPartitionState2 = new PartitionState(
        ypPartition2, clickHousePartition, new PartitionClickHouseState(ytState2, now, now),
        ytState2, PartitionState.Status.TRANSFERRED, 2
    );

    private PartitionState partitionState2() {
        return new PartitionState(
            ypPartition2, clickHousePartition, null, ytState2, PartitionState.Status.TRANSFERRED, 1
        );
    }

    private PartitionState partitionState3() {
        return new PartitionState(
            ypPartition3, clickHousePartition, null, ytState3, PartitionState.Status.NEW
        );
    }

    private OperationContext defaultOperationContextMock() throws Exception {
        OperationContext context = Mockito.mock(OperationContext.class);
        DealerGlobalConfig dealerGlobalConfig = Mockito.mock(DealerGlobalConfig.class);
        Mockito.when(context.startTmCopyOperation(ypPartition1)).thenReturn(tmTaskId1);
        Mockito.when(context.pollTmTask(tmTaskId1)).thenReturn(tmTaskState1);

        Mockito.when(context.startTmCopyOperation(ypPartition2)).thenReturn(tmTaskId2);
        Mockito.when(context.pollTmTask(tmTaskId2)).thenReturn(tmTaskState2);
        Mockito.when(context.countTempTableRows()).thenReturn(41L, 41L + 42L);

        Mockito.when(context.startTmCopyOperation(ypPartition3)).thenReturn(tmTaskId3);
        Mockito.when(context.pollTmTask(tmTaskId3)).thenReturn(tmTaskState3);
        Mockito.when(context.countTempTableRows()).thenReturn(41L, 41L + 42L, 41L + 42L + 43L);

        Mockito.when(context.countTargetTableRows(clickHousePartition)).thenReturn(41L + 42L);

        Mockito.when(dealerGlobalConfig.getRowCountValidationAttempts()).thenReturn(3);
        Mockito.when(dealerGlobalConfig.getRowCountValidationSleepBeforeRetriesSeconds()).thenReturn(0);
        Mockito.when(context.getGlobalConfig()).thenReturn(dealerGlobalConfig);
        return context;
    }
}
