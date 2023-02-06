package ru.yandex.common.util.jmx;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.management.MXBean;

import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorObserverRegistrarTest {
    Predicate<Object> isMBean = o -> JmxUtils.isMBean(o.getClass());

    public interface MBeanExecutorMBean extends Executor {
    }

    public interface ExecutorMXBean extends Executor {
    }

    @MXBean
    public interface ExecutorObserver extends Executor {
    }

    class BaseExecutor extends ThreadPoolExecutor {
        BaseExecutor() {
            super(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        }
    }

    // напоминание, конвенция такова что класс X должен наследовать интерфейс XMBean
    class MBeanExecutor extends BaseExecutor implements MBeanExecutorMBean {
    }

    class MXBeanExecutor extends BaseExecutor implements ExecutorMXBean {
    }

    class AnnotatedMXBeanExecutor extends BaseExecutor implements ExecutorObserver {
    }

    @Test
    public void makeObserverForMBeanReturnsNull() {
        Executor e = new MBeanExecutor();
        Object observer = ExecutorObserverRegistrar.makeObserverFor(e);
        assertThat(observer).as("MBean should not be additionally observed").isNull();
    }

    @Test
    public void makeObserverForMXBeanReturnsNull() {
        Executor e = new MXBeanExecutor();
        Object observer = ExecutorObserverRegistrar.makeObserverFor(e);
        assertThat(observer).as("MXBean should not be additionally observed").isNull();
    }

    @Test
    public void makeObserverForAnnotatedMXBeanReturnsNull() {
        Executor e = new AnnotatedMXBeanExecutor();
        Object observer = ExecutorObserverRegistrar.makeObserverFor(e);
        assertThat(observer).as("annotated MXBean should not be additionally observed").isNull();
    }

    @Test
    public void makeObserverForThreadPoolExecutor() {
        Executor e = new BaseExecutor();
        Object observer = ExecutorObserverRegistrar.makeObserverFor(e);
        assertThat(observer)
                .isNotNull()
                .matches(isMBean);
    }

    @Test
    public void makeObserverForArbitraryExecutor() {
        Executor e = Runnable::run;
        Object observer = ExecutorObserverRegistrar.makeObserverFor(e);
        assertThat(observer).as("arbitrary Executor is not yet supported").isNull();
    }

    @Test
    public void postProcessBeanFactory() {
        // given
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        ExecutorObserverRegistrar registrar = new ExecutorObserverRegistrar(factory);
        factory.registerBeanDefinition(
                "someObject",
                BeanDefinitionBuilder.genericBeanDefinition(String.class)
                        .addConstructorArgValue("some")
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "someObjectLazy",
                BeanDefinitionBuilder.genericBeanDefinition(String.class)
                        .addConstructorArgValue("someLazy")
                        .setLazyInit(true) // для примера
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "notThreadPoolExecutor",
                BeanDefinitionBuilder.genericBeanDefinition(Executors.class)
                        .setFactoryMethod("newSingleThreadExecutor") // возвращает прокси, а не сам тредпул
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "jdkExecutors",
                BeanDefinitionBuilder.genericBeanDefinition(Executors.class)
                        .setFactoryMethod("newFixedThreadPool")
                        .addConstructorArgValue(1)
                        .setLazyInit(true) // для проверки преинициализации
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "yaExecutors",
                BeanDefinitionBuilder.genericBeanDefinition(ru.yandex.common.util.concurrent.Executors.class)
                        .setFactoryMethod("newFixedThreadPool")
                        .addConstructorArgValue(1)
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "threadPoolExecutor",
                BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolExecutor.class)
                        .addConstructorArgValue(1)
                        .addConstructorArgValue(1)
                        .addConstructorArgValue(0L)
                        .addConstructorArgValue(TimeUnit.MILLISECONDS)
                        .addConstructorArgValue(new LinkedBlockingQueue<Runnable>())
                        .setLazyInit(true) // для проверки преинициализации
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "threadPoolTaskExecutor",
                BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskExecutor.class)
                        .setLazyInit(true) // для проверки преинициализации
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "threadPoolTaskScheduler",
                BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskScheduler.class)
                        .setLazyInit(true) // для проверки преинициализации
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "threadPoolExecutorFactoryBean",
                BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolExecutorFactoryBean.class)
                        .addPropertyValue("maxPoolSize", 1)
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "alreadyObserved",
                BeanDefinitionBuilder.genericBeanDefinition(Executors.class)
                        .setFactoryMethod("newSingleThreadExecutor") // не важно что здесь, главное что уже есть observer
                        .setLazyInit(true)
                        .getBeanDefinition()
        );
        factory.registerBeanDefinition(
                "alreadyObservedObserver",
                BeanDefinitionBuilder.genericBeanDefinition(String.class)
                        .addConstructorArgValue("whatever")
                        .setLazyInit(true)
                        .getBeanDefinition()
        );

        // when
        registrar.postProcessBeanFactory(factory);
        factory.freezeConfiguration(); // для стабильного ассерта
        factory.preInstantiateSingletons(); // для проверки обработки lazy бинов

        // then
        assertThat(factory.getSingletonNames())
                .as("observers are eagerly initialized together with their dependants")
                .containsExactlyInAnyOrder(
                        "someObject",
                        "notThreadPoolExecutor",
                        "notThreadPoolExecutorObserver",
                        "jdkExecutors",
                        "jdkExecutorsObserver",
                        "yaExecutors",
                        "yaExecutorsObserver",
                        "threadPoolExecutor",
                        "threadPoolExecutorObserver",
                        "threadPoolTaskExecutor",
                        "threadPoolTaskExecutorObserver",
                        "threadPoolTaskScheduler",
                        "threadPoolTaskSchedulerObserver",
                        "threadPoolExecutorFactoryBean",
                        "threadPoolExecutorFactoryBeanObserver"
                );
        assertThat(factory.getBean("someObject"))
                .as("observer should be registered only for Executor")
                .isEqualTo("some");
        assertThat(factory.getBean("notThreadPoolExecutorObserver"))
                .as("observer for non-observable executors is noop but is still registered")
                .matches(b -> b.equals(null) && !isMBean.test(b)); // Spring's NullBean equals null
        Executor threadPoolExecutor = factory.getBean("notThreadPoolExecutor", Executor.class);
        assertThat(threadPoolExecutor).isNotInstanceOf(ThreadPoolExecutor.class);
        checkExecutorAndObserver(factory, "jdkExecutors", ThreadPoolExecutor.class);
        checkExecutorAndObserver(factory, "yaExecutors", ThreadPoolExecutor.class);
        checkExecutorAndObserver(factory, "threadPoolExecutor", ThreadPoolExecutor.class);
        checkExecutorAndObserver(factory, "threadPoolExecutorFactoryBean", ThreadPoolExecutor.class);
        checkExecutorAndObserver(factory, "threadPoolTaskExecutor", ThreadPoolTaskExecutor.class);
        checkExecutorAndObserver(factory, "threadPoolTaskScheduler", ThreadPoolTaskScheduler.class);
    }

    private void checkExecutorAndObserver(DefaultListableBeanFactory factory,
                                          String beanName,
                                          Class<? extends Executor> expectedExecutorClass) {
        assertThat(factory.containsSingleton(beanName))
                .as(beanName + " should be eagerly initialized")
                .isTrue();
        Object threadPoolExecutorObserver = factory.getBean(beanName + "Observer");
        assertThat(threadPoolExecutorObserver)
                .as("observer for " + beanName + " should be registered")
                .isNotNull()
                .matches(isMBean);
        Executor threadPoolExecutor = factory.getBean(beanName, Executor.class);
        assertThat(threadPoolExecutor).isInstanceOf(expectedExecutorClass);
    }
}
