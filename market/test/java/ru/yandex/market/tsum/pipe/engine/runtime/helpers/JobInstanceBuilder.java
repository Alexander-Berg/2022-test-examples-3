package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceInjector;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 08.02.18
 */
public class JobInstanceBuilder<T extends JobExecutor> {
    private final Class<T> jobType;
    private GenericApplicationContext context;
    private final Multimap<String, Resource> resourceMultimap = LinkedHashMultimap.create();
    private ResourceInjector resourceInjector;

    public JobInstanceBuilder(Class<T> jobType, GenericApplicationContext context, ResourceInjector resourceInjector) {
        this.jobType = jobType;
        this.context = context;
        this.resourceInjector = resourceInjector;
    }

    public JobInstanceBuilder(Class<T> jobType) {
        this.jobType = jobType;

        context = new AnnotationConfigApplicationContext();
        context.refresh();

        resourceInjector = new TestResourceInjector();
    }

    public static <T extends JobExecutor> JobInstanceBuilder<T> create(Class<T> jobType) {
        return new JobInstanceBuilder<>(jobType);
    }

    public JobInstanceBuilder<T> withResource(Resource resource) {
        resourceMultimap.put(resource.getClass().getName(), resource);
        return this;
    }

    public JobInstanceBuilder<T> withResources(Resource... resources) {
        for (Resource resource : resources) {
            resourceMultimap.put(resource.getClass().getName(), resource);
        }
        return this;
    }

    public JobInstanceBuilder<T> withBean(Object bean) {
        context.getBeanFactory().registerSingleton(bean.getClass().getName(), bean);
        return this;
    }

    public JobInstanceBuilder<T> replaceBean(Object bean) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        ((DefaultListableBeanFactory) beanFactory).destroySingleton(bean.getClass().getName());
        beanFactory.registerSingleton(bean.getClass().getName(), bean);
        return this;
    }

    public JobInstanceBuilder<T> withBeanIfNotPresent(Object bean) {
        if (context.containsBean(bean.getClass().getName())) {
            return this;
        }
        context.getBeanFactory().registerSingleton(bean.getClass().getName(), bean);
        return this;
    }

    public JobInstanceBuilder<T> withBeans(Object... beans) {
        for (Object bean : beans) {
            context.getBeanFactory().registerSingleton(bean.getClass().getName(), bean);
        }
        return this;
    }

    public T create() {
        T instance = context.getAutowireCapableBeanFactory().createBean(jobType);

        resourceInjector.inject(instance, new ResourceContainer(resourceMultimap));

        return instance;
    }
}
