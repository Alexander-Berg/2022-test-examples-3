package ru.yandex.market.common.sentryerrorbooster;

import java.io.IOException;
import java.nio.file.Paths;

import javax.annotation.Nonnull;

import org.junit.rules.TemporaryFolder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * Подсовываем динамически сгенерированную tmp директорию и файл с именем test.out
 * в качестве дестинейшена для записи
 */
public class PropertyOverrideContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String FILEPATH = "errorbooster.outputfilepath=";
    static final String FILENAME = "test.out";


    @Override
    public void initialize(@Nonnull ConfigurableApplicationContext configurableApplicationContext) {
        TemporaryFolder tmpFolder = new TemporaryFolder();
        try {
            tmpFolder.create();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String outputFilePath = Paths.get(tmpFolder.getRoot().getAbsolutePath(), FILENAME).toString();

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext, FILEPATH + outputFilePath);
    }

}
