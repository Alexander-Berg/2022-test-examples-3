package ru.yandex.market.common.test.spring;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ResourceUtils;

/**
 * Инициализатор контекста, который в тесты подкладывает проперти из основного контекста.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@ParametersAreNonnullByDefault
public class PropertiesDirInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.setProperty("configs.path", getPropertiesRoot());
    }

    private String getPropertiesRoot() {
        // Получаем путь до главного файла с пропертями и берем его директорию
        try {
            return ResourceUtils.getFile("classpath:00_application.properties").toPath().getParent().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
