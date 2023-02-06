package ru.yandex.market.hrms.tms.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.hrms.core.config.HrmsCoreTestConfig;
import ru.yandex.market.ispring.ISpringClient;
import ru.yandex.market.ispring.ISpringClientMock;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.wms.WmsClient;
import ru.yandex.market.wms.WmsClientMock;

@Import({
        HrmsTmsInternalConfig.class,
        HrmsCoreTestConfig.class,
        DbQueueTestUtil.class
})
@Configuration
public class HrmsTmsTestConfig {
    @Bean
    public SchedulerFactoryBeanDependsOnBeanFactoryPostProcessor dependsOnBeanFactoryPostProcessor() {
        return new SchedulerFactoryBeanDependsOnBeanFactoryPostProcessor("springLiquibase");
    }

    @Bean
    @Primary
    public ISpringClient getIspringClientMock() {
        return new ISpringClientMock();
    }

    @Bean
    @Primary
    public WmsClient getWmsClientMock() {
        return new WmsClientMock();
    }

    @Bean
    public RestTemplate hrmsRestTemplate() {
        return new RestTemplateBuilder()
                .build();
    }
}
