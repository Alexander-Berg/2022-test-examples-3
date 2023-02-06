package ru.yandex.market.mbo.appcontext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author moskovkin@yandex-team.ru
 * @since 30.08.18
 */
public class DeferredInitTest {
    private static final int RETRY_INTERVAL_SECONDS = 1;

    private TestBean correct1;
    private TestBean correct2;
    private TestBean correct3;
    private FailingInitBean failing;
    private UnstableInitBean unstable;
    private List<TestBean> initializationOrder;

    private ConfigurableListableBeanFactory beanFactory;
    private ApplicationContext applicationContext;
    private DeferredContextInitializer initializer;
    private ApplicationEventPublisher applicationEventPublisher;

    @Before
    public void init() {
        beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);
        applicationContext = Mockito.mock(ApplicationContext.class);
        initializer = new DeferredContextInitializer(RETRY_INTERVAL_SECONDS);
        initializer.postProcessBeanFactory(beanFactory);
        applicationEventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        initializer.setApplicationEventPublisher(applicationEventPublisher);

        initializationOrder = new ArrayList<>();
        correct1 = new TestBean("correct1", initializationOrder);
        correct2 = new TestBean("correct2", initializationOrder);
        correct3 = new TestBean("correct3", initializationOrder);

        failing = new FailingInitBean("failing", initializationOrder);
        unstable = new UnstableInitBean("unstable", initializationOrder, 2);
    }

    @Test
    public void testInitHierarchy() {
        mockBeans(correct1, correct2, correct3);
        mockDependencies(ImmutableMap.of(
                correct1.getName(), ImmutableList.of(correct2.getName(), correct3.getName()),
                correct2.getName(), ImmutableList.of(correct3.getName())
            )
        );

        initializer.onApplicationEvent(new ContextRefreshedEvent(applicationContext));

        Assertions.assertThat(initializationOrder).containsExactly(correct3, correct2, correct1);

        verifyInitializedEventPublished();
    }

    @Test
    public void testInitAllPossible() {
        mockBeans(correct1, correct2, failing);
        mockDependencies(ImmutableMap.of(
                correct1.getName(), ImmutableList.of(correct2.getName(), failing.getName())
            )
        );

        initializer.onApplicationEvent(new ContextRefreshedEvent(applicationContext));

        Assert.assertFalse(correct1.isInitialized());
        Assert.assertTrue(correct2.isInitialized());

        verifyInitializedEventNotPublished();
    }

    @Test
    public void testInitIntermediate() {
        mockBeans(correct1, correct3);
        mockDependencies(ImmutableMap.of(
                correct1.getName(), ImmutableList.of("intermediate"),
                "intermediate", ImmutableList.of(correct3.getName())
            )
        );
        initializer.onApplicationEvent(new ContextRefreshedEvent(applicationContext));

        Assert.assertTrue(correct1.isInitialized());
        Assert.assertTrue(correct3.isInitialized());

        verifyInitializedEventPublished();
    }

    @Test
    public void testCircular() {
        mockBeans(correct1, correct3);
        mockDependencies(ImmutableMap.of(
                correct1.getName(), ImmutableList.of("intermediate1"),
                "intermediate1", ImmutableList.of("intermediate2"),
                "intermediate2", ImmutableList.of("intermediate1", correct3.getName())
            )
        );

        initializer.onApplicationEvent(new ContextRefreshedEvent(applicationContext));

        Assert.assertTrue(correct1.isInitialized());
        Assert.assertTrue(correct3.isInitialized());

        verifyInitializedEventPublished();
    }

    @Test
    public void testInitUnstable() throws InterruptedException {
        mockBeans(unstable);
        mockDependencies(Collections.emptyMap());

        initializer.onApplicationEvent(new ContextRefreshedEvent(applicationContext));

        Assert.assertFalse(unstable.isInitialized());
        verifyInitializedEventNotPublished();

        Thread.sleep(TimeUnit.SECONDS.toMillis(RETRY_INTERVAL_SECONDS + 1));

        Assert.assertTrue(unstable.isInitialized());
        verifyInitializedEventPublished();
    }

    private void mockDependencies(Map<String, List<String>> dependencies) {
        Mockito.when(beanFactory.getDependenciesForBean(Mockito.any())).thenReturn(
            new String[0]
        );

        for (Map.Entry<String, List<String>> entry : dependencies.entrySet()) {
            Mockito.when(beanFactory.getDependenciesForBean(Mockito.eq(entry.getKey()))).thenReturn(
                entry.getValue().toArray(new String[0])
            );
        }
    }

    private void mockBeans(TestBean... beans) {
        Map<String, DeferredInitialization> mock = Arrays.stream(beans)
            .collect(Collectors.toMap(b -> b.getName(), b -> b));

        Mockito.when(applicationContext.getBeansOfType(
                Mockito.eq(DeferredInitialization.class),
                Mockito.eq(true),
                Mockito.eq(false)
            )
        )
        .thenReturn(mock);
    }

    private void verifyInitializedEventPublished() {
        Mockito.verify(applicationEventPublisher)
            .publishEvent(Mockito.any(DeferredContextInitializer.DeferredInitializationDone.class));
    }

    private void verifyInitializedEventNotPublished() {
        Mockito.verify(applicationEventPublisher, Mockito.never())
            .publishEvent(Mockito.any(DeferredContextInitializer.DeferredInitializationDone.class));
    }

    private static class TestBean implements DeferredInitialization {
        private final String name;
        protected boolean initialized;
        protected final List<TestBean> order;

        private TestBean(String name, List<TestBean> order) {
            this.name = name;
            this.order = order;
        }

        public String getName() {
            return name;
        }

        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void deferredInit() {
            order.add(this);
            initialized = true;
        }

        @Override
        public String toString() {
            return "TestBean{" +
                "name='" + name + '\'' +
                '}';
        }
    }

    private static class FailingInitBean extends TestBean {
        private FailingInitBean(String name, List<TestBean> order) {
            super(name, order);
        }

        @Override
        public void deferredInit() {
            order.add(this);
            throw new RuntimeException(getName() + " initialization failed");
        }
    }

    private static class UnstableInitBean extends TestBean {
        private int attemptsToInit;
        private int attempt = 0;

        private UnstableInitBean(String name, List<TestBean> order, int attemptsToInit) {
            super(name, order);
            this.attemptsToInit = attemptsToInit;
        }

        @Override
        public void deferredInit() {
            order.add(this);
            attempt++;
            if (attempt < attemptsToInit) {
                throw new RuntimeException(getName() + " initialization failed");
            }
            initialized = true;
        }
    }
}
