package ru.yandex.market.delivery.mdbclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.delivery.mdbclient.MdbClient;
import ru.yandex.market.delivery.mdbclient.MdbClientFactory;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.request.trace.Module;

@Configuration
public class RestTemplateConfig {

    @Bean
    public HttpTemplate mdbHttpTemplate(@Value("${mdb.app.url}") String mdbUrl) {
        ExternalServiceProperties mdbProperties = new ExternalServiceProperties();
        mdbProperties.setUrl(mdbUrl);
        return HttpTemplateBuilder
            .create(mdbProperties, Module.DELIVERY_MDB)
            .withObjectMapper(new ObjectMapper()
                .registerModule(new JavaTimeModule())
            )
            .build();
    }

    @Bean
    public MdbClient mdbClient(HttpTemplate mdbHttpTemplate) {
        return MdbClientFactory.getMdbClient(mdbHttpTemplate);
    }
}
