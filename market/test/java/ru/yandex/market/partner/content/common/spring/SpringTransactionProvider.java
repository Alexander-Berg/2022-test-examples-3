package ru.yandex.market.partner.content.common.spring;

import org.jooq.ConnectionProvider;
import org.jooq.Transaction;
import org.jooq.TransactionContext;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.jooq.tools.JooqLogger;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED;

/**
 * Class, which connects jooq transaction system and spring transaction system in order to use @Transactional in tests.
 * Smart copy-paste from https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-spring-example
 *
 * @author s-ermakov
 */
public class SpringTransactionProvider extends ThreadLocalTransactionProvider {

    private static final JooqLogger log = JooqLogger.getLogger(SpringTransactionProvider.class);

    private DataSourceTransactionManager txMgr;

    public SpringTransactionProvider(DataSourceTransactionManager txMgr, ConnectionProvider connectionProvider) {
        super(connectionProvider);
        this.txMgr = txMgr;
    }

    @Override
    public void begin(TransactionContext ctx) {
        log.info("Begin transaction");

        // This TransactionProvider behaves like jOOQ's DefaultTransactionProvider,
        // which supports nested transactions using Savepoints
        TransactionStatus tx = txMgr.getTransaction(new DefaultTransactionDefinition(PROPAGATION_NESTED));
        ctx.transaction(new SpringTransaction(tx));
    }

    @Override
    public void commit(TransactionContext ctx) {
        log.info("commit transaction");

        txMgr.commit(((SpringTransaction) ctx.transaction()).tx);
    }

    @Override
    public void rollback(TransactionContext ctx) {
        log.info("rollback transaction");

        txMgr.rollback(((SpringTransaction) ctx.transaction()).tx);
    }

    public static class SpringTransaction implements Transaction {
        final TransactionStatus tx;

        SpringTransaction(TransactionStatus tx) {
            this.tx = tx;
        }
    }
}