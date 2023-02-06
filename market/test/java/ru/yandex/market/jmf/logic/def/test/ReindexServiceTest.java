package ru.yandex.market.jmf.logic.def.test;


import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.ReindexService;
import ru.yandex.market.jmf.db.test.CleanDb;
import ru.yandex.market.jmf.logic.def.impl.EntityLoggingServiceImpl;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
@CleanDb
public class ReindexServiceTest {

    @Inject
    BcpService bcpService;
    @Inject
    ReindexService reindexService;
    @Inject
    EntityLoggingServiceImpl entityLoggingService;
    @Inject
    private TxService txService;

    @AfterEach
    public void tearDown() {
        entityLoggingService.resetLoggerProvider();
    }

    @Test
    public void entityLogging() {
        createEntityWithEntityLogging();
        createEntityWithEntityLogging();

        doReindexTest(Fqn.of("entityWithEntityLogging"));
    }

    @Test
    public void entityLoggingAndStingId() {
        createEntityWithEntityLoggingAndStringId();
        createEntityWithEntityLoggingAndStringId();

        doReindexTest(Fqn.of("entityWithEntityLoggingAndStringId"));
    }

    void createEntityWithEntityLogging() {
        txService.runInTx(() -> bcpService.create(Fqn.of("entityWithEntityLogging"), Maps.of()));
    }

    void createEntityWithEntityLoggingAndStringId() {
        txService.runInTx(() -> bcpService.create(Fqn.of("entityWithEntityLoggingAndStringId"),
                Maps.of("id", Randoms.string())));
    }

    private void doReindexTest(Fqn fqn) {
        AtomicInteger counter = new AtomicInteger(0);
        entityLoggingService.setLoggerProvider((Fqn f) ->
                msg -> counter.incrementAndGet());

        // Запускаем реиндексацию
        reindexService.reindex(fqn, "entityLogging");
        reindexService.doReindex();

        // должны проиндексировать две записи созданные в начале теста
        Assertions.assertEquals(2, counter.get());
    }
}
