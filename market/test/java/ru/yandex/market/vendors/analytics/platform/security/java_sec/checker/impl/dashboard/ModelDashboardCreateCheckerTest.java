package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * Тесты на проверку {@link ModelDashboardCreateChecker}
 *
 * @author sergeymironov.
 */
@DbUnitDataSet(before = "ModelDashboardCreateCheckerTest.before.csv")
@ClickhouseDbUnitDataSet(before = "ModelDashboardCreateCheckerTest.ch.before.csv")
class ModelDashboardCreateCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Есть возможность создавать дашборд")
    void accessCreateDashBoard() {
        var requestBody = checkerRequestBody(1001, 11111111);
        checkAccess("modelDashboardCreateChecker", requestBody, true);
    }

    @Test
    @DisplayName("Для категории этой модели недостаточный уровень доступа для создания дашборда")
    void noaccessCreateDashBoard() {
        var requestBody = checkerRequestBody(1001, 11223344);
        checkAccess("modelDashboardCreateChecker", requestBody, false);
    }

    @Test
    @DisplayName("К этому пользователю не прикреплен магазин")
    void noUserShop() {
        var requestBody = checkerRequestBody(1002, 11111111);
        checkAccess("modelDashboardCreateChecker", requestBody, false);
    }

    private void checkAccess(String checker, String requestBody, boolean expectedAccess) {
        var response = check(checker, requestBody);
        var expected = responseBody(expectedAccess);
        JsonTestUtil.assertEquals(expected, response);
    }

    private static String checkerRequestBody(long uid, long modelId) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"modelId\": %s\n"
                        + "}",
                uid,
                modelId
        );
    }
}