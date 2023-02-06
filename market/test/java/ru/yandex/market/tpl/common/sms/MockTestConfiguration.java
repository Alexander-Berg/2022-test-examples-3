package ru.yandex.market.tpl.common.sms;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(value = YaSmsProperties.class)
public class MockTestConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public YaSmsClient yaSmsCommonClient(RestTemplate restTemplate, YaSmsProperties yaSmsProperties) {
        return new YaSmsClient(restTemplate, yaSmsProperties);
    }
}
