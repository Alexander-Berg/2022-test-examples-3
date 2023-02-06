package ru.yandex.market.yql_test.service;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ru.yandex.market.yql_test.proxy.YqlProxyServer;

@Service
public class YqlProxyServerService extends YqlProxyServer {

    public YqlProxyServerService(
            @Value("${yql.test.url}") String yqlUrl,
            @Value("${yql.test.proxy.port}") int proxyPort,
            @Value("${yql.test.proxy.maxRetries:3}") int maxRetries
    ) {
        super(yqlUrl, proxyPort, maxRetries);
    }

    @Override
    @PreDestroy
    public synchronized void stop() throws Exception {
        super.stop();
    }
}
