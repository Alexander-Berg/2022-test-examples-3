package ru.yandex.chemodan.app.djfs.core.db.mongo;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.bolts.collection.Cf;

/**
 * @author eoshch
 */
@Configuration
@Profile(ActivateInMemoryMongo.PROFILE)
public class DjfsInMemoryMongoContextConfiguration {
    @Bean
    @Qualifier("common")
    public MongoClient commonMongoClient() {
        return new Fongo("common").getMongo();
    }

    @Bean
    @Qualifier("blockings")
    public MongoClient blockingsMongoClient() {
        return new Fongo("blockings").getMongo();
    }

    @Bean
    @Qualifier("sharded")
    public MongoClientFactory shardedMongoClientFactory() {
        return serverAddresses -> new Fongo(String.join(";", Cf.wrap(serverAddresses).map(ServerAddress::toString)))
                .getMongo();
    }
}
