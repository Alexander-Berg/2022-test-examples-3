package ru.yandex.market.marketpromo.core.test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.marketpromo.core.config.ApplicationSchedulingConfig;
import ru.yandex.market.marketpromo.core.scheduling.SchedulingTaskCompleteEvent;
import ru.yandex.market.marketpromo.core.test.config.ApplicationCoreTestConfig;
import ru.yandex.market.marketpromo.core.test.context.initializer.YdbContainerContextInitializer;
import ru.yandex.market.marketpromo.core.utils.RequestContextUtils;
import ru.yandex.market.ydb.integration.DataCleaner;

@ContextConfiguration(
        classes = {
                ApplicationCoreTestConfig.class,
                ApplicationSchedulingConfig.class
        },
        initializers = YdbContainerContextInitializer.class
)
@ExtendWith(SpringExtension.class)
public abstract class ServiceTaskTestBase {

    @Autowired
    private DataCleaner dataCleaner;
    @Autowired
    protected SchedulingTaskObserver taskObserver;

    public ServiceTaskTestBase() {
        RequestContextUtils.setupContext(Map.of());
    }

    @AfterEach
    private void cleanData() {
        dataCleaner.cleanData();
    }

    @Component
    public static class SchedulingTaskObserver {

        private final Multimap<String, Consumer<SchedulingTaskCompleteEvent>> listeners = HashMultimap.create();

        @EventListener
        public void onComplete(SchedulingTaskCompleteEvent event) {
            listeners.get(event.getTaskName()).forEach(c -> c.accept(event));
        }

        @Nonnull
        public CompletableFuture<SchedulingTaskCompleteEvent> once(@Nonnull String taskName) {
            CompletableFuture<SchedulingTaskCompleteEvent> future = new CompletableFuture<>();
            once(taskName, future::complete);
            return future;
        }

        public void once(
                @Nonnull String taskName,
                @Nonnull Consumer<SchedulingTaskCompleteEvent> eventConsumer
        ) {
            listeners.put(taskName, new SchedulingTaskObserver.SelfClearingListener(listeners,
                    eventConsumer));
        }

        static class SelfClearingListener implements Consumer<SchedulingTaskCompleteEvent> {

            private final Multimap<String, Consumer<SchedulingTaskCompleteEvent>> listeners;
            private final Consumer<SchedulingTaskCompleteEvent> listener;

            SelfClearingListener(Multimap<String, Consumer<SchedulingTaskCompleteEvent>> listeners,
                                 Consumer<SchedulingTaskCompleteEvent> listener) {
                this.listeners = listeners;
                this.listener = listener;
            }

            @Override
            public void accept(SchedulingTaskCompleteEvent event) {
                listeners.remove(event.getTaskName(), this);
                listener.accept(event);
            }
        }
    }
}
