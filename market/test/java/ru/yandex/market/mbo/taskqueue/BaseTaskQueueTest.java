package ru.yandex.market.mbo.taskqueue;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.mbo.taskqueue.config.DbConfig;
import ru.yandex.market.mbo.taskqueue.config.PgInitializer;
import ru.yandex.market.mbo.taskqueue.config.TaskQueueTestConfig;


@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = PgInitializer.class,
    classes = {DbConfig.class, TaskQueueTestConfig.class}
)
public abstract class BaseTaskQueueTest {
}
