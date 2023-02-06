package ru.yandex.market.fulfillment.stockstorage.configuration;

import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

import ru.yandex.market.fulfillment.stockstorage.events.stock.EventAuditListenerImpl;
import ru.yandex.market.fulfillment.stockstorage.service.RetryingService;
import ru.yandex.market.fulfillment.stockstorage.service.audit.StockEventsHandler;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.strategy.availability.DefaultSkuChangeAvailabilityProducingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.strategy.stocks.DefaultSkuChangeStocksAmountProducingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;

@Configuration
public class ServicesTestConfiguration {

    @Bean
    public RetryingService retryingService(SystemPropertyService systemPropertyService) {
        return new RetryingService(3, 0, systemPropertyService);
    }

    @Bean
    public RetryingService stockSyncJobDeleteService(SystemPropertyService systemPropertyService) {
        return new RetryingService(3, 0, systemPropertyService);
    }

    @Bean
    @Primary
    //Should be defied explicitly because postprocessor does not invoked
    public EventAuditListenerImpl stockEventsListener(List<StockEventsHandler> handlers) {
        return new EventAuditListenerImpl(handlers);
    }

    // enable spy
    @Bean
    public BeanPostProcessor skuChangeAvailabilityMessageProducerPostProcessor() {
        return new ProxiedMockPostProcessor(SkuChangeAvailabilityMessageProducer.class);
    }

    @Bean
    public BeanPostProcessor skuChangeStocksAmountMessageProducerPostProcessor() {
        return new ProxiedMockPostProcessor(SkuChangeStocksAmountMessageProducer.class);
    }

    @Bean
    public BeanPostProcessor defaultSkuChangeAvailabilityProducingStrategyPostProcessor() {
        return new ProxiedMockPostProcessor(DefaultSkuChangeAvailabilityProducingStrategy.class);
    }

    @Bean
    public BeanPostProcessor defaultSkuChangeStocksAmountProducingStrategyPostProcessor() {
        return new ProxiedMockPostProcessor(DefaultSkuChangeStocksAmountProducingStrategy.class);
    }

    static class ProxiedMockPostProcessor implements BeanPostProcessor {
        private final Class<?> mockedClass;

        private ProxiedMockPostProcessor(Class<?> mockedClass) {
            this.mockedClass = mockedClass;
        }

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
                throws BeansException {
            return bean;
        }
    }

}
