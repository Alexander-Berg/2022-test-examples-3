package ru.yandex.market.checkout.common.tasks;

import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;

import ru.yandex.market.checkout.common.tasks.config.ZooTaskConfigsHolder;

public class ZooTaskConfigServiceDependsOnZooMigratorSpringBFPP extends AbstractDependsOnBeanFactoryPostProcessor {

    public ZooTaskConfigServiceDependsOnZooMigratorSpringBFPP(String beanName) {
        super(ZooTaskConfigsHolder.class, beanName);
    }
}
