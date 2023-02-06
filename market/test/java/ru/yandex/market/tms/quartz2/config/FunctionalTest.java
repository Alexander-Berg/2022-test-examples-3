package ru.yandex.market.tms.quartz2.config;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;


@SpringJUnitConfig(classes = FunctionalTestConfig.class)
@TestExecutionListeners({
        DirtiesContextTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
public abstract class FunctionalTest {
}
