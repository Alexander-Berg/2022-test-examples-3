package ru.yandex.market.mbo.taskqueue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;


@Configuration
public class TaskQueueTestConfig {

    @Value("${taskqueue.tables.schema}")
    private String taskqueueTablesSchema;

    @Bean
    public TaskQueueRepository taskQueueRepository(NamedParameterJdbcTemplate jdbcTemplate,
                                                   TransactionTemplate transactionTemplate) {
        return new TaskQueueRepository(jdbcTemplate, transactionTemplate, taskqueueTablesSchema);
    }
}
