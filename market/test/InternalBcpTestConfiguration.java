package ru.yandex.market.jmf.bcp.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.bcp.ComputeIfAbsentExtensionStrategy;
import ru.yandex.market.jmf.bcp.test.internal.ComputeIfAbsentTest;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Configuration
@Import(BcpTestConfiguration.class)
@ComponentScan("ru.yandex.market.jmf.bcp.test.internal")
public class InternalBcpTestConfiguration extends AbstractModuleConfiguration {
    protected InternalBcpTestConfiguration() {
        super("bcp/test");
    }

    @Bean("computeIfAbsentTestStrategy1")
    public ComputeIfAbsentExtensionStrategy<ComputeIfAbsentTest> computeIfAbsentTest1() {
        return Mockito.mock(ComputeIfAbsentExtensionStrategy.class);
    }

    @Bean("computeIfAbsentTestStrategy2")
    public ComputeIfAbsentExtensionStrategy<ComputeIfAbsentTest> computeIfAbsentTest2() {
        return Mockito.mock(ComputeIfAbsentExtensionStrategy.class);
    }

    @Bean
    @Primary
    public TxService spyTxService(TxService txService) {
        return Mockito.spy(txService);
    }
}
