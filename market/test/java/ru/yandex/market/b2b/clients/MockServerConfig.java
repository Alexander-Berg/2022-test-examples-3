package ru.yandex.market.b2b.clients;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientInfo;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.clients.ClientsProperties;
import ru.yandex.market.starter.properties.tvm.TvmProperties;

@Configuration
public class MockServerConfig implements BeanPostProcessor {

    @Autowired
    WireMockServer mockServer;

    /**
     * Подстановка у внешнего taxi API на адрес WireMockServer.
     * Отключение TVM для тестов.
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ClientsProperties) {
            final ClientsProperties clientsProperties = (ClientsProperties) bean;
            Map<String, ClientInfo> infos = clientsProperties.getList();
            infos.get("taxi_corp_request_market_offer").setUrl(mockServer.baseUrl());
            infos.get("taxi_authproxy").setUrl(mockServer.baseUrl());
            infos.get("taxi_can_order").setUrl(mockServer.baseUrl());
            infos.get("taxi_corp_contracts").setUrl(mockServer.baseUrl());
            infos.get("uzedo").setUrl(mockServer.baseUrl());
        }
        if (bean instanceof TvmProperties) {
            final TvmProperties tvmProperties = (TvmProperties) bean;
            tvmProperties.setClientsTvmDisabled(true);
        }
        return bean;
    }
}
