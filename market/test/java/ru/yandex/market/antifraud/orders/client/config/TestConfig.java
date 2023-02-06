package ru.yandex.market.antifraud.orders.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;


/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 21.06.19
 */
@Import(MstatAntifraudOrdersClientConfig.class)
@Configuration
@PropertySource("classpath:test-client.properties")
public class TestConfig {

    /**
     * Единственная цель создания этого бина, это проверять, что при наличии в контексте других конвертеров,
     * всё продолжает работать. Защита от истории с ABO. У них по умолчанию была конвертация в xml
     *
     * @return
     */
    @Bean
    public HttpMessageConverter httpMessageConverter() {
        return new MappingJackson2XmlHttpMessageConverter();
    }

}