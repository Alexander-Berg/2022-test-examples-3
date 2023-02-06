package ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.security.AbstractCheckerTest;
import ru.yandex.market.vendors.analytics.platform.security.java_sec.checker.impl.AbstractLevelChecker;

/**
 * Тесты на проверку:
 * <ol>
 * <li> возможности использовать фильтры {@link AbstractLevelChecker}</li>
 * </ol>
 *
 * @author sergeymironov.
 */
@DbUnitDataSet(before = "FilterCheckerTest.before.csv")
class FilterCheckerTest extends AbstractCheckerTest {

    @Test
    @DisplayName("Есть возможность использовать геофильтр")
    void accessFilter() {
        var requestBody = filterCheckerRequestBody(1001, 91491);
        assertAccess("geoFilterChecker", requestBody, true);
    }

    @Test
    @DisplayName("У этого пользователя нет возможности фильтровать что-либо")
    void noAccessUserFilter() {
        var requestBody = filterCheckerRequestBody(1002, 91491);
        assertAccess("geoFilterChecker", requestBody, false);
        assertAccess("dateFilterChecker", requestBody, false);
    }

    @Test
    @DisplayName("Недостаточный уровень доступа использовать геофильтр в этой категории")
    void noAccessFilterCategory() {
        var requestBody = filterCheckerRequestBody(1001, 91492);
        assertAccess("geoFilterChecker", requestBody, false);
    }

    @Test
    @DisplayName("Есть возможность фильтровать соцдем у вендора")
    void vendorCanFilterSocdem() {
        var requestBody = filterCheckerRequestBody(1, 91491);
        assertAccess("socdemFilterChecker", requestBody, true);
    }

    @Test
    @DisplayName("Нет возможности фильтровать соцдем у вендора")
    void vendorCanNotFilterSocdem() {
        var requestBody = filterCheckerRequestBody(1, 91492);
        assertAccess("socdemFilterChecker", requestBody, false);
    }

    @Test
    @DisplayName("Нет возможности фильтровать соцдем у магазина")
    void shopCanNotFilterSocdem() {
        var requestBody = filterCheckerRequestBody(1001, 91491);
        assertAccess("socdemFilterChecker", requestBody, false);
    }

    @Test
    @DisplayName("Вендорский пользователь может создавать виджет")
    void vendorCanCreateWidget() {
        var requestBody = filterCheckerRequestBody(1, 91491);
        assertAccess("categoryWidgetCreationChecker", requestBody, true);
    }

    @Test
    @DisplayName("Вендорский пользователь не может создавать виджет")
    void vendorCantCreateWidget() {
        var requestBody = filterCheckerRequestBody(1, 91492);
        assertAccess("categoryWidgetCreationChecker", requestBody, false);
    }

    @Test
    @DisplayName("Магазинный пользователь может создавать виджет")
    void shopCanCreateWidget() {
        var requestBody = filterCheckerRequestBody(1001, 91491);
        assertAccess("categoryWidgetCreationChecker", requestBody, true);
    }


    @Test
    @DisplayName("Магазинный пользователь не может создавать виджет (первый уровень)")
    void shopCantCreateWidgetLowLevel() {
        var requestBody = filterCheckerRequestBody(1001, 91494);
        assertAccess("categoryWidgetCreationChecker", requestBody, false);
    }

    @Test
    @DisplayName("Магазинный пользователь не может создавать виджет (у магазина нет категории)")
    void shopCantCreateWidgetNoLevel() {
        var requestBody = filterCheckerRequestBody(1001, 91495);
        assertAccess("categoryWidgetCreationChecker", requestBody, false);
    }

    private static String filterCheckerRequestBody(long uid, long hid) {
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