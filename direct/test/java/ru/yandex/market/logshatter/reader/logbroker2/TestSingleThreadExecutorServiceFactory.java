package ru.yandex.market.logshatter.reader.logbroker2;

import ru.yandex.market.logshatter.reader.logbroker2.threads.SingleThreadExecutorServiceFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.01.2019
 */
public class TestSingleThreadExecutorServiceFactory implements SingleThreadExecutorServiceFactory {
    private final Supplier<TestScheduledExecutorService> scheduledExecutorServiceSupplier;
    private final List<TestScheduledExecutorService> createdScheduledExecutorServices = new ArrayList<>();

    public TestSingleThreadExecutorServiceFactory() {
        this.scheduledExecutorServiceSupplier = TestScheduledExecutorService::new;
    }

    public TestSingleThreadExecutorServiceFactory(List<TestScheduledExecutorService> scheduledExecutorServices) {
        this.scheduledExecutorServiceSupplier = scheduledExecutorServices.iterator()::next;
    }

    @Override
    public ScheduledExecutorService create(String nameFormat) {
        TestScheduledExecutorService result = scheduledExecutorServiceSupplier.get();
        createdScheduledExecutorServices.add(result);
        return result;
    }

    public void runScheduledTasks() {
        // Копируем потому что таски могут добавлять в список другие таски
        new ArrayList<>(createdScheduledExecutorServices).stream()
            .filter(scheduledExecutorService -> !scheduledExecutorService.isShutdown())
            .forEach(TestScheduledExecutorService::runScheduledTasks);
    }
}
