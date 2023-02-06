package ru.yandex.market.mbo.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.ir.yt.util.tables.YtClientWrapper;
import ru.yandex.market.mbo.audit.conf.MboAuditTableConfig;
import ru.yandex.market.mbo.configs.init.UnstableInitExecutorConfig;
import ru.yandex.market.mbo.configs.yt.YtHttpConfig;
import ru.yandex.market.mbo.yt.utils.UnstableInit;

/**
 * @author apluhin
 * @created 9/22/21
 */
@Configuration
@Primary
@Import({
    UnstableInitExecutorConfig.class,
    YtHttpConfig.class
})
public class TestAuditTableConfig extends MboAuditTableConfig {

    public TestAuditTableConfig(UnstableInitExecutorConfig unstableInitExecutorConfig, YtHttpConfig ytHttpConfig) {
        super(unstableInitExecutorConfig, ytHttpConfig);
    }

    @Bean
    public UnstableInit<YtClientWrapper> ytClientWrapperHahn() {
        return getYtClientWrapperUnstableInit("hahn.yt.yandex.net");
    }
}
