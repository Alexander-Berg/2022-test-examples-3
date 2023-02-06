package ru.yandex.market.crm;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.tvm.support.test.TvmSupportTestConfiguration;

@Configuration
@Import({
        SpringInfrastructureConfiguration.class,
        TvmSupportTestConfiguration.class,
})
public class SpringInfrastructureTestConfiguration {
}
