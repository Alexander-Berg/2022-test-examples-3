package ru.yandex.market.logshatter.config.storage;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.health.configs.logshatter.LogshatterConfigDao;
import ru.yandex.market.health.configs.logshatter.config_history.LogshatterVersionHistoryDao;
import ru.yandex.market.logshatter.config.storage.json.LogshatterConfigFileJsonLoader;
import ru.yandex.market.monitoring.ComplicatedMonitoring;

@Ignore
public class LogshatterConfigCodeToMongoCopierTest {
    private static final String CONF_PATH = "market/infra/market-health/config-cs-logshatter/src/conf.d";

    @Test
    public void test() {
        MongoTemplate mongoTemplate = new MongoTemplate(
            new SimpleMongoDbFactory(
                new MongoClient(new MongoClientURI("mongodb://localhost:27017")),
                "health-local"
            ));
        LogshatterConfigCodeToMongoCopier copier = new LogshatterConfigCodeToMongoCopier(
            new LogshatterConfigFileJsonLoader(Paths.getSourcePath(CONF_PATH)),
            new LogshatterConfigDao(
                mongoTemplate,
                new LogshatterVersionHistoryDao(mongoTemplate),
                new LocalValidatorFactoryBean(),
                null, null, null, null
            ),
            new ComplicatedMonitoring()
        );

        copier.copyConfigsFromCodeToMongo();
    }
}
