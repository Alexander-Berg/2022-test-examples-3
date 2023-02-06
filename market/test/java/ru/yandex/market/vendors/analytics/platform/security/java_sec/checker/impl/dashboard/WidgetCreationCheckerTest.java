package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;

/**
 * Тесты на проверку чекера про создание виджетов
 *
 * @author ogonek.
 */
@DbUnitDataSet(before = "WidgetCreationCheckerTest.before.csv")
public class WidgetCreationCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Вендорский пользователь может создавать виджет")
    void vendorCanCreateWidget() {
        var requestBody = filterCheckerRequestBody(1, 11);
        assertAccess("widgetCreationChecker", requestBody, true);
    }

    @Test
    @DisplayName("Вендорский пользователь может создавать виджет с хидом")
    void vendorCanCreateWidgetWithHid() {
        var requestBody = filterCheckerRequestBodyWithHid(1, 91491);
        assertAccess("widgetCreationChecker", requestBody, true);
    }

    @Test
    @DisplayName("Вендорский пользователь не может создавать виджет")
    void vendorCantCreateWidget() {
        var requestBody = filterCheckerRequestBody(1, 12);
        assertAccess("widgetCreationChecker", requestBody, false);
    }

    @Test
    @DisplayName("Вендорский пользователь не может создавать виджет с хидом")
    void vendorCantCreateWidgetWithHid() {
        var requestBody = filterCheckerRequestBodyWithHid(1, 91492);
        assertAccess("widgetCreationChecker", requestBody, false);
    }

    @Test
    @DisplayName("Магазинный пользователь может создавать виджет")
    void shopCanCreateWidget() {
        var requestBody = filterCheckerRequestBody(1001, 11);
        assertAccess("widgetCreationChecker", requestBody, true);
    }


    @Test
    @DisplayName("Магазинный пользователь не может создавать виджет (первый уровень)")
    void shopCantCreateWidgetLowLevel() {
        var requestBody = filterCheckerRequestBody(1001, 14);
        assertAccess("widgetCreationChecker", requestBody, false);
    }

    @Test
    @DisplayName("Магазинный пользователь не может создавать виджет (у магазина нет категории)")
    void shopCantCreateWidgetNoLevel() {
        var requestBody = filterCheckerRequestBody(1001, 15);
        assertAccess("widgetCreationChecker", requestBody, false);
    }

    private static String filterCheckerRequestBody(long uid, long dashboardId) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"dashboardId\": %s\n"
                        + "}",
                uid,
                dashboardId
        );
    }

    private static String filterCheckerRequestBodyWithHid(long uid, long hid) {
        return String.format(""
                        + "{\n"
                        + "   \"uid\": %s,\n"
                        + "   \"hid\": %s\n"
                        + "}",
                uid,
                hid
        );
    }
}
