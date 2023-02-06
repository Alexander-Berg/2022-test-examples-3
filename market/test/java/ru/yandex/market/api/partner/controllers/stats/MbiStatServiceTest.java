package ru.yandex.market.api.partner.controllers.stats;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Тесты для {@link MbiStatService}.
 *
 * @author vbudnev
 */
public class MbiStatServiceTest extends FunctionalTest {

    @Autowired
    private MbiStatService mbiStatService;

    /**
     * Группа пустая.
     */
    @Test
    @DbUnitDataSet
    public void test_getMobilePpFilter_when_hasPp_should_returnValidFilter() {
        assertThat(mbiStatService.getMobilePpFilterCondition(), equalTo(" 1=1 "));
    }

    /**
     * Есть данные для целевых групп.
     */
    @Test
    @DbUnitDataSet(before = "db/PlatformsPpData.before.csv")
    public void test_getMobilePpFilter_when_noPp_should_returnTrueCondition() {
        assertThat(mbiStatService.getMobilePpFilterCondition(), equalTo(" pp in (612,613,614) "));
    }
}
