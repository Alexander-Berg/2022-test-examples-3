package ru.yandex.market.tsum.core.mongo;

import com.mongodb.ClientSessionOptions;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.function.Function;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 16/10/2019
 */
public class MockMongoTransactions extends MongoTransactions {

    public MockMongoTransactions(MongoTemplate mongoTemplate, ApplicationContext applicationContext) {
        super(mongoTemplate, null, applicationContext);
    }

    @Override
    public <R> R executeWithResult(
        ClientSessionOptions options, Function<MongoTransaction, R> callback
    ) {
        synchronized (this) { // emulate transaction.
            return callback.apply(new MongoTransaction(0L, null, mongoTemplate, applicationContext));
        }
    }
}
