package ru.yandex.market.adv.test;

import java.time.ZoneId;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.adv.loader.file.FileLoader;
import ru.yandex.market.adv.test.configuration.TestBeanConfiguration;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.mockito.MockitoTestExecutionListener;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * Общий наследник для всех тестовых классов. Нужен, чтобы честно инициализировать контекст приложения в тестах.
 * Date: 23.11.2021
 * Project: arcadia-market_adv_adv_shop
 *
 * @author alexminakov
 */
@ExtendWith({
        SpringExtension.class,
        MockitoExtension.class,
        MockServerExtension.class
})
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class,
                DbUnitTestExecutionListener.class,
                MockitoTestExecutionListener.class
        },
        mergeMode = MERGE_WITH_DEFAULTS
)
@Import({
        TestBeanConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functionalTest")
public abstract class AbstractTest {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Europe/Moscow");

    @Autowired
    private FileLoader fileLoader;

    @BeforeAll
    static void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_ZONE_ID));
    }

    /**
     * Вычитываем файл, относящийся к ресурсам тестового класса, и преобразуем его в {@link String}
     *
     * @param fileName название файла
     * @return файл в виде строки
     */
    @Nonnull
    protected String loadFile(@Nonnull String fileName) {
        return fileLoader.loadFile(fileName, this.getClass());
    }

    /**
     * Вычитываем файл, относящийся к ресурсам тестового класса, и отдаем в бинарном виде
     *
     * @param fileName название файла
     * @return бинарный файл
     */
    @Nonnull
    protected byte[] loadFileBinary(@Nonnull String fileName) {
        return fileLoader.loadFileBinary(fileName, this.getClass());
    }
}
