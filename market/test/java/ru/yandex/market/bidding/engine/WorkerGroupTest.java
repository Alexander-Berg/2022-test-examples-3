package ru.yandex.market.bidding.engine;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicStampedReference;

import org.apache.commons.lang.math.LongRange;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.failover.FailoverTestUtils;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;

public class WorkerGroupTest {


    private int workerCount;
    private int dice;

    /**
     * Новый магазин - получаем произвольный worker
     */
    @Test
    public void testGetWorkerForNewShop() throws Exception {
        int workerCount = 10;
        for (int i = 0; i < workerCount; i++) {
            testGetWorkerForNewShop(i, workerCount);
        }

    }

    protected void testGetWorkerForNewShop(int dice, int workerCount) throws Exception {
        Long id = 1L;
        Dispatcher.WorkerGroup<Long> workerGroup = createWorkerGroupMock();
        Dispatcher.Worker[] workers = workers(workerCount);

        Mockito.when(workerGroup.dice()).thenReturn(dice);
        Mockito.when(workerGroup.find(anyLong())).thenCallRealMethod();

        FailoverTestUtils.setPrivate(workerGroup, "shards", new HashMap());
        FailoverTestUtils.setPrivate(workerGroup, "workers", workers);

        long start = System.currentTimeMillis() / 1_000;
        AtomicStampedReference workerRef = workerGroup.find(id);
        long end = System.currentTimeMillis() / 1_000;

        assertTrue(workerRef.getReference() == workers[dice]);
        assertTrue(format("%d not in [%d;%d]", workerRef.getStamp(), start, end),
                new LongRange(start, end).containsLong(workerRef.getStamp()));

        Mockito.verify(workerGroup).find(anyLong());
        Mockito.verify(workerGroup).dice();
        Mockito.verifyNoMoreInteractions(workerGroup);

    }


    /**
     * Worker по магазину уже есть
     */
    @Test
    public void testGetWorkerForExistingShop() throws Exception {
        this.workerCount = 10;

        for (int shopWorkerIdx = 0; shopWorkerIdx < workerCount; shopWorkerIdx++) {
            for (int dice = 0; dice < workerCount; dice++) {
                this.dice = dice;
                // проверяем, что номер worker-а у магазина не изменился,
                // так как он в кэше, несмотря на то, что dice возвращает что-то другое
                int initialStamp = (int) (System.currentTimeMillis() / 1_000);
                assertWorkerIdxChanged(shopWorkerIdx, shopWorkerIdx, initialStamp, false);
            }
        }
    }

    /**
     * Ворекр уже слшком давно не использовался - меняем
     */
    @Test
    public void testTooOldWorker() throws Exception {
        this.workerCount = 10;

        for (int shopWorkerIdx = 0; shopWorkerIdx < workerCount; shopWorkerIdx++) {
            for (int dice = 0; dice < workerCount; dice++) {
                this.dice = dice;
                // проверяем, что номер worker-а у магазина изменился,
                // так как он устарел. новый номер worker-а определяется dice
                if (shopWorkerIdx != dice) {
                    assertWorkerIdxChanged(shopWorkerIdx, dice, 0, true);
                }
            }
        }
    }


    private void assertWorkerIdxChanged(int initialWorkerIndex, int shouldBecame,
                                        int initialStamp, boolean shouldChangeWorker) throws Exception {
        Long id = 1L;
        Dispatcher.WorkerGroup<Long> workerGroup = createWorkerGroupMock();
        Dispatcher.Worker[] workers = workers(workerCount);

        Mockito.when(workerGroup.dice()).thenReturn(dice);
        Mockito.when(workerGroup.find(anyLong())).thenCallRealMethod();

        HashMap<Long, AtomicStampedReference<Dispatcher.Worker>> shards = new HashMap<>();
        shards.put(id, new AtomicStampedReference<>(workers[initialWorkerIndex], initialStamp));
        FailoverTestUtils.setPrivate(workerGroup, "shards", shards);
        FailoverTestUtils.setPrivate(workerGroup, "workers", workers);


        long start = System.currentTimeMillis() / 1_000;
        AtomicStampedReference workerRef = workerGroup.find(id);
        long end = System.currentTimeMillis() / 1_000;

        assertTrue(format("%s!=%s", workerRef.getReference(), workers[shouldBecame]),
                workerRef.getReference() == workers[shouldBecame]);
        assertTrue(format("%d not in [%d;%d]", workerRef.getStamp(), start, end),
                new LongRange(start, end).containsLong(workerRef.getStamp()));

        Mockito.verify(workerGroup).find(anyLong());
        if (shouldChangeWorker) {
            Mockito.verify(workerGroup).dice();
        }
        Mockito.verifyNoMoreInteractions(workerGroup);
    }

    private Dispatcher.Worker[] workers(int cnt) throws Exception {
        Dispatcher.Worker[] arr = new Dispatcher.Worker[cnt];
        for (int i = 0; i < cnt; i++) {
            arr[i] = Mockito.mock(Dispatcher.Worker.class);
            FailoverTestUtils.setPrivate(arr[i], "no", i);
        }
        return arr;
    }

    protected Dispatcher.WorkerGroup<Long> createWorkerGroupMock()
            throws NoSuchFieldException, IllegalAccessException {
        Dispatcher.WorkerGroup<Long> workerGroup = (Dispatcher.WorkerGroup<Long>) Mockito.mock(Dispatcher.WorkerGroup.class);
        FailoverTestUtils.setPrivate(workerGroup, "abandonTimeSeconds", new Integer(10_000));
        return workerGroup;
    }

}