package ru.yandex.market.checkout.checkouter.test.config.services;

import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.stereotype.Component;

import ru.yandex.market.queuedcalls.QueuedCallService;

@Component
public class QueuedCallsServiceDependsOnLiqubaseBeanPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

    protected QueuedCallsServiceDependsOnLiqubaseBeanPostProcessor() {
        super(QueuedCallService.class, "liquibaseCheckouter");
    }
}
