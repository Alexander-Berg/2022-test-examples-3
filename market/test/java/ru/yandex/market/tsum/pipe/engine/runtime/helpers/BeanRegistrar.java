package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.05.17
 */
public class BeanRegistrar {
    public static String registerNamedBean(Object bean, GenericApplicationContext applicationContext) {
        String beanName = UUID.randomUUID().toString();
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        beanFactory.registerSingleton(beanName, bean);
        return beanName;
    }
}
