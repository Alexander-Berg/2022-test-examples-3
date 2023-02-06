package ru.yandex.market.health;

import java.net.InetSocketAddress;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoTestConfig {
    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        return new MongoServer(new MemoryBackend());
    }

    @Bean
    public MongoClient mongoClient(
        MongoServer mongoServer,
        @Value("${mongo.port:#{null}}") Integer mongoPort
    ) {
        final InetSocketAddress mongoSocketAddress;
        if (mongoPort == null) {
            mongoSocketAddress = mongoServer.bind();
        } else {
            mongoSocketAddress = new InetSocketAddress("localhost", mongoPort);
            mongoServer.bind(mongoSocketAddress);
        }
        return new MongoClient(new ServerAddress(mongoSocketAddress));
    }

    @Bean
    public MongoTemplate clickphiteConfigsMongoTemplate(
        MongoClient mongoClient,
        @Value("${clickphite.mongo.db}") String databaseName
    ){
        return Mockito.spy(new MongoTemplate(mongoClient, databaseName));
    }
}
