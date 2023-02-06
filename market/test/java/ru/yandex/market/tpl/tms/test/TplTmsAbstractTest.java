package ru.yandex.market.tpl.tms.test;

import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.tms.config.TestTplTmsConfiguration;
@TestPropertySource(properties = {"tpl.dbQueue.runQueueLoop=false"})
@Import(TestTplTmsConfiguration.class)
public abstract class TplTmsAbstractTest extends TplAbstractTest {

    @Autowired
    private JmsTemplate jmsTemplate;

    @AfterEach
    void tearDown() {
        Mockito.reset(jmsTemplate);
    }
}
