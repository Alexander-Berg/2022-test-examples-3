package ru.yandex.market.tpl.core.dbqueue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.test.AbstractDbDictTest;

public class DbQueueDictTest extends AbstractDbDictTest {

    @Autowired
    public DbQueueDictTest(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, QueueType.class, "queue", "queue_name");
    }

}
