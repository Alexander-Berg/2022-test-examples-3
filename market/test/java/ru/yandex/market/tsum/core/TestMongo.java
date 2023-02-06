package ru.yandex.market.tsum.core;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import ru.yandex.market.tsum.core.dao.ParamsDao;
import ru.yandex.market.tsum.core.mongo.MockMongoTransactions;
import ru.yandex.market.tsum.core.mongo.MongoTransactions;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 30/09/16
 */
@Configuration
public class TestMongo {

    @Value("${tsum.mongo.params-collection:params}")
    private String paramsCollection;

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoDbFactory(mongoClient));
    }

    @Bean
    public MongoDbFactory mongoDbFactory(MongoClient mongoClient) {
        return new SimpleMongoDbFactory(mongoClient, "test");
    }

    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(MongoServer mongoServer) {
        return new MongoClient(new ServerAddress(mongoServer.getLocalAddress()));
    }

    @Bean
    public ParamsDao parametersDao(MongoTemplate mongoTemplate) {
        return new ParamsDao(mongoTemplate, paramsCollection);
    }

    @Bean
    public MongoTransactions mongoTransactions(MongoTemplate mongoTemplate, ApplicationContext applicationContext) {
        return new MockMongoTransactions(mongoTemplate, applicationContext);
    }
}
