package ru.yandex.market.logistics.management.configuration;

import java.net.MalformedURLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.common.util.xmlrpc.XmlRPCServceExecutor;
import ru.yandex.market.logistics.management.configuration.properties.BalanceProperties;
import ru.yandex.market.logistics.management.service.balance.Balance2;
import ru.yandex.market.logistics.management.util.balance.Balance2XmlRPCServiceFactory;

@Configuration
public class TestBalanceConfiguration {

    @Bean
    @Primary
    public XmlRPCServceExecutor<Balance2> testXmlRPCServiceExecutor(
        Balance2XmlRPCServiceFactory balance2XmlRPCServiceFactory,
        BalanceProperties balanceProperties,
        Balance2 balance2
    ) {
        XmlRPCServceExecutor<Balance2> xmlRPCServiceExecutor = new XmlRPCServceExecutor<>() {
            @Override
            public Balance2 getService(String... callTimeoutName) {
                return balance2;
            }
        };
        xmlRPCServiceExecutor.setServiceFactory(balance2XmlRPCServiceFactory);
        xmlRPCServiceExecutor.setReplyTimeout(balanceProperties.getTimeout());
        return xmlRPCServiceExecutor;
    }

    @Bean
    public Balance2XmlRPCServiceFactory balance2XmlRPCServiceFactory() {
        return new Balance2XmlRPCServiceFactory() {
            @Override
            @Value("${balance.xmlrpc.url}")
            public void setServerUrl(String serverUrl) throws MalformedURLException {
                super.setServerUrl(serverUrl);
            }
        };
    }
}
