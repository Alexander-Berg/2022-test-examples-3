package ru.yandex.market.vendors.analytics.platform.controller.widget.access;

import java.util.Collection;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static ru.yandex.market.vendors.analytics.core.utils.json.JsonUnitUtils.assertJsonEqualsIgnoreArrayOrder;

/**
 * @author antipov93.
 */
@DbUnitDataSet(before = "WidgetAccessControllerTest.before.csv")
public class WidgetAccessControllerTest extends FunctionalTest {

    @Test
    @DisplayName("У пользователя есть доступ к виджету через партнёра-магазина")
    void hasAccessViaShop() {
        var actual = checkWidgetAccess(5001, 1003);
        var expected = loadFromFile("hasAccessViaShop.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    @Test
    @DisplayName("У пользователя есть доступ к виджету через партнёра-вендора")
    void hasAccessViaVendor() {
        var actual = checkWidgetAccess(5001, 1005);
        var expected = loadFromFile("hasAccessViaVendor.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    @Test
    @DisplayName("У магазинного пользователя нет доступа к виджету, запрещенному для показа магазинам")
    void hasNoAccessShopRestricted() {
        var actual = checkWidgetAccess(5000, 1005);
        var expected = loadFromFile("hasNoAccessShopRestricted.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    @Test
    @DisplayName("У магазинного пользователя нет доступа к виджету из-за низкого уровня категории")
    void hasNoAccessLowAccess() {
        var actual = checkWidgetAccess(5000, 1004);
        var expected = loadFromFile("hasNoAccessLowLevel.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    @Test
    @DisplayName("У пользователя есть доступ к виджету через оба типа партнёров")
    void hasAccessViaBothPartners() {
        var actual = checkWidgetAccess(5001, 1002);
        var expected = loadFromFile("hasAccessViaBothPartner.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    @Test
    @DisplayName("У пользователя есть доступ к виджету-сравнению")
    void hasAccessToCompareWidget() {
        var actual = checkWidgetAccess(5001, 2000);
        var expected = loadFromFile("hasAccessToCompareWidget.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    @Test
    @DisplayName("У пользователя нет доступа к виджету-сравнению")
    void hasNoAccessToCompareWidget() {
        var actual = checkWidgetAccess(5001, 2001);
        var expected = loadFromFile("hasNoAccessToCompareWidget.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    @Test
    @DisplayName("Проверка батчевой лапки")
    void batchCheck() {
        var actual = checkWidgetsAccess(5001, List.of(1001L, 1002L, 1003L, 1004L, 1005L));
        var expected = loadFromFile("userWidgetAccessBatchCheck.json");
        assertJsonEqualsIgnoreArrayOrder(expected, actual);
    }

    private String checkWidgetAccess(long uid, long widgetId) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/user/{userId}/widget/{widgetId}/check")
                .buildAndExpand(uid, widgetId)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }

    private String checkWidgetsAccess(long uid, Collection<Long> widgetIds) {
        var url = UriComponentsBuilder.fromUriString(baseUrl())
                .path("/user/{userId}/widgets/check")
                .queryParam("widgetIds", StreamEx.of(widgetIds).map(String::valueOf).joining(","))
                .buildAndExpand(uid)
                .toUriString();
        return FunctionalTestHelper.get(url).getBody();
    }
}
