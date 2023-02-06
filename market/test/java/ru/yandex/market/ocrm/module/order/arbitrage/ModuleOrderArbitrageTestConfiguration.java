package ru.yandex.market.ocrm.module.order.arbitrage;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.lock.LockService;
import ru.yandex.market.jmf.module.chat.ModuleChatTestConfiguration;
import ru.yandex.market.jmf.security.AuthRunnerService;
import ru.yandex.market.jmf.trigger.test.TriggerTestConfiguration;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.jmf.utils.UrlCreationService;
import ru.yandex.market.ocrm.module.checkouter.RobotCheckouterService;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.ArbitrageController;

@Configuration
@Import({
        ModuleChatTestConfiguration.class,
        ModuleOrderArbitrageConfiguration.class,
        ModuleOrderTestConfiguration.class,
        MbiApiTestConfiguration.class,
        TriggerTestConfiguration.class
})
public class ModuleOrderArbitrageTestConfiguration extends AbstractModuleConfiguration {
    protected ModuleOrderArbitrageTestConfiguration() {
        super("module/order/arbitrage/test");
    }

    @Bean
    public ArbitrageController arbitrageController(
            EntityStorageService entityStorageService,
            OrderConsultationService consultationService,
            TxService txService,
            AuthRunnerService authRunnerService,
            LockService lockService,
            RobotCheckouterService checkouterService
    ) {
        return new ArbitrageController(entityStorageService, consultationService, txService, authRunnerService,
                lockService, checkouterService);
    }

    @Bean
    @Primary
    public UrlCreationService urlCreationService() {
        return Mockito.mock(UrlCreationService.class);
    }
}
