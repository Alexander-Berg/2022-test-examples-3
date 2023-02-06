package ru.yandex.direct.mysql.ytsync.common.compability;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.mysql.ytsync.common.compatibility.YtLockUtil;
import ru.yandex.direct.utils.InterruptedRuntimeException;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.LockMode;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class YtLockUtilTests {

    private static final YPath LOCK_PATH = YPath.simple("//somePath");

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Mock
    private YtTransactions ytTransactions;

    @Mock
    private Transaction transaction;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        doReturn(cypress).when(yt).cypress();
        doReturn(ytTransactions).when(yt).transactions();

        doReturn(GUID.create())
                .when(cypress).lock(any(), eq(LOCK_PATH), eq(LockMode.EXCLUSIVE), eq(true));
        doReturn(transaction)
                .when(ytTransactions).startAndGet(any(), anyBoolean(), any());
    }


    @Test
    public void checkInterruptTransactionByConsumer() {
        try {
            YtLockUtil.runInLock(yt, LOCK_PATH, () -> true, t -> t.start(Duration.ZERO));
        } catch (InterruptedRuntimeException e) {
            //
        }

        verify(cypress, never()).get(any(YPath.class));
        verify(transaction, never()).start(any());
    }

    @Test
    public void checkRunTransaction() {
        doReturn(YTree.stringNode("acquired")).when(cypress).get(any(YPath.class));
        YtLockUtil.runInLock(yt, LOCK_PATH, () -> false, t -> t.start(Duration.ZERO));

        verify(cypress).get(any(YPath.class));
        verify(transaction).start(Duration.ZERO);
    }
}
