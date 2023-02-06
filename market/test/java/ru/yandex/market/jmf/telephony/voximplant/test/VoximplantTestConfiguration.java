package ru.yandex.market.jmf.telephony.voximplant.test;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.jmf.logic.def.test.LogicDefaultTestConfiguration;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.jmf.telephony.voximplant.VoximplantBaseConfiguration;
import ru.yandex.market.jmf.telephony.voximplant.secret.VoximplantCredentialsSecretSupplier;
import ru.yandex.market.jmf.telephony.voximplant.secret.VoximplantErrorsSecret;
import ru.yandex.market.jmf.trigger.test.TriggerTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import({
        VoximplantBaseConfiguration.class,
        LogicDefaultTestConfiguration.class,
        ModuleTicketTestConfiguration.class,
        TriggerTestConfiguration.class
})
@ComponentScan("ru.yandex.market.jmf.telephony.voximplant.test.utils")
@PropertySource(name = "testVoximplantProperties", value = "classpath:vox_test.properties")
public class VoximplantTestConfiguration extends AbstractModuleConfiguration {
    protected VoximplantTestConfiguration() {
        super("module/voximplant/test");
    }

    @Bean
    public VoximplantCredentialsSecretSupplier testVoximplantServiceAccountCredentialsSecretSupplier(
            ResourceLoader resourceLoader
    ) throws IOException {
        var mock = Mockito.mock(VoximplantCredentialsSecretSupplier.class);
        Mockito.when(mock.get()).thenReturn(
                IOUtils.readInputStream(resourceLoader.getResource("classpath:vox.auth.json").getInputStream())
        );
        return mock;
    }

    @Bean
    public VoximplantErrorsSecret testVoximplantErrorsSecret() {
        var mock = Mockito.mock(VoximplantErrorsSecret.class);
        Mockito.when(mock.get()).thenReturn("123");
        return mock;
    }

    @Bean
    public EnvironmentResolver environmentResolver() {
        return new EnvironmentResolver() {
            @Nonnull
            @Override
            public Environment get() {
                return Environment.INTEGRATION_TEST;
            }
        };
    }
}
