package ru.yandex.market.checkout.checkouter.test.config.web;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;

public class CheckouterRestTemplateBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private static final String REQUEST_FACTORY_PROPERTY_NAME = "requestFactory";

    private final String mockCheckouterClientHttpFactoryBeanName;
    private final String checkouterRestTemplateBeanName;

    public CheckouterRestTemplateBeanFactoryPostProcessor(String checkouterRestTemplateBeanName,
                                                          String mockCheckouterClientHttpFactoryBeanName) {
        this.checkouterRestTemplateBeanName = checkouterRestTemplateBeanName;
        this.mockCheckouterClientHttpFactoryBeanName = mockCheckouterClientHttpFactoryBeanName;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanDefinition checkouterRestTemplateBeanDefinition = beanFactory.getBeanDefinition(
                checkouterRestTemplateBeanName
        );

        RuntimeBeanReference mockCheckouterClientReference = new RuntimeBeanReference(
                mockCheckouterClientHttpFactoryBeanName
        );

        MutablePropertyValues propertyValues = checkouterRestTemplateBeanDefinition.getPropertyValues();
        propertyValues.addPropertyValue(REQUEST_FACTORY_PROPERTY_NAME, mockCheckouterClientReference);

    }
}
