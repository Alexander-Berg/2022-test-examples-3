package ru.yandex.market.sc.core.test;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.api.service.RequestService;
import ru.yandex.market.sc.core.OrderScanLogSomeIdConstraintCheckListener;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.report.core.ReportTemplateRepository;
import ru.yandex.market.tpl.report.test.JasperTemplateTester;

import static org.mockito.Mockito.mock;

/**
 * @author valter
 */
@Configuration
@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.common.db.test",
        "ru.yandex.market.sc.core.util.flow.xdoc"
})
public class TestInternalConfiguration {

    @Bean
    @Scope("prototype")
    Clock clock() {
        return Clock.fixed(Instant.ofEpochMilli(0), DateTimeUtil.DEFAULT_ZONE_ID);
    }

    @Bean
    TestFactory testFactory() {
        return new TestFactory();
    }

    @Bean
    SortableTestFactory sortableTestFactory() {
        return new SortableTestFactory();
    }

    @Bean
    JasperTemplateTester jasperTemplateTester(ReportTemplateRepository templateRepository) {
        return new JasperTemplateTester(templateRepository);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @Scope("prototype")
    RequestService requestService() {
        return mock(RequestService.class);
    }

    @Bean
    @Scope("prototype")
    RestTemplate restTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean
    OrderScanLogSomeIdConstraintCheckListener eventListener() {
        return new OrderScanLogSomeIdConstraintCheckListener();
    }

}
