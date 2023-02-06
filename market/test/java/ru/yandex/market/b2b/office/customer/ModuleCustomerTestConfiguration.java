package ru.yandex.market.b2b.office.customer;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.b2b.office.b2bcustomers.support.test.ModuleB2bCustomersSupportTestConfiguration;
import ru.yandex.market.crm.lb.writer.LbWriter;
import ru.yandex.market.fps.module.balance.test.ModuleBalanceTestConfiguration;
import ru.yandex.market.jmf.blackbox.support.test.BlackBoxSupportTestConfiguration;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.mail.test.ModuleMailTestConfiguration;
import ru.yandex.market.jmf.timings.test.TimingTestConfiguration;

@Configuration
@Import({
        ModuleBalanceTestConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        BlackBoxSupportTestConfiguration.class,
        ModuleMailTestConfiguration.class,
        TimingTestConfiguration.class,
        ModuleCustomerConfiguration.class,
        ModuleB2bCustomersSupportTestConfiguration.class
})
public class ModuleCustomerTestConfiguration {
    @Bean("logbrokerB2bCustomersNotifyWriter")
    public LbWriter mockLogbrokerB2bCustomersNotifyWriter() {
        return Mockito.mock(LbWriter.class);
    }
}
