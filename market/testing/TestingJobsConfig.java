package ru.yandex.market.vendors.analytics.tms.jobs.testing;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.MonitoringConfig;
import ru.yandex.market.vendors.analytics.core.service.category.CategoryService;
import ru.yandex.market.vendors.analytics.core.service.partner.PartnerCategoryService;

/**
 * @author antipov93.
 */
@Configuration
@RequiredArgsConstructor
@Profile({"testing", "functionalTest"})
public class TestingJobsConfig {
    private static final int MAX_EXECUTION_TIME_MILLIS = 5 * 60 * 1000; // 5 минут
    private static final int MAX_DELAY_TIME_MILLIS = 2 * 24 * 60 * 60 * 1000; // 2 дня

    private final CategoryService categoryService;
    private final PartnerCategoryService partnerCategoryService;

    /**
     * Запускается каждый день в 2 часа ночи.
     */
    @Bean
    @CronTrigger(
            cronExpression = "0 0 2 * * ?",
            description = "Update testing partner categories"
    )
    @MonitoringConfig(
            failsToCritCount = 2,
            maxExecutionTimeMillis = MAX_EXECUTION_TIME_MILLIS,
            maxDelayTimeMillis = MAX_DELAY_TIME_MILLIS
    )
    public UpdateTestingPartnerCategoriesExecutor updateTestingPartnerCategoriesExecutor() {
        return new UpdateTestingPartnerCategoriesExecutor(categoryService, partnerCategoryService);
    }
}
