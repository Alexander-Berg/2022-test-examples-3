package ru.yandex.market.mcrm.db.test;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ru.yandex.embedded.postgresql.PostgresRunner;

@Component
public class TestDbCleaner implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(TestDbCleaner.class);
    private static final AtomicBoolean templateCreated = new AtomicBoolean(false);

    private static TestDbCleaner instance;

    private final PostgresRunner postgresRunner;

    public static void reinitializeDb() {
        instance.restoreTemplate();
    }

    public TestDbCleaner(PostgresRunner postgresRunner) {
        this.postgresRunner = postgresRunner;
    }

    @EventListener({ContextStartedEvent.class, ContextRefreshedEvent.class})
    public void handleContextStartedEvent() {
        if (templateCreated.compareAndSet(false, true)) {
            try {
                postgresRunner.saveTemplate();
            } catch (Exception e) {
                LOG.error("Unable to save template from current db", e);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    private void restoreTemplate() {
        postgresRunner.reinitializeDb();
    }
}
