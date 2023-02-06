package ru.yandex.market.deliverycalculator.workflow.test;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;

@DbUnitDataSet
@ExtendWith(SpringExtension.class)
@ActiveProfiles({"functionalTest", "development"})
@TestExecutionListeners(TransactionalTestExecutionListener.class)
@ContextConfiguration(classes = FunctionalTestConfiguration.class)
@ParametersAreNonnullByDefault
public abstract class FunctionalTest extends JupiterDbUnitTest {

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUpConfigurations() {
        CacheTestUtils.cleanDictionaryCache(applicationContext);
        CacheTestUtils.cleanTariffCaches(applicationContext);
    }

}
