package ru.yandex.direct.core.testing;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.method.ControllerAdviceBean;

import ru.yandex.direct.utils.FunctionalUtils;

/**
 * Helper для настройки {@link StandaloneMockMvcBuilder}.
 * <p>
 * Решает проблему конфигурирования:
 * в случае {@link StandaloneMockMvcBuilder} ему надо явно указывать,
 * какие классы используются для донастройки контроллера ({@link StandaloneMockMvcBuilder#setControllerAdvice(Object...)}),
 * преобразования аргументов {@link StandaloneMockMvcBuilder#setMessageConverters(HttpMessageConverter[])}, etc.
 * <p>
 * Для донастройки используется {@link ApplicationContext} приложения.
 */
public class MockMvcCreator {
    private final List<Object> controllerAdviceBeans;

    @Autowired
    public MockMvcCreator(ApplicationContext ctxt) {
        controllerAdviceBeans = FunctionalUtils.mapList(
                ControllerAdviceBean.findAnnotatedBeans(ctxt),
                ControllerAdviceBean::resolveBean);
    }

    /**
     * Создает и настраивает {@link StandaloneMockMvcBuilder}
     *
     * @param controllers - тестируемые контроллеры
     * @return настроенный {@link StandaloneMockMvcBuilder}
     */
    public StandaloneMockMvcBuilder setup(Object... controllers) {
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(controllerAdviceBeans.toArray());
    }
}
