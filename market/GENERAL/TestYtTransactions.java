package ru.yandex.market.mbo.yt;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.impl.transactions.TransactionImpl;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

/**
 * @author s-ermakov
 */
public class TestYtTransactions implements YtTransactions {

    private final TestYt testYt;

    TestYtTransactions(TestYt testYt) {
        this.testYt = testYt;
    }

    @Override
    public GUID start(Optional<GUID> transactionId,
                      boolean pingAncestorTransactions,
                      Duration timeout, Optional<Instant> deadline,
                      Optional<List<GUID>> prerequisiteTransactions,
                      Map<String, YTreeNode> attributes) {
        return transactionId.orElse(GUID.create());
    }

    @Override
    public GUID start(Optional<GUID> transactionId, boolean pingAncestorTransactions,
                      Duration timeout, Optional<Instant> deadline, Map<String, YTreeNode> attributes) {
        return transactionId.orElse(GUID.create());
    }

    @Override
    public void ping(GUID transactionId, boolean pingAncestorTransactions) {
    }

    @Override
    public void commit(GUID transactionId, boolean pingAncestorTransactions) {
        testYt.cypress().commit(transactionId);
    }

    @Override
    public void commit(GUID transactionId, boolean pingAncestorTransactions,
                       Optional<List<GUID>> prerequisiteTransactions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void abort(GUID transactionId, boolean pingAncestorTransactions) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Transaction getTransaction(GUID transactionId) {
        return new TransactionImpl(transactionId, null, testYt, Instant.now(), Duration.ofMillis(100));
    }
}
