package ru.yandex.market.crm.triggers.services.bpm.correlation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

@Configuration
public class PendingMessageSerializerConfiguration {

    @Bean
    public PendingMessageSerializer testPendingMessageSerializer(JsonSerializer jsonSerializer,
                                                                 JsonDeserializer jsonDeserializer) {
        return new PendingMessageSerializer(jsonSerializer, jsonDeserializer);
    }
}
