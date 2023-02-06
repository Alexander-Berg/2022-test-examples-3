package ru.yandex.market.mcrm.http.internal;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mcrm.queue.retry.CreateTaskRequest;
import ru.yandex.market.mcrm.queue.retry.RetryTaskConfiguration;
import ru.yandex.market.mcrm.queue.retry.RetryTaskService;

@Configuration
@ComponentScan("ru.yandex.market.mcrm.http.internal")
public class HttpClientFactoryTestConfiguration {

    @Bean
    public RetryTaskService retryTaskService() {
        return new RetryTaskService() {
            @Override
            public void addTask(@Nonnull RetryTaskConfiguration configuration, @Nonnull Object context) {
                // do nothing
            }

            @Override
            public void addTask(@Nonnull RetryTaskConfiguration configuration, @Nonnull String code,
                                @Nonnull Object context) {
                // do nothing
            }

            @Override
            public <T> void addTask(CreateTaskRequest<T> createTaskRequest) {
                // do nothing
            }

            @Override
            public <T> void addTaskBatch(@Nonnull RetryTaskConfiguration configuration, @Nonnull String code,
                                         @Nonnull T context) {
                // do nothing
            }

            @Override
            public void deleteTask(@Nonnull String taskCode) {
                // do nothing
            }

            @Override
            public void deleteTaskBatch(@Nonnull String code) {
                // do nothing
            }
        };
    }

}
