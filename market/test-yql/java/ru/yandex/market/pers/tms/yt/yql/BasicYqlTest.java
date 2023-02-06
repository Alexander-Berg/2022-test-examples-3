package ru.yandex.market.pers.tms.yt.yql;


import org.junit.jupiter.api.Test;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.08.2021
 */
public class BasicYqlTest extends AbstractPersYqlTest {

    @Test
    public void testVerifyGrade() {
        runTest(
            loadScript("/yql/verified_grade.sql"),
            "/basic/verify_expected.json",
            "/basic/verify.mock"
        );
    }

    @Test
    public void testCpaSupplierMarker() {
        runTest(
            loadScript("/yql/cpa_update_supplier.sql"),
            "/basic/cpa_update_supplier_expected.json",
            "/basic/cpa_update_supplier.mock"
        );
    }

    @Test
    public void testCpaShopMarker() {
        runTest(
            loadScript("/yql/cpa_update_shop.sql"),
            "/basic/cpa_update_shop_expected.json",
            "/basic/cpa_update_shop.mock"
        );
    }

    @Test
    public void testCpaModelMarker() {
        runTest(
            loadScript("/yql/cpa_update_model.sql"),
            "/basic/cpa_update_model_expected.json",
            "/basic/cpa_update_model.mock"
        );
    }

    @Test
    public void testUpdateOrderForModelGrades() {
        runTest(
            loadScript("/yql/tables/update_order_for_model_grades.sql"),
            "/basic/cpa_update_order_expected.json",
            "/basic/cpa_update_order.mock"
        );
    }

}
