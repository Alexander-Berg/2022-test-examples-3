package ru.yandex.market.mboc.tms.service.acceptance.rule;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleStagingRepository;
import ru.yandex.market.mboc.common.offers.acceptance.rule.StagedCategoryRule;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

public class CategoryRuleDownloaderTest extends BaseDbTestClass {
    private static final String FILTER_URL = "filterUrlTest";

    @Autowired
    private CategoryRuleStagingRepository repository;

    private CategoryRuleDownloader downloader;

    @Before
    public void setUp() throws IOException {
        var testFileData = IOUtils.resourceToByteArray(
            "/category-vendor-rule/current_business-category-filter.pbuf"
        );
        downloader = new CategoryRuleDownloader(FILTER_URL, TransactionHelper.MOCK, repository) {
            @Override
            protected byte[] getCurrentFilterData() {
                return testFileData;
            }
        };
    }

    @Test
    public void testUpdater() {
        downloader.download(1);

        Assertions.assertThat(repository.findAll()).containsExactlyInAnyOrder(
            new StagedCategoryRule(123123, 123123, 0, false),
            new StagedCategoryRule(357345, 566575756, 74657465, false),
            new StagedCategoryRule(6575756, 566575756, 74657465, false),
            new StagedCategoryRule(565675565, 566575756, 74657465, false),
            new StagedCategoryRule(575765, 566575756, 74657465, false)
        );
    }
}
