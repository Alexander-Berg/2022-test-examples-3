package ru.yandex.common.framework;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/common-framework-lite/framework-config-lite.xml")
public class SpringContextTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;


    @Test
    public void testStartupContext() {
        final String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        assertThat(beanDefinitionNames.length > 0, equalTo(true));

        for (final String name : beanDefinitionNames) {
            final BeanDefinition definition = beanFactory.getBeanDefinition(name);

            if (!definition.isAbstract() && definition.isLazyInit()) {
                final Object bean = beanFactory.getBean(name);
                assertThat(bean, notNullValue());
            }
        }
    }

}
