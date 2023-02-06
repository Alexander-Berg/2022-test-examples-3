package ru.yandex.market.jmf.logic.def.test;


import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.ReindexService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.logic.def.impl.EntityLoggingServiceImpl;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
@ActiveProfiles("singleTx")
public class ReindexServiceTest {

    @Inject
    BcpService bcpService;
    @Inject
    ReindexService reindexService;
    @Inject
    EntityLoggingServiceImpl entityLoggingService;

    @AfterEach
    public void tearDown() {
        entityLoggingService.resetLoggerProvider();
    }

    @Test
    public void entityLogging() {
        createEntityWithEntityLogging();
        createEntityWithEntityLogging();

        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        doReindexTest(Fqn.of("entityWithEntityLogging"));
    }

    @Test
    public void entityLoggingAndStingId() {
        createEntityWithEntityLoggingAndStringId();
        createEntityWithEntityLoggingAndStringId();

        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        doReindexTest(Fqn.of("entityWithEntityLoggingAndStringId"));
    }

    Entity createEntityWithEntityLogging() {
        return bcpService.create(Fqn.of("entityWithEntityLogging"), Maps.of());
    }

    Entity createEntityWithEntityLoggingAndStringId() {
        return bcpService.create(Fqn.of("entityWithEntityLoggingAndStringId"),
                Maps.of("id", Randoms.string()));
    }

    private void doReindexTest(Fqn fqn) {
        AtomicInteger counter = new AtomicInteger(0);
        entityLoggingService.setLoggerProvider((Fqn f) ->
                msg -> counter.incrementAndGet());

        // Запускаем реиндексацию
        reindexService.reindex(fqn, "entityLogging");
        reindexService.doReindex();

        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        // должны проиндексировать две записи созданные в начале теста
        Assertions.assertEquals(2, counter.get());
    }
}
