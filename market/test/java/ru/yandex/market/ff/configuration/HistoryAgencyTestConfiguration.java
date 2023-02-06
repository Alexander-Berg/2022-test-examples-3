package ru.yandex.market.ff.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.ff.framework.history.HistoryAgencyDispatcher;
import ru.yandex.market.ff.framework.history.HistoryAgentImpl;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.framework.history.wrapper.MyThreadContext;

@Slf4j
@Configuration
public class HistoryAgencyTestConfiguration {

    @Bean
    public HistoryAgent getHistoryAgent(ObjectFactory<MyThreadContext> historyBlockContextGetter) {
        HistoryAgencyDispatcher dispatcher = event -> {
            log.info("HistoryAgencyDispatcher EVENT: {}  ", event);
        };
        return new HistoryAgentImpl(dispatcher, historyBlockContextGetter::getObject);
    }
}
