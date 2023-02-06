package ru.yandex.market.common.sentryerrorbooster;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import io.sentry.connection.Connection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.sentryerrorbooster.connection.ErrorBoosterFileConnection;
import ru.yandex.market.common.sentryerrorbooster.errorbooster.ErrorBoosterEvent;


@SpringJUnitConfig(
        initializers = PropertyOverrideContextInitializer.class,
        classes = SentryErrorBoosterTestConfig.class)

@ActiveProfiles("functionalTest")
public class SentryErrorBoosterTest {
    private static final Logger log = LogManager.getLogger(SentryErrorBoosterTest.class);
    private static final String TEST_MESSAGE = "This is test message.";

    @Value("${errorbooster.enable:false}")
    private boolean isEnabled;

    @Value("${errorbooster.outputfilepath}")
    private String outputFilePath;

    @Required
    @Value("${errorbooster.project}")
    private String project;

    @Value("${errorbooster.service:defaultService}")
    private String service;

    @Value("${errorbooster.platform:unsupported}")
    private String platform;

    @Test
    @DisplayName("Клиент sentry инициализирован, не нулевой")
    public void sentryClientInitializedProperly() {
        Assertions.assertNotNull(Sentry.getStoredClient());
    }

    @Test
    @DisplayName("Инициализирован connection типа ErrorBoosterFileConnection")
    public void sentryConnectionInitializedProperly() throws NoSuchFieldException, IllegalAccessException {
        Class obj = Sentry.getStoredClient().getClass();
        Field field = obj.getDeclaredField("connection");
        field.setAccessible(true);
        Connection value = (Connection) field.get(Sentry.getStoredClient());
        Assertions.assertTrue(value instanceof ErrorBoosterFileConnection);
    }

    @Test
    @DisplayName("Проверяем полную цепочку, от отправки до записи в файл")
    public void errorBoosterFullCircuit() throws IOException {
        Sentry.getStoredClient().sendException(new Throwable(TEST_MESSAGE));
        ObjectMapper mapper = new ObjectMapper();
        ErrorBoosterEvent errorBoosterEvent = mapper.readValue(Paths.get(outputFilePath).toFile(),
                ErrorBoosterEvent.class);
        Assertions.assertEquals(errorBoosterEvent.getMessage(), TEST_MESSAGE);
        Assertions.assertEquals(errorBoosterEvent.getProject(), project);
        Assertions.assertEquals(errorBoosterEvent.getPlatform().name(), platform);
        Assertions.assertEquals(errorBoosterEvent.getMethod(),
                "ru.yandex.market.common.sentryerrorbooster.SentryErrorBoosterTest.errorBoosterFullCircuit");
    }
}
