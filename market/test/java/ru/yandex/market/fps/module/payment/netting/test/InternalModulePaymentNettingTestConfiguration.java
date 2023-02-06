package ru.yandex.market.fps.module.payment.netting.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.dataimport.conf.datasource.Yt2DataSourceConf;
import ru.yandex.market.jmf.dataimport.datasource.AbstractDataSourceStrategy;
import ru.yandex.market.jmf.dataimport.test.MockableDataSourceStrategy;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(ModulePaymentNettingTestConfiguration.class)
public class InternalModulePaymentNettingTestConfiguration extends AbstractModuleConfiguration {
    protected InternalModulePaymentNettingTestConfiguration() {
        super("module/payment/netting/test");
    }

    @Bean
    @Primary
    public AbstractDataSourceStrategy<Yt2DataSourceConf> mockYt2DataSourceStrategy() {
        return Mockito.mock(MockableDataSourceStrategy.class, invocation -> {
            if ("type".equals(invocation.getMethod().getName())) {
                return Yt2DataSourceConf.class;
            }

            return null;
        });
    }

}
