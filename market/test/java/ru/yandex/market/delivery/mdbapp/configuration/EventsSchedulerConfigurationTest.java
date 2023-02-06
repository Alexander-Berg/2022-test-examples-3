package ru.yandex.market.delivery.mdbapp.configuration;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

public class EventsSchedulerConfigurationTest {

    @Test
    public void postProcessBeanFactory() {
        EventsSchedulerConfiguration config = new EventsSchedulerConfiguration();
        DefaultListableBeanFactory beanFactory = Mockito.mock(DefaultListableBeanFactory.class);

        config.postProcessBeanFactory(beanFactory);

        for (int i = 0; i < 60; i++) {
            checkPollerCreation(beanFactory, i);
        }
        Mockito.verifyNoMoreInteractions(beanFactory);
    }

    private void checkPollerCreation(DefaultListableBeanFactory beanFactory, int bucket) {
        checkBeanFactoryCall(
            beanFactory,
            bucket,
            EventsSchedulerConfiguration.eventManagerBeanName(bucket)
        );

        checkBeanFactoryCall(
            beanFactory,
            bucket,
            EventsSchedulerConfiguration.pollerBeanName(bucket)
        );


        checkBeanFactoryCall(
            beanFactory,
            bucket,
            EventsSchedulerConfiguration.schedulerBeanName(bucket)
        );
    }

    private void checkBeanFactoryCall(DefaultListableBeanFactory beanFactory, int bucket, String s) {
        Mockito.verify(beanFactory, Mockito.times(1)).registerBeanDefinition(
            Mockito.eq(s),
            Mockito.argThat(bd -> {
                Object arg0 = bd.getConstructorArgumentValues().getGenericArgumentValues().get(0).getValue();

                return arg0 == Integer.valueOf(bucket);
            })
        );
    }
}
