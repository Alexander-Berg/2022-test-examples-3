package ru.yandex.direct.dbutil.testing;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;

import ru.yandex.direct.mysql.MySQLInstance;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;
import ru.yandex.direct.utils.Interrupts;

import static com.google.common.base.Preconditions.checkState;

/**
 * Запускает БД для тестов и закрывает ее по окончании всех тестов.
 * <p>
 * Запуск происходит в конструкторе, поэтому данные для подключения
 * к базе доступны сразу после создания бина.
 * <p>
 * Для запуска тестов на фиксированной БД может быть отключен (enabled = false),
 * в этом случае база не запустится, а при попытке получить ее конфигурацию
 * (вызвать метод {@link #getConnector} будет сгенерировано исключение.
 */
public class TestDbInitializer implements Lifecycle {

    private static final Duration DEFAULT_DURATION = Duration.of(60, ChronoUnit.SECONDS);
    private static final Logger logger = LoggerFactory.getLogger(TestDbInitializer.class);

    private final boolean enabled;

    private boolean running;
    private MySQLInstance connector;

    public TestDbInitializer(boolean enabled) {
        this.enabled = enabled;
        startInternal();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MySQLInstance getConnector() {
        checkState(enabled, "test database initializer is disabled");
        return connector;
    }

    @Override
    public void start() {
        startInternal();
    }

    @Override
    public void stop() {
        try {
            connector.close();
        } catch (Exception e) {
            logger.warn("Cannot stop mysql " + connector.toString(), e);
        }
        finally {
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void startInternal() {
        if (!enabled || running) {
            return;
        }
        DirectMysqlDb directMysqlDb = new DirectMysqlDb(TestMysqlConfig.directConfig());
        connector = Interrupts.failingGet(directMysqlDb::start);
        connector.awaitConnectivity(DEFAULT_DURATION);
        running = true;
    }
}
