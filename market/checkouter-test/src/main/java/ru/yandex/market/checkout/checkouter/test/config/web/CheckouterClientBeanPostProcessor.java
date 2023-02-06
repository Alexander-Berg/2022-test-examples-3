package ru.yandex.market.checkout.checkouter.test.config.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

public class CheckouterClientBeanPostProcessor implements BeanPostProcessor {
    private final TvmTicketProvider tvmTicketProvider;

    public CheckouterClientBeanPostProcessor(TvmTicketProvider tvmTicketProvider) {
        this.tvmTicketProvider = tvmTicketProvider;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof CheckouterClient && beanName.equals("checkouterClient"))) {
            return bean;
        }

        CheckouterClient checkouterClient = (CheckouterClient) bean;
        checkouterClient.setTvmTicketProvider(tvmTicketProvider);
        return checkouterClient;
    }
}
