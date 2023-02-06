package ru.yandex.market.sc.internal.test;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.util.SqsEventFactory;

/**
 * @author: dbryndin
 * @date: 5/27/22
 */
@EmbeddedDbIntTest
public abstract class AbstractBaseIntTest {

    @Autowired
    protected TestFactory testFactory;
    @Autowired
    protected SqsEventFactory sqsEventFactory;
    @Autowired
    protected Clock clock;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

}
