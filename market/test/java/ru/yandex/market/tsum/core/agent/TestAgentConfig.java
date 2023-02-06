package ru.yandex.market.tsum.core.agent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.yandex.market.tsum.core.TestMongo;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 30/09/16
 */
@Configuration
@Import({TestMongo.class})
public class TestAgentConfig {

    @Bean
    public AgentMongoDao mongoAgentService(MongoTemplate mongoTemplate) {
        return new AgentMongoDao(mongoTemplate);
    }

}
