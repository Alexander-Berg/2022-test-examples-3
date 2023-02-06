package ru.yandex.market.core.test.context;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Настройка контекста, разрешающая пост-обработку уже использованных бинов. Нужно для работы
 * {@link EarlyDelegatingSpy}.
 * <p>
 * // TODO удалить после исправления тикета https://st.yandex-team.ru/MBI-30432
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 * @see EarlyDelegatingSpy
 */
public class AllowSpiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        DefaultListableBeanFactory defaultListableBeanFactory =
                (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        defaultListableBeanFactory.setAllowRawInjectionDespiteWrapping(true);
    }
}
