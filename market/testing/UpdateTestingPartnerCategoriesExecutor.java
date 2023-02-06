package ru.yandex.market.vendors.analytics.tms.jobs.testing;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.vendors.analytics.core.service.category.CategoryService;
import ru.yandex.market.vendors.analytics.core.service.partner.PartnerCategoryService;

/**
 * Добавляет тестовому партнёру все существующие категории как купленные.
 *
 * @author ogonek.
 */
@Slf4j
@AllArgsConstructor
public class UpdateTestingPartnerCategoriesExecutor implements Executor {

    private static final long TEST_PARTNER_ID = 2;

    private final CategoryService categoryService;
    private final PartnerCategoryService partnerCategoryService;

    @Override
    public void doJob(JobExecutionContext context) {
        var allHids = categoryService.allCategories().keySet();
        log.info("Trying to set {} categories to test partner", allHids.size());
        partnerCategoryService.updateVendorCategories(TEST_PARTNER_ID, allHids);
        log.info("Test partner categories have been updated");
    }

}
