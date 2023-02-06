package ru.yandex.market.delivery.tracker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistic.gateway.client.config.SqsProperties;

@Configuration
public class LgwSqsMockConfiguration {

    @Bean
    public SqsProperties sqsProperties() {
        return new SqsProperties()
            .setRegion("")
            .setS3EndpointHost("http://none")
            .setS3AccessKey("")
            .setS3SecretKey("")
            .setS3BucketName("")
            .setSqsAccessKey("")
            .setSqsSecretKey("")
            .setSqsEndpointHost("http://none");

    }
}
