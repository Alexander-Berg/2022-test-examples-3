package ru.yandex.market.cocon;

import java.io.File;
import java.io.FileNotFoundException;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ResourceUtils;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class PropertiesDirInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.setProperty("configs.path", getPropertiesRoot());
    }

    private static String getPropertiesRoot() {
        try {
            // Получаем путь до главного файла с пропертями
            final File propertiesFile = ResourceUtils.getFile("classpath:00_application.properties");
            // И берем его директорию
            return propertiesFile.toPath().getParent().toString();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
