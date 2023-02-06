package ru.yandex.market.vendors.analytics.core;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.TestMetadataBuilderFactory;

/**
 * @author antipov93.
 */
@ActiveProfiles("functionalTest")
@ExtendWith(SpringExtension.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class,
        MockitoTestExecutionListener.class,
        ru.yandex.market.common.test.mockito.MockitoTestExecutionListener.class
})
@DbUnitDataSet(
        nonTruncatedTables = {
                "analytics.widget_group",
                "analytics.color",
                "analytics.lock",
                "analytics.region_population",
                "analytics.widget_default_name"
        },
        nonRestartedSequences = "analytics.s_partner_shopId")
public abstract class EmptyTest {

    @BeforeEach
    void resetAllocatedSequenceValues() {
        TestMetadataBuilderFactory.reset();
    }


    @Nonnull
    protected String loadFromFile(@Nonnull String file) {
        return StringTestUtil.getString(getClass(), file);
    }
}
