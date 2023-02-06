package ru.yandex.market.ocrm.module.order.arbitrage;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.lock.LockService;
import ru.yandex.market.jmf.module.chat.ModuleChatTestConfiguration;
import ru.yandex.market.jmf.security.AuthRunnerService;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;
import ru.yandex.market.jmf.utils.UrlCreationService;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;
import ru.yandex.market.ocrm.module.order.arbitrage.controller.ConsultationController;

@Configuration
@Import({
        ModuleChatTestConfiguration.class,
        ModuleOrderArbitrageConfiguration.class,
        ModuleOrderTestConfiguration.class,
        MbiApiTestConfiguration.class,
})
public class ModuleArbitrageTestConfiguration extends AbstractModuleConfiguration {

    protected ModuleArbitrageTestConfiguration() {
        super("module/order/arbitrage/test");
    }

    @Bean
    public ConsultationController arbitrageController(
            EntityStorageService entityStorageService,
            TxService txService,
            AuthRunnerService authRunnerService,
            LockService lockService,
            ConsultationService consultationService,
            ConfigurationService configurationService
    ) {
        return new ConsultationController(entityStorageService, txService, authRunnerService, lockService,
                consultationService, configurationService);
    }

    @Bean
    @Primary
    public UrlCreationService urlCreationService() {
        return Mockito.mock(UrlCreationService.class);
    }
}
