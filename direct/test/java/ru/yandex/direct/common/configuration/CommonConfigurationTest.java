package ru.yandex.direct.common.configuration;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class CommonConfigurationTest {
    @Test
    public void testApplicationContextInitialization() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(CommonConfiguration.class);
        ctx.refresh();

        // проверяем получение всех бинов, чтобы никакой @Lazy бин не остался обиженным
        for (String beanName : ctx.getBeanDefinitionNames()) {
            ctx.getBean(beanName);
        }
    }
}
