package ru.yandex.market.jmf.module.def.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.logic.def.operations.CheckSingletonOperationHandler;
import ru.yandex.market.jmf.module.def.Root;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(classes = SingletonTest.Configuration.class)
public class SingletonTest {

    @Inject
    BcpService bcpService;

    /**
     * Проверяем, что не создаются объекты singleton-ы
     */
    @Test
    public void createRoot() {
        Assertions.assertThrows(CheckSingletonOperationHandler.SingletonException.class,
                () -> bcpService.create(Root.FQN, Maps.of("title", Randoms.string())));
    }

    @Import(ModuleDefaultTestConfiguration.class)
    public static class Configuration {
    }
}
