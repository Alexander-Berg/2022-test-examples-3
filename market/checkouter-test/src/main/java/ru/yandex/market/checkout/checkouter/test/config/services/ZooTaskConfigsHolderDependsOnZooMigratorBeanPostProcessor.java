package ru.yandex.market.checkout.checkouter.test.config.services;

import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.common.tasks.config.ZooTaskConfigsHolder;

@Component
public class ZooTaskConfigsHolderDependsOnZooMigratorBeanPostProcessor
        extends AbstractDependsOnBeanFactoryPostProcessor {

    protected ZooTaskConfigsHolderDependsOnZooMigratorBeanPostProcessor() {
        super(ZooTaskConfigsHolder.class, "zooMigratorSpring");
    }
}
