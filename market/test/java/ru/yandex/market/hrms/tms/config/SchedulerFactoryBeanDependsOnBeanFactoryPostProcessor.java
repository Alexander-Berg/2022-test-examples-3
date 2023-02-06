package ru.yandex.market.hrms.tms.config;


import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

public class SchedulerFactoryBeanDependsOnBeanFactoryPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {
    protected SchedulerFactoryBeanDependsOnBeanFactoryPostProcessor(String... dependsOn) {
        super(Scheduler.class, SchedulerFactoryBean.class, dependsOn);
    }
}
