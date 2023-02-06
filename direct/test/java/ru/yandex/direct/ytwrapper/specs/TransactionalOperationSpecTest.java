package ru.yandex.direct.ytwrapper.specs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.transactions.Transaction;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionalOperationSpecTest {
    private Yt yt;
    private OperationSpec specOne;
    private OperationSpec specTwo;
    private OperationSpec transactionalSpec;
    private List<Object> callStack;

    @Before
    public void before() {
        yt = mock(Yt.class, RETURNS_DEEP_STUBS);
        specOne = mock(OperationSpec.class);
        doAnswer(i -> callStack.add(i.getMock())).when(specOne).run(any(), any());

        specTwo = mock(OperationSpec.class);
        doAnswer(i -> callStack.add(i.getMock())).when(specTwo).run(any(), any());

        transactionalSpec = new TransactionalOperationSpec(Arrays.asList(specOne, specTwo));
        callStack = new ArrayList<>();
    }

    @Test
    public void testRunWithTransaction() {
        GUID transctionUid = GUID.valueOf("1234-1234-1234-1234");
        transactionalSpec.run(yt, transctionUid);
        verify(specOne).run(eq(yt), eq(transctionUid));
        verify(specTwo).run(eq(yt), eq(transctionUid));

        assertThat("Общее количество вызовов верное", callStack.size(), equalTo(2));
        assertThat("Вызовы сделаны в ожидаемом порядке", callStack.get(0), equalTo(specOne));
        assertThat("Вызовы сделаны в ожидаемом порядке", callStack.get(1), equalTo(specTwo));
        System.err.println(callStack);
    }

    @Test
    public void testRun() {
        Transaction transaction = mock(Transaction.class);
        when(yt.transactions().startAndGet(any(), anyBoolean(), any())).thenReturn(transaction);

        transactionalSpec.run(yt);
        verify(specOne).run(eq(yt), any());
        verify(specTwo).run(eq(yt), any());

        assertThat("Общее количество вызовов верное", callStack.size(), equalTo(2));
        assertThat("Вызовы сделаны в ожидаемом порядке", callStack.get(0), equalTo(specOne));
        assertThat("Вызовы сделаны в ожидаемом порядке", callStack.get(1), equalTo(specTwo));
    }
}
