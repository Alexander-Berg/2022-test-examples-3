package ru.yandex.market.tpl.billing.service.moneyflowsync;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.entity.Environment;
import ru.yandex.market.tpl.billing.repository.EnvironmentRepository;
import ru.yandex.market.tpl.billing.service.yt.YtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

/**
 * Тесты для {@link MoneyFlowSyncService}
 */
public class MoneyFlowSyncServiceTest extends AbstractFunctionalTest {

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private MoneyFlowSyncService service;

    @Autowired
    private YtService ytService;

    @Test
    void simpleTest() {
        environmentRepository.save(new Environment("EnvironmentMoneyFlowSyncStrategy.maxValue", "10"));
        environmentRepository.save(new Environment("MoneyFlowSyncService.syncDataEnabled", "true"));

        service.syncData();

        Mockito.verify(ytService).export(any(), any(), any(), any(), any(), anyBoolean());
    }
}
