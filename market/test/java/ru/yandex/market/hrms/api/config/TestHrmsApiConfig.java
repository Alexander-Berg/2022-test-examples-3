package ru.yandex.market.hrms.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import ru.yandex.market.hrms.core.config.HrmsCoreTestConfig;

@EnableSwagger2
@Import({
        HrmsInternalConfig.class,
        HrmsCoreTestConfig.class,
        HrmsSecurityConfigTest.class,
        HrmsRoleConfig.class})
@Configuration
public class TestHrmsApiConfig {
}
