package ru.yandex.crypta.graph2.dao.yt.ops;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.operations.Operation;

public class AwaitTest extends EasyMockSupport {

    @Test
    public void all() throws Exception {

        ListF<Operation> ops = Cf.range(0, 1000).map(i -> niceMock(Operation.class));

        // expected behaviour
        ops.forEach(Operation::awaitAndThrowIfNotSuccess);

        replayAll();

        Await.all(ops.map(op -> (() -> op)));

        verifyAll();

    }

}
