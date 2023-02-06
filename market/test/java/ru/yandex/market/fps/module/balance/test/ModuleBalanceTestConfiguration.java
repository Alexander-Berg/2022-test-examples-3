package ru.yandex.market.fps.module.balance.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fps.balance.BalanceService;
import ru.yandex.market.fps.balance.ContractService;
import ru.yandex.market.fps.balance.ModuleBalanceConfiguration;
import ru.yandex.market.jmf.tvm.support.test.TvmSupportTestConfiguration;

@Configuration
@Import({
        ModuleBalanceConfiguration.class,
        TvmSupportTestConfiguration.class,
})
public class ModuleBalanceTestConfiguration {
    @Bean
    public BalanceService mockBalanceService() {
        return Mockito.mock(BalanceService.class);
    }

    @Bean
    public ContractService mockBalanceContractService() {
        return Mockito.mock(ContractService.class);
    }
}
