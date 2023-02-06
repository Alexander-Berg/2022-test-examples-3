package ru.yandex.market.deliverycalculator.searchengine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.deliverycalculator.searchengine.service.ApplicationStatusService;
import ru.yandex.market.deliverycalculator.workflow.test.CacheTestUtils;

@DbUnitDataSet
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FunctionalTestConfiguration.class)
@WebAppConfiguration
public abstract class FunctionalTest extends JupiterDbUnitTest {

    static {
        ApplicationStatusService.onServiceStart();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void beforeEach() {
        CacheTestUtils.cleanDictionaryCache(applicationContext);
        CacheTestUtils.cleanFeedParserWorkflowService(applicationContext);
        CacheTestUtils.cleanTariffCaches(applicationContext);
        CacheTestUtils.cleanMetaDataCacheService(applicationContext);
        CacheTestUtils.cleanSolomonCache(applicationContext);
    }

}
