/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.yandex.market.psku.postprocessor.common.spring;

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