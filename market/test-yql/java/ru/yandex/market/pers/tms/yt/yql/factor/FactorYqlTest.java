package ru.yandex.market.pers.tms.yt.yql.factor;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.tms.yt.yql.AbstractPersYqlTest;

public class FactorYqlTest extends AbstractPersYqlTest {

    @Test
    public void runShopTest() {
        runTest(
            loadScript("/yql/grade/saas_summary_raw.sql").requestProperty("shop_grade_summary_test"),
            "/factor/saas_summary_raw_shop.json",
            "/factor/saas_summary_raw_shop.mock");
    }

    @Test
    public void runModelTest() {
        runTest(
            loadScript("/yql/grade/saas_summary_raw.sql").requestProperty("model_grade_summary_test"),
            "/factor/saas_summary_raw_model.json",
            "/factor/saas_summary_raw_model.mock");
    }
}
