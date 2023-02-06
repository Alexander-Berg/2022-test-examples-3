package ru.yandex.market.core.environment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveParamServiceTest extends FunctionalTest {
    @Autowired
    @Qualifier("importFeedCategoriesActiveParamService")
    private ActiveParamService activeParamService;

    @Test
    @DbUnitDataSet(
            before = "ActiveParamServiceTest.before.csv",
            after = "ActiveParamServiceTest.after.csv"
    )
    void testSwitch() {
        var activeParamValue = activeParamService.getActiveParamValue();
        var idleParamValue = activeParamService.getIdleParamValue();
        assertThat(activeParamValue).isEqualTo("shops_web.feed_categories_2");
        assertThat(idleParamValue).isEqualTo("shops_web.feed_categories_1");
        activeParamService.setActiveParamValueTo(idleParamValue);
    }
}
